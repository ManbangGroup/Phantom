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

package com.wlqq.phantom.gradle.utils


import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

interface JarVisitor {
    /**
     * 访问Jar文件中的每个entry
     *
     * @param entry 当前被访问的entry
     * @param inputJar 读取Jar的inputStream
     * @param outputJar Jar输出到的outputStream
     * @return true表示将当前entry输出到outputJar，false表示过滤掉当前entry
     */
    boolean visitEntry(ZipEntry entry, ZipInputStream inputJar, ZipOutputStream outputJar)
}
