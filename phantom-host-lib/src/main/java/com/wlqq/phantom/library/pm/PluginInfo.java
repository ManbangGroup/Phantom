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

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.TimingLogger;

import dalvik.system.DexFile;

import com.taobao.android.dex.interpret.ARTUtils;
import com.wlqq.phantom.library.PhantomCore;
import com.wlqq.phantom.library.PhantomEventCallback;
import com.wlqq.phantom.library.env.Constants;
import com.wlqq.phantom.library.log.LogReporter;
import com.wlqq.phantom.library.proxy.ApplicationHostProxy;
import com.wlqq.phantom.library.proxy.PluginClassLoader;
import com.wlqq.phantom.library.proxy.ResourcesProxy;
import com.wlqq.phantom.library.utils.FileUtils;
import com.wlqq.phantom.library.utils.SuppressFBWarnings;
import com.wlqq.phantom.library.utils.TimingUtils;
import com.wlqq.phantom.library.utils.VLog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 已安装插件的描述信息
 */
@SuppressWarnings("PMD.ConfusingTernary")
public final class PluginInfo {
    /**
     * 通过 AndroidManifest.xml 中 meta-data 配置导出(提供)服务信息. e.g.
     * <pre>{@code
     *     <application>
     *         ...
     *         <!-- 提供的高德地图服务（版本 2） -->
     *         <meta-data android:name="phantom.service.export.amap" android:value="2"/>
     *         ...
     *     </application>}
     * </pre>
     */
    public static final String META_DATA_KEY_EXPORT_SERVICE_PREFIX = "phantom.service.export.";
    /**
     * 通过 AndroidManifest.xml 中 meta-data 配置导入(依赖)服务信息. e.g.
     * <pre>{@code
     *     <application>
     *         ...
     *         <!-- 该插件依赖宿主提供的高德地图服务（版本 2） ，宿主提供的高徳地图服务版本必须大于等于版本 2 ， 该插件才能安装成功-->
     *         <meta-data android:name="phantom.service.import.amap" android:value="2"/>
     *         ...
     *     </application>}
     * </pre>
     */
    public static final String META_DATA_KEY_IMPORT_PHANTOM_VERSION_SERVICE =
            "phantom.service.import.PhantomVersionService";
    /**
     * 是否需要在插件管理页中隐藏该插件。取值
     * <ul>
     * <li><code>true</code></li>
     * <li><code>false(默认)</code></li>
     * </ul>
     * <pre>{@code
     * <application>
     *         ...
     *         <!-- 该插件不需要在插件管理器中展示，声明其 phantom.hidden 属性为 true -->
     *         <meta-data android:name="phantom.hidden" android:value="true"/>
     *         ...
     * </application>
     * }</pre>
     *
     * @see #isHidden()
     */
    public static final String META_DATA_KEY_HIDDEN = "phantom.hidden";
    /**
     * 插件是否支持 <b>热升级</b> (即在插件已启动的情况下，安装插件的新版本并运行，而无需重启应用)，取值
     * <ul>
     * <li><code>true</code></li>
     * <li><code>false(默认)</code></li>
     * </ul>
     * <pre>{@code
     *     <application>
     *         ...
     *         <!-- 该插件支持热升级，声明其 phantom.hot_upgrade 属性为 true -->
     *         <meta-data android:name="phantom.hot_upgrade" android:value="true"/>
     *         ...
     *     </application>}
     * </pre>
     * 插件支持 <b>热升级</b> 的条件：
     * <ul>
     * <li>没有 so 本地库</li>
     * <li>没有 Service 组件</li>
     * <li>不提供 View 供宿主嵌入</li>
     * </ul>
     *
     * @see #isHotUpgrade()
     */
    public static final String META_DATA_KEY_HOT_UPGRADE = "phantom.hot_upgrade";
    static final String META_DATA_KEY_IMPORT_SERVICE_PREFIX = "phantom.service.import.";
    static final int META_DATA_KEY_EXPORT_SERVICE_PREFIX_LENGTH = META_DATA_KEY_EXPORT_SERVICE_PREFIX.length();
    static final int META_DATA_KEY_IMPORT_SERVICE_PREFIX_LENGTH = META_DATA_KEY_IMPORT_SERVICE_PREFIX.length();
    /**
     * <pre>{@code
     *     <application>
     *         ...
     *         <meta-data android:name="JENKINS_BUILD_NUMBER" android:value="123"/>
     *         ...
     *     </application>}
     * </pre>
     *
     * @see #getJenkinsBuildNumber()
     */
    private static final String META_DATA_KEY_JENKINS_BUILD_NUMBER = "JENKINS_BUILD_NUMBER";
    /**
     * 插件 AndroidManifest.xml 中 application 元素的 label 属性值
     */
    @Nullable
    public final String label;
    @Nullable
    /**
     * 插件 AndroidManifest.xml 中 application 元素的 icon 属性值
     */
    public final Drawable icon;
    /**
     * 插件 AndroidManifest.xml 中 manifest 元素的 package 属性值
     */
    public final String packageName;
    /**
     * 插件 APK 在手机文件系统中的安装路径
     */
    public final String apkPath;
    /**
     * 插件 APK 包含的 so 在手机文件系统中的路径
     */
    public final String libPath;
    /**
     * 插件 odex 文件所在目录
     */
    public final String odexDir;
    /**
     * 插件 odex 文件路径
     */
    public final String odexPath;
    /**
     * 插件版本名
     */
    public final String versionName;
    /**
     * 插件版本号
     */
    public final int versionCode;
    /**
     * 插件对应的 PackageInfo
     */
    public final PackageInfo packageInfo;

