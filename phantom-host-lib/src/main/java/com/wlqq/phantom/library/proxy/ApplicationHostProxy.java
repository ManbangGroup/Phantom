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

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.ConditionVariable;

import com.wlqq.phantom.library.log.LogReporter;
import com.wlqq.phantom.library.pm.PluginInfo;
import com.wlqq.phantom.library.utils.ClassUtils;
import com.wlqq.phantom.library.utils.ThreadUtils;
import com.wlqq.phantom.library.utils.VLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 插件 {@link Application} 在宿主中的坑位 {@link Application}
 */
public class ApplicationHostProxy {
    private final Context mHostApplicationContext;
    private String mPluginApplicationClassName;
    private PluginInterceptApplication mPluginApplication;
    private final PluginInfo mPluginInfo;

    private boolean mAttached;

    private final HashMap<String, Object> mExtMsg = new HashMap<>();

    public ApplicationHostProxy(Context hostAppContext, PluginInfo pluginInfo) throws Throwable {
        mHostApplicationContext = hostAppContext;
        mPluginInfo = pluginInfo;
        ApplicationInfo applicationInfo = pluginInfo.getApplicationInfo();


        mExtMsg.put(LogReporter.Key.PACKAGE_NAME, pluginInfo.packageName);
        mExtMsg.put(LogReporter.Key.VERSION_NAME, pluginInfo.versionName);

        try {
            if (applicationInfo == null) {
                String msg = "mApplicationInfo is null";
                VLog.w(msg);
                throw new Exception(msg);
            }

            mPluginApplicationClassName = applicationInfo.className;
            if (null == mPluginApplicationClassName) {
                VLog.i("plugin does not have Application subclass, use android.app.Application");
                mPluginApplicationClassName = "com.wlqq.phantom.library.proxy.PluginInterceptApplication";
            }

            mExtMsg.put(LogReporter.Key.CLASS, mPluginApplicationClassName);

            loadApplication();
            mPluginApplication.setContextProxy(new ContextProxy<>(pluginInfo, hostAppContext));
            attach();

            mAttached = true;

            LogReporter.reportState(LogReporter.EventId.PLUGIN_APPLICATION, true, pluginInfo.packageName, mExtMsg);
            LogReporter.reportLog(pluginInfo.packageName + "_" + pluginInfo.versionName
                    + "/" + ClassUtils.getSimpleName(mPluginApplicationClassName)
                    + " load success");
        } catch (Throwable e) {
            final String message = e.getMessage();
            VLog.w(e, "ApplicationHostProxy %s load error", mPluginApplicationClassName);
            LogReporter.reportLog(pluginInfo.packageName + "_" + pluginInfo.versionName
                    + "/" + ClassUtils.getSimpleName(mPluginApplicationClassName)
                    + " load fail");

            mExtMsg.put(LogReporter.Key.MESSAGE, message);

            LogReporter.reportState(LogReporter.EventId.PLUGIN_APPLICATION, false, pluginInfo.packageName, mExtMsg);
            LogReporter.reportException(e, mExtMsg);

            throw e;
        }
    }

