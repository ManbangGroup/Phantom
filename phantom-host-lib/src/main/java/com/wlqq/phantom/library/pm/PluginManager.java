/*
 * Copyright (C) 2017-2018 Manbang Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wlqq.phantom.library.pm;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.ArraySet;
import android.text.TextUtils;
import android.util.TimingLogger;

import com.wlqq.mavenversion.Version;
import com.wlqq.mavenversion.VersionVerifier;
import com.wlqq.phantom.communication.PhantomServiceManager;
import com.wlqq.phantom.communication.IService;
import com.wlqq.phantom.library.env.Constants;
import com.wlqq.phantom.library.log.LogReporter;
import com.wlqq.phantom.library.proxy.PluginClassLoader;
import com.wlqq.phantom.library.utils.DigestUtils;
import com.wlqq.phantom.library.utils.FileUtils;
import com.wlqq.phantom.library.utils.IoUtils;
import com.wlqq.phantom.library.utils.TimingUtils;
import com.wlqq.phantom.library.utils.VLog;
import com.wlqq.phantom.library.utils.VmUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 插件管理类
 */
@SuppressWarnings("PMD.ConfusingTernary")
public class PluginManager {
    // 插件 so 库目录
    private static final String LIB_DIR = "lib";

    // 插件优化后的 dex 目录（Android 5.0 及以上设备）
    private static final String OAT_DIR = "oat";

    // 插件优化后的 dex 目录（Android 5.0 以下设备）
    private static final String ODEX_DIR = "odex";

    // 插件多 dex 目录（Android 5.0 以下设备）
    static final String EXTRA_DEX_DIR = "ed";

    // 插件优化后多 dex 目录（Android 5.0 以下设备）
    static final String EXTRA_ODEX_DIR = "eod";

    // 用于记录额外的 dex 数量（Android 5.0 以下设备）
    static final String EXTRA_DEX_COUNT_FILE = PluginClassLoader.EXTRA_DEX_COUNT_FILE;

    // 插件原始 APK 文件名
    private static final String BASE_APK = "base.apk";

    // 插件优化后 DEX 文件名
    private static final String BASE_DEX = "base.dex";

    private static final String COMPILE_DEPENDENCIES_FILE = "compile_dependencies.txt";
    // Phantom 2.0.0 开始增加的共享公共库依赖需求配置文件
    private static final String PROVIDED_DEPENDENCIES_V2_FILE = "provided_dependencies_v2.txt";
    // Phantom 2 最小版本版本号 2.0.0(20000)
    private static final int PHANTOM_2_MIN_VERSION_CODE = 20000;

    private Context mContext;
    // 插件安装目录
    private File mPluginDir;
    private boolean mInitialized;
    private Signature[] mHostSignatures;

    // 信任的插件签名 MD5 列表，用于校验合作方的插件（插件签名与宿主签名不一致的情况）
    private List<String> mTrustedSignatures;

    // service name -> version
    private Map<String, Integer> mHostExportServiceMap;
    // groupId:artifactId -> version e.g. {"junit:junit": "4.12"}
    private Map<String, String> mHostCompileDependencyMap;
    // groupId:artifactId:version e.g. ["junit:junit:4.12"]
    private Set<String> mHostCompileDependencySet;

    // package_name -> PluginInfo
    private ArrayMap<String, PluginInfo> mPackages = new ArrayMap<>();
    // Activity ComponentName -> PluginInfo
    private ArrayMap<ComponentName, PluginInfo> mActivities = new ArrayMap<>();
    // Service ComponentName -> PluginInfo
    private ArrayMap<ComponentName, PluginInfo> mServices = new ArrayMap<>();

    private PluginManager() {
    }

    public static PluginManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    public synchronized void init(@NonNull Context context, @NonNull List<String> trustedSignatures) {
        if (mInitialized) {
            VLog.w("already initialized, skip this time");
            return;
        }

        TimingLogger logger = new TimingLogger(Constants.TAG, "PluginManager init");
        mContext = context.getApplicationContext();
        mTrustedSignatures = trustedSignatures;

        mPluginDir = mContext.getDir("plugins", Context.MODE_PRIVATE);
        logger.addSplit("create plugin dir");

        initHostCompileDependencies();
        logger.addSplit("init host compile dependencies");

        initHostExportServices();
        logger.addSplit("init host export service map");

        int count = scanInstalledPlugins();
        logger.addSplit("scanInstalledPlugins, count: " + count);

        logger.dumpToLog();

        mInitialized = true;
    }