    /**
     * 插件安装目录
     */
    public final File installDir;
    /**
     * key is Activity ComponentName
     */
    private final ArrayMap<ComponentName, ActivityInfo> mActivitiesInfo;
    /**
     * Registered global BroadcastReceiver list
     */
    private final List<BroadcastReceiver> mGlobalBroadcastReceivers;
    // 依赖宿主提供的公共库 GAV Set
    private final Map<String, String> mProvidedDependencies;
    // activity -> intent filters
    private final Map<String, List<IntentFilter>> mActivities;
    // service -> intent filters
    private final Map<String, List<IntentFilter>> mServices;
    // receiver -> intent filters
    private final Map<String, List<IntentFilter>> mReceivers;
    private int mJenkinsBuildNumber;
    // 是否在插件管理页中隐藏该插件，默认 false
    private boolean mHidden;
    // 是否支持 **热升级**
    private boolean mHotUpgrade;
    // 对外提供的服务 { service_name -> service_version }
    private ArrayMap<String, Integer> mExportServiceMap;
    // 需要使用的服务 { service_name -> service_version }
    private ArrayMap<String, Integer> mImportServiceMap;
    /**
     * The launcher Activities class list
     */
    private List<String> mLauncherActivities;
    private AssetManager mPluginAssetManager;
    private Resources mPluginResources;
    private PluginClassLoader mPluginClassLoader;
    private ApplicationHostProxy mApplication;

    private volatile boolean mStarted;

    private final Lock mLock = new ReentrantLock();