    private void loadApplication() throws Throwable {
        if (null == mPluginApplicationClassName || null == mPluginInfo) {
            throw new Exception("appBundle is null or plugin Application is null");
        }

        final PluginClassLoader pluginClassLoader = mPluginInfo.getPluginClassLoader();
        if (pluginClassLoader == null) {
            throw new Exception("plugin class loader is null");
        }

        final Class targetCls = mPluginInfo.getPluginClassLoader().loadClass(mPluginApplicationClassName);
        try {
            mPluginApplication = (PluginInterceptApplication) targetCls.newInstance();
        } catch (RuntimeException e) {
            VLog.w(e, "ProxyBuilder build application proxy error");

            // **特殊机型兼容**
            //
            // 问题描述：
            // 部分设备 Android Framework 中的 Application 类构造函数被修改了，其内部会创建 Handler ，且没
            // 有明确使用 main looper，导致在后台线程中创建 Handler 出现异常
            //
            // 链接：https://bugly.qq.com/v2/crash-reporting/errors/900024060/760798/report?pid=1&search=&searchType
            // =detail&bundleId=&channelId=&version=5.7.6.3&tagList=&start=0&date=custom&startDateStr=2017-03-21&
            // endDateStr=2017-03-21
            //
            // 已知设备：
            // - 华为 C8815 4.1.2 LeWa_OS5.1_15.06.06
            // - TCL tcl/TCL_M2M_15.04.14 4.4.4
            //
            // 解决方案：
            // 1. 调用线程捕获该异常
            // 2. Post 到 UI 线程中调用创建 Application 代理的方法
            // 3. 调用线程等待 UI 线程中调用创建 Application 代理的方法执行完毕，并检查是否有异常。若有，则抛出该异常
            if ("Can't create handler inside thread that has not called Looper.prepare()".equals(e.getMessage())) {
                final AtomicReference<Throwable> innerThrowable = new AtomicReference<>();
                final ConditionVariable conditionVariable = new ConditionVariable(false);
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        VLog.d("ProxyBuilder build application proxy runOnUiThread E");
                        try {
                            mPluginApplication = (PluginInterceptApplication) targetCls.newInstance();
                        } catch (Throwable throwable) {
                            VLog.w(throwable, "ProxyBuilder build application proxy error");
                            innerThrowable.set(throwable);

                        }
                        VLog.d("ProxyBuilder build application proxy runOnUiThread X");
                        conditionVariable.open();
                    }
                });
                VLog.d("ProxyBuilder build application proxy before block");
                conditionVariable.block();
                VLog.d("ProxyBuilder build application proxy after block");
                final Throwable throwable = innerThrowable.get();
                if (throwable != null) {
                    throw throwable;
                }
            } else {
                throw e;
            }
        }

    }

    private void attach() {
        mPluginApplication.attachBaseContext(mHostApplicationContext);
    }

    /**
     * 1. 若调用该方法的线程是 UI 线程，则直接调用 Application#onCreate
     * 2. 若调用该方法的线程是后台线程
     * 2.1 将 {@link Application#onCreate} post 到 UI 线程中执行
     * 2.2 当前线程等待 {@link Application#onCreate} 执行完之后再继续执行
     */
    public void callApplicationOnCreateInUiThread() throws Throwable {
        VLog.d("callApplicationOnCreateInUiThread E");
        final List<Throwable> throwable = new ArrayList<>(1);
        if (ThreadUtils.isInUiThread()) {
            if (mAttached) {
                try {
                    mPluginApplication.onCreate();
                } catch (Throwable e) {
                    throwable.add(e);
                }
            }
            VLog.d("callApplicationOnCreateInUiThread X, isInUiThread: true");
        } else {
            final ConditionVariable conditionVariable = new ConditionVariable(false);

            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    VLog.d("callApplicationOnCreateInUiThread runOnUiThread E");
                    if (mAttached) {
                        try {
                            mPluginApplication.onCreate();
                        } catch (Throwable e) {
                            throwable.add(e);
                        }
                    }
                    VLog.d("callApplicationOnCreateInUiThread runOnUiThread X");
                    conditionVariable.open();
                }
            });

            VLog.d("callApplicationOnCreateInUiThread before block");
            conditionVariable.block();
            VLog.d("callApplicationOnCreateInUiThread after block");

            VLog.d("callApplicationOnCreateInUiThread X");

            if (throwable.isEmpty()) {
                LogReporter.reportLog(mPluginInfo.packageName + "_" + mPluginInfo.versionName
                        + "/" + ClassUtils.getSimpleName(mPluginApplicationClassName)
                        + " onCreate success");
            } else {
                Throwable e = throwable.get(0);
                VLog.w(e, "ApplicationHostProxy %s onCreate error", mPluginApplicationClassName);
                LogReporter.reportLog(mPluginInfo.packageName + "_" + mPluginInfo.versionName
                        + "/" + ClassUtils.getSimpleName(mPluginApplicationClassName)
                        + " onCreate fail");
                LogReporter.reportException(e, mExtMsg);

                throw e;
            }
        }
    }

    public Application getPluginApplication() {
        return this.mPluginApplication;
    }
}
