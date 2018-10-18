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

package com.wlqq.phantom.library.utils;


import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.wlqq.phantom.library.PhantomCore;
import com.wlqq.phantom.library.env.Constants;
import com.wlqq.phantom.library.log.LogReporter;
import com.wlqq.phantom.library.pm.PluginInfo;
import com.wlqq.phantom.library.pool.LaunchModeManager;
import com.wlqq.phantom.library.proxy.ServiceHostProxyManager;

import java.util.Locale;
import java.util.Random;

public final class IntentUtils {
    private static final String ACTION_FAKE = "ServiceHostProxy";
    private static final Random RANDOM = new Random();

    private static final IntentResolver ACTIVITY_INTENT_RESOLVER = new IntentResolver() {

        @Nullable
        @Override
        protected String getPluginPackageName(@NonNull ComponentName component) {
            final PluginInfo pluginInfo = PhantomCore.getInstance().findPluginInfoByActivityName(component);
            if (pluginInfo == null) {
                return null;
            }
            return pluginInfo.packageName;
        }
    };

    private static final IntentResolver SERVICE_INTENT_RESOLVER = new IntentResolver() {

        @Nullable
        @Override
        protected String getPluginPackageName(@NonNull ComponentName component) {
            final PluginInfo pluginInfo = PhantomCore.getInstance().findPluginInfoByServiceName(component);
            if (pluginInfo == null) {
                return null;
            }
            return pluginInfo.packageName;
        }
    };

    private IntentUtils() {
        // prevent instantiation
    }

    @NonNull
    public static Intent wrapToServiceHostProxyIntentIfNeeded(Intent intent) {
        if (intent.hasExtra(Constants.ORIGIN_INTENT)) {
            VLog.v("skip ServiceHostProxy intent: %s", intent);
            // 已经是代理 Intent 了
            return intent;
        }

        // Service 是否在插件中 ?
        final String packageName = SERVICE_INTENT_RESOLVER.resolveIntentTarget(intent);
        VLog.v("resolveIntentTarget: %s", packageName);
        if (packageName == null) {
            // Service 在宿主或其他应用，忽略掉
            VLog.v("skip service not in plugin: %s", intent);
            return intent;
        }

        // Service 在插件中, 返回代理 Service
        Intent newIntent = new Intent(intent);
        if (null != newIntent.getExtras()) {
            //清空extras
            newIntent.replaceExtras(new Bundle());
        }
        // FIXBUG：https://github.com/Qihoo360/DroidPlugin/issues/122
        // 如果插件中有两个Service：ServiceA和ServiceB，在bind ServiceA的时候会调用ServiceA的onBind并返回其IBinder对象，
        // 但是再次bind ServiceA的时候还是会返回ServiceA的IBinder对象，这是因为插件系统对多个Service使用了同一个StubService
        // 来代理，而系统对StubService的IBinder做了缓存的问题。这里设置一个Action则会穿透这种缓存。
        newIntent.setAction(ACTION_FAKE + RANDOM.nextInt());

        final ComponentName component = intent.getComponent();
        final String className = component.getClassName();
        final String proxyServiceName = ServiceHostProxyManager.INSTANCE.getProxyServiceName(className);

        VLog.d("pn: %s, class: %s, proxyServiceName: %s", packageName, className, proxyServiceName);

        if (proxyServiceName == null) {
            final String message = String.format(Locale.US, "no available ServiceHostProxy for intent: %s", intent);
            VLog.w(message);
            ServiceHostProxyManager.INSTANCE.dumpProxyServiceClassMap();
            LogReporter.reportException(new Exception(message), null);
            return intent;
        } else {
            ComponentName proxyComponentName = new ComponentName(PhantomCore.getInstance().getContext(),
                    proxyServiceName);
            newIntent.setComponent(proxyComponentName);
            newIntent.putExtra(Constants.ORIGIN_INTENT, intent.setComponent(new ComponentName(packageName, className)));
            return newIntent;
        }
    }

    @NonNull
    public static Intent wrapToActivityHostProxyIntentIfNeeded(Intent intent) {
        if (intent.hasExtra(Constants.ORIGIN_INTENT)) {
            VLog.v("skip ActivityHostProxy intent: %s", intent);
            // 已经是代理 Intent 了
            return intent;
        }

        // Activity 是否在插件中 ?
        final String packageName = ACTIVITY_INTENT_RESOLVER.resolveIntentTarget(intent);
        VLog.v("intent: %s, resolveIntentTarget: %s", intent, packageName);
        if (packageName == null) {
            // Activity 在宿主或其他应用，忽略掉
            VLog.v("skip intent not in plugin: %s", intent);
            return intent;
        }

        // Activity 在插件中, 返回代理 Activity
        Intent newIntent = new Intent(intent);
        if (null != newIntent.getExtras()) {
            //清空extras
            newIntent.replaceExtras(new Bundle());
        }

        final ComponentName oldComponent = intent.getComponent();
        final String oldClassName = oldComponent.getClassName();

        final ComponentName newComponent = new ComponentName(packageName, oldClassName);
        VLog.d("old: %s, new: %s", oldComponent, newComponent);

        ActivityInfo ai = PhantomCore.getInstance()
                .findActivityInfo(new ComponentName(packageName, oldClassName));
        int launchMode = ai.launchMode;
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0) {
            launchMode = ActivityInfo.LAUNCH_SINGLE_TOP;
        }

        try {
            String proxyClass = LaunchModeManager.getInstance()
                    .resolveActivity(packageName + "/" + oldClassName, launchMode);
            newIntent.setComponent(new ComponentName(PhantomCore.getInstance().getContext(), proxyClass));
            newIntent.putExtra(Constants.ORIGIN_INTENT, intent.setComponent(newComponent));
            return newIntent;
        } catch (LaunchModeManager.ProxyActivityLessException e) {
            VLog.w(e, "no available activity for intent: %s, launch mode: %d", intent, launchMode);
            // TODO: dump proxy activity pool

            return intent;
        }
    }

    /**
     * 将 intent 的 extras 合并到 originIntent 的 extras
     *
     * @param intent The intent
     * @param pluginClassLoader 插件的 {@link ClassLoader}
     */
    public static void mergeIntentExtras(Intent intent, ClassLoader pluginClassLoader) {
        Intent originIntent = intent.getParcelableExtra(Constants.ORIGIN_INTENT);
        if (null == originIntent) {
            return;
        }

        ComponentName hostComponent = intent.getComponent();
        intent.setComponent(originIntent.getComponent());
        intent.putExtra(Constants.HOST_COMPONENT, hostComponent);
        intent.removeExtra(Constants.ORIGIN_INTENT);
        originIntent.setExtrasClassLoader(pluginClassLoader);
        mergeExtras(intent, originIntent);
        intent.setExtrasClassLoader(pluginClassLoader);
    }

    /**
     * 4.x系统或某些定制机Intent中的Serializable数据反序列化时没有使用intent.setExtrasClassLoader(pluginClassLoader)设置的classloader，
     * 而是使用的VMStack.getClosestUserClassLoader,这意味着4.x的系统插件Intent中包含有自定义的Serializable对象只能在插件的类中
     * 才能被反序列化。为了能在宿主类中合并Intent中的数据，在4.x系统上直接合并Intent的序列化对象mParcelledData。
     * Parcel的内存结构为：
     * ------------------------------------------------------------------------------
     * |   dataLen(int)   |   BUNDLE_MAGIC(int)   |    item_count(int)    |   data
     * ------------------------------------------------------------------------------
     *将Parcel2合并到Parcel1的步骤为：
     * 1.将Parcel2从data开始追加到Parcel1后面
     * 2.修改Parcel1的dataLen为合并都的数据长度
     * 3.修改Parcel1的item_count为合并后台的序列化元素个数
     *
     * 该方法是将intent中的数据合并到originIntent，再用合并后的数据填充intent
     */
    private static void mergeExtras(Intent intent, Intent originIntent) {
        Bundle originBundle = originIntent.getExtras();
        if (null == originBundle) {
            return;
        }
        //序列化originIntent中的Bundle
        Parcel originParcel = Parcel.obtain();
        originBundle.writeToParcel(originParcel, 0);

        //序列化intent中的bundle
        Parcel intentParcel = Parcel.obtain();
        Bundle intentBundle = intent.getExtras();
        intentBundle.writeToParcel(intentParcel, 0);

        //读取intentParcel的前3个整数，分别为：数据长度，魔术数，保存的数据个数
        intentParcel.setDataPosition(0);
        int intentParcelLen = intentParcel.readInt();
        intentParcel.readInt();
        int intentParcelOffset = intentParcel.dataPosition();
        int start = intentParcel.dataPosition();
        int intentItemCount = intentParcel.readInt();
        int intSize = intentParcel.dataPosition() - start;

        //读取originParcel的前3个整数，分别为：数据长度，魔术数，保存的数据个数
        originParcel.setDataPosition(0);
        int originParcelLen = originParcel.readInt();
        originParcel.readInt();
        int originParcelOffset = originParcel.dataPosition();
        int originItemCount = originParcel.readInt();
        originParcel.setDataPosition(originParcel.dataPosition() - intSize);
        //修改originParcel的数据个数为合并后的数据个数
        originParcel.writeInt(intentItemCount + originItemCount);

        //将intentParcel追加到originParcel
        originParcel.setDataPosition(originParcelOffset + originParcelLen);
        intentParcel.setDataPosition(intentParcelLen + intentParcelOffset);
        originParcel.appendFrom(intentParcel, intentParcelOffset + intSize, intentParcelLen - intSize);
        originParcel.setDataPosition(0);
        //修改originParcel的数据长度为合并后的数据长度
        originParcel.writeInt(originParcelLen + intentParcelLen - intSize);
        originParcel.setDataPosition(0);

        //用合并后的的数据替换intent中的数据
        Bundle newBundle = new Bundle();
        newBundle.readFromParcel(originParcel);
        intent.replaceExtras(newBundle);
        originParcel.recycle();
        intentParcel.recycle();
    }
}
