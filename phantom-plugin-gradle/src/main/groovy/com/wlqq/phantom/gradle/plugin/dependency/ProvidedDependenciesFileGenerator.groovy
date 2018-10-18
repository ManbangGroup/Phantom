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
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.UnknownConfigurationException


class ProvidedDependenciesFileGenerator extends FileGenerator {
    Project project

    ProvidedDependenciesFileGenerator(Project project, File outputFileDir, String outputFileName) {
        super(outputFileDir, outputFileName)
        this.project = project
    }

    @Override
    protected String getContent() {
        def dependencies = resolveProvidedDependencies()
        return dependencies.join('\n')
    }

    private Set<String> findDependencies(String type) {
        Set<String> dependencies = []
        project.rootProject.allprojects.each {
            try {
                it.configurations.getByName(type)
            } catch (UnknownConfigurationException e) {
                return;
            }

            Iterator<Dependency> iterator = it.configurations.getByName(type).dependencies.iterator()
            while (iterator.hasNext()) {
                Dependency dependency = iterator.next()
                if (null != dependency.group && "unspecified" != dependency.version) {
                    dependencies.add("${dependency.group}:${dependency.name}:${dependency.version}")
                }
            }
        }

        return dependencies
    }

    private Set<String> resolveProvidedDependencies() {
        def dependencies = findDependencies('provided').findAll { gav ->
            // 忽略掉 com.wlqq.phantom:phantom-plugin-lib
            !gav.startsWith('com.wlqq.phantom:phantom-plugin-lib:')
        }

        def excludes = project.extensions.findByName(Constant.USER_CONFIG)
        if (null != excludes) {
            def compileLibs = findDependencies('compile')
            // excludeLib type : ExcludeConfig
            for (def excludeLib : excludes.excludeLibs) {
                compileLibs.each { compileLib ->
                    if (compileLib == excludeLib.name) {
                        dependencies.add("${excludeLib.groupId}:${excludeLib.artifactId}:${excludeLib.versionRequirement}")
                    }
                }
            }
        }

        return dependencies
    }
}