    /**
     * 创建插件描述实例
     *
     * @param apkPath                插件 APK 路径
     * @param libPath                插件 APK 中 so 释放路径
     * @param odexDir                插件 odex 文件所在目录
     * @param odexPath               插件 odex 文件路径
     * @param packageInfo            插件对应的 PackageInfo 对象
     * @param packageManager         宿主 PackageManager 实例
     * @param componentIntentFilters 插件中各类型组件 Intent-Filter 列表
     * @param providedDependencies   插件对宿主 maven 库依赖描述信息
     */
    public PluginInfo(String apkPath, String libPath, String odexDir, String odexPath,
            PackageInfo packageInfo, PackageManager packageManager,
            AndroidManifestParser.ComponentIntentFilters componentIntentFilters,
            Map<String, String> providedDependencies) {
        this.apkPath = apkPath;
        this.packageName = packageInfo.packageName;
        this.libPath = libPath;
        this.odexDir = odexDir;
        this.odexPath = odexPath;
        this.versionName = packageInfo.versionName;
        this.versionCode = packageInfo.versionCode;
        this.packageInfo = packageInfo;
        this.installDir = new File(apkPath).getParentFile();

        mActivitiesInfo = new ArrayMap<>();
        mProvidedDependencies = providedDependencies;

        mActivities = componentIntentFilters.mActivities;
        mServices = componentIntentFilters.mServices;
        mReceivers = componentIntentFilters.mReceivers;
        mLauncherActivities = new ArrayList<>(componentIntentFilters.mLauncherActivities);

        parseActivityInfo();
        parseMetaData();

        mGlobalBroadcastReceivers = new ArrayList<>();

        final ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        if (applicationInfo != null) {
            applicationInfo.sourceDir = apkPath;
            applicationInfo.publicSourceDir = apkPath;
            applicationInfo.nativeLibraryDir = libPath;
            this.label = applicationInfo.loadLabel(packageManager).toString();
            this.icon = loadApplicationIconSafe(applicationInfo, packageManager);
        } else {
            this.label = null;
            this.icon = null;
        }
    }

    // 处理兼容问题：在某些设备上调用 PackageItemInfo#loadIcon 会出现 NPE
    private Drawable loadApplicationIconSafe(@NonNull ApplicationInfo applicationInfo,
            @NonNull PackageManager packageManager) {
        Drawable icon = null;
        try {
            icon = applicationInfo.loadIcon(packageManager);
        } catch (Exception e) {
            VLog.w(e, "fail to load plugin %s_%s icon", this.packageName, this.versionName);
            LogReporter.reportException(e);
        }
        return icon;
    }

    /**
     * 获取插件 <code>AndroidManifest.xml</code> 中 <code>meta-data</code> 配置的 <code>JENKINS_BUILD_NUMBER</code> 值
     *
     * @return jenkins build number
     */
    public int getJenkinsBuildNumber() {
        return mJenkinsBuildNumber;
    }

    /**
     * 是否需要在插件管理页隐藏该插件，默认 false
     *
     * @return true 需要隐藏；false 不需要隐藏
     * @see #META_DATA_KEY_HIDDEN
     */
    public boolean isHidden() {
        return mHidden;
    }

    /**
     * 插件是否支持 <b>热升级</b> (即在插件已启动的情况下，安装插件的新版本并运行，而无需重启应用)
     *
     * @return true 支持 <b>热升级</b>；false 不支持 <b>热升级</b>
     * @see #META_DATA_KEY_HOT_UPGRADE
     */
    public boolean isHotUpgrade() {
        return mHotUpgrade;
    }

    /**
     * multidex extra dex file dir for Android 4.x only
     *
     * @return multi-dex extra dex file dir
     */
    public File getExtraDexesDir() {
        return getDexDir(PluginManager.EXTRA_DEX_DIR);
    }

