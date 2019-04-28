
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

package com.wlqq.phantom.gradle.exclude

import com.wlqq.phantom.gradle.utils.IOUtils

import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream;

/**
 * 用户获取jar/aar中以及module工程输出的 assets，res/drawable, res/layout文件列表
 */
class ExcludeFileUtils {
    /**
     * 获取jar中的lib，assets文件列表
     * @return
     */
    public static Set<String> getOtherExcludeFromJar(String jarPath) {
        Set<String> excludeList = new HashSet<>();
        Pattern soPattern = Pattern.compile("lib/[^/]+/[^/]+\\.so")

        ZipInputStream zis = null
        try {
            zis = new ZipInputStream(new FileInputStream(jarPath))
            ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String entryName = entry.getName()
                if (soPattern.matcher(entryName).matches()
                        || entryName.startsWith("assets/")) {
                    excludeList.add(entryName)
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(zis);
        }

        return excludeList
    }

    /**
     * 获取aar中的lib，assets文件列表
     * @return
     */
    public static Set<String> getOtherExcludeFromAar(String aarPath) {
        Set<String> excludeList = new HashSet<>();
        Pattern soPattern = Pattern.compile("jni/[^/]+/[^/]+\\.so")
        ZipInputStream zis = null
        try {
            zis = new ZipInputStream(new FileInputStream(aarPath))
            ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String entryName = entry.getName()
                if (soPattern.matcher(entryName).matches()) {
                    //这里得到的entryName类似jni/armeabi/xx.so
                    //需要替换成**/armeabi/xx.so
                    excludeList.add('**/' + entryName.substring(4))
                }

                if (entryName.startsWith("assets/")) {
                    excludeList.add(entryName)
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(zis);
        }

        return excludeList
    }

    /**
     * 获取module输出目录中的lib，assets文件列表
     * @return
     */
    public static Set<String> getOtherExcludeFromDir(String path) {
        File rootDir = new File(path)
        Set<String> excludeList = new HashSet<>()

        File jniDir = new File(rootDir, 'jni')

        if (jniDir.exists()) {
            dirBrowser(excludeList, "**", jniDir)
        }

        File assetsDir = new File(rootDir, 'assets')
        if (assetsDir.exists()) {
            dirBrowser(excludeList, "assets", assetsDir)
        }

        return excludeList
    }

    private static void dirBrowser(Set<String> nameSet, String base, File baseDir) {
        File[] files = baseDir.listFiles()
        for (File file : files) {
            if (file.isDirectory()) {
                dirBrowser(nameSet, base + "/" + file.name, file)
            } else {
                nameSet.add(base == '' ? file.name : base + "/" + file.name)
            }
        }
    }
}