    private void initHostExportServices() {
        final List<IService> services = PhantomServiceManager.getServices(mContext.getPackageName());
        mHostExportServiceMap = new ArrayMap<>(services.size());
        for (IService service : services) {
            mHostExportServiceMap.put(service.getServiceName(), service.getServiceVersion());
        }
    }

    private void initHostCompileDependencies() {
        final AssetManager assetManager = mContext.getAssets();
        InputStream inputStream = null;
        try {
            inputStream = assetManager.open(COMPILE_DEPENDENCIES_FILE, AssetManager.ACCESS_BUFFER);
            final List<String> lines = IoUtils.readLines(inputStream, IoUtils.DEFAULT_CHARSET);

            ArrayMap<String, String> librariesMap = new ArrayMap<>();  // lib -> version
            for (String line : lines) {
                final String[] parts = line.split(":");
                if (parts.length == 3) {
                    final String lib = parts[0] + ":" + parts[1];   // groupId:artifactId
                    final String newVersion = parts[2];
                    final String oldVersion = librariesMap.get(lib);
                    if (oldVersion != null && new Version(newVersion).lessThan(new Version(oldVersion))) {
                        // skip lower version
                        continue;
                    }

                    librariesMap.put(lib, newVersion);
                }
            }
            mHostCompileDependencyMap = librariesMap;

            ArraySet<String> librariesSet = new ArraySet<>();
            for (Map.Entry entry : librariesMap.entrySet()) {
                librariesSet.add(entry.getKey() + ":" + entry.getValue());
            }
            mHostCompileDependencySet = librariesSet;

        } catch (IOException e) {
            VLog.w(e, "error initHostCompileDependencies");
            mHostCompileDependencyMap = Collections.emptyMap();
            mHostCompileDependencySet = Collections.emptySet();
        } finally {
            IoUtils.closeQuietly(inputStream);
        }
    }

    private File createPackageDirectoryIfNeeded(String packageName) {
        return FileUtils.ensureDirectoryCreated(getPackageDirectory(packageName));
    }

    private File getPackageDirectory(String packageName) {
        return new File(mPluginDir, packageName);
    }

    private void checkInit() throws IllegalStateException {
        if (!mInitialized) {
            throw new IllegalStateException("should call init first");
        }
    }

    /**
     * 获取已安装的所有插件
     *
     * @return 已安装的所有插件列表
     */
    @NonNull
    public List<PluginInfo> getAllPlugins() {
        checkInit();

        List<PluginInfo> settings = new ArrayList<>(getPluginCount());
        settings.addAll(mPackages.values());
        return settings;
    }

    /**
     * 获取已安装插件数量
     *
     * @return 已安装插件数量
     */
    public int getPluginCount() {
        checkInit();

        return mPackages.size();
    }

    /**
     * 卸载插件
     *
     * @param pkg 插件包名
     * @return true 若卸载成功；否则 false
     */
    public synchronized boolean uninstallPlugin(String pkg) {
        checkInit();

        if (!isPluginInstalled(pkg)) {
            return false;
        }

        removePackage(pkg);

        final boolean ret = FileUtils.deleteDir(getPackageDirectory(pkg));
        if (!ret) {
            final String msg = String.format(Locale.ENGLISH, "uninstall plugin delete dir fail: %s", pkg);
            VLog.w(msg);
            LogReporter.reportException(new UninstallPluginException(msg));
        }

        return true;
    }

    /**
     * 判断插件是否已安装
     *
     * @param pkg 插件包名
     * @return true 若插件已安装；否则 false
     */
    public boolean isPluginInstalled(String pkg) {
        checkInit();

        return pkg != null && getPackage(pkg) != null;
    }

    /**
     * 卸载所有已安装的插件
     */
    public synchronized void uninstallAllPlugins() {
        checkInit();

        List<PluginInfo> allApps = getAllPlugins();
        for (PluginInfo pluginInfo : allApps) {
            final String packageName = pluginInfo.packageName;
            FileUtils.deleteDir(getPackageDirectory(packageName));
            removePackage(packageName);
        }
    }

