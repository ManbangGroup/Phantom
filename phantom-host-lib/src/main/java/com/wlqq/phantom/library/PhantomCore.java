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

package com.wlqq.phantom.library;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.ConditionVariable;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.TimingLogger;

import com.taobao.android.dex.interpret.ARTUtils;
import com.wlqq.phantom.communication.PhantomServiceManager;
import com.wlqq.phantom.communication.PhantomService;
import com.wlqq.phantom.communication.PhantomServiceIndex;
import com.wlqq.phantom.communication.PhantomUtils;
import com.wlqq.phantom.library.env.Constants;
import com.wlqq.phantom.library.log.ILogReporter;
import com.wlqq.phantom.library.log.LogReporter;
import com.wlqq.phantom.library.pm.InstallPluginException;
import com.wlqq.phantom.library.pm.InstallResult;
import com.wlqq.phantom.library.pm.PluginInfo;
import com.wlqq.phantom.library.pm.PluginManager;
import com.wlqq.phantom.library.pool.LaunchModeManager;
import com.wlqq.phantom.library.proxy.PhantomUtilsImpl;
import com.wlqq.phantom.library.proxy.PluginContext;
import com.wlqq.phantom.library.utils.IntentUtils;
import com.wlqq.phantom.library.utils.TimingUtils;
import com.wlqq.phantom.library.utils.VLog;
import com.wlqq.phantom.library.utils.VmUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Phantom 对外核心类，提供了主要的公开 API
 * <ul>
 * <li>插件框架初始化</li>
 * <li>插件安装</li>
 * <li>插件升级</li>
 * <li>插件卸载</li>
 * <li>获取已安装的插件</li>
 * <li>启动插件中的 {@link Activity}</li>
 * </ul>
 */
@SuppressWarnings("PMD.ConfusingTernary")
public class PhantomCore {
    private static final String TAG = Constants.TAG;

    private volatile boolean mInitialized;
    private ConditionVariable mPluginManagerInitialized;
    private Context mContext;
    private String mHostPkgName;
    private PluginManager mPluginManager;
    @Nullable
    private PhantomEventCallback mPhantomEventCallback;

    private boolean mCheckVersion;
    private boolean mCheckSignature;
    private boolean mPreloadAsync;
    private boolean mTurboDexEnabled;

    /**
     * 创建 Phantom 实例
     */
    private PhantomCore() {
    }

    /**
     * 获取 {@link PhantomCore} 单例对象
     *
     * @return {@link PhantomCore} 单例对象
     */
    public static PhantomCore getInstance() {
        return LazyHolder.INSTANCE;
    }

    /**
     * 获取插件框架版本名
     *
     * @return 插件框架版本名
     */
    public static String getVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * 获取插件框架版本号
     *
     * @return 插件框架版本号
     */
    public static int getVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    /**
     * 初始化 Phantom，建议在应用的 {@link Application#onCreate()} 方法中调用。
     * 在调用其它实例方法之前，必须先调用该方法。
     *
     * @param context the application context
     * @param config  Phantom 配置对象
     */
    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    public synchronized void init(@NonNull Context context, @NonNull final Config config) {
        if (mInitialized) {
            VLog.w("already initialized, skip this time");
            return;
        }

        try {
            TimingUtils.startTime(TAG);

            TimingLogger logger = new TimingLogger(TAG, "PhantomCore init");

            mCheckVersion = config.mCheckVersion;
            mCheckSignature = config.mCheckSignature;
            mPreloadAsync = config.mPreloadAsync;
            mTurboDexEnabled = VmUtils.isVmSupportTurboDex() && config.mTurboDexEnabled;

            LogReporter.setImpl(config.mLogReporter);

            VLog.setTag(TAG);
            VLog.setLevel(config.mLogLevel);

            mPhantomEventCallback = config.mPhantomEventCallback;

            logger.addSplit("init config");

            mContext = context.getApplicationContext();
            mHostPkgName = context.getPackageName();
            final PackageInfo hostPkgInfo = context.getPackageManager().getPackageInfo(mHostPkgName,
                    PackageManager.GET_PROVIDERS);

            PhantomServiceManager.init(mHostPkgName, hostPkgInfo.versionName, hostPkgInfo.versionCode,
                    BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
            PhantomUtils.setImpl(new PhantomUtilsImpl());

            // 注册自动生成的 PhantomServiceIndex 提供的服务
            for (PhantomServiceIndex index : config.mPhantomServiceIndices) {
                for (Object phantomService : index.getPhantomServiceList()) {
                    PhantomServiceManager.registerService(phantomService);
                }
            }
            // 注册用户手动添加的服务
            for (Object phantomService : config.mPhantomServices) {
                PhantomServiceManager.registerService(phantomService);
            }
            // 注册 PhantomLib 内部提供的服务
            PhantomServiceManager.registerService(new PhantomVersionService());
            logger.addSplit("PhantomServiceManager init");

            if (mTurboDexEnabled) {
                ARTUtils.init(mContext);
                logger.addSplit("ARTUtils.init");
            }

            mPluginManager = PluginManager.getInstance();
            mPluginManagerInitialized = new ConditionVariable(false);
            if (mPreloadAsync) {
                new Thread() {
                    @Override
                    public void run() {
                        mPluginManager.init(mContext, config.mTrustedSignatures);
                        mPluginManagerInitialized.open();
                    }
                }.start();
                logger.addSplit("PluginManager init async");
            } else {
                mPluginManager.init(mContext, config.mTrustedSignatures);
                mPluginManagerInitialized.open();
                logger.addSplit("PluginManager init sync");
            }

            LaunchModeManager.getInstance().init(mContext);
            logger.addSplit("LaunchModeManager init");

            // 调试模式
            if (config.mDebug) {
                // 启用调试服务
                DebugReceiver.init(mContext);
                logger.addSplit("DebugService init");
            }

            logger.dumpToLog();

            mInitialized = true;

            HashMap<String, Object> params = new HashMap<>();
            params.put(LogReporter.Key.TIME,
                    TimingUtils.getNormalizedDuration(TAG, TimingUtils.SECTION_DURATION_20_MS,
                            TimingUtils.MAX_SECTION_50));
            LogReporter.reportState(LogReporter.EventId.PHANTOM_INIT, true, params);

            VLog.i("PhantomCore init ok");
        } catch (Exception e) {
            LogReporter.reportException(e, null);
            HashMap<String, Object> params = new HashMap<>();
            params.put(LogReporter.Key.MESSAGE, e.getMessage());
            LogReporter.reportState(LogReporter.EventId.PHANTOM_INIT, false, params);

            VLog.w(e, "PhantomCore init error");
        }
    }

    /**
     * 从宿主 Context 中启动插件中的 Activity 必须显式指定 {@link ComponentName}
     *
     * @param context The context to use
     * @param intent  插件 Activity，必须显式指定 {@link ComponentName}
     */
    public void startActivity(@NonNull Context context, @NonNull Intent intent) {
        checkInit();
        Intent proxyIntent = IntentUtils.wrapToActivityHostProxyIntentIfNeeded(intent);
        if (!(context instanceof Activity)) {
            proxyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(proxyIntent);
    }

    /**
     * 从宿主 android.app.Fragment 中启动插件中的 Activity 必须显式指定 {@link ComponentName}
     *
     * @param fragment The fragment to use
     * @param intent   插件 Activity，必须显式指定 {@link ComponentName}
     */
    public void startActivity(@NonNull android.app.Fragment fragment, @NonNull Intent intent) {
        checkInit();
        fragment.startActivity(IntentUtils.wrapToActivityHostProxyIntentIfNeeded(intent));
    }

    /**
     * 从宿主 android.support.v4.app.Fragment 中启动插件中的 Activity 必须显式指定 {@link ComponentName}
     *
     * @param fragment The fragment to use
     * @param intent   插件 Activity，必须显式指定 {@link ComponentName}
     */
    public void startActivity(@NonNull android.support.v4.app.Fragment fragment, @NonNull Intent intent) {
        checkInit();
        fragment.startActivity(IntentUtils.wrapToActivityHostProxyIntentIfNeeded(intent));
    }

    /**
     * 从宿主 Activity 中启动 {@link Activity#startActivityForResult(Intent, int)}, 插件中的 Activity 必须显式指定 {@link ComponentName}
     *
     * @param activity    The activity to use
     * @param intent      插件 Activity，必须显式指定 {@link ComponentName}
     * @param requestCode If {@literal requestCode >= 0}, this code will be returned in onActivityResult() when the
     *                    activity exits.
     */
    public void startActivityForResult(@NonNull Activity activity, @NonNull Intent intent, int requestCode) {
        checkInit();
        activity.startActivityForResult(IntentUtils.wrapToActivityHostProxyIntentIfNeeded(intent), requestCode);
    }

    /**
     * 从宿主 android.app.Fragment 中启动 {@link Activity#startActivityForResult(Intent, int)}, 插件中的 Activity 必须显式
     * 指定 {@link ComponentName}
     *
     * @param fragment    The fragment to use
     * @param intent      插件 Activity，必须显式指定 {@link ComponentName}
     * @param requestCode the request code
     */
    public void startActivityForResult(@NonNull android.app.Fragment fragment, @NonNull Intent intent,
            int requestCode) {
        checkInit();
        fragment.startActivityForResult(IntentUtils.wrapToActivityHostProxyIntentIfNeeded(intent), requestCode);
    }

    /**
     * 从宿主 android.support.v4.app.Fragment 中启动 {@link Activity#startActivityForResult(Intent, int)}, 插件中的 Activity 必须显式
     * 指定 {@link ComponentName}
     *
     * @param fragment    The fragment to use
     * @param intent      插件 Activity，必须显式指定 {@link ComponentName}
     * @param requestCode the request code
     */
    public void startActivityForResult(@NonNull android.support.v4.app.Fragment fragment, @NonNull Intent intent,
            int requestCode) {
        checkInit();
        fragment.startActivityForResult(IntentUtils.wrapToActivityHostProxyIntentIfNeeded(intent), requestCode);
    }

    /**
     * 判断是否已经初始化完成
     *
     * @return 若已初始化完成，则返回 true；否则返回 false
     */
    public boolean isInitialized() {
        return mInitialized;
    }

    /**
     * 获取宿主的 Context
     *
     * @return 宿主的 Context
     */
    public Context getContext() {
        checkInit();
        return mContext;
    }

    /**
     * 获取宿主的包名
     *
     * @return 宿主的包名
     */
    public String getHostPkg() {
        checkInit();
        return mHostPkgName;
    }

    /**
     * 获取宿主 compile 依赖的 maven 库坐标集合
     * <p>
     * 坐标格式 <code>groupId:artifactId:version</code>，例如：<code>com.android.support:support-v4:25.3.1</code>
     *
     * @return 宿主 compile 依赖的 maven 库坐标集合
     */
    @NonNull
    public Set<String> getHostCompileDependencies() {
        checkInit();
        waitForPluginManagerInitCompletion();

        return mPluginManager.getHostCompileDependencySet();
    }

    /**
     * 是否加速首次 dex load
     *
     * @return 若开启，则返回 true；否则返回 false
     */
    public boolean isTurboDexEnabled() {
        checkInit();
        return mTurboDexEnabled;
    }

    /**
     * 获取所有已安装插件的信息列表
     *
     * @return 已安装插件信息列表
     * @see PluginInfo
     */
    @NonNull
    public List<PluginInfo> getAllPlugins() {
        checkInit();
        waitForPluginManagerInitCompletion();

        return mPluginManager.getAllPlugins();
    }

    /**
     * 获取已安装的插件数量
     *
     * @return 已安装的插件数量
     */
    public int getPluginCount() {
        checkInit();
        waitForPluginManagerInitCompletion();

        return mPluginManager.getPluginCount();
    }

    /**
     * 判断插件是否已安装
     *
     * @param packageName 插件的包名
     * @return true 若已安装；否则 false
     */
    public boolean isPluginInstalled(String packageName) {
        checkInit();
        waitForPluginManagerInitCompletion();

        return mPluginManager.isPluginInstalled(packageName);
    }

    /**
     * 安装 assets 中的插件
     *
     * @param assetsApkPath 位于 assets 中的安装包文件路径（相对于 assets 根目录，比如：<code>"plugins/com.wlqq.phantom.plugin
     *                      .test1_1.0.0.apk"</code>）
     * @return 插件安装结果
     */
    public InstallResult installPluginFromAssets(String assetsApkPath) {
        checkInit();
        waitForPluginManagerInitCompletion();

        notifyPluginInstallStartIfNeeded(assetsApkPath, true);

        TimingUtils.startTime(assetsApkPath);

        final InstallResult result = mPluginManager.installPluginFromAssets(assetsApkPath, mCheckVersion,
                mCheckSignature);

        trackInstallResult(result, assetsApkPath, mCheckVersion, mCheckSignature, true);

        notifyPluginInstallResultIfNeeded(assetsApkPath, true, result);

        return result;
    }

    /**
     * 安装插件
     *
     * @param apkPath     插件安装包文件绝对路径
     * @param packageName 插件包名
     * @param versionName 插件版本名
     * @return 插件安装结果
     */
    public InstallResult installPlugin(String apkPath, String packageName, String versionName) {
        return installPlugin(apkPath, packageName, versionName, false);
    }

    // internal use only
    InstallResult installPlugin(String apkPath, String packageName, String versionName, boolean forceReplace) {
        checkInit();
        waitForPluginManagerInitCompletion();

        String name = String.format(Locale.ENGLISH, "%s_%s.apk", packageName, versionName);

        notifyPluginInstallStartIfNeeded(name, false);

        TimingUtils.startTime(name);

        final InstallResult result = mPluginManager.installPlugin(apkPath, mCheckVersion, mCheckSignature,
                forceReplace);

        trackInstallResult(result, name, mCheckVersion, mCheckSignature, false);

        notifyPluginInstallResultIfNeeded(name, false, result);

        return result;
    }

    /**
     * 安装插件
     *
     * @param apkPath 安装包绝对路径
     * @return 插件安装结果
     */
    public InstallResult installPlugin(String apkPath) {
        checkInit();
        waitForPluginManagerInitCompletion();

        final String name = new File(apkPath).getName();

        notifyPluginInstallStartIfNeeded(name, false);

        TimingUtils.startTime(name);

        final InstallResult result = mPluginManager.installPlugin(apkPath, mCheckVersion,
                mCheckSignature);

        trackInstallResult(result, name, mCheckVersion, mCheckSignature, false);

        notifyPluginInstallResultIfNeeded(name, false, result);

        return result;
    }

    private void trackInstallResult(InstallResult installResult, String name, boolean checkVersion,
            boolean checkSignatures, boolean fromAssets) {
        HashMap<String, Object> params = new HashMap<>(3);
        params.put(LogReporter.Key.CHECK_VERSION, String.valueOf(checkVersion));
        params.put(LogReporter.Key.CHECK_SIGNATURE, String.valueOf(checkSignatures));
        params.put(LogReporter.Key.FROM_ASSETS, String.valueOf(fromAssets));
        params.put(LogReporter.Key.STATUS, String.valueOf(installResult.status));
        final String normalizedDuration = TimingUtils.getNormalizedDuration(name,
                TimingUtils.SECTION_DURATION_100_MS, TimingUtils.MAX_SECTION_20);
        params.put(LogReporter.Key.TIME, normalizedDuration);

        if (installResult.isSuccess() && installResult.plugin != null) {
            params.put(LogReporter.Key.VERSION_NAME, installResult.plugin.versionName);
        } else {
            LogReporter.reportUsableSpaceMegabytes();
            LogReporter.reportException(
                    installResult.throwable != null ? new InstallPluginException(installResult.throwable)
                            : new InstallPluginException(installResult.message));
        }

        if (fromAssets) {
            LogReporter.reportState(LogReporter.EventId.PLUGIN_INSTALL, installResult.isSuccess(),
                    LogReporter.Label.ASSETS_PREFIX + name, params);
        } else {
            LogReporter.reportState(LogReporter.EventId.PLUGIN_INSTALL, installResult.isSuccess(),
                    LogReporter.Label.FILE_PREFIX + name, params);
        }

        VLog.i("install: %s, result: %s, cost: %s, checkVersion: %s, checkSignature: %s, "
                        + "fromAssets: %s",
                name, installResult, normalizedDuration, checkVersion, checkSignatures, fromAssets);
    }

    /**
     * 卸载指定的插件
     *
     * @param packageName 插件包名
     * @return true 若成功；false 失败
     */
    public boolean uninstallPlugin(String packageName) {
        VLog.i("uninstallPlugin E: %s", packageName);
        checkInit();
        waitForPluginManagerInitCompletion();

        boolean ret = mPluginManager.uninstallPlugin(packageName);
        VLog.i("uninstallPlugin X: %s, ret: %s", packageName, ret);
        return ret;
    }

    /**
     * 卸载所有的插件
     */
    public void uninstallAllPlugins() {
        VLog.i("uninstallAllPlugins E");
        checkInit();
        waitForPluginManagerInitCompletion();

        mPluginManager.uninstallAllPlugins();
        VLog.i("uninstallAllPlugins X");
    }

    /**
     * 通过包名获取对应插件的信息
     *
     * @param packageName 插件包名
     * @return 插件信息
     */
    @Nullable
    public PluginInfo findPluginInfoByPackageName(String packageName) {
        checkInit();
        waitForPluginManagerInitCompletion();

        return mPluginManager.findPluginByPackageName(packageName);
    }

    /**
     * 获取 activity 组件所属插件的信息
     *
     * @param activityName activity 组件名
     * @return 插件信息
     */
    @Nullable
    public PluginInfo findPluginInfoByActivityName(@NonNull ComponentName activityName) {
        checkInit();
        waitForPluginManagerInitCompletion();

        return mPluginManager.findPluginByActivityName(activityName);
    }

    /**
     * 获取 service 组件所属插件的信息
     *
     * @param serviceName service 组件名
     * @return 插件信息
     */
    @Nullable
    public PluginInfo findPluginInfoByServiceName(@NonNull ComponentName serviceName) {
        checkInit();
        waitForPluginManagerInitCompletion();

        return mPluginManager.findPluginByServiceName(serviceName);
    }

    /**
     * 获取 activity 组件名对应的 ActivityInfo
     *
     * @param activityName activity 组件名
     * @return 对应的 ActivityInfo
     */
    public ActivityInfo findActivityInfo(ComponentName activityName) {
        checkInit();
        waitForPluginManagerInitCompletion();

        return mPluginManager.findActivityInfo(activityName);
    }

    /**
     * 确保 {@link #init(Context, Config)} 被调用
     *
     * @throws IllegalStateException 若初始化方法 {@link #init(Context, Config)} 还没有被调用
     */
    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    private synchronized void checkInit() throws IllegalStateException {
        if (!mInitialized) {
            throw new IllegalStateException("should call init first!!!");
        }
    }

    /**
     * 等待 {@link PluginManager#init(Context, List)} 完成。若没有完成，则调用线程会阻塞
     */
    private void waitForPluginManagerInitCompletion() {
        // 只有在异步加载的情况下才需要等待
        if (mPreloadAsync) {
            final long begin = SystemClock.elapsedRealtime();
            mPluginManagerInitialized.block();
            VLog.v("waitForPluginManagerInitCompletion cost ms: %d",
                    (SystemClock.elapsedRealtime() - begin));
        }
    }

    /**
     * 创建指定插件的 Context
     *
     * @param activity          宿主的某个 Activity
     * @param pluginPackageName 插件包名
     * @return 插件的 Context
     */
    @Nullable
    public Context createPluginContext(@NonNull Activity activity, @NonNull String pluginPackageName) {
        checkInit();

        PluginInfo pluginInfo = findPluginInfoByPackageName(pluginPackageName);
        if (null == pluginInfo) {
            HashMap<String, Object> params = new HashMap<>();
            params.put(LogReporter.Key.PACKAGE_NAME, pluginPackageName);
            params.put(LogReporter.Key.MESSAGE, "findPluginByPackageName return null");
            LogReporter.reportState(LogReporter.EventId.PLUGIN_CONTEXT_CREATE, false,
                    LogReporter.Label.FAIL + pluginPackageName, params);
            return null;
        }

        return new PluginContext(activity, pluginInfo).createContext();
    }

    /**
     * 返回用户设置的 {@link PhantomEventCallback}
     *
     * @return 用户设置的 {@link PhantomEventCallback}
     */
    @Nullable
    public PhantomEventCallback getPhantomEventCallback() {
        return mPhantomEventCallback;
    }

    private void notifyPluginInstallStartIfNeeded(String name, boolean fromAssets) {
        if (mPhantomEventCallback != null) {
            mPhantomEventCallback.onPluginInstallStart(name, fromAssets);
        }
    }

    private void notifyPluginInstallResultIfNeeded(String name, boolean fromAssets,
            @NonNull InstallResult installResult) {
        if (mPhantomEventCallback != null) {
            if (installResult.isSuccess()) {
                mPhantomEventCallback.onPluginInstallSuccess(name, fromAssets, installResult);
            } else {
                mPhantomEventCallback.onPluginInstallFail(name, fromAssets, installResult);
            }
        }
    }

    @SuppressWarnings("PMD.AccessorClassGeneration")
    private static class LazyHolder {
        @SuppressLint("StaticFieldLeak")
        static final PhantomCore INSTANCE = new PhantomCore();
    }

    /**
     * PhantomCore SDK 初始化配置参数
     */
    public static class Config {
        private final List<Object> mPhantomServices;
        private final List<PhantomServiceIndex> mPhantomServiceIndices;
        private final List<String> mTrustedSignatures;
        private int mLogLevel;
        private boolean mCheckVersion;
        private boolean mCheckSignature;
        private boolean mPreloadAsync;
        private boolean mTurboDexEnabled;
        private boolean mDebug;

        private ILogReporter mLogReporter;
        private PhantomEventCallback mPhantomEventCallback;

        /**
         * 默认配置
         * <ul>
         * <li>日志级别 {@linkplain Log#WARN}</li>
         * <li>检查插件版本 true</li>
         * <li>校验插件签名 true</li>
         * <li>异步解析已安装的插件 true</li>
         * <li>优化首次加载插件速度 true</li>
         * <li>调试模式 false</li>
         * </ul>
         */
        public Config() {
            mLogLevel = Log.WARN;
            mCheckVersion = true;
            mCheckSignature = true;
            mPreloadAsync = true;
            mTurboDexEnabled = true;
            mPhantomServices = new ArrayList<>();
            mPhantomServiceIndices = new ArrayList<>();
            mTrustedSignatures = new ArrayList<>();
        }

        /**
         * 设置调试模式。在调试模式下
         * <ul>
         * <li>插件生命周期方法调用中的异常不会被捕获，应用会崩溃</li>
         * <li>支持插件快速部署到宿主</li>
         * </ul>
         * <p>默认 false</p>
         *
         * @param debug 是否开启调试模式
         * @return 该配置对象
         */
        public Config setDebug(boolean debug) {
            mDebug = debug;
            return this;
        }

        /**
         * 设置日志级别；默认 {@link android.util.Log#WARN}
         *
         * @param logLevel 见 {@link android.util.Log}
         * @return 该配置对象
         */
        public Config setLogLevel(int logLevel) {
            mLogLevel = logLevel;
            return this;
        }

        /**
         * 安装插件时是否检查版本号；若为 true，则仅支持升级安装；默认 true
         *
         * @param checkVersion 是否检查版本号
         * @return 该配置对象
         */
        public Config setCheckVersion(boolean checkVersion) {
            mCheckVersion = checkVersion;
            return this;
        }

        /**
         * 安装插件时是否校验签名；若为 true, 则插件与宿主签名一致才能安装；默认 true
         *
         * @param checkSignature 是否校验签名
         * @return 该配置对象
         */
        public Config setCheckSignature(boolean checkSignature) {
            mCheckSignature = checkSignature;
            return this;
        }

        /**
         * 是否优化首次加载插件速度；若为 true, 首次加载插件禁用 dex2oat ，而以解释执行的方式运行
         * <p>
         * <b>NOTE:</b>由于是以 hack 的方式实现，可能在部分设备上存在兼容性问题
         * </p>
         * 默认 true
         *
         * @param enabled 是否启用快速加载插件
         * @return 该配置对象
         */
        public Config setTurboDexEnabled(boolean enabled) {
            mTurboDexEnabled = enabled;
            return this;
        }

        /**
         * 设置数据统计上报类对象
         *
         * @param logReporter 数据统计上报类对象
         * @return 该配置对象
         */
        public Config setLogReporter(@NonNull ILogReporter logReporter) {
            mLogReporter = logReporter;
            return this;
        }

        /**
         * 设置是否在 SDK 初始化时<b>异步执行</b>解析已安装的插件 apk 信息
         * <ul>
         * <li>true - 异步加载，SDK 初始化时间会减小，但后续首次调用插件管理的 API (安装/卸载/查询) 可能会 block (毫秒级)</li>
         * <li>false - 同步加载，SDK 初始化时间会增大（随已安装的插件 apk 数量线性增长），不会影响后续调用插件管理的 API (安装/卸载/查询) </li>
         * </ul>
         * 默认为 <b>true</b>
         *
         * @param preloadAsync 是否异步加载，默认为 <b>true</b>
         * @return 该配置对象
         */
        public Config setPreloadAsync(boolean preloadAsync) {
            mPreloadAsync = preloadAsync;
            return this;
        }

        /**
         * 添加宿主提供的供插件调用的服务对象，服务对象类必须使用 {@link PhantomService} 注解
         *
         * @param phantomService 服务对象
         * @return 该配置对象
         */
        public Config addPhantomService(@NonNull Object phantomService) {
            mPhantomServices.add(phantomService);
            return this;
        }

        /**
         * 添加宿主提供的供插件调用的服务索引
         *
         * @param index 服务索引
         * @return 该配置对象
         */
        public Config addPhantomServiceIndex(@NonNull PhantomServiceIndex index) {
            mPhantomServiceIndices.add(index);
            return this;
        }

        /**
         * 添加信任的插件签名 MD5 列表。插件安装时，检查插件安装包签名 MD5 是否在信任的列表中。若不在，安装插件会失败。
         *
         * @param signatures 信任的插件签名 MD5 列表
         * @return 该配置对象
         * @see InstallResult#ERR_SIGNATURE_MISMATCH
         */
        public Config addTrustedSignatures(@NonNull String... signatures) {
            Collections.addAll(mTrustedSignatures, signatures);
            return this;
        }

        /**
         * 设置 Phantom 事件通知回调
         *
         * @param phantomEventCallback Phantom 事件通知回调
         * @return 该配置对象
         */
        public Config setPhantomEventCallback(@NonNull PhantomEventCallback phantomEventCallback) {
            mPhantomEventCallback = phantomEventCallback;
            return this;
        }
    }
}
