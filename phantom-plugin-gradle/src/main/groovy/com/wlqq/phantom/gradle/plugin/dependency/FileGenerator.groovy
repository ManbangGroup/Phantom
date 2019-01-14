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

package com.wlqq.phantom.gradle.plugin.dependency

import com.wlqq.phantom.gradle.plugin.Constant
import org.gradle.api.GradleException

abstract class FileGenerator {
    File outputFileDir
    String outputFileName

    FileGenerator(File outputFileDir, String outputFileName) {
        this.outputFileDir = outputFileDir
        this.outputFileName = outputFileName
    }

    protected abstract String getContent()

    final void generateFile() throws GradleException {
        try {
            if (!outputFileDir.exists()) {
                println "${Constant.TAG} mkdirs ${outputFileDir.absolutePath}: ${outputFileDir.mkdirs()}"
            }

            def outputFile = new File(outputFileDir, outputFileName)
            outputFile.withWriter("utf-8") { writer ->
                // write to file
                writer.write(content)

                // dump to console
                println "${Constant.TAG} [createPropertiesFile: ${outputFileName}] ====== BEGIN DUMP ======"
                println content
                println "${Constant.TAG} [createPropertiesFile: ${outputFileName}] ====== END   DUMP ======"
            }
        } catch (Exception e) {
            throw new GradleException("generateFile exception", e)
        }
    }
}