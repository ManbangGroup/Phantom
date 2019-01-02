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

package com.wlqq.phantom.gradle.host;

import com.android.build.gradle.internal.api.ApplicationVariantImpl;
import com.android.build.gradle.internal.ide.ArtifactDependencyGraph;
import com.android.build.gradle.internal.ide.ModelBuilder;
import com.android.build.gradle.internal.publishing.AndroidArtifacts;

import com.google.common.collect.ImmutableMap;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.joor.Reflect;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;


class CompileDependenciesFileGenerator extends FileGenerator {


    private Project project;
    private ApplicationVariantImpl applicationVariant;
    private ComparableVersion agpVersion;

    CompileDependenciesFileGenerator(Project project, ApplicationVariantImpl variant, File outputFileDir,
            String outputFileName) {
        super(outputFileDir, outputFileName);
        this.project = project;
        this.applicationVariant = variant;
        this.agpVersion = (ComparableVersion) project.getExtensions().getExtraProperties().get(Constant.AGP_VERSION);
    }

    @Override
    protected String getContent() {
        Set<String> dependencies;

        if (agpVersion.greaterThanOrEqualTo(Constant.AGP_3_1)) {
            dependencies = getCompileArtifactsForAgp31x();
        } else if (agpVersion.greaterThanOrEqualTo(Constant.AGP_3_0)) {
            dependencies = getCompileArtifactsForAgp30x();
        } else {
            dependencies = getCompileArtifactsForAgp2x();
        }

        return String.join("\n", dependencies);
    }

    // for gradle android plugin 2.x.x
    private Set<String> getCompileArtifactsForAgp2x() {
        Set<String> compileLibs = new HashSet<>();

        Configuration configuration = project.getConfigurations().getByName("compile");
        if (configuration.isCanBeResolved()) {
            ResolvableDependencies incoming = configuration.getIncoming();
            ResolutionResult resolutionResult = incoming.getResolutionResult();
            Set<ResolvedComponentResult> components = resolutionResult.getAllComponents();

            for (ResolvedComponentResult result : components) {
                ModuleVersionIdentifier identifier = result.getModuleVersion();
                if (identifier != null && !"unspecified".equals(identifier.getVersion())) {
                    compileLibs.add(
                            String.join(":", identifier.getGroup(), identifier.getName(), identifier.getVersion()));
                }
            }
        }

        return compileLibs;
    }

    // for gradle android plugin 3.0.x
    private Set<String> getCompileArtifactsForAgp31x() {
        ImmutableMap<String, String> buildMapping = ModelBuilder.computeBuildMapping(project.getGradle());
        final Set<ArtifactDependencyGraph.HashableResolvedArtifactResult> allArtifacts =
                ArtifactDependencyGraph.getAllArtifacts(
                        applicationVariant.getVariantData().getScope(),
                        AndroidArtifacts.ConsumedConfigType.COMPILE_CLASSPATH,
                        null,
                        buildMapping);
        return getMavenArtifacts(allArtifacts);
    }

    // for gradle android plugin 3.1.x
    private Set<String> getCompileArtifactsForAgp30x() {
        final Set<ArtifactDependencyGraph.HashableResolvedArtifactResult> allArtifacts = Reflect.on("com.android.build.gradle.internal.ide.ArtifactDependencyGraph")
                .call("getAllArtifacts",
                        applicationVariant.getVariantData().getScope(),
                        AndroidArtifacts.ConsumedConfigType.COMPILE_CLASSPATH,
                        null)
                .get();
        return getMavenArtifacts(allArtifacts);
    }

    private Set<String> getMavenArtifacts(Set<ArtifactDependencyGraph.HashableResolvedArtifactResult> allArtifacts) {
        Set<String> deps = new HashSet<>();

        for (ArtifactDependencyGraph.HashableResolvedArtifactResult result : allArtifacts) {
            ComponentIdentifier id = result.getId().getComponentIdentifier();
            if (id instanceof ModuleComponentIdentifier) {
                ModuleComponentIdentifier module = (ModuleComponentIdentifier) id;
                deps.add(String.join(":", module.getGroup(), module.getModule(), module.getVersion()));
            }
        }

        return deps;
    }
}
