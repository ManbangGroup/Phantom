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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.wlqq.phantom.library.pm.PluginInfo;
import com.wlqq.phantom.library.pool.LaunchModeManager;
import com.wlqq.phantom.library.proxy.ServiceHostProxyManager;
import com.wlqq.phantom.library.utils.VLog;

import java.util.List;


class DebugReceiver extends BroadcastReceiver {

    /**
     * 插件安装广播 action, 需要以 extras 形式提供以下参数
     * <ul>
     *     <li>{@link #EXTRA_PATH}</li>
     *     <li>{@link #EXTRA_PACKAGE_NAME}</li>
     *     <li>{@link #EXTRA_VERSION_NAME}</li>
     * </ul>
     *
     * @see #EXTRA_PATH
     * @see #EXTRA_PACKAGE_NAME
     * @see #EXTRA_VERSION_NAME
     */
    public static final String ACTION_INSTALL_PLUGIN = ".phantom.debug.action.INSTALL_PLUGIN";

    /**
     * 插件卸载广播 action，需要以 extras 形式提供以下参数
     * <ul>
     *     <li>{@link #EXTRA_PACKAGE_NAME}</li>
     * </ul>
     *
     * @see #EXTRA_PACKAGE_NAME
     */
    public static final String ACTION_UNINSTALL_PLUGIN = ".phantom.debug.action.UNINSTALL_PLUGIN";

    /**
     * dump 已安装的插件 action
     */
    public static final String ACTION_DUMP_INSTALLED_PLUGINS = ".phantom.debug.action.DUMP_INSTALLED_PLUGINS";

    /**
     * dump {@link com.wlqq.phantom.library.proxy.ServiceHostProxy} 坑位
     */
    public static final String ACTION_DUMP_SERVICE_PROXY = ".phantom.debug.action.DUMP_SERVICE_PROXY";

    /**
     * dump {@link com.wlqq.phantom.library.proxy.ActivityHostProxy} 坑位
     */
    public static final String ACTION_DUMP_ACTIVITY_PROXY = ".phantom.debug.action.DUMP_ACTIVITY_PROXY";

    private static String sActionInstallPlugin;
    private static String sActionUninstallPlugin;
    private static String sActionDumpInstalledPlugins;
    private static String sActionDumpServiceProxy;
    private static String sActionDumpActivityProxy;

    /**
     * 待安装的插件 APK 包名，类型 String
     */
    public static final String EXTRA_PACKAGE_NAME = "package_name";
    /**
     * 待安装的插件 APK 在设备上的绝对路径，类型 String
     */
    public static final String EXTRA_PATH = "path";
    /**
     * 待安装的插件 APK 版本名，类型：String
     */
    public static final String EXTRA_VERSION_NAME = "version_name";

    public static void init(Context context) {
        final String applicationId = context.getPackageName();

        sActionInstallPlugin = applicationId + ACTION_INSTALL_PLUGIN;
        sActionUninstallPlugin = applicationId + ACTION_UNINSTALL_PLUGIN;
        sActionDumpInstalledPlugins = applicationId + ACTION_DUMP_INSTALLED_PLUGINS;
        sActionDumpActivityProxy = applicationId + ACTION_DUMP_ACTIVITY_PROXY;
        sActionDumpServiceProxy = applicationId + ACTION_DUMP_SERVICE_PROXY;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(sActionInstallPlugin);
        intentFilter.addAction(sActionUninstallPlugin);
        intentFilter.addAction(sActionDumpInstalledPlugins);
        intentFilter.addAction(sActionDumpActivityProxy);
        intentFilter.addAction(sActionDumpServiceProxy);
        context.registerReceiver(new DebugReceiver(), intentFilter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        VLog.v("intent: %s", intent);
        if (intent == null) {
            return;
        }

        final String action = intent.getAction();
        if (action == null) {
            return;
        }

        if (sActionInstallPlugin.equals(action)) {
            install(intent.getStringExtra(EXTRA_PATH),
                    intent.getStringExtra(EXTRA_PACKAGE_NAME),
                    intent.getStringExtra(EXTRA_VERSION_NAME));
        } else if (sActionUninstallPlugin.equals(action)) {
            uninstall(intent.getStringExtra(EXTRA_PACKAGE_NAME));
        } else if (sActionDumpInstalledPlugins.equals(action)) {
            dumpInstalledPlugins();
        } else if (sActionDumpActivityProxy.equals(action)) {
            dumpActivityProxy();
        } else if (sActionDumpServiceProxy.equals(action)) {
            dumpServiceProxy();
        }
    }

    private void install(final String path, final String packageName, final String versionName) {
        VLog.v("install, path: %s, pn: %s, vn: %s", path, packageName, versionName);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                PhantomCore.getInstance().installPlugin(path, packageName, versionName, true);
            }
        });
    }

    private void dumpActivityProxy() {
        VLog.v("dumpActivityProxy");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                LaunchModeManager.getInstance().dump();
            }
        });
    }

    private void dumpServiceProxy() {
        VLog.v("dumpServiceProxy");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                ServiceHostProxyManager.INSTANCE.dumpProxyServiceClassMap();
            }
        });
    }

    private void dumpInstalledPlugins() {
        VLog.v("dumpInstalledPlugins");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final List<PluginInfo> plugins = PhantomCore.getInstance().getAllPlugins();
                VLog.w("======== INSTALLED PLUGINS ========");
                for (PluginInfo pluginInfo : plugins) {
                    VLog.w(pluginInfo.toString());
                }
                VLog.w("===================================");
            }
        });
    }

    private void uninstall(@Nullable final String packageName) {
        VLog.d("uninstall, pn: %s", packageName);
        if (TextUtils.isEmpty(packageName)) {
            VLog.w("package name is empty");
            return;
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                PhantomCore.getInstance().uninstallPlugin(packageName);
            }
        });
    }
}
