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

package com.wlqq.phantom.gradle.dependency

import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.wlqq.phantom.gradle.Constant
import com.wlqq.phantom.gradle.utils.Log
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.util.GFileUtils

import java.nio.file.Paths

abstract class FileGenerator {
    protected Project project;
    protected ApplicationVariantImpl applicationVariant;
    File outputFileDir
    String outputFileName
    String tag

    FileGenerator(String tag, Project project, ApplicationVariantImpl variant, File outputFileDir, String outputFileName) {
        this.project = project
        this.applicationVariant = variant
        this.tag = tag
        this.outputFileDir = outputFileDir
        this.outputFileName = outputFileName
    }

    protected abstract String getContent()

    final void generateFile() throws GradleException {
        try {
            if (!outputFileDir.exists()) {
                Log.i(tag, "mkdirs ${outputFileDir.absolutePath}: ${outputFileDir.mkdirs()}")
            }

            final def outputFile = new File(outputFileDir, outputFileName)
            outputFile.withWriter("utf-8") { writer ->
                // write to file
                writer.write(content)

                // dump to console
                Log.i(tag, "[createPropertiesFile: ${outputFileName}] ====== BEGIN DUMP ======")
                Log.i(tag, content)
                Log.i(tag, "[createPropertiesFile: ${outputFileName}] ====== END   DUMP ======")
            }

            // backup file to <application module>/build/intermediates/phantom/<build variant>/<outputFileName> for debug purpose
            final def scope = applicationVariant.variantData.scope
            final def intermediatesFile = new File(scope.globalScope.intermediatesDir,
                    Paths.get(Constant.INTERMEDIATES_DIR, scope.variantConfiguration.dirName, outputFileName).toString())
            GFileUtils.copyFile(outputFile, intermediatesFile)

        } catch (Exception e) {
            throw new GradleException("error generateFile", e)
        }
    }

}