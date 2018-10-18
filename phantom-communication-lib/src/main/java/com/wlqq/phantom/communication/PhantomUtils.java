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

package com.wlqq.phantom.communication;


import android.content.Context;
import android.content.Intent;

/**
 * 用于获取宿主的 Context 对象的工具类
 *
 * @since Phantom 0.4.0
 */
public final class PhantomUtils {
    private static IPhantomUtils sPhantomUtilsImpl;

    private PhantomUtils() {
    }

    public static void setImpl(IPhantomUtils impl) {
        sPhantomUtilsImpl = impl;
    }

    /**
     * 通过插件的 context 对象获取宿主 Context
     *
     * @param pluginContext 插件 Context 对象，必须是插件的 Activity 实例或者插件的 Application 实例
     * @return 如果 pluginContext 是 Activity 实例，则返回宿主中代理当前 activity 的实例
     *         如果 pluginContext 是 Application 的实例，则返回宿主 Application 对象
     *         否则返回 null
     * @since Phantom 0.4.0
     */
    public static Context getHostContext(Context pluginContext) {
        return sPhantomUtilsImpl.getHostContext(pluginContext);
    }

    /**
     * 在插件中使用宿主 context 来启动插件的 Activity，典型的应用场景是：插件 BroadcastReceiver 接收到广播消息时，
     * 得到的 Context 是宿主 Application 实例，要使用这个 Context 来启动插件的 Activity 需要依赖 PhantomCore 的 startActivity 方法
     *
     * @param hostContext 宿主context
     * @param intent      启动Activity的intent
     * @since Phantom 1.0.2
     */
    @SuppressWarnings("unchecked")
    public static void startActivity(Context hostContext, Intent intent) {
        sPhantomUtilsImpl.startActivity(hostContext, intent);
    }

    /**
     * 用于包装需要由系统启动的插件 activity。例如：Notification, NFC 使用的 {@link android.app.PendingIntent}
     *
     * @param originIntent 需要系统处理的 intent
     * @param launchMode   originIntent 中 activity 的启动模式，取值为
     *                     {@link android.content.pm.ActivityInfo#LAUNCH_MULTIPLE},
     *                     {@link android.content.pm.ActivityInfo#LAUNCH_SINGLE_INSTANCE},
     *                     {@link android.content.pm.ActivityInfo#LAUNCH_SINGLE_TASK},
     *                     {@link android.content.pm.ActivityInfo#LAUNCH_SINGLE_TOP}
     * @return 包装后的 originIntent。获取占位 activity 失败时返回 null
     * @since Phantom 0.4.0
     */
    public static Intent resolveActivity(Intent originIntent, int launchMode) {
        return sPhantomUtilsImpl.resolveActivity(originIntent, launchMode);
    }
}
