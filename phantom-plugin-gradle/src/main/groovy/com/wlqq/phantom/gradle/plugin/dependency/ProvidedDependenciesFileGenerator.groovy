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

import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.build.gradle.internal.ide.ArtifactDependencyGraph
import com.android.build.gradle.internal.ide.ModelBuilder
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.google.common.collect.ImmutableMap
import com.wlqq.phantom.gradle.plugin.ComparableVersion
import com.wlqq.phantom.gradle.plugin.Constant
import com.wlqq.phantom.gradle.plugin.PhantomPluginConfig
import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.joor.Reflect

class ProvidedDependenciesFileGenerator extends FileGenerator {
    private static final def COMPILE_ONLY_FILTER = { String gav ->
        !gav.startsWith('com.wlqq.phantom:phantom-plugin-lib:') &&
                !gav.startsWith('com.android.tools.build:gradle:') &&
                !gav.startsWith('com.google.android:android:')
    }

    Project project
    ApplicationVariantImpl applicationVariant
    ComparableVersion agpVersion


    ProvidedDependenciesFileGenerator(Project project, ApplicationVariantImpl variant, File outputFileDir, String outputFileName) {
        super(outputFileDir, outputFileName)
        this.project = project
        this.applicationVariant = variant
        this.agpVersion = (ComparableVersion) project.extensions.extraProperties[Constant.AGP_VERSION]
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
                return
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

        def dependencies = findDependencies('compileOnly').findAll(COMPILE_ONLY_FILTER)

        if (dependencies.empty) {
            dependencies = findDependencies('provided').findAll(COMPILE_ONLY_FILTER)
        }

        PhantomPluginConfig config = project.extensions.findByName(Constant.USER_CONFIG)
        if (null != config) {
            def compileLibs = getCompileArtifacts()
            // excludeLib type : ExcludeConfig
            for (def excludeLib : config.excludeLibs) {
                compileLibs.each { compileLib ->
                    if (compileLib == excludeLib.name) {
                        dependencies.add("${excludeLib.groupId}:${excludeLib.artifactId}:${excludeLib.versionRequirement}")
                    }
                }
            }
        }

        return dependencies
    }

    private Set<String> getCompileArtifacts() {
        Set<String> dependencies

        if (agpVersion.greaterThanOrEqualTo(Constant.AGP_3_1)) {
            dependencies = getCompileArtifactsForAgp31x()
        } else if (agpVersion.greaterThanOrEqualTo(Constant.AGP_3_0)) {
            dependencies = getCompileArtifactsForAgp30x()
        } else {
            dependencies = getCompileArtifactsForAgp2x()
        }

        return dependencies
    }

    // for gradle android plugin 2.x.x
    private Set<String> getCompileArtifactsForAgp2x() {
        Set<String> compileLibs = new HashSet<>()

        Configuration configuration = project.getConfigurations().getByName("compile")
        if (configuration.isCanBeResolved()) {
            ResolvableDependencies incoming = configuration.getIncoming()
            ResolutionResult resolutionResult = incoming.getResolutionResult()
            Set<ResolvedComponentResult> components = resolutionResult.getAllComponents()

            for (ResolvedComponentResult result : components) {
                ModuleVersionIdentifier identifier = result.getModuleVersion()
                if (identifier != null && !"unspecified".equals(identifier.getVersion())) {
                    compileLibs.add(
                            String.join(":", identifier.getGroup(), identifier.getName(), identifier.getVersion()))
                }
            }
        }

        return compileLibs
    }

    // for gradle android plugin 3.0.x
    private Set<String> getCompileArtifactsForAgp31x() {
        ImmutableMap<String, String> buildMapping = ModelBuilder.computeBuildMapping(project.getGradle())
        final Set<ArtifactDependencyGraph.HashableResolvedArtifactResult> allArtifacts =
                ArtifactDependencyGraph.getAllArtifacts(
                        applicationVariant.getVariantData().getScope(),
                        AndroidArtifacts.ConsumedConfigType.COMPILE_CLASSPATH,
                        null,
                        buildMapping)
        return getMavenArtifacts(allArtifacts)
    }

    // for gradle android plugin 3.1.x
    private Set<String> getCompileArtifactsForAgp30x() {
        final Set<ArtifactDependencyGraph.HashableResolvedArtifactResult> allArtifacts = Reflect.on("com.android.build.gradle.internal.ide.ArtifactDependencyGraph")
                .call("getAllArtifacts",
                applicationVariant.getVariantData().getScope(),
                AndroidArtifacts.ConsumedConfigType.COMPILE_CLASSPATH,
                null)
                .get()
        return getMavenArtifacts(allArtifacts)
    }

    private
    static Set<String> getMavenArtifacts(Set<ArtifactDependencyGraph.HashableResolvedArtifactResult> allArtifacts) {
        Set<String> deps = new HashSet<>()

        for (ArtifactDependencyGraph.HashableResolvedArtifactResult result : allArtifacts) {
            ComponentIdentifier id = result.getId().getComponentIdentifier()
            if (id instanceof ModuleComponentIdentifier) {
                ModuleComponentIdentifier module = (ModuleComponentIdentifier) id
                deps.add(String.join(":", module.getGroup(), module.getModule(), module.getVersion()))
            }
        }

        return deps
    }
}
