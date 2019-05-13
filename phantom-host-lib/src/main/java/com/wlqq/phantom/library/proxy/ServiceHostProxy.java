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

package com.wlqq.phantom.library.proxy;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.TimingLogger;

import com.wlqq.phantom.library.PhantomCore;
import com.wlqq.phantom.library.env.Constants;
import com.wlqq.phantom.library.log.LogReporter;
import com.wlqq.phantom.library.pm.PluginInfo;
import com.wlqq.phantom.library.utils.TimingUtils;
import com.wlqq.phantom.library.utils.VLog;

import java.util.HashMap;

/**
 * 宿主中用于占坑的 Service 代理基类
 */
@SuppressWarnings("PMD.ConfusingTernary")
abstract class ServiceHostProxy extends Service {
    private static final String TAG = Constants.TAG;
    private Service mPluginService;

    @Override
    public void onCreate() {
        super.onCreate();
        VLog.d("onCreate E");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        VLog.d("onStartCommand, intent: %s", intent);

        HashMap<String, Object> params = new HashMap<>();
        params.put(LogReporter.Key.METHOD, "onStartCommand");

        if (intent == null) {
            final String msg = "onStartCommand intent is null";
            VLog.w(msg);

            params.put(LogReporter.Key.MESSAGE, msg);
            LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_START, false, params);

            return START_STICKY;
        }

        Intent targetIntent = intent.getParcelableExtra(Constants.ORIGIN_INTENT);
        if (targetIntent == null) {
            final String msg = "onStartCommand targetIntent is null";
            VLog.w(msg);

            params.put(LogReporter.Key.MESSAGE, msg);
            LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_START, false, params);

