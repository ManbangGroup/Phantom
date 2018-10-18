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

package com.wlqq.phantom.library.log;

import android.app.Application;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.wlqq.phantom.library.BuildConfig;
import com.wlqq.phantom.library.env.Constants;

import java.util.HashMap;
import java.util.Locale;

public final class LogReporter {
    private static ILogReporter sReporter;
    /**
     * 默认数据上报对象，供框架内部使用。
     */
    private static ILogReporter sDefault;
    private static HashMap<String, Object> sDeviceInfo;

    static {
        initDeviceInfo();
        setDefaultLogReport(new DefaultLogReporter());
    }
    private LogReporter() {
        // prevent instantiation
    }

    public static void setImpl(ILogReporter reporter) {
        sReporter = reporter;
    }

    private static void setDefaultLogReport(ILogReporter report) {
        sDefault = report;
    }

    public static void reportLog(String message) {
        if (null != sReporter) {
            sReporter.reportLog(Constants.TAG, message);
        }

        if (null != sDefault) {
            sDefault.reportLog(Constants.TAG, message);
        }
    }

    public static void reportException(Throwable throwable) {
        reportException(throwable, null);
    }

    public static void reportException(Throwable throwable, @Nullable HashMap<String, Object> params) {
        HashMap<String, Object> newParams = appendDeviceInfo(params);
        if (null != sReporter) {
            sReporter.reportException(throwable, newParams);
        }

        if (null != sDefault) {
            sDefault.reportException(throwable, newParams);
        }
    }

    /**
     * 上报内部存储可用的磁盘空间
     */
    public static void reportUsableSpaceMegabytes() {
        final long usableSpaceMegabytes = Environment.getDataDirectory().getUsableSpace() / 1024 / 1024;
        reportLog("usableSpace MB: " + usableSpaceMegabytes);
    }

    public static void reportState(String tag, boolean state, String label, @Nullable HashMap<String, Object> params) {
        String eventId = tag + "_" + (state ? "success" : "fail");
        HashMap<String, Object> newParams = appendDeviceInfo(params);
        if (null != sReporter) {
            sReporter.reportEvent(eventId, label, newParams);
        }

        if (null != sDefault) {
            sDefault.reportEvent(eventId, label, newParams);
        }
    }

    public static void reportState(String tag, boolean state, @Nullable HashMap<String, Object> params) {
        final String suffix = state ? "success" : "fail";
        String eventId = tag + "_" + suffix;
        HashMap<String, Object> newParams = appendDeviceInfo(params);
        if (null != sReporter) {
            sReporter.reportEvent(eventId, suffix, newParams);
        }

        if (null != sDefault) {
            sDefault.reportEvent(eventId, suffix, newParams);
        }
    }

    public static void reportEvent(String eventId, String label) {
        reportEvent(eventId, label, null);
    }

    public static void reportEvent(String eventId, String label, @Nullable HashMap<String, Object> params) {
        HashMap<String, Object> newParams = appendDeviceInfo(params);
        if (null != sReporter) {
            sReporter.reportEvent(eventId, label, newParams);
        }

        if (null != sDefault) {
            sDefault.reportEvent(eventId, label, newParams);
        }
    }

    @NonNull
    private static HashMap<String, Object> appendDeviceInfo(@Nullable HashMap<String, Object> params) {
        HashMap<String, Object> newParams;
        if (params == null) {
            newParams = new HashMap<>(sDeviceInfo.size());
        } else {
            newParams = new HashMap<>(params.size() + sDeviceInfo.size());
            newParams.putAll(params);
        }
        newParams.putAll(sDeviceInfo);
        return newParams;
    }

    private static void initDeviceInfo() {
        sDeviceInfo = new HashMap<>(2);
        sDeviceInfo.put("SDK_INT", String.valueOf(Build.VERSION.SDK_INT));
        sDeviceInfo.put("MODEL", String.format(Locale.US, "%s_%s_%d", Build.BRAND, Build.MODEL, Build.VERSION.SDK_INT));
    }

    /**
     * 插件框架自定义事件 Event Id
     */
    public static final class EventId {

        private static final String PREFIX = "_ph_" + BuildConfig.VERSION_NAME;

        /**
         * 插件框架初始化
         */
        public static final String PHANTOM_INIT = PREFIX + "_init";

        /**
         * 插件<b>非首次</b>加载
         */
        public static final String PLUGIN_LOAD = PREFIX + "_plugin_load";

        /**
         * 插件<b>首次</b>加载
         */
        public static final String PLUGIN_LOAD_FIRST = PREFIX + "_plugin_load_first";

