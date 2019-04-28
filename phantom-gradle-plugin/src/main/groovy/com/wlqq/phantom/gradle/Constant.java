/*
 * Copyright (C) 2017-2019 Manbang Group
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

package com.wlqq.phantom.gradle;


import com.wlqq.phantom.gradle.dependency.ComparableVersion;

public class Constant {
    /**
     * 版本号
     */
    public static final String VER = "3.1.3";

    public static final String HOST_TAG = "phantom-host " + VER;

    public static final String PLUGIN_TAG = "phantom-plugin " + VER;

    /** 外部用户配置信息 */
    public static final String USER_CONFIG = "phantomPluginConfig";

    /**
     * Task 组
     */
    public static final String TASKS_GROUP = "phantom-plugin";

    /**
     * Task 前缀
     */
    private static final String TASKS_PREFIX = "ph";

    /**
     * Generate Task
     */
    public static final String TASK_GENERATE = TASKS_PREFIX + "Generate";

    /** 用户Task:安装插件 */
    public static final String TASK_INSTALL_PLUGIN = TASKS_PREFIX + "InstallPlugin";

    /** 配置例子 */
    public static final String CONFIG_EXAMPLE = "    // 这个 plugin 需要放在 android 配置之后，因为需要读取 android 中的配置项\n"
            + "    apply plugin: \"com.wlqq.phantom.plugin\"\n"
            + "\n"
            + "    phantomPluginConfig {\n"
            + "        hostApplicationId = \"com.wlqq.phantom.sample\"  // 宿主包名\n"
            + "        hostAppLauncherActivity = \"com.wlqq.phantom.sample.MainActivity\"  // 宿主 launcher Activity "
            + "full class name\n"
            + "\n"
            + "        pluginApplicationId = \"com.wlqq.phantom.plugin.sample1\" // 插件包名\n"
            + "        pluginVersionName = \"1.0.0\"    // 插件版本名\n"
            + "        // 其它配置 ...\n"
            + "    }";

    public static final ComparableVersion AGP_3_0 = new ComparableVersion("3.0.0");
    public static final ComparableVersion AGP_3_1 = new ComparableVersion("3.1.0");

    public static final String AGP_VERSION = "AGP_VERSION";

    public static final String INTERMEDIATES_DIR = "phantom";

    private Constant() {
        // prevent instantiation
    }
}