            return START_STICKY;
        }

        PluginInfo targetInfo = resolveServiceInfo(targetIntent);
        if (targetInfo == null) {
            final String msg = "onStartCommand targetInfo is null";
            VLog.w(msg);

            params.put(LogReporter.Key.MESSAGE, msg);
            LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_START, false, params);

            return START_STICKY;
        }

        params.put(LogReporter.Key.PACKAGE_NAME, targetInfo.packageName);

        String serviceClassName = targetIntent.getComponent().getClassName();

        params.put(LogReporter.Key.TARGET_SERVICE, serviceClassName);

        if (mPluginService == null) {
            try {
                TimingUtils.startTime(serviceClassName);
                mPluginService = handleCreateService(targetInfo, serviceClassName);
                trackServiceLoadTime(targetInfo.packageName, serviceClassName);
                VLog.i("onStartCommand handleCreateService ok");
            } catch (Throwable throwable) {
                final String msg = "onStartCommand handleCreateService error";
                VLog.w(throwable, msg);

                params.put(LogReporter.Key.MESSAGE, msg);
                LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_START, false, targetInfo.packageName,
                        params);
                LogReporter.reportException(throwable, params);

                return START_STICKY;
            }
        }

        final PluginClassLoader pluginClassLoader = targetInfo.getPluginClassLoader();
        if (pluginClassLoader != null) {
            targetIntent.setExtrasClassLoader(pluginClassLoader);
        } else {
            VLog.w("onStartCommand, pluginClassLoader is null !!!");
        }

        final int ret = mPluginService.onStartCommand(targetIntent, flags, startId);

        LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_START, true, targetInfo.packageName, params);

        return ret;
    }

    private Service handleCreateService(final PluginInfo pluginInfo, String serviceClassName) throws Throwable {
        VLog.d("handleCreateService, pluginInfo: %s, serviceClassName: %s", pluginInfo.packageName, serviceClassName);
        TimingLogger logger = new TimingLogger(TAG, "handleCreateService");

        if (!pluginInfo.start()) {
            throw new IllegalStateException("PluginInfo start failed");
        }
        logger.addSplit("start plugin");

        final PluginClassLoader pluginClassLoader = pluginInfo.getPluginClassLoader();
        if (pluginClassLoader == null) {
            throw new IllegalStateException("PluginInfo#getPluginClassLoader is null");
        }
        logger.addSplit("get plugin classloader");

        Class<?> serviceClass = pluginClassLoader.loadClass(serviceClassName);
        if (serviceClass == null) {
            throw new IllegalStateException("PluginClassLoader#loadClass return null");
        }
        logger.addSplit("load service class");

        Service service = (Service) serviceClass.newInstance();
        logger.addSplit("create service proxy");

        if (PluginInterceptService.class.isAssignableFrom(serviceClass)) {
            final PluginInterceptService pluginInterceptService = (PluginInterceptService) service;
            pluginInterceptService.setContextProxy(new ContextProxy<Service>(pluginInfo, this));
            pluginInterceptService.attachBaseContext(getBaseContext());
        } else if (PluginInterceptIntentService.class.isAssignableFrom(serviceClass)) {
            final PluginInterceptIntentService pluginInterceptIntentService = (PluginInterceptIntentService) service;
            pluginInterceptIntentService.setContextProxy(new ContextProxy<Service>(pluginInfo, this));
            pluginInterceptIntentService.attachBaseContext(getBaseContext());
        }

        logger.addSplit("call service attachBaseContext");

        service.onCreate();
        logger.addSplit("call service onCreate");

        logger.dumpToLog();

        ServiceHostProxyManager.INSTANCE.putPluginService(getClass().getName(), service);

        return service;
    }

    @Override
    public IBinder onBind(Intent intent) {
        VLog.d("onBind, intent: %s", intent);

        HashMap<String, Object> params = new HashMap<>();
        params.put(LogReporter.Key.METHOD, "onBind");

        if (intent == null) {
            final String msg = "onBind intent is null";
            VLog.w(msg);

            params.put(LogReporter.Key.MESSAGE, msg);
            LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_BIND, false, params);

            return null;
        }

        Intent targetIntent = intent.getParcelableExtra(Constants.ORIGIN_INTENT);
        if (targetIntent == null) {
            final String msg = "onBind targetIntent is null";
            VLog.w(msg);

            params.put(LogReporter.Key.MESSAGE, msg);
            LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_BIND, false, params);

            return null;
        }

        PluginInfo targetInfo = resolveServiceInfo(targetIntent);
        if (targetInfo == null) {
            final String msg = "onBind targetInfo is null";
            VLog.w(msg);

            params.put(LogReporter.Key.MESSAGE, msg);
            LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_BIND, false, params);

            return null;
        }

        params.put(LogReporter.Key.PACKAGE_NAME, targetInfo.packageName);

        String serviceClassName = targetIntent.getComponent().getClassName();

        params.put(LogReporter.Key.TARGET_SERVICE, serviceClassName);

        if (mPluginService == null) {
            try {
                TimingUtils.startTime(serviceClassName);
                mPluginService = handleCreateService(targetInfo, serviceClassName);
                trackServiceLoadTime(targetInfo.packageName, serviceClassName);
                VLog.i("onBind handleCreateService ok");
            } catch (Throwable throwable) {
                final String msg = "onBind handleCreateService error";
                VLog.w(throwable, msg);

                params.put(LogReporter.Key.MESSAGE, msg);
                LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_BIND, false, targetInfo.packageName, params);
                LogReporter.reportException(throwable, params);

                return null;
            }
        }

        final PluginClassLoader pluginClassLoader = targetInfo.getPluginClassLoader();
        if (pluginClassLoader != null) {
            targetIntent.setExtrasClassLoader(pluginClassLoader);
        } else {
            VLog.w("onBind, pluginClassLoader is null !!!");
        }

        final IBinder iBinder = mPluginService.onBind(targetIntent);

        LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_BIND, true, targetInfo.packageName, params);

        return iBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        VLog.d("onUnbind, intent: %s", intent);

        HashMap<String, Object> params = new HashMap<>();
        params.put(LogReporter.Key.METHOD, "onUnbind");

        if (intent == null) {
            final String msg = "onUnbind intent is null";
            VLog.w(msg);

            params.put(LogReporter.Key.MESSAGE, msg);
            LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_UNBIND, false, params);

            return false;
        }

        Intent targetIntent = intent.getParcelableExtra(Constants.ORIGIN_INTENT);
        if (targetIntent == null) {
            final String msg = "onUnbind targetIntent is null";
            VLog.w(msg);

            params.put(LogReporter.Key.MESSAGE, msg);
            LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_UNBIND, false, params);

            return false;
        }


        PluginInfo targetInfo = resolveServiceInfo(targetIntent);
        if (targetInfo == null) {
            final String msg = "onUnbind targetInfo is null";
            VLog.w(msg);

            params.put(LogReporter.Key.MESSAGE, msg);
            LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_UNBIND, false, params);

            return false;
        }

        params.put(LogReporter.Key.PACKAGE_NAME, targetInfo.packageName);

        //FIXME check target service class name?
        String serviceClassName = targetIntent.getComponent().getClassName();

        params.put(LogReporter.Key.TARGET_SERVICE, serviceClassName);

        if (mPluginService == null) {
            final String msg = "onUnbind service is null";
            VLog.w(msg);

            params.put(LogReporter.Key.MESSAGE, msg);
            LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_UNBIND, false, targetInfo.packageName, params);

            return false;
        }

        final boolean result = mPluginService.onUnbind(intent);

        LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_UNBIND, true, targetInfo.packageName, params);

        return result;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        VLog.d("onRebind, intent: %s", intent);

        HashMap<String, Object> params = new HashMap<>();
        params.put(LogReporter.Key.METHOD, "onRebind");

        if (intent == null) {
            final String msg = "onRebind intent is null";
            VLog.w(msg);

            params.put(LogReporter.Key.MESSAGE, msg);
            LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_REBIND, false, params);

            return;
        }

        Intent targetIntent = intent.getParcelableExtra(Constants.ORIGIN_INTENT);
        if (targetIntent == null) {
            final String msg = "onRebind targetIntent is null";
            VLog.w(msg);

            params.put(LogReporter.Key.MESSAGE, msg);
            LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_REBIND, false, params);

            return;
        }

        PluginInfo targetInfo = resolveServiceInfo(targetIntent);
        if (targetInfo == null) {
            final String msg = "onRebind targetInfo is null";
            VLog.w(msg);

            params.put(LogReporter.Key.MESSAGE, msg);
            LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_REBIND, false, params);

            return;
        }

        params.put(LogReporter.Key.PACKAGE_NAME, targetInfo.packageName);

        //FIXME check target service class name?
        String serviceClassName = targetIntent.getComponent().getClassName();

        params.put(LogReporter.Key.TARGET_SERVICE, serviceClassName);

        if (mPluginService == null) {
            final String msg = "onRebind service is null";
            VLog.w(msg);

            params.put(LogReporter.Key.MESSAGE, msg);
            LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_REBIND, false, targetInfo.packageName, params);

            return;
        }

        mPluginService.onRebind(intent);

        LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_REBIND, true, targetInfo.packageName, params);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        VLog.d("onTaskRemoved, intent: %s", rootIntent);
    }

    @SuppressWarnings("PMD.CallSuperLast")
    @Override
    public void onDestroy() {
        super.onDestroy();
        VLog.d("onDestroy");

        HashMap<String, Object> params = new HashMap<>();
        params.put(LogReporter.Key.METHOD, "onDestroy");

        if (mPluginService == null) {
            final String msg = "onDestroy service is null";
            VLog.w(msg);

            params.put(LogReporter.Key.MESSAGE, msg);
            LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_DESTROY, false, params);

            return;
        }

        mPluginService.onDestroy();
        mPluginService = null;
        ServiceHostProxyManager.INSTANCE.removePluginService(getClass().getName());
        LogReporter.reportState(LogReporter.EventId.PLUGIN_SERVICE_DESTROY, true, params);
    }

    private void trackServiceLoadTime(String packageName, String serviceClassName) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(LogReporter.Key.PACKAGE_NAME, packageName);
        params.put(LogReporter.Key.TIME,
                TimingUtils.getNormalizedDuration(serviceClassName, TimingUtils.SECTION_DURATION_50_MS,
                        TimingUtils.MAX_SECTION_20));
        LogReporter.reportEvent(LogReporter.EventId.PLUGIN_SERVICE_LOAD, serviceClassName, params);
    }

    /**
     * <b>NOTE: </b>目前只支持 explicit intent
     *
     * @param intent 要启动的 Service
     */
    @Nullable
    private PluginInfo resolveServiceInfo(Intent intent) {
        // FIXME: 目前只支持 explicit intent
        ComponentName component = intent.getComponent();
        if (component == null) {
            return null;
        }

        return PhantomCore.getInstance().findPluginInfoByServiceName(component);
    }

    public static class P1 extends ServiceHostProxy {

    }

    public static class P2 extends ServiceHostProxy {

    }

    public static class P3 extends ServiceHostProxy {

    }

    public static class P4 extends ServiceHostProxy {

    }

    public static class P5 extends ServiceHostProxy {

    }

    public static class P6 extends ServiceHostProxy {

    }

    public static class P7 extends ServiceHostProxy {

    }

    public static class P8 extends ServiceHostProxy {

    }

    public static class P9 extends ServiceHostProxy {

    }

    public static class P10 extends ServiceHostProxy {

    }
}
