/*
 * Copyright (C) 2017-2018 Manbang Group
 * Copyright (C) 2005-2017 Qihoo 360 Inc.
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

package com.wlqq.phantom.gradle.plugin

/**
 * Modified from https://github.com/Qihoo360/RePlugin/blob/dev/replugin-plugin-gradle/src/main/groovy/com/qihoo360/replugin/gradle/plugin/AppConstant.groovy
 */
class Constant {
    /** 版本号 */
    def static final VER = "3.1.0"

    /** 打印信息时候的前缀 */
    def static final TAG = "[ phantom-plugin-v${VER} ]"

    /** 外部用户配置信息 */
    def static final USER_CONFIG = "phantomPluginConfig"

    /** 用户Task组 */
    def static final TASKS_GROUP = "phantom-plugin"

    /** Task前缀 */
    def static final TASKS_PREFIX = "ph"

    /** 用户Task:Generate任务 */
    def static final TASK_GENERATE = TASKS_PREFIX + "Generate"

    /** 用户Task:安装插件 */
    def static final TASK_INSTALL_PLUGIN = TASKS_PREFIX + "InstallPlugin"

    /** 配置例子 */
    static final String CONFIG_EXAMPLE = '''
// 这个 plugin 需要放在 android 配置之后，因为需要读取 android 中的配置项
apply plugin: "com.wlqq.phantom.plugin"

phantomPluginConfig {
    hostApplicationId = "com.wlqq.phantom.sample"  // 宿主包名
    hostAppLauncherActivity = "com.wlqq.phantom.sample.MainActivity"  // 宿主 launcher Activity full class name

    pluginApplicationId = "com.wlqq.phantom.plugin.sample1" // 插件包名
    pluginVersionName = "1.0.0"    // 插件版本名
    // 其它配置 ...
}
'''
    static final ComparableVersion AGP_3_0 = new ComparableVersion("3.0.0")
    static final ComparableVersion AGP_3_1 = new ComparableVersion("3.1.0")

    static final String AGP_VERSION = "AGP_VERSION"

    private Constant() {}
}
