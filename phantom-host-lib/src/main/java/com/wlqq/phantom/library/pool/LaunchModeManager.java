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

package com.wlqq.phantom.library.pool;


import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.wlqq.phantom.library.PhantomCore;
import com.wlqq.phantom.library.log.LogReporter;
import com.wlqq.phantom.library.pm.PluginInfo;
import com.wlqq.phantom.library.utils.VLog;

/**
 * Launch mode 管理器
 */
public class LaunchModeManager {
    private static final String PACKAGE_NAME = "com.wlqq.phantom.library.proxy";

    private static final String MODE_STANDARD = PACKAGE_NAME + ".ActivityHostProxy";
    private static final String MODE_STANDARD_TRANSLUCENT = MODE_STANDARD + "$ActivityProxyTranslucent";
    private static final String MODE_SINGLE_TOP_PREFIX = MODE_STANDARD + "$ActivityProxySingleTop";
    private static final String MODE_SINGLE_INSTANCE_PREFIX = MODE_STANDARD + "$ActivityProxySingleInstance";
    private static final String MODE_SINGLE_TASK_PREFIX = MODE_STANDARD + "$ActivityProxySingleTask";

    private static final int SINGLE_TOP_COUNT = 50;
    private static final int SINGLE_INSTANCE_COUNT = 30;
    private static final int SINGLE_TASK_COUNT = 50;

    private ActivityPool mSingleTopPool;
    private ActivityPool mSingleInstancePool;
    private ActivityPool mSingleTaskPool;

    private FixedActivityCache mCache;
    private boolean mInitialized;
    private Context mContext;

    @SuppressWarnings("PMD.AccessorClassGeneration")
    private static class LazyHolder {
        @SuppressLint("StaticFieldLeak")
        static final LaunchModeManager INSTANCE = new LaunchModeManager();
    }

    public static LaunchModeManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private LaunchModeManager() {
    }

    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    public synchronized void init(Context context) {
        if (mInitialized) {
            return;
        }

        mContext = context;
        mCache = new FixedActivityCache(context);
        cleanCacheIfNeeded();
        mCache.init();
        this.mSingleInstancePool = new ActivityPool(SINGLE_INSTANCE_COUNT, MODE_SINGLE_INSTANCE_PREFIX,
                mCache.getSingleInstanceActivities());
        this.mSingleTaskPool = new ActivityPool(SINGLE_TASK_COUNT, MODE_SINGLE_TASK_PREFIX,
                mCache.getSingleTaskActivities());
        this.mSingleTopPool = new ActivityPool(SINGLE_TOP_COUNT, MODE_SINGLE_TOP_PREFIX,
                mCache.getSingleTopActivities());

        mInitialized = true;
    }

    public String resolveActivity(String pluginActivity, int launchMode) throws ProxyActivityLessException {
        return findActivity(pluginActivity, launchMode, false);
    }

    /**
     * 主要用于发送通知时建立占位activity与插件activity固定的映射关系
     * 如果应用不能发送通知，则不需要建立固定映射关系
     *
     * @param pluginActivity 插件Activity全名，格式为：packageName/className
     * @param launchMode     Activity启动模式
     * @return 返回占位activity
     * @throws ProxyActivityLessException 代理不够使用
     */
    @SuppressWarnings("unused")
    public String resolveFixedActivity(String pluginActivity, int launchMode) throws ProxyActivityLessException {
        boolean canUseNotification = hasNotificationRights();
        String activity = findActivity(pluginActivity, launchMode, canUseNotification);
        if (canUseNotification) {
            mCache.save(new FixedActivity(activity, pluginActivity), launchMode);
        }
        return activity;
    }

    /**
     * 如果应用启动的时候通知栏没有关于应用的通知，就删除为通知建立的占位Activity的固定映射关系.
     * 将占位activity空出来供其他activity使用
     * 但只有6.0及以上系统才有用。4.3到5.1系统需要特殊权限才能访问通知栏，这里不做兼容
     */
    private void cleanCacheIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            try {
                // 1. 目前发现在OPPO R9S PLUST，OPPO R9SK，OPPO R9S，OPPO A57机器上 getActiveNotifications()
                //    可能抛出 NullPointerException 导致 Phantom 初始化失败(bugly显示程序都是在后台)
                // 2. 双开软件软件会导致 NotificationManager.getActiveNotifications 出现 SecurityException 。例如：
                //    https://bugly.qq.com/v2/crash-reporting/errors/900016079/201216/report?pid=1
                StatusBarNotification[] sbn = null == nm ? null : nm.getActiveNotifications();
                VLog.w("cleanCacheIfNeeded active notifications count is %d", null == sbn ? -1 : sbn.length);

                if (null != sbn && sbn.length == 0) {
                    mCache.clean();
                }
            } catch (Exception e) {
                VLog.w(e, "NotificationManager.getActiveNotifications error!");
                LogReporter.reportException(e);
            }
        }
    }

    private boolean hasNotificationRights() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            return null != nm && nm.areNotificationsEnabled();
        }

        return true;
    }

    /**
     * 获取一个占位activity
     *
     * @param pluginActivity 插件activity
     * @param launchMode     插件activity启动模式
     * @param isFixed        是否建立固定映射关系
     * @return 占位activity
     * @throws ProxyActivityLessException 占位activity不够异常
     */
    private String findActivity(String pluginActivity, int launchMode, boolean isFixed)
            throws ProxyActivityLessException {

        String activity = MODE_STANDARD;
        ActivityPool pool = null;

        switch (launchMode) {
            case ActivityInfo.LAUNCH_MULTIPLE:
                final PhantomCore phantomCore = PhantomCore.getInstance();
                final ComponentName componentName = ComponentName.unflattenFromString(pluginActivity);
                if (componentName != null) {
                    final ActivityInfo ai = phantomCore.findActivityInfo(componentName);
                    final PluginInfo pluginInfo = phantomCore.findPluginInfoByActivityName(componentName);
                    final int themeResourceId = ai == null ? -1 : ai.getThemeResource();
                    if (themeResourceId != -1 && pluginInfo != null) {
                        final Resources resources = pluginInfo.getPluginResources();
                        if (resources != null) {
                            final Resources.Theme theme = resources.newTheme();
                            theme.applyStyle(themeResourceId, true);
                            final TypedArray sa = theme.obtainStyledAttributes(
                                    new int[]{android.R.attr.windowIsTranslucent});
                            final boolean translucent = sa.getBoolean(0, false);
                            sa.recycle();
                            activity = translucent ? MODE_STANDARD_TRANSLUCENT : MODE_STANDARD;
                        }
                    }
                }
                break;
            case ActivityInfo.LAUNCH_SINGLE_TOP:
                pool = mSingleTopPool;
                break;
            case ActivityInfo.LAUNCH_SINGLE_INSTANCE:
                pool = mSingleInstancePool;
                break;
            case ActivityInfo.LAUNCH_SINGLE_TASK:
                pool = mSingleTaskPool;
                break;
            default:
                break;
        }

        if (null != pool) {
            activity = isFixed ? pool.resolveFixedActivity(pluginActivity) : pool.resolveActivity(pluginActivity);
        }

        String msg = String.format("resolve %s Activity for %s proxy is %s, fixed is %s",
                launchModeToString(launchMode), pluginActivity, activity, isFixed);
        VLog.d(msg);
        if (null == activity) {
            //占位activity不够使用, 这种情况不做处理，宿主提供足够的占位activity
            //这里不做主动回收，如果做主动回收可能会使程序正常执行流程发送改变
            ProxyActivityLessException pae = new ProxyActivityLessException(msg);
            LogReporter.reportException(pae, null);
            mCache.clean();
            throw pae;
        }

        return activity;
    }

    private String launchModeToString(int launchMode) {
        String mode = "standard";
        if (launchMode == ActivityInfo.LAUNCH_SINGLE_TOP) {
            mode = "singleTop";
        } else if (launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
            mode = "singleInstance";
        } else if (launchMode == ActivityInfo.LAUNCH_SINGLE_TASK) {
            mode = "singleTask";
        }

        return mode;
    }

    /**
     * 查询插件Activity对应的占位Activity
     *
     * @param pluginActivity 插件Activity全名，格式为：packageName/activityName
     * @return 返回插件Activity与占位Activity。如果pluginActivity是非标准启动模式，pluginActivity还没启动就返回null；
     * 如果pluginActivity是标准启动模式，始终返回{@link LaunchModeManager#MODE_STANDARD}
     */
    @SuppressWarnings("unused")
    @Nullable
    public String queryProxyActivity(@NonNull String pluginActivity) {
        String proxyActivity = mSingleTaskPool.findProxyActivity(pluginActivity);
        if (null == proxyActivity) {
            proxyActivity = mSingleInstancePool.findProxyActivity(pluginActivity);
        }

        if (null == proxyActivity) {
            proxyActivity = mSingleTopPool.findProxyActivity(pluginActivity);
        }

        //标准启动模式
        if (null == proxyActivity) {
            String[] componentItems = pluginActivity.split("/");
            if (componentItems.length != 2) {
                return null;
            }
            ActivityInfo info = PhantomCore.getInstance()
                    .findActivityInfo(new ComponentName(componentItems[0], componentItems[1]));
            if (null == info) {
                return null;
            }
            if (info.launchMode == ActivityInfo.LAUNCH_MULTIPLE) {
                proxyActivity = MODE_STANDARD;
            }
        }


        return proxyActivity;
    }

    /**
     * 打印Activity映射关系
     */
    @SuppressWarnings("unused")
    public void dump() {
        VLog.w("----------------------SingleTop-----------------");
        mSingleTopPool.dump();
        VLog.w("----------------------SingleTask-----------------");
        mSingleTaskPool.dump();
        VLog.w("----------------------SingleInstance-----------------");
        mSingleInstancePool.dump();
    }

    /**
     * 减小proxyActivity的引用计数， 如果proxyActivity的引用计数小于0并且不是建立的固定映射关系的则会进行回收
     *
     * @param proxyActivity  占位activity
     * @param pluginActivity 对应的插件activity，格式为packageName/fullActivityName
     */
    public void unrefActivity(String proxyActivity, String pluginActivity) {
        if (null == pluginActivity) {
            VLog.w("unrefActivity proxyActivity %s, but pluginActivity is null", proxyActivity);
            return;
        }

        if (proxyActivity.contains("SingleTop")) {
            mSingleTopPool.unrefActivity(pluginActivity);
        } else if (proxyActivity.contains("SingleInstance")) {
            mSingleInstancePool.unrefActivity(pluginActivity);
        } else if (proxyActivity.contains("SingleTask")) {
            mSingleTaskPool.unrefActivity(pluginActivity);
        }
    }

    /**
     * 占位Activity不够使用时抛出该异常，方便搜集数据并对占位Activity数量进行调整
     */
    public static class ProxyActivityLessException extends Exception {
        ProxyActivityLessException(String message) {
            super(message);
        }
    }
}
