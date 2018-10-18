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

import android.support.annotation.NonNull;


public final class ClassUtils {
    private ClassUtils() {
        // prevent instantiation
    }

    @NonNull
    public static String getSimpleName(@NonNull String className) {
        final int dot = className.lastIndexOf('.');
        if (dot > 0) {
            return className.substring(dot + 1); // strip the package name
        } else {
            return className;
        }
    }
}
