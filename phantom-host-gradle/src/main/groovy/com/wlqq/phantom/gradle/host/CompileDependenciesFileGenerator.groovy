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

package com.wlqq.phantom.gradle.host

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.artifacts.result.ResolvedComponentResult


class CompileDependenciesFileGenerator extends FileGenerator {
    Project project

    CompileDependenciesFileGenerator(Project project, File outputFileDir, String outputFileName) {
        super(outputFileDir, outputFileName)
        this.project = project
    }

    @Override
    protected String getContent() {
        def dependencies = resolveCompileDependencies()
        return dependencies.join('\n')
    }

    private Set<String> resolveCompileDependencies() {
        Set<String> compileLibs = []

        Configuration configuration = project.getConfigurations().getByName("compile")
        if (configuration != null && configuration.isCanBeResolved()) {
            ResolvableDependencies incoming = configuration.getIncoming()
            ResolutionResult resolutionResult = incoming.getResolutionResult()
            Set<ResolvedComponentResult> components = resolutionResult.getAllComponents()
            components.each { ResolvedComponentResult result ->
                ModuleVersionIdentifier identifier = result.getModuleVersion()
                if (identifier != null && "unspecified" != identifier.version) {
                    compileLibs.add("${identifier.group}:${identifier.name}:${identifier.version}")
                }
            }
        }

        return compileLibs
    }
}