    @Nullable
    public PluginInfo findPluginByPackageName(String pkg) {
        checkInit();
        return getPackage(pkg);
    }

    @Nullable
    public PluginInfo findPluginByActivityName(@NonNull ComponentName activityName) {
        checkInit();
        return getPluginByActivityName(activityName);
    }

    @Nullable
    public PluginInfo findPluginByServiceName(@NonNull ComponentName serviceName) {
        checkInit();
        return getPluginByServiceName(serviceName);
    }

    @Nullable
    public ActivityInfo findActivityInfo(@NonNull ComponentName activityName) {
        checkInit();
        PluginInfo pluginInfo = getPluginByActivityName(activityName);

        return pluginInfo != null ? pluginInfo.getActivityInfo(activityName) : null;
    }

    /**
     * 解析已安装的插件包，生成内存对象,需要在使用插件之前调用
     */
    private synchronized int scanInstalledPlugins() {
        int pluginCount = 0;
        final File[] files = mPluginDir.listFiles();
        if (files == null) {
            // should not happen
            return 0;
        }
        for (File appDir : files) {
            final HashMap<String, Object> params = new HashMap<>(2);

            final String pkgName = appDir.getName();
            final File apkFile = new File(appDir, BASE_APK);
            if (!apkFile.exists()) {
                final boolean ret = FileUtils.deleteDir(appDir);

                String msg = String.format("Unable to preload app %s, error: apk missing, remove it ret: %s", pkgName,
                        ret);
                VLog.e(msg);

                params.put(LogReporter.Key.STATUS, String.valueOf(InstallResult.ERR_IO_EXCEPTION));
                params.put(LogReporter.Key.MESSAGE, msg);
                LogReporter.reportException(new PreloadPluginException(msg));
                LogReporter.reportState(LogReporter.EventId.PLUGIN_PRELOAD, false,
                        LogReporter.Label.FILE_PREFIX + pkgName, params);
                continue;
            }

            TimingUtils.startTime(pkgName);
            InstallResult res = scanInstalledPlugin(apkFile.getAbsolutePath());
            final boolean success = res.isSuccess();
            if (success) {
                pluginCount++;
                params.put(LogReporter.Key.VERSION_NAME, res.plugin == null ? "N/A" : res.plugin.versionName);
            } else {
                final boolean ret = FileUtils.deleteDir(appDir);
                VLog.e("Unable to preload app %s, error: %s, remove it ret: %s", pkgName, res, ret);
                params.put(LogReporter.Key.STATUS, String.valueOf(res.status));
                params.put(LogReporter.Key.MESSAGE, res.message);
                LogReporter.reportException(new PreloadPluginException(res.message));
            }

            // TODO: remove this event report if it harm initialization performance

            params.put(LogReporter.Key.TIME,
                    TimingUtils.getNormalizedDuration(pkgName, TimingUtils.SECTION_DURATION_10_MS,
                            TimingUtils.MAX_SECTION_20));
            LogReporter.reportState(LogReporter.EventId.PLUGIN_PRELOAD, success,
                    LogReporter.Label.FILE_PREFIX + pkgName, params);
        }
        return pluginCount;
    }

    private InstallResult scanInstalledPlugin(String apkPath) {
        return install(apkPath, true, false, false, false);
    }

    /**
     * 安装 assets 中的插件
     *
     * @param assetsApkPath   位于 assets 中的安装包文件路径（相对于 assets 根目录，比如：
     *                        <code>"plugins/com.wlqq.phantom.plugin.test1_1.0.0.apk"</code>）
     * @param checkVersion    是否检查版本号，若为 true, 则仅支持升级安装
     * @param checkSignatures 是否校验签名，若为 true, 则插件与宿主签名一致才能安装
     * @return 安装结果
     */
    public synchronized InstallResult installPluginFromAssets(String assetsApkPath, boolean checkVersion,
            boolean checkSignatures) {
        File tmpFile = new File(mContext.getDir("assets_plugins", Context.MODE_PRIVATE),
                SystemClock.elapsedRealtime() + ".apk");
        AssetManager assets = mContext.getAssets();
        InstallResult result;
        try {
            FileUtils.copyInputStreamToFile(assets.open(assetsApkPath), tmpFile);
            result = installPlugin(tmpFile.getAbsolutePath(), checkVersion, checkSignatures, false);
        } catch (IOException e) {
            String msg = "error copy assets apk to tmp dir: " + assetsApkPath;
            VLog.e(e, msg);
            result = new InstallResult(InstallResult.ERR_IO_EXCEPTION, msg, e);
        } finally {
            if (tmpFile.isFile() && (!tmpFile.delete())) {
                VLog.w("delete %s error", tmpFile.getName());
            }
        }

        return result;
    }

