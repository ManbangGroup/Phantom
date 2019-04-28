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

package com.wlqq.phantom.gradle.utils;

import org.joor.Reflect;
import org.joor.ReflectException;

public class VersionUtils {
    // <= 3.0
    private static final String VERSION_3_ZERO_FIELD = "com.android.builder.Version";
    // > 3.1
    private static final String VERSION_3_ONE_FIELD = "com.android.builder.model.Version";
    private static final String AGP_VERSION_FIELD = "ANDROID_GRADLE_PLUGIN_VERSION";

    private VersionUtils() {
        // prevent instantiation
    }

    public static String getAgpVersion() {

        String gradlePluginVersion = "";
        try {
            gradlePluginVersion = Reflect.on(VERSION_3_ZERO_FIELD).get(AGP_VERSION_FIELD);
        } catch (ReflectException e) {
        }

        try {
            gradlePluginVersion = Reflect.on(VERSION_3_ONE_FIELD).get(AGP_VERSION_FIELD);
        } catch (ReflectException e) {
        }

        return gradlePluginVersion;
    }
}
