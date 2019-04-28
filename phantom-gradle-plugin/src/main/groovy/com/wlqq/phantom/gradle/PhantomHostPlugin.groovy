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

package com.wlqq.phantom.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.wlqq.phantom.gradle.dependency.ComparableVersion
import com.wlqq.phantom.gradle.dependency.CompileDependenciesFileGenerator
import com.wlqq.phantom.gradle.utils.Log
import com.wlqq.phantom.gradle.utils.VersionUtils
import org.gradle.api.Plugin
import org.gradle.api.Project


class PhantomHostPlugin implements Plugin<Project> {
    def static TAG = Constant.HOST_TAG

    @Override
    void apply(Project project) {
        Log.i(TAG, "apply plugin")

        if (project.plugins.hasPlugin(AppPlugin)) {
            def android = project.extensions.getByType(AppExtension)

            def version = new ComparableVersion(VersionUtils.getAgpVersion())
            Log.i(TAG, "Your Gradle Android Plugin version is: ${version}")
            project.extensions.extraProperties[Constant.AGP_VERSION] = version

            android.applicationVariants.all { ApplicationVariantImpl variant ->
                def variantData = variant.variantData
                def scope = variantData.scope

                // builtin_plugin_list.csv generate task
                def generateCompileDependenciesTaskName = scope.getTaskName(Constant.TASK_GENERATE, 'CompileDependencies')
                def generateCompileDependenciesTask = project.task(generateCompileDependenciesTaskName)
                generateCompileDependenciesTask.group = Constant.TASKS_GROUP

                // depends on mergeAssets Task
                def mergeAssetsTaskName = variant.variantData.scope.mergeAssetsTask.name
                def mergeAssetsTask = project.tasks.getByName(mergeAssetsTaskName)
                if (mergeAssetsTask) {
                    generateCompileDependenciesTask.doLast {
                        new CompileDependenciesFileGenerator(project, variant, mergeAssetsTask.outputDir, 'compile_dependencies.txt').generateFile()
                    }

                    generateCompileDependenciesTask.dependsOn mergeAssetsTask
                    mergeAssetsTask.finalizedBy generateCompileDependenciesTask
                }
            }
        }
    }

}