    /**
     * 安装插件
     *
     * @param apkPath         安装包绝对路径
     * @param checkVersion    是否检查版本号，若为 true, 则仅支持升级安装
     * @param checkSignatures 是否校验签名，若为 true, 则插件与宿主签名一致才能安装
     * @return 安装结果
     */
    public synchronized InstallResult installPlugin(String apkPath, boolean checkVersion, boolean checkSignatures) {
        checkInit();

        return install(apkPath, false, checkVersion, checkSignatures, false);
    }

    /**
     * 安装插件
     *
     * @param apkPath         安装包绝对路径
     * @param checkVersion    是否检查版本号，若为 true, 则仅支持升级安装
     * @param checkSignatures 是否校验签名，若为 true, 则插件与宿主签名一致才能安装
     * @param forceReplace    是否忽略版本检查，是否支持热更新检查，强制替换插件
     * @return 安装结果
     */
    public synchronized InstallResult installPlugin(String apkPath, boolean checkVersion, boolean checkSignatures,
            boolean forceReplace) {
        checkInit();

        return install(apkPath, false, checkVersion, checkSignatures, forceReplace);
    }

    private InstallResult install(String apkPath, boolean onlyScan, boolean checkVersion, boolean checkSignatures,
            boolean forceReplace) {
        if (apkPath == null) {
            final String msg = "install error, apkPath is null";
            VLog.w(msg);
            return new InstallResult(InstallResult.ERR_FILE_NOT_EXIST, msg, new SourceFileNotExistException(msg));
        }

        File apk = new File(apkPath);
        if (!apk.exists() || !apk.isFile()) {
            String msg = "install error, file not exist: " + apkPath;
            VLog.w(msg);
            return new InstallResult(InstallResult.ERR_FILE_NOT_EXIST, msg, new SourceFileNotExistException(msg));
        }

        int flags = PackageManager.GET_META_DATA | PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES
                | PackageManager.GET_PROVIDERS | PackageManager.GET_RECEIVERS;
        if (checkSignatures) {
            flags |= PackageManager.GET_SIGNATURES;
        }

        final PackageManager packageManager = mContext.getPackageManager();
        final PackageInfo packageInfo = packageManager.getPackageArchiveInfo(apkPath, flags);
        if (packageInfo == null) {
            final String msg = "install error, packageInfo is null, parse apk: " + apkPath;
            VLog.w(msg);
            return new InstallResult(InstallResult.ERR_PARSE_APK, msg, new ParseApkException(msg));
        }

        final ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        if (applicationInfo == null) {
            final String msg = "install error, applicationInfo is null, parse apk: " + apkPath;
            VLog.w(msg);
            return new InstallResult(InstallResult.ERR_PARSE_APK, msg, new ParseApkException(msg));
        }
        // fix sourceDir and publicSourceDir
        applicationInfo.sourceDir = applicationInfo.publicSourceDir = apkPath;

        Resources resources;
        try {
            resources = packageManager.getResourcesForApplication(applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            final String msg = "install error, getResourcesForApplication NameNotFoundException, parse apk: " + apkPath;
            VLog.w(e, msg);
            return new InstallResult(InstallResult.ERR_PARSE_APK, msg, new ParseApkException(msg));
        }

        final AssetManager assets = resources.getAssets();

        // check shared library dependencies
        CheckSharedLibraryDependenciesResult sharedLibraryDependenciesResult;
        try {
            sharedLibraryDependenciesResult = checkSharedLibraryDependencies(apkPath, assets);
            if (!sharedLibraryDependenciesResult.success) {
                return new InstallResult(InstallResult.ERR_SHARED_LIBRARY_DEPENDENCY_MISMATCH,
                        sharedLibraryDependenciesResult.message,
                        new SharedLibraryDependenciesMismatchException(sharedLibraryDependenciesResult.message,
                                sharedLibraryDependenciesResult.hostCompileDependencies,
                                sharedLibraryDependenciesResult.pluginProvidedDependencies));
            }
        } catch (ParseProvidedDependenciesException e) {
            final String msg = "error parsePluginProvidedDependencies: " + apkPath;
            VLog.w(e, msg);
            return new InstallResult(InstallResult.ERR_PARSE_PROVIDED_LIBRARIES, msg, e);
        }

        // check phantom service dependencies
        CheckPhantomServiceDependenciesResult phantomServiceDependenciesResult = checkPhantomServiceDependencies(
                packageInfo);
        VLog.i("phantomServiceDependenciesResult: %s, msg: %s", phantomServiceDependenciesResult.success,
                phantomServiceDependenciesResult.message);

        boolean isReplace = false;

        // PackageManagerService holds all packages, try to check if need update.
        PluginInfo existOne = getPackage(packageInfo.packageName);
        if (existOne != null) {
            if (!phantomServiceDependenciesResult.success) {
                // 宿主不满足插件对 PhantomService 的依赖
                final String msg = "install skip upgrade, PhantomService dependencies mismatch, apkPath: " + apkPath;
                VLog.w(msg);
                return new InstallResult(InstallResult.ERR_INSTALL_NOT_UPGRADE, msg, existOne);
            }

            if (!forceReplace && !checkVersion(existOne.packageInfo, packageInfo, checkVersion)) {
                // 若配置了检查版本信息，则不允许降版本或同版本覆盖
                final String msg = "install skip upgrade, version downgrade or replace, apkPath: " + apkPath;
                VLog.w(msg);
                return new InstallResult(InstallResult.ERR_INSTALL_NOT_UPGRADE, msg, existOne);
            }

            if (!forceReplace && existOne.isStarted() && !existOne.isHotUpgrade()) {
                // 插件已启动，但不支持热升级
                final String msg = "install skip upgrade, the old does not support hot upgrade, apkPath: " + apkPath;
                VLog.w(msg);
                return new InstallResult(InstallResult.ERR_INSTALL_NOT_UPGRADE, msg, existOne);
            }

            isReplace = true;
        } else if (!phantomServiceDependenciesResult.success) {
            // 全新安装
            return new InstallResult(InstallResult.ERR_PHANTOM_SERVICE_DEPENDENCY_MISMATCH,
                    phantomServiceDependenciesResult.message,
                    new PhantomServiceDependenciesMismatchException(phantomServiceDependenciesResult.message));
        }

        File appDir = createPackageDirectoryIfNeeded(packageInfo.packageName);
        if (!appDir.isDirectory()) {
            final String msg = "install error, unable to create app dir: " + appDir.getAbsolutePath();
            VLog.w(msg);
            return new InstallResult(InstallResult.ERR_IO_EXCEPTION, msg, new IOException(msg));
        }

        File libDir = new File(appDir, LIB_DIR);
        if (!libDir.exists() && !libDir.mkdirs()) {
            final String msg = "install error, unable to create lib dir: " + libDir.getAbsolutePath();
            VLog.w(msg);
            return new InstallResult(InstallResult.ERR_IO_EXCEPTION, msg, new IOException(msg));
        }

        File odexDir = new File(appDir, VmUtils.IS_VM_ART ? OAT_DIR : ODEX_DIR);
        if (!odexDir.exists() && !odexDir.mkdirs()) {
            final String msg = "install error, unable to create odex folder: " + odexDir.getAbsolutePath();
            VLog.w(msg);
            return new InstallResult(InstallResult.ERR_IO_EXCEPTION, msg, new IOException(msg));
        }
        File odexFile = new File(odexDir, BASE_DEX);

        if (!onlyScan) {
            if (checkSignatures && !checkSignatures(packageInfo)) {
                final String msg = "install error, signature mismatch, apkPath: " + apkPath;
                VLog.w(msg);
                return new InstallResult(InstallResult.ERR_SIGNATURE_MISMATCH, msg,
                        new SignatureMismatchException(msg));
            }

            File dstApk = new File(appDir, BASE_APK);

            if (isReplace) {
                FileUtils.cleanDir(libDir);
                FileUtils.cleanDir(odexDir);

                if (dstApk.exists() && (!dstApk.delete())) {
                    VLog.w("delete %s error", dstApk.getName());
                }
            }

            try {
                NativeLibraryUtils.copyNativeBinaries(apk, libDir);
            } catch (CopyNativeSoException e) {
                final String msg = "copyNativeBinaries error: " + apk;
                VLog.w(e, msg);
                return new InstallResult(InstallResult.ERR_COPY_NATIVE_SO, msg, e);
            }

            try {
                FileUtils.copyFile(apk, dstApk);
            } catch (IOException e) {
                final String msg = "install error, copyFile error base.apk: " + apkPath;
                VLog.w(e, msg);
                return new InstallResult(InstallResult.ERR_IO_EXCEPTION, msg, e);
            }

            apk = dstApk;
        }

        if (isReplace) {
            removePackage(packageInfo.packageName);
        }

        TimingLogger timingLogger = new TimingLogger(Constants.TAG, "install");
        AndroidManifestParser.ComponentIntentFilters componentIntentFilters;
        try {
            componentIntentFilters = AndroidManifestParser.parse(assets);
            timingLogger.addSplit("AndroidManifestParser#parse ok");
        } catch (ParseApkException e) {
            timingLogger.addSplit("AndroidManifestParser#parse error");
            final String msg = "parse manifest from apk error: " + apk;
            VLog.w(e, msg);
            return new InstallResult(InstallResult.ERR_PARSE_APK, msg, e);
        } finally {
            timingLogger.dumpToLog();
        }

        PluginInfo pluginInfo = new PluginInfo(apk.getAbsolutePath(), libDir.getAbsolutePath(),
                odexDir.getAbsolutePath(), odexFile.getAbsolutePath(), packageInfo, mContext.getPackageManager(),
                componentIntentFilters, sharedLibraryDependenciesResult.pluginProvidedDependencies);
        putPackage(pluginInfo);
        final int status = isReplace ? InstallResult.ERR_INSTALL_UPGRADE : InstallResult.ERR_INSTALL_NEW;
        final String msg = "install ok, status: " + status;
        VLog.i(msg);
        return new InstallResult(status, msg, pluginInfo);
    }

    private CheckSharedLibraryDependenciesResult checkSharedLibraryDependencies(String apkPath,
            AssetManager assetManager)
            throws ParseProvidedDependenciesException {
        final Map<String, String> pluginProvidedDependencies = parsePluginProvidedDependencies(apkPath, assetManager);
        final Map<String, String> hostCompileDependencies = getHostCompileDependencyMap();
        final VersionVerifier.Result result = VersionVerifier.satisfies(hostCompileDependencies,
                pluginProvidedDependencies);
        if (!result.success) {
            final String msg = "error shared library dependencies mismatch: " + apkPath + ", " + result.message;
            VLog.w(msg);
            VLog.w("hostCompileDependencies: %s", hostCompileDependencies);
            VLog.w("pluginProvidedDependencies: %s", pluginProvidedDependencies);
            return new CheckSharedLibraryDependenciesResult(false, msg, hostCompileDependencies,
                    pluginProvidedDependencies);
        } else {
            return new CheckSharedLibraryDependenciesResult(true, "ok", hostCompileDependencies,
                    pluginProvidedDependencies);
        }
    }

    private CheckPhantomServiceDependenciesResult checkPhantomServiceDependencies(@NonNull PackageInfo newOne) {
        // Phantom 2.x 插件必须在其 AndroidManifest.xml 声明
        // <!-- 2.x 的插件需要运行在 Phantom 2.0.0 及以上版本 -->
        // <meta-data
        //     android:name="phantom.service.import.PhantomVersionService"
        //     android:value="20000" />
        final ApplicationInfo applicationInfo = newOne.applicationInfo;
        if (applicationInfo == null || applicationInfo.metaData == null) {
            VLog.w("missing meta-data");
            return new CheckPhantomServiceDependenciesResult(false,
                    "Phantom2 expect plugin declare min PhantomVersion requirement in meta-data, but missing");
        }

        final Bundle metaData = applicationInfo.metaData;
        final int requiredPhantomVersion = metaData.getInt(PluginInfo.META_DATA_KEY_IMPORT_PHANTOM_VERSION_SERVICE);
        if (requiredPhantomVersion < PHANTOM_2_MIN_VERSION_CODE) {
            // 非 Phantom2 插件
            final String msg = String.format(Locale.ENGLISH,
                    "Phantom2 expect plugin declare min PhantomVersion requirement >= 20000 in meta-data, actual: %d",
                    requiredPhantomVersion);
            VLog.w(msg);
            return new CheckPhantomServiceDependenciesResult(false, msg);
        }

        for (String key : metaData.keySet()) {
            if (key.startsWith(PluginInfo.META_DATA_KEY_IMPORT_SERVICE_PREFIX)) {
                final String serviceName = key.substring(PluginInfo.META_DATA_KEY_IMPORT_SERVICE_PREFIX_LENGTH);
                final Integer hostExportVersion = mHostExportServiceMap.get(serviceName);
                // 宿主没有提供插件需要的该服务
                if (hostExportVersion == null) {
                    return new CheckPhantomServiceDependenciesResult(false,
                            "host missing phantom service: " + serviceName);
                }

                final int pluginImportVersion = metaData.getInt(key);
                if (hostExportVersion < pluginImportVersion) {
                    return new CheckPhantomServiceDependenciesResult(false, String.format(Locale.ENGLISH,
                            "host phantom service(%s: %d) is lower than plugin(%d) required",
                            serviceName, hostExportVersion, pluginImportVersion));
                }
            }
        }
        return new CheckPhantomServiceDependenciesResult(true, "host provide all phantom service that plugin required");
    }

    private boolean checkVersion(PackageInfo oldOne, PackageInfo newOne,
            boolean compareVersion) {
        return (!compareVersion || oldOne.versionCode < newOne.versionCode);
    }

    @SuppressLint("PackageManagerGetSignatures")
    private Signature[] getHostSignatures() {
        if (mHostSignatures == null) {
            final String packageName = mContext.getPackageName();
            try {
                PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(packageName,
                        PackageManager.GET_SIGNATURES);
                mHostSignatures = packageInfo.signatures;
            } catch (PackageManager.NameNotFoundException e) {
                // should not happen
                VLog.w(e, "package %s not found", packageName);
            }
        }
        return mHostSignatures;
    }

    /**
     * 校验插件 APK 的签名信息，满足其中一条即可
     * <ol>
     * <li>插件签名 MD5 是否在信任的签名 MD5 列表中</li>
     * <li>插件签名是否与宿主签名一致</li>
     * </ol>
     *
     * @param packageInfo 插件 {@linkplain PackageInfo}
     * @return true 校验成功； false 校验失败
     */
    private boolean checkSignatures(@NonNull PackageInfo packageInfo) {
        final Signature[] pluginSignatures = packageInfo.signatures;
        if (pluginSignatures == null) {
            // 没有获取到插件的签名信息，可能插件 APK 没有签名，安装失败
            return false;
        }

        // 只有信任的签名 MD5 列表不为空，才计算插件签名 MD5
        if (mTrustedSignatures != null && !mTrustedSignatures.isEmpty()) {
            for (Signature signature : pluginSignatures) {
                final String signatureMd5 = DigestUtils.md5Hex(signature.toByteArray());
                if (mTrustedSignatures.contains(signatureMd5)) {
                    return true;
                }
            }
        }

        // 若插件签名 MD5 不在信任的签名 MD5 列表中，则校验插件签名是否与宿主签名一致
        Signature[] hostSignatures = getHostSignatures();
        return Arrays.equals(hostSignatures, pluginSignatures);
    }

    @NonNull
    public Map<String, String> getHostCompileDependencyMap() {
        return mHostCompileDependencyMap;
    }

    @NonNull
    public Set<String> getHostCompileDependencySet() {
        return mHostCompileDependencySet;
    }

    private Map<String, String> parsePluginProvidedDependencies(String apkPath, AssetManager assetManager)
            throws ParseProvidedDependenciesException {
        InputStream inputStream = null;
        try {
            inputStream = assetManager.open(PROVIDED_DEPENDENCIES_V2_FILE, AssetManager.ACCESS_BUFFER);
            final List<String> lines = IoUtils.readLines(inputStream, IoUtils.DEFAULT_CHARSET);

            return parseDependencyRequirements(lines);
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                return Collections.emptyMap();
            } else {
                VLog.w(e, "error parse provided_dependencies file: %s", apkPath);
                throw new ParseProvidedDependenciesException("error parse provided_dependencies: " + apkPath, e);
            }
        } finally {
            IoUtils.closeQuietly(inputStream);
        }
    }

    @NonNull
    private ArrayMap<String, String> parseDependencyRequirements(List<String> lines) {
        ArrayMap<String, String> dependencies = new ArrayMap<>();   // lib -> requirement
        for (String line : lines) {
            final String[] parts = line.split(":");
            if (parts.length == 3) {
                final String lib = parts[0] + ":" + parts[1];   // groupId:artifactId
                final String requirement = parts[2];
                dependencies.put(lib, requirement);
            }
        }
        return dependencies;
    }

    private synchronized void putPackage(@NonNull PluginInfo pluginInfo) {
        mPackages.put(pluginInfo.packageName, pluginInfo);

        final PackageInfo packageInfo = pluginInfo.packageInfo;

        final ActivityInfo[] activities = packageInfo.activities;
        if (activities != null) {
            for (ActivityInfo activity : activities) {
                mActivities.put(new ComponentName(activity.packageName, activity.name), pluginInfo);
            }
        }

        final ServiceInfo[] services = packageInfo.services;
        if (services != null) {
            for (ServiceInfo service : services) {
                mServices.put(new ComponentName(service.packageName, service.name), pluginInfo);
            }
        }
    }

    @Nullable
    private synchronized PluginInfo getPackage(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }
        return mPackages.get(packageName);
    }

    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    private synchronized void removePackage(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }

        final PluginInfo removed = mPackages.remove(packageName);

        if (removed == null) {
            return;
        }

        final ActivityInfo[] activities = removed.packageInfo.activities;
        if (activities != null) {
            for (ActivityInfo activity : activities) {
                mActivities.remove(new ComponentName(activity.packageName, activity.name));
            }
        }

        final ServiceInfo[] services = removed.packageInfo.services;
        if (services != null) {
            for (ServiceInfo service : services) {
                mServices.remove(new ComponentName(service.packageName, service.name));
            }
        }

        PhantomServiceManager.unregisterService(packageName);

        removed.unregisterStaticBroadcastReceiver(mContext);
    }

    @Nullable
    private PluginInfo getPluginByActivityName(@NonNull ComponentName activity) {
        return mActivities.get(activity);
    }

    @Nullable
    private PluginInfo getPluginByServiceName(@NonNull ComponentName service) {
        return mServices.get(service);
    }

    @SuppressWarnings("PMD.AccessorClassGeneration")
    private static class LazyHolder {
        @SuppressLint("StaticFieldLeak")
        static final PluginManager INSTANCE = new PluginManager();
    }

    private static final class CheckSharedLibraryDependenciesResult {
        public final boolean success;
        @NonNull
        public final String message;
        @NonNull
        public final Map<String, String> hostCompileDependencies;
        @NonNull
        public final Map<String, String> pluginProvidedDependencies;

        CheckSharedLibraryDependenciesResult(boolean success,
                @NonNull String message,
                @Nullable Map<String, String> hostCompileDependencies,
                @Nullable Map<String, String> pluginProvidedDependencies) {
            this.success = success;
            this.message = message;
            this.hostCompileDependencies =
                    hostCompileDependencies == null ? Collections.<String, String>emptyMap()
                            : hostCompileDependencies;
            this.pluginProvidedDependencies =
                    pluginProvidedDependencies == null ? Collections.<String, String>emptyMap()
                            : pluginProvidedDependencies;
        }
    }

    private static final class CheckPhantomServiceDependenciesResult {
        public final boolean success;
        @NonNull
        public final String message;

        CheckPhantomServiceDependenciesResult(boolean success, @NonNull String message) {
            this.success = success;
            this.message = message;
        }
    }
}