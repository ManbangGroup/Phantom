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

package com.wlqq.phantom.gradle.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.wlqq.phantom.gradle.plugin.debugger.PhantomDebugger
import com.wlqq.phantom.gradle.plugin.dependency.ProvidedDependenciesFileGenerator
import com.wlqq.phantom.gradle.plugin.exclude.ExcludeClassesTransform
import com.wlqq.phantom.gradle.plugin.replace.ReplaceSuperTransform
import com.wlqq.phantom.gradle.plugin.utils.Utils
import org.gradle.api.Plugin
import org.gradle.api.Project


class PhantomPluginPlugin implements Plugin<Project> {
    def static TAG = Constant.TAG

    @Override
    public void apply(Project project) {
        println "${TAG} Welcome to Phantom world!"

        if (project.plugins.hasPlugin(AppPlugin)) {
            def extensions = project.extensions

            extensions.create(Constant.USER_CONFIG, PhantomPluginConfig)

            def config = extensions.getByName(Constant.USER_CONFIG)

            def android = extensions.getByType(AppExtension)

            def version = new ComparableVersion(Utils.getAgpVersion())
            println "${TAG} Your Gradle Android Plugin version is: ${version}"
            project.extensions.extraProperties[Constant.AGP_VERSION] = version

            android.applicationVariants.all { variant ->
                def variantData = variant.variantData
                def scope = variantData.scope

                // provided_dependencies_v2.txt generate task
                def generateCompileDependenciesTaskName = scope.getTaskName(Constant.TASK_GENERATE, 'ProvidedDependencies')
                def generateCompileDependenciesTask = project.task(generateCompileDependenciesTaskName)
                generateCompileDependenciesTask.group = Constant.TASKS_GROUP

                //depends on mergeAssets Task
                def mergeAssetsTaskName = variant.getVariantData().getScope().getMergeAssetsTask().name
                def mergeAssetsTask = project.tasks.getByName(mergeAssetsTaskName)
                if (mergeAssetsTask) {
                    generateCompileDependenciesTask.doLast {
                        new ProvidedDependenciesFileGenerator(project, variant, mergeAssetsTask.outputDir, 'provided_dependencies_v2.txt').generateFile()
                    }

                    generateCompileDependenciesTask.dependsOn mergeAssetsTask
                    mergeAssetsTask.finalizedBy generateCompileDependenciesTask
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

