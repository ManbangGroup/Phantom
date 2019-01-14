package com.wlqq.phantom.gradle.plugin.dependency

import com.android.builder.model.JavaLibrary
import com.wlqq.phantom.gradle.plugin.utils.Log

/**
 * Represents a Jar library. This could be the output of a Java project.
 *
 * @author zhengtao
 */
class JarDependenceInfo extends DependenceInfo {

    JavaLibrary library

    JarDependenceInfo(String group, String artifact, String version, JavaLibrary library) {
        super(group, artifact, version)
        this.library = library
    }

    @Override
    File getJarFile() {
        Log.i 'JarDependenceInfo', "Found [${library.resolvedCoordinates}]'s jar file: ${library.jarFile}"
        return library.jarFile
    }

    @Override
    DependenceType getDependenceType() {
        return DependenceType.JAR
    }
}