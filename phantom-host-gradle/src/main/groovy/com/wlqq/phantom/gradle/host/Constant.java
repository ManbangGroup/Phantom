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

package com.wlqq.phantom.gradle.host;


class Constant {
    /**
     * 版本号
     */
    private static final String VER = "3.1.0";

    /**
     * 打印信息时候的前缀
     */
    static final String TAG = "[ phantom-host-v" + VER + " ]";

    /**
     * Task 组
     */
    static final String TASKS_GROUP = "phantom-plugin";

    /**
     * Task 前缀
     */
    private static final String TASKS_PREFIX = "ph";

    /**
     * Generate Task
     */
    static final String TASK_GENERATE = TASKS_PREFIX + "Generate";

    static final ComparableVersion AGP_3_0 = new ComparableVersion("3.0.0");
    static final ComparableVersion AGP_3_1 = new ComparableVersion("3.1.0");

    static final String AGP_VERSION = "AGP_VERSION";

    private Constant() {}
}