    /**
     * multidex extra odex file dir for Android 4.x only
     *
     * @return multi-dex extra odex file dir
     */
    public File getExtraOdexesDir() {
        return getDexDir(PluginManager.EXTRA_ODEX_DIR);
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private File getDexDir(String dir) {
        File extraDexDir = new File(installDir, dir);
        if (!extraDexDir.exists()) {
            extraDexDir.mkdir();
        }
        return extraDexDir;
    }

    // 解析插件 AndroidManifest 中的 meta-data 元素
    private void parseMetaData() {
        mExportServiceMap = new ArrayMap<>();
        mImportServiceMap = new ArrayMap<>();

        final ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        if (applicationInfo == null) {
            return;
        }

        final Bundle appMetaData = applicationInfo.metaData;
        if (appMetaData == null) {
            return;
        }

        mJenkinsBuildNumber = appMetaData.getInt(META_DATA_KEY_JENKINS_BUILD_NUMBER);
        mHidden = appMetaData.getBoolean(META_DATA_KEY_HIDDEN);
        mHotUpgrade = appMetaData.getBoolean(META_DATA_KEY_HOT_UPGRADE);

        for (String key : appMetaData.keySet()) {
            if (TextUtils.isEmpty(key)) {
                continue;
            }

            if (key.startsWith(META_DATA_KEY_EXPORT_SERVICE_PREFIX)) {
                final String serviceName = key.substring(META_DATA_KEY_EXPORT_SERVICE_PREFIX_LENGTH);
                if (!TextUtils.isEmpty(serviceName)) {
                    mExportServiceMap.put(serviceName, appMetaData.getInt(key));
                }
            } else if (key.startsWith(META_DATA_KEY_IMPORT_SERVICE_PREFIX)) {
                final String serviceName = key.substring(META_DATA_KEY_IMPORT_SERVICE_PREFIX_LENGTH);
                if (!TextUtils.isEmpty(serviceName)) {
                    mImportServiceMap.put(serviceName, appMetaData.getInt(key));
                }
            }
        }
    }

    private void parseActivityInfo() {
        final ActivityInfo[] activities = packageInfo.activities;

        if (activities == null) {
            return;
        }

        for (ActivityInfo activityInfo : activities) {
            mActivitiesInfo.put(new ComponentName(activityInfo.packageName, activityInfo.name), activityInfo);
        }
    }

    /**
     * 获取插件声明的对宿主公共库的依赖
     *
     * @return 依赖集合
     */
    public Map<String, String> getProvidedDependencies() {
        return mProvidedDependencies;
    }

    @NonNull
    public Map<String, List<IntentFilter>> getActivityIntentFilterMap() {
        return mActivities;
    }

    @NonNull
    public Map<String, List<IntentFilter>> getServiceIntentFilterMap() {
        return mServices;
    }

    @NonNull
    public Map<String, List<IntentFilter>> getReceiverIntentFilterMap() {
        return mReceivers;
    }

    /**
     * 获取插件 AndroidManifest 中配置的所有 {@link android.app.Activity}
     *
     * @return 插件 AndroidManifest 中配置的所有 {@link android.app.Activity}
     */
    @NonNull
    public List<String> getActivities() {
        return new ArrayList<>(mActivities.keySet());
    }

    /**
     * 获取插件 AndroidManifest 中配置了 {@link Intent#CATEGORY_LAUNCHER} Intent-Filter 的 {@link
     * android.app.Activity}
     *
     * @return {@link android.app.Activity} 列表
     */
    @NonNull
    public List<String> getLauncherActivities() {
        // FIXME: 11/3/16 return unmodifiable copy of list
        return mLauncherActivities;
    }

    /**
     * 获取插件的 {@link ApplicationInfo}
     *
     * @return 插件的 {@link ApplicationInfo}
     */
    @Nullable
    public ApplicationInfo getApplicationInfo() {
        return packageInfo.applicationInfo;
    }

    /**
     * 获取插件的 {@link PackageInfo}
     *
     * @return 插件的 {@link PackageInfo}
     */
    @Nullable
    public PackageInfo getPackageInfo() {
        return packageInfo;
    }

    /**
     * 获取插件导出（提供）的 {@link com.wlqq.phantom.communication.PhantomService} 信息
     *
     * @return 插件导出（提供）的 {@link com.wlqq.phantom.communication.PhantomService} 信息, map key: service name, value: service
     * version
     */
    public ArrayMap<String, Integer> getExportServiceMap() {
        return mExportServiceMap;
    }

    /**
     * 获取插件导入（依赖）的 {@link com.wlqq.phantom.communication.PhantomService} 信息
     *
     * @return 插件导入（依赖）的 {@link com.wlqq.phantom.communication.PhantomService} 信息, map key: service name, value: service
     * version
     */
    public ArrayMap<String, Integer> getImportServiceMap() {
        return mImportServiceMap;
    }

    /**
     * 启动插件
     * <ul>
     * <li>将插件加载到内存</li>
     * <li>执行插件 {@link Application#onCreate()} 中的代码</li>
     * </ul>
     * <b>注意</b><p>
     * 插件启动成功之后，才能调用 {@link PhantomCore} 中启动插件 Activity 的方法
     *
     * @return true 启动成功；false 启动失败
     */
    public boolean start() {
        if (mLock.tryLock()) {
            VLog.v("start tryLock ok");
            try {
                if (mStarted) {
                    VLog.w("PluginInfo#start already started, skip this time");
                    return true;
                }

                final boolean firstStart = isFirstStart();

                Context context = PhantomCore.getInstance().getContext();
                HashMap<String, Object> params = new HashMap<>(1);
                params.put(LogReporter.Key.VERSION_NAME, versionName);
                try {
                    final String tagLoad = packageName;

                    notifyPluginStartStartIfNeeded(this, firstStart);

                    TimingUtils.startTime(tagLoad);
                    loadPlugin(context, firstStart);
                    final String normalizedDuration = TimingUtils.getNormalizedDuration(tagLoad,
                            TimingUtils.SECTION_DURATION_500_MS, TimingUtils.MAX_SECTION_40);
                    trackPluginLoadTime(firstStart, normalizedDuration);

                    mStarted = true;

                    VLog.i("PluginInfo#start ok, first start: %s, pn: %s, vn: %s, cost: %s", firstStart, packageName,
                            versionName, normalizedDuration);

                    LogReporter.reportState(LogReporter.EventId.PLUGIN_LOAD, true, packageName, params);
                    if (firstStart) {
                        LogReporter.reportState(LogReporter.EventId.PLUGIN_LOAD_FIRST, true, packageName, params);
                    }

                    notifyPluginStartSuccessIfNeeded(this, firstStart);
                } catch (Throwable throwable) {
                    final String msg = String.format(Locale.ENGLISH,
                            "PluginInfo#start error, first start: %s, pn: %s, vn: %s",
                            firstStart, packageName, versionName);
                    VLog.w(throwable, msg);
                    LogReporter.reportState(LogReporter.EventId.PLUGIN_LOAD, false, packageName, params);
                    if (firstStart) {
                        LogReporter.reportState(LogReporter.EventId.PLUGIN_LOAD_FIRST, false, packageName, params);
                    }

                    LogReporter.reportLog(String.format(Locale.ENGLISH, "%s_%s.apk, md5: %s", packageName, versionName,
                            FileUtils.calculateMd5(new File(apkPath))));
                    LogReporter.reportUsableSpaceMegabytes();
                    LogReporter.reportException(new LoadPluginException(msg, throwable), null);

                    notifyPluginStartFailIfNeeded(this, firstStart, throwable);

                    // ANDROID_PHANTOM-160 插件启动失败，卸载该插件
                    PhantomCore.getInstance().uninstallPlugin(packageName);
                }

                return mStarted;
            } finally {
                mLock.unlock();
            }
        } else {
            VLog.v("start tryLock fail");
            return false;
        }
    }

    /**
     * 上报插件启动耗时（包含加载插件 dex 和 调用插件 Application#onCreate）
     *
     * @param firstStart 是否是首次启动，首次启动因为需要进行 dex-opt，会比较耗时
     * @param time       通过 {@link TimingUtils#normalizeDuration(long, int, int)} 归一化的耗时描述
     */
    private void trackPluginLoadTime(boolean firstStart, String time) {
        trackLoadTime(firstStart ? LogReporter.EventId.PLUGIN_LOAD_FIRST : LogReporter.EventId.PLUGIN_LOAD, time);
    }

    /**
     * 上报加载插件 dex 耗时
     *
     * @param firstStart 是否是首次启动，首次启动因为需要进行 dex-opt，会比较耗时
     * @param time       通过 {@link TimingUtils#normalizeDuration(long, int, int)} 归一化的耗时描述
     */
    private void trackDexLoadTime(boolean firstStart, String time) {
        trackLoadTime(firstStart ? LogReporter.EventId.PLUGIN_DEX_LOAD_FIRST : LogReporter.EventId.PLUGIN_DEX_LOAD,
                time);
    }

    /**
     * 上报调用插件 Application#onCreate 耗时
     *
     * @param firstStart 是否是首次启动，首次启动因为需要进行 dex-opt，会比较耗时
     * @param time       通过 {@link TimingUtils#normalizeDuration(long, int, int)} 归一化的耗时描述
     */
    private void trackAppCreateTime(boolean firstStart, String time) {
        trackLoadTime(firstStart ? LogReporter.EventId.PLUGIN_APPLICATION_LOAD_FIRST
                : LogReporter.EventId.PLUGIN_APPLICATION_LOAD, time);
    }

    private void trackLoadTime(String eventId, String time) {
        final HashMap<String, Object> params = new HashMap<>(2);
        params.put(LogReporter.Key.TIME, time);
        params.put(LogReporter.Key.VERSION_NAME, versionName);
        LogReporter.reportEvent(eventId, packageName, params);
    }

    /**
     * 获取插件的 ActivityInfo
     *
     * @param componentName the Activity ComponentName
     * @return activity 对应的 ActivityInfo 对象，如果 activity 不存在则返回 null
     */
    @Nullable
    public ActivityInfo getActivityInfo(@NonNull ComponentName componentName) {
        return mActivitiesInfo.get(componentName);
    }

    /**
     * 获取插件的 Resources 对象
     *
     * @return 插件的 Resources 对象
     */
    @Nullable
    public Resources getPluginResources() {
        return mPluginResources;
    }

    /**
     * 获取插件的 ClassLoader 对象
     *
     * @return 插件的 ClassLoader 对象
     */
    @Nullable
    public PluginClassLoader getPluginClassLoader() {
        return mPluginClassLoader;
    }

    /**
     * 获取插件的 AssetManager 对象
     *
     * @return 插件的 AssetManager 对象
     */
    public AssetManager getPluginAssetManager() {
        return mPluginAssetManager;
    }

    /**
     * 插件是否已启动
     *
     * @return true 已启动；false 未启动
     */
    public boolean isStarted() {
        return mStarted;
    }

    private void loadPlugin(Context ctx, boolean firstStart) throws Throwable {
        TimingLogger logger = new TimingLogger(Constants.TAG,
                "PluginInfo#loadPlugin -> " + packageName + ", firstStart: " + firstStart);

        final String tagDexLoad =
                (firstStart ? LogReporter.EventId.PLUGIN_DEX_LOAD_FIRST : LogReporter.EventId.PLUGIN_DEX_LOAD)
                        + packageName;
        TimingUtils.startTime(tagDexLoad);

        final PhantomCore phantomCore = PhantomCore.getInstance();
        boolean turboDexEnabled = phantomCore.isTurboDexEnabled();

        VLog.w("packageName: %s, firstStart: %s, turboDexEnabled: %s", packageName, firstStart, turboDexEnabled);

        // Workaround for ANDROID_PHANTOM-204 异常信息为：创建 DexClassLoader 时，optimizedDirectory 不存在
        // 这里再次确保目录已创建好
        File odexDirFile = new File(odexDir);
        File libDirFile = new File(libPath);
        FileUtils.ensureDirectoryCreated(odexDirFile);
        FileUtils.ensureDirectoryCreated(libDirFile);

        // 首次加载插件是否需要加速 ?
        final boolean shouldBoostFirstDexLoad = firstStart && turboDexEnabled;
        if (shouldBoostFirstDexLoad) {
            // 优化 ART 虚拟机(Android 5.0 及更高版本)冷启动 **首次** 加载插件耗时
            // 1. 使用 alibaba-atlas 中的 ARTUtils 禁用 dexopt
            // 2. 创建插件 ClassLoader 加载插件(这里由于禁用了 dexopt ，耗时会在 1 秒以内)
            // 3. 重新启用 dexopt
            // 4. 在后台线程中进行 dexopt ，提升在后续冷启动 **非首次** 运行插件效率
            ARTUtils.setIsDex2oatEnabled(false);
            mPluginClassLoader = new PluginClassLoader(this, ctx.getClassLoader());
            ARTUtils.setIsDex2oatEnabled(true);
            AsyncTask.execute(new DexOptTask(apkPath, odexPath));
        } else {
            mPluginClassLoader = new PluginClassLoader(this, ctx.getClassLoader());
        }

        trackDexLoadTime(firstStart, TimingUtils.getNormalizedDuration(tagDexLoad, TimingUtils.SECTION_DURATION_500_MS,
                TimingUtils.MAX_SECTION_40));
        logger.addSplit("create plugin classloader, firstStart: " + firstStart);

        mPluginAssetManager = createAssetManager(ctx);

        logger.addSplit("create asset manager");

        mPluginResources = createResources(ctx, mPluginAssetManager);
        logger.addSplit("create resource");

        final String tagAppLoad = (firstStart ? LogReporter.EventId.PLUGIN_APPLICATION_LOAD_FIRST
                : LogReporter.EventId.PLUGIN_APPLICATION_LOAD) + packageName;
        TimingUtils.startTime(tagAppLoad);
        createApplication(ctx.getApplicationContext());
        trackAppCreateTime(firstStart,
                TimingUtils.getNormalizedDuration(tagAppLoad, TimingUtils.SECTION_DURATION_100_MS,
                        TimingUtils.MAX_SECTION_20));
        logger.addSplit("create application");

        registerStaticBroadcastReceiver(ctx);
        logger.addSplit("register static broadcast receiver");

        logger.dumpToLog();
    }

    /**
     * 是否安装插件后首次启动插件，包含以下场景
     * <ul>
     * <li>全新安装</li>
     * <li>升级安装</li>
     * </ul>
     *
     * @return true 首次启动；否则 false
     */
    private boolean isFirstStart() {
        // 通过判断 odex 目录是否有空判断是否插件是首次启动
        return FileUtils.isDirectoryEmpty(new File(odexDir));
    }

    /**
     * 动态注册插件中的静态广播，参考 http://weishu.me/2016/04/12/understand-plugin-framework-receiver/
     */
    private void registerStaticBroadcastReceiver(Context context) {
        final ActivityInfo[] receivers = packageInfo.receivers;
        if (receivers == null) {
            return;
        }

        for (ActivityInfo receiver : receivers) {
            final List<IntentFilter> intentFilters = mReceivers.get(receiver.name);

            if (intentFilters == null || intentFilters.isEmpty()) {
                continue;
            }

            try {
                BroadcastReceiver instance = (BroadcastReceiver) mPluginClassLoader.loadClass(
                        receiver.name).newInstance();
                for (IntentFilter filter : intentFilters) {
                    context.registerReceiver(instance, filter);
                    mGlobalBroadcastReceivers.add(instance);
                }
            } catch (Exception e) {
                LogReporter.reportException(e, null);
                VLog.w(e, "registerStaticBroadcastReceiver error");
            }

        }
    }

    void unregisterStaticBroadcastReceiver(Context context) {
        for (BroadcastReceiver receiver : mGlobalBroadcastReceivers) {
            try {
                context.unregisterReceiver(receiver);
            } catch (Exception e) {
                // just ignore java.lang.IllegalArgumentException when Receiver not registered
                VLog.w(e, "error unregisterReceiver mGlobalBroadcastReceivers");
                LogReporter.reportException(e);
            }
        }
        mGlobalBroadcastReceivers.clear();
    }

    private AssetManager createAssetManager(Context ctx) throws Throwable {
        PackageManager pm = ctx.getPackageManager();
        Resources res = pm.getResourcesForApplication(packageInfo.applicationInfo);
        AssetManager am = res.getAssets();
        return am;
    }

    private Resources createResources(Context ctx, AssetManager assetManager) {
        return new ResourcesProxy(assetManager, ctx.getResources().getDisplayMetrics(),
                ctx.getResources().getConfiguration(), packageName);
    }


    private void createApplication(Context appContext) throws Throwable {
        if (null != mApplication) {
            VLog.w("application has already been created, skip this time");
            return;
        }
        mApplication = new ApplicationHostProxy(appContext, this);
        mApplication.callApplicationOnCreateInUiThread();
    }

    /**
     * 获取插件的 Application 对象
     *
     * @return 插件的 Application 对象
     */
    public Application getApplication() {
        return null == mApplication ? null : mApplication.getPluginApplication();
    }

    @SuppressWarnings({"PMD.ConsecutiveAppendsShouldReuse", "PMD.InsufficientStringBufferDeclaration",
            "PMD.ConsecutiveLiteralAppends"})
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PluginInfo{");
        sb.append("packageName='").append(packageName).append('\'');
        sb.append(", apkPath='").append(apkPath).append('\'');
        sb.append(", versionName='").append(versionName).append('\'');
        sb.append(", versionCode=").append(versionCode);
        sb.append('}');
        return sb.toString();
    }

    private void notifyPluginStartStartIfNeeded(@NonNull PluginInfo pluginInfo, boolean firstStart) {
        final PhantomEventCallback callback = PhantomCore.getInstance().getPhantomEventCallback();
        if (callback != null) {
            callback.onPluginStartStart(pluginInfo, firstStart);
        }
    }

    private void notifyPluginStartSuccessIfNeeded(@NonNull PluginInfo pluginInfo, boolean firstStart) {
        final PhantomEventCallback callback = PhantomCore.getInstance().getPhantomEventCallback();
        if (callback != null) {
            callback.onPluginStartSuccess(pluginInfo, firstStart);
        }
    }

    private void notifyPluginStartFailIfNeeded(@NonNull PluginInfo pluginInfo, boolean firstStart,
            @NonNull Throwable throwable) {
        final PhantomEventCallback callback = PhantomCore.getInstance().getPhantomEventCallback();
        if (callback != null) {
            callback.onPluginStartFail(pluginInfo, firstStart, throwable);
        }
    }

    /**
     * 在后台线程中进行 dexopt，以减少非首次加载插件的耗时
     */
    private static class DexOptTask implements Runnable {

        private final String mSourcePathName;
        private final String mOutputPathName;

        private final TimingLogger mTimingLogger;

        DexOptTask(String sourcePathName, String outputPathName) {
            mTimingLogger = new TimingLogger(Constants.TAG, "DexOptTask");
            mSourcePathName = sourcePathName;
            mOutputPathName = outputPathName;
        }

        @Override
        public void run() {
            DexFile dexFile = null;
            try {
                mTimingLogger.addSplit("DexFile.loadDex E: " + mSourcePathName);
                ARTUtils.setIsDex2oatEnabled(true);
                dexFile = DexFile.loadDex(mSourcePathName, mOutputPathName, 0);
                mTimingLogger.addSplit("DexFile.loadDex X");
            } catch (IOException e) {
                VLog.w(e, "DexFile.loadDex error");
                mTimingLogger.addSplit("Error");
            } finally {
                if (dexFile != null) {
                    try {
                        dexFile.close();
                    } catch (IOException e) {
                        // nothing can be done, just ignore it
                    }
                }
            }
            mTimingLogger.dumpToLog();
        }
    }
}
