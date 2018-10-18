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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.wlqq.phantom.library.PhantomCore;
import com.wlqq.phantom.library.env.Constants;

/**
 * 判断 Intent 是否交由插件处理
 */
abstract class IntentResolver {
    /**
     * 判断是否由插件代理处理 Intent
     *
     * @param intent 要启动的组件
     * @return 要启动的组件所属的插件包名；若找不到对应的插件，则返回 <b>null</b>，表明由系统来处理
     * @see com.wlqq.phantom.library.proxy.ActivityHostProxy
     * @see com.wlqq.phantom.library.proxy.ServiceHostProxy
     */
    @Nullable
    String resolveIntentTarget(@NonNull Intent intent) {
        final ComponentName component = intent.getComponent();
        VLog.v("intent ComponentName: %s", component);
        if (component == null) {
            // 隐式 Intent ,交给系统处理
            return null;
        }

        // 若 Intent 从插件中发出，则 Intent 会携带插件的包名
        final String sourcePackageName = intent.getStringExtra(Constants.EXTRA_FROM_PLUGIN);
        VLog.v("sourcePackageName: %s", sourcePackageName);
        if (TextUtils.isEmpty(sourcePackageName)) {
            // 对于从宿主中发出的 Intent, 则只检查要启动的组件是否在某个插件中
            // - 若在，则启动相应插件的 Activity
            // - 否则交由系统处理。
            final String pluginPackageName = getPluginPackageName(intent);
            VLog.v("getPluginPackageName: %s", pluginPackageName);
            return pluginPackageName;
        }

        intent.removeExtra(Constants.EXTRA_FROM_PLUGIN);
        // 对于从插件发出的 Intent, 组件查找顺序
        // 1. 自身的插件
        // 2. 宿主
        // 3. 其他的插件
        // 4. 其他应用
        final String hostPackageName = PhantomCore.getInstance().getHostPkg();
        final String intentPackageName = component.getPackageName();
        VLog.v("hostPackageName: %s, intentPackageName: %s", hostPackageName, intentPackageName);
        if (hostPackageName.equals(intentPackageName)) {
            // 包名是宿主包名，可能是
            // 1. 启动自身插件中的组件
            // 2. 启动宿主中的组件，交给系统处理
            final String pluginPackageName = getPluginPackageName(
                    new ComponentName(sourcePackageName, component.getClassName()));
            VLog.v("getPluginPackageName: %s", pluginPackageName);
            return pluginPackageName;
        } else {
            // 包名不是宿主包名，可能是
            // 1. 启动其他插件中的组件
            // 2. 启动系统其他应用中的组件，交给系统处理
            final String pluginPackageName = getPluginPackageName(intent);
            VLog.v("getPluginPackageName: %s", pluginPackageName);
            return pluginPackageName;
        }
    }

    /**
     * 获取要启动的组件所属的插件包名；若找不到对应的插件，则返回 <b>null</b>
     *
     * @param intent 要启动的组件
     * @return 要启动的组件所属的插件包名；若找不到对应的插件，则返回 <b>null</b>
     */
    @Nullable
    private String getPluginPackageName(@NonNull Intent intent) {
        final ComponentName component = intent.getComponent();
        if (component == null) {
            return null;
        }
        return getPluginPackageName(component);
    }

    @Nullable
    protected abstract String getPluginPackageName(@NonNull ComponentName component);
}
