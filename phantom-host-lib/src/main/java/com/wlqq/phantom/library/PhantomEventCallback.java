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

import android.support.annotation.NonNull;

import com.wlqq.phantom.library.pm.PluginInfo;
import com.wlqq.phantom.library.pm.InstallResult;

/**
 * Phantom 相关事件回调
 * <ul>
 * <li>插件安装</li>
 * <li>插件启动</li>
 * </ul>
 */
public interface PhantomEventCallback {
    /**
     * 插件安装开始
     * <p>
     * <b>注意：</b>该回调方法会在调用 {@link PhantomCore#installPlugin(String)}, {@link PhantomCore#installPlugin(String, String,
     * String)}, {@link PhantomCore#installPluginFromAssets(String)}的线程里调用
     *
     * @param name       插件名字，包含 包名 和 版本名
     * @param fromAssets 安装来源是否是 assets
     * @see PhantomCore#installPlugin(String)
     * @see PhantomCore#installPlugin(String, String, String)
     * @see PhantomCore#installPluginFromAssets(String)
     */
    void onPluginInstallStart(String name, boolean fromAssets);

    /**
     * 插件安装成功
     * <p>
     * <b>注意：</b>该回调方法会在调用 {@link PhantomCore#installPlugin(String)}, {@link PhantomCore#installPlugin(String, String,
     * String)}, {@link PhantomCore#installPluginFromAssets(String)}的线程里调用
     *
     * @param name          插件名字，包含 包名 和 版本名
     * @param fromAssets    安装来源是否是 assets
     * @param installResult 安装结果
     * @see PhantomCore#installPlugin(String)
     * @see PhantomCore#installPlugin(String, String, String)
     * @see PhantomCore#installPluginFromAssets(String)
     */
    void onPluginInstallSuccess(String name, boolean fromAssets, @NonNull InstallResult installResult);

    /**
     * 插件安装失败
     * <p>
     * <b>注意：</b>该回调方法会在调用 {@link PhantomCore#installPlugin(String)}, {@link PhantomCore#installPlugin(String, String,
     * String)}, {@link PhantomCore#installPluginFromAssets(String)}的线程里调用
     *
     * @param name          插件名字，包含 包名 和 版本名
     * @param fromAssets    安装来源是否是 assets
     * @param installResult 安装结果
     * @see PhantomCore#installPlugin(String)
     * @see PhantomCore#installPlugin(String, String, String)
     * @see PhantomCore#installPluginFromAssets(String)
     */
    void onPluginInstallFail(String name, boolean fromAssets, @NonNull InstallResult installResult);

    /**
     * 插件启动开始
     * <p>
     * <b>注意：</b>该回调方法会在调用 {@link PluginInfo#start()} 的线程里调用
     *
     * @param pluginInfo 已安装插件信息
     * @param firstStart 是否是安装之后首次启动
     * @see PluginInfo#start()
     */
    void onPluginStartStart(@NonNull PluginInfo pluginInfo, boolean firstStart);

    /**
     * 插件启动成功
     * <p>
     * <b>注意：</b>该回调方法会在调用 {@link PluginInfo#start()} 的线程里调用
     *
     * @param pluginInfo 已安装插件信息
     * @param firstStart 是否是安装之后首次启动
     * @see PluginInfo#start()
     */
    void onPluginStartSuccess(@NonNull PluginInfo pluginInfo, boolean firstStart);

    /**
     * 插件启动失败
     * <p>
     * <b>注意：</b>该回调方法会在调用 {@link PluginInfo#start()} 的线程里调用
     *
     * @param pluginInfo 已安装插件信息
     * @param firstStart 是否是安装之后首次启动
     * @param throwable  启动异常
     * @see PluginInfo#start()
     */
    void onPluginStartFail(@NonNull PluginInfo pluginInfo, boolean firstStart, @NonNull Throwable throwable);
}
