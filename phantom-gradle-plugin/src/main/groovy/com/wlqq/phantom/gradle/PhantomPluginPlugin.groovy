

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
import com.wlqq.phantom.gradle.debugger.PhantomDebugger
import com.wlqq.phantom.gradle.dependency.ComparableVersion
import com.wlqq.phantom.gradle.dependency.ProvidedDependenciesFileGenerator
import com.wlqq.phantom.gradle.exclude.ExcludeClassesTransform
import com.wlqq.phantom.gradle.replace.ReplaceSuperTransform
import com.wlqq.phantom.gradle.utils.Log
import com.wlqq.phantom.gradle.utils.VersionUtils
import org.gradle.api.Plugin
import org.gradle.api.Project


class PhantomPluginPlugin implements Plugin<Project> {
    def static TAG = Constant.PLUGIN_TAG

    @Override
    void apply(Project project) {
        Log.i(TAG, "apply plugin")

        if (project.plugins.hasPlugin(AppPlugin)) {
            def extensions = project.extensions

            extensions.create(Constant.USER_CONFIG, PhantomPluginConfig)

            PhantomPluginConfig config = extensions.getByName(Constant.USER_CONFIG)

            def android = extensions.getByType(AppExtension)

            def version = new ComparableVersion(VersionUtils.getAgpVersion())
            Log.i(TAG, "Your Gradle Android Plugin version is: ${version}")
            project.extensions.extraProperties[Constant.AGP_VERSION] = version

            android.applicationVariants.all { variant ->
                def variantData = variant.variantData
                def scope = variantData.scope

                if (config.genProvidedDeps) {
                    // provided_dependencies_v2.txt generate task
                    def generateProvidedDependenciesTaskName = scope.getTaskName(Constant.TASK_GENERATE, 'ProvidedDependencies')
                    def generateProvidedDependenciesTask = project.task(generateProvidedDependenciesTaskName)
                    generateProvidedDependenciesTask.group = Constant.TASKS_GROUP

                    //depends on mergeAssets Task
                    def mergeAssetsTaskName = variant.getVariantData().getScope().getMergeAssetsTask().name
                    def mergeAssetsTask = project.tasks.getByName(mergeAssetsTaskName)
                    if (mergeAssetsTask) {
                        generateProvidedDependenciesTask.doLast {
                            new ProvidedDependenciesFileGenerator(project, variant, mergeAssetsTask.outputDir, 'provided_dependencies_v2.txt').generateFile()
                        }

                        generateProvidedDependenciesTask.dependsOn mergeAssetsTask
                        mergeAssetsTask.finalizedBy generateProvidedDependenciesTask
                    }
                }

                PhantomDebugger pluginDebugger = new PhantomDebugger(project, config, variant)

                def assembleTask = variant.getAssemble()

                def installPluginTaskName = scope.getTaskName(Constant.TASK_INSTALL_PLUGIN, "")
                def installPluginTask = project.task(installPluginTaskName)

                installPluginTask.doLast {
                    pluginDebugger.init()

                    pluginDebugger.checkConfig()

                    pluginDebugger.checkHostRunning()

                    pluginDebugger.install()
                    // 等待宿主异步安装插件完成
                    Thread.sleep(5000)

                    pluginDebugger.forceStopHostApp()

                    Thread.sleep(1000)

                    pluginDebugger.startHostApp()
                }
                installPluginTask.group = Constant.TASKS_GROUP

                if (assembleTask) {
                    installPluginTask.dependsOn assembleTask
                }
            }

            project.android.registerTransform(new ExcludeClassesTransform(project))
            project.android.registerTransform(new ReplaceSuperTransform(project))
        }
    }

}

