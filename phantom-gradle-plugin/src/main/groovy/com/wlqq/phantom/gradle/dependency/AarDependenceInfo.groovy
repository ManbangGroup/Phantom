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

import com.android.SdkConstants
import com.android.builder.model.AndroidLibrary
import com.android.utils.FileUtils
import com.wlqq.phantom.gradle.utils.Log

/**
 * Represents a AAR dependence from Maven repository or Android library module
 *
 * @author zhengtao
 */
class AarDependenceInfo extends DependenceInfo {

    /**
     * Android library dependence in android build system, delegate of AarDependenceInfo
     */
    AndroidLibrary library

    File intermediatesFile

    AarDependenceInfo(String group, String artifact, String version, AndroidLibrary library) {
        super(group, artifact, version)
        this.library = library
    }

    @Override
    File getJarFile() {
        Log.i 'AarDependenceInfo', "Found [${library.resolvedCoordinates}]'s jar file: ${library.jarFile}"
        return library.jarFile
    }

    @Override
    DependenceType getDependenceType() {
        return DependenceType.AAR
    }

    File getAssetsFolder() {
        Log.i 'AarDependenceInfo', "Found [${library.resolvedCoordinates}]'s assets folder: ${library.assetsFolder}"
        return library.assetsFolder
    }

    File getJniFolder() {
        Log.i 'AarDependenceInfo', "Found [${library.resolvedCoordinates}]'s jni folder: ${library.jniFolder}"
        return library.jniFolder
    }

    Collection<File> getLocalJars() {
        Log.i 'AarDependenceInfo', "Found [${library.resolvedCoordinates}]'s local jars: ${library.localJars}"
        return library.localJars
    }

    File getBundle() {
        Log.i 'AarDependenceInfo', "Found [${library.resolvedCoordinates}]'s bundle: ${library.bundle}"
        return library.bundle
    }

    /**
     * Return the package name of this library, parse from manifest file
     * manifest file are obtained by delegating to "library"
     * @return package name of this library
     */
    public String getPackage() {
        File manifest = getFile(library.manifest, 'manifests', 'full', library.projectVariant, SdkConstants.ANDROID_MANIFEST_XML)
        Log.i 'AarDependenceInfo', "Found [${library.resolvedCoordinates}]'s manifest file: ${manifest}"
        def xmlManifest = new XmlParser().parse(manifest)
        return xmlManifest.@package
    }

    File getIntermediatesDir() {
        if (intermediatesFile == null) {
            String path = library.folder.path
            try {
                intermediatesFile = new File(path.substring(0, path.indexOf("${File.separator}intermediates${File.separator}")), 'intermediates')

            } catch (Exception e) {
                Log.e('AarDependenceInfo', "Can not find [${library.resolvedCoordinates}]'s intermediates dir from the path: ${path}")
                intermediatesFile = library.folder
            }
        }
        return intermediatesFile
    }

    File getFile(File defaultFile, String... paths) {
        if (library.projectVariant == null) {
            return defaultFile
        }

        if (defaultFile.exists()) {
            return defaultFile
        }

        // module library
        return FileUtils.join(intermediatesDir, paths)
    }

    @Override
    String toString() {
        return "${super.toString()} -> ${library}"
    }
}