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

import android.util.TimingLogger;

import com.wlqq.phantom.library.env.Constants;

import dalvik.system.DexClassLoader;

public class PluginClassLoader extends DexClassLoader {
    private static final boolean PERF_CLASS_LOADING = false;

    public PluginClassLoader(String dexPath, String optimizedDirectory, String libraryPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, libraryPath, parent);
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        TimingLogger logger = null;

        if (PERF_CLASS_LOADING) {
            logger = new TimingLogger(Constants.TAG, "PluginClassLoader#loadClass -> " + className);
        }

        Class<?> clz = null;
        try {
            clz = findClass(className);

            if (PERF_CLASS_LOADING) {
                logger.addSplit("findClass");
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }

        if (null == clz) {
            clz = super.loadClass(className);

            if (PERF_CLASS_LOADING) {
                logger.addSplit("super#loadClass");
            }
        }

        if (PERF_CLASS_LOADING) {
            logger.dumpToLog();
        }

        return clz;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }
}
