package com.wlqq.phantom.gradle.plugin.dependency

import com.android.SdkConstants
import com.android.builder.model.AndroidLibrary
import com.android.utils.FileUtils
import com.wlqq.phantom.gradle.plugin.utils.Log

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