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

package com.wlqq.phantom.library.pm;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * 插件安装结果
 *
 * @see com.wlqq.phantom.library.PhantomCore#installPlugin(String)
 * @see com.wlqq.phantom.library.PhantomCore#installPlugin(String, String, String)
 * @see com.wlqq.phantom.library.PhantomCore#installPluginFromAssets(String)
 */
public class InstallResult {
    /**
     * 错误：插件 APK 不存在
     */
    public static final int ERR_FILE_NOT_EXIST = 1;
    /**
     * 错误：解析插件 APK 失败
     */
    public static final int ERR_PARSE_APK = 2;
    /**
     * 错误：拷贝插件 APK 文件 IO 异常
     */
    public static final int ERR_IO_EXCEPTION = 3;
    /**
     * 错误：插件 APK 的签名和宿主不一致
     */
    public static final int ERR_SIGNATURE_MISMATCH = 4;
    /**
     * 成功：全新安装
     */
    public static final int ERR_INSTALL_NEW = 5;
    /**
     * 成功：升级安装
     */
    public static final int ERR_INSTALL_UPGRADE = 6;
    /**
     * 成功：升级包安装失败，继续使用老包
     */
    public static final int ERR_INSTALL_NOT_UPGRADE = 7;
    /**
     * 失败：插件 APK 是用于 Apkplug 插件框架
     *
     * @deprecated
     */
    @Deprecated
    public static final int ERR_APKPLUG_APK = 8;
    /**
     * 失败：宿主提供的服务满足不了插件的要求(服务名/版本号)
     *
     * @see PluginInfo#META_DATA_KEY_EXPORT_SERVICE_PREFIX
     * @see PluginInfo#META_DATA_KEY_IMPORT_SERVICE_PREFIX
     */
    public static final int ERR_PHANTOM_SERVICE_DEPENDENCY_MISMATCH = 9;
    /**
     * 失败：拷贝 native so 出错
     */
    public static final int ERR_COPY_NATIVE_SO = 10;
    /**
     * 失败：解析插件声明的依赖宿主提供的公共库属性文件 provided_dependencies.properties 失败
     */
    public static final int ERR_PARSE_PROVIDED_LIBRARIES = 11;
    /**
     * 失败：宿主提供的公共库不能满足插件声明的依赖要求
     *
     * @see PluginManager#COMPILE_DEPENDENCIES_FILE
     * @see PluginManager#PROVIDED_DEPENDENCIES_V2_FILE
     */
    public static final int ERR_SHARED_LIBRARY_DEPENDENCY_MISMATCH = 12;
    /**
     * 结果状态码
     *
     * @see #ERR_FILE_NOT_EXIST
     * @see #ERR_PARSE_APK
     * @see #ERR_IO_EXCEPTION
     * @see #ERR_SIGNATURE_MISMATCH
     * @see #ERR_INSTALL_NEW
     * @see #ERR_INSTALL_UPGRADE
     * @see #ERR_INSTALL_NOT_UPGRADE
     * @see #ERR_PHANTOM_SERVICE_DEPENDENCY_MISMATCH
     * @see #ERR_COPY_NATIVE_SO
     * @see #ERR_PARSE_PROVIDED_LIBRARIES
     * @see #ERR_SHARED_LIBRARY_DEPENDENCY_MISMATCH
     */
    public final int status;
    /**
     * 结果描述
     */
    public final String message;
    /**
     * 可选的异常
     */
    @Nullable
    public final Throwable throwable;
    /**
     * 插件 APK 安装成功后，生成的插件信息 {@link PluginInfo}
     */
    @Nullable
    public final PluginInfo plugin;

    /**
     * 构造方法
     *
     * @param status    状态码
     * @param message   描述信息
     * @param throwable 异常信息
     * @param plugin    插件信息
     */
    InstallResult(int status, @NonNull String message, @Nullable Throwable throwable,
            @Nullable PluginInfo plugin) {
        this.status = status;
        this.message = message;
        this.throwable = throwable;
        this.plugin = plugin;
    }

    /**
     * 构造方法
     *
     * @param status    状态码
     * @param message   描述信息
     * @param throwable 异常信息
     */
    InstallResult(int status, @NonNull String message, @Nullable Throwable throwable) {
        this(status, message, throwable, null);
    }

    /**
     * 构造方法
     *
     * @param status  状态码
     * @param message 描述信息
     * @param plugin  插件信息
     */
    InstallResult(int status, @NonNull String message, @Nullable PluginInfo plugin) {
        this(status, message, null, plugin);
    }

    /**
     * 构造方法
     *
     * @param status  状态码
     * @param message 描述信息
     */
    InstallResult(int status, @NonNull String message) {
        this(status, message, null, null);
    }

    /**
     * 是否安装成功
     *
     * @return true 安装成功；false 安装失败
     */
    public boolean isSuccess() {
        switch (status) {
            case ERR_INSTALL_NEW:
            case ERR_INSTALL_UPGRADE:
            case ERR_INSTALL_NOT_UPGRADE:
                return true;
            default:
                return false;
        }
    }

    @SuppressWarnings({"PMD.ConsecutiveAppendsShouldReuse", "PMD.InsufficientStringBufferDeclaration",
            "PMD.ConsecutiveLiteralAppends"})
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("InstallResult{");
        sb.append("status=").append(status);
        sb.append(", message='").append(message).append('\'');
        sb.append(", throwable=").append(throwable);
        sb.append(", plugin=").append(plugin);
        sb.append('}');
        return sb.toString();
    }
}