        /**
         * 插件<b>非首次</b>load dex
         */
        public static final String PLUGIN_DEX_LOAD = PREFIX + "_plugin_dex_load";

        /**
         * 插件<b>首次</b>load dex
         */
        public static final String PLUGIN_DEX_LOAD_FIRST = PREFIX + "_plugin_dex_load_first";

        /**
         * 插件<b>非首次</b>调用其 {@link Application#onCreate()}
         */
        public static final String PLUGIN_APPLICATION_LOAD = PREFIX + "_plugin_app_load";

        /**
         * 插件<b>首次</b>调用其 {@link Application#onCreate()}
         */
        public static final String PLUGIN_APPLICATION_LOAD_FIRST = PREFIX + "_plugin_app_load_first";

        /**
         * Service 加载耗时
         */
        public static final String PLUGIN_SERVICE_LOAD = PREFIX + "_service_load";

        /**
         * Activity 相关流程
         */
        public static final String PLUGIN_ACTIVITY = PREFIX + "_activity";

        /**
         * Activity 加载耗时
         */
        public static final String PLUGIN_ACTIVITY_LOAD = PREFIX + "_activity_load";

        /**
         * Activity onCreate 相关流程
         */
        public static final String PLUGIN_ACTIVITY_ON_CREATE = PREFIX + "_activity_onCreate";

        /**
         * Application 相关流程
         */
        public static final String PLUGIN_APPLICATION = PREFIX + "_plugin_application";


        // 备注：因只有 EventId 能统计独立用户数（即事件影响到的用户），所以事件的成功和失败都定义为独立的事件
        public static final String PLUGIN_SERVICE_START = PREFIX + "_service_start";
        public static final String PLUGIN_SERVICE_BIND = PREFIX + "_service_bind";
        public static final String PLUGIN_SERVICE_UNBIND = PREFIX + "_service_unbind";
        public static final String PLUGIN_SERVICE_REBIND = PREFIX + "_service_rebind";
        public static final String PLUGIN_SERVICE_DESTROY = PREFIX + "_service_destroy";
        public static final String PLUGIN_SERVICE_HOOK_START = PREFIX + "_service_hook_start";
        public static final String PLUGIN_SERVICE_HOOK_STOP = PREFIX + "_service_hook_stop";
        public static final String PLUGIN_SERVICE_HOOK_BIND = PREFIX + "_service_hook_bind";

        /**
         * 扫描已安装的插件信息
         */
        public static final String PLUGIN_PRELOAD = PREFIX + "_plugin_preload";

        /**
         * 安装插件
         */
        public static final String PLUGIN_INSTALL = PREFIX + "_plugin_install";

        /**
         * 卸载插件
         */
        public static final String PLUGIN_UNINSTALL = PREFIX + "_plugin_uninstall";

        /**
         * 创建插件 {@link android.content.Context}
         */
        public static final String PLUGIN_CONTEXT_CREATE = PREFIX + "_plugin_create_context";

        /**
         * 占位Activity clone
         */
        public static final String PROXY_ACTIVITY_CANNOT_CLONE = PREFIX + "_activity_cannot_clone";

        /**
         * 插件崩溃(plugin crashed)，在统计插件崩溃率中作为分子
         *
         * @see #PLUGIN_USAGE
         */
        public static final String PLUGIN_CRASHED = "_pc_";

        /**
         * 插件使用(plugin usage)，在统计插件崩溃率中作为分母
         *
         * @see #PLUGIN_CRASHED
         */
        public static final String PLUGIN_USAGE = "_pu_";
    }

    public static final class Label {
        public static final String SUCCESS = "success";
        public static final String FAIL = "fail";
        public static final String FILE_PREFIX = "file_";
        public static final String ASSETS_PREFIX = "assets_";
        public static final String COMPONENT_PREFIX = "comp_";
    }

    public static final class Key {
        public static final String TARGET_ACTIVITY = "target_activity";
        public static final String TARGET_SERVICE = "target_service";
        public static final String PACKAGE_NAME = "package_name";
        public static final String METHOD = "method";
        public static final String FIELD = "field";
        public static final String MESSAGE = "message";
        public static final String VERSION_CODE = "vc";
        public static final String VERSION_NAME = "vn";
        public static final String STATUS = "status";
        public static final String CLASS = "class";
        public static final String TIME = "time";
        public static final String CHECK_VERSION = "check_version";
        public static final String CHECK_SIGNATURE = "check_signature";
        public static final String FROM_ASSETS = "from_assets";
    }
}
