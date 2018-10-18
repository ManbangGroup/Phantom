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

import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;


public class ResourcesProxy extends Resources {
    private final String mPluginPackage;

    /**
     * Create a new Resources object on top of an existing set of assets in an
     * AssetManager.
     *
     * @param assets        Previously created AssetManager.
     * @param metrics       Current display metrics to consider when
     *                      selecting/computing resource values.
     * @param config        Desired device configuration to consider when
     * @param pluginPackage 插件包名
     */
    public ResourcesProxy(AssetManager assets, DisplayMetrics metrics, Configuration config, String pluginPackage) {
        super(assets, metrics, config);
        this.mPluginPackage = pluginPackage;
    }

    @Override
    public int getIdentifier(String name, String defType, String defPackage) {
        int idt = super.getIdentifier(name, defType, mPluginPackage);

        if (idt == 0) {
            idt = super.getIdentifier(name, defType, defPackage);
        }

        return idt;
    }
}
