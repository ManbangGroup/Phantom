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

package com.wlqq.phantom.gradle.plugin.exclude

import com.android.build.api.transform.*
import com.android.build.gradle.AndroidGradleOptions
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.internal.dsl.PackagingOptions
import com.android.build.gradle.internal.ide.ArtifactDependencyGraph
import com.android.build.gradle.internal.ide.ModelBuilder
import com.android.build.gradle.internal.packaging.ParsedPackagingOptions
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.transforms.MergeJavaResourcesTransform
import com.android.build.gradle.tasks.MergeManifests
import com.android.build.gradle.tasks.MergeResources
import com.android.builder.model.Dependencies
import com.android.builder.model.SyncIssue
import com.android.ide.common.res2.ResourcePreprocessor
import com.android.ide.common.res2.ResourceSet
import com.android.manifmerger.ManifestProvider
import com.android.utils.FileUtils
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.wlqq.phantom.gradle.plugin.ComparableVersion
import com.wlqq.phantom.gradle.plugin.Constant
import com.wlqq.phantom.gradle.plugin.PhantomPluginConfig
import com.wlqq.phantom.gradle.plugin.dependency.AarDependenceInfo
import com.wlqq.phantom.gradle.plugin.dependency.DependenceInfo
import com.wlqq.phantom.gradle.plugin.dependency.JarDependenceInfo
import com.wlqq.phantom.gradle.plugin.utils.IOUtils
import com.wlqq.phantom.gradle.plugin.utils.JarUtils
import com.wlqq.phantom.gradle.plugin.utils.JarVisitor
import com.wlqq.phantom.gradle.plugin.utils.Log
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.artifacts.result.ResolvedArtifactResult

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.function.Consumer
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ExcludeClassesTransform extends Transform {

    Project project
    PhantomPluginConfig mWlExclude
    // for agp 2.x
    Set<String> mRelativeLibs
    // for agp 3.x
    List<DependenceInfo> mStripDependencies
    ComparableVersion agpVersion


    ExcludeClassesTransform(Project project) {
        this.project = project
        this.agpVersion = (ComparableVersion) project.extensions.extraProperties[Constant.AGP_VERSION]
        project.afterEvaluate {
            mWlExclude = project.extensions.getByName(Constant.USER_CONFIG)
            if (agpVersion.greaterThanOrEqualTo(Constant.AGP_3_0)) {
                mStripDependencies = computeStripDependenceListForAgp3x()
                mRelativeLibs = new HashSet<>(mStripDependencies.collect { it.jarFile.absolutePath })
                excludeRes2()
            } else {
                mRelativeLibs = parseRelativeLibs(mWlExclude.excludeLibs)
                excludeRes()
            }
        }
    }

    @Override
    String getName() {
        return "__ExcludeClasses__"
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        def tag = 'transform'

        if (!isIncremental()) {
            transformInvocation.outputProvider.deleteAll()
        }

        String transformsDir = "${project.buildDir.toString()}${File.separator}intermediates${File.separator}transforms${File.separator}${getName()}"
        File tempFile = new File(transformsDir)
        if (tempFile.exists() && tempFile.isDirectory()) {
            FileUtils.deletePath(tempFile)
        }

        String tempClassDir = project.buildDir.toString() + File.separator + "tmp" + File.separator + "tmpClasses"
        tempFile = new File(tempClassDir)
        if (tempFile.exists() && tempFile.isDirectory()) {
            FileUtils.deletePath(tempFile)
        }

        writeLibraryJarsToProguardFile(mRelativeLibs, mWlExclude.libraryJarsProguardFile)

        List<String> excludeClasses = new ArrayList<>()

        String buildCacheDir = getBuildCacheDir()
        Log.i tag, "build cache dir: ${buildCacheDir}"

        Set<String> excludeOtherFile = new HashSet<>()

        transformInvocation.inputs.each { TransformInput input ->
            /**
             * 遍历输入目录并拷贝到tempClassDir
             */
            input.directoryInputs.each { DirectoryInput directoryInput ->
                Log.i tag, "Copying ${directoryInput.file.absolutePath} to ${tempClassDir} start"
                FileUtils.copyDirectory(directoryInput.file, new File(tempClassDir))
                Log.i tag, "Copying ${directoryInput.file.absolutePath} to ${tempClassDir} end"
            }

            /**
             * 遍历输入jar并解压到tempClassDir
             */
            input.jarInputs.eachWithIndex { JarInput jarInput, int idx ->

                def absolutePath = jarInput.file.absolutePath
                Log.i tag, "jarInput $idx absolutePath: $absolutePath"

                for (PhantomPluginConfig.ExcludeConfig module : mWlExclude.excludeModules) {
                    if (absolutePath.indexOf(File.separator + module.name + File.separator) >= 0) {
                        Log.i tag, "exclude module ${module.name} jarPath is ${absolutePath}"
                        String modulePackage = parsePackageName(getModuleManifestByJar(jarInput.file))
                        if (null != modulePackage && module.isExcludeRes) {
                            excludeClasses.add(modulePackage + '.R.class')
                        }
                        excludeOtherFile.addAll(ExcludeFileUtils.getOtherExcludeFromDir(jarInput.file.getParentFile().absolutePath))
                        excludeOtherFile.addAll(ExcludeFileUtils.getOtherExcludeFromJar(absolutePath))

                        writeLibraryJarsToProguardFile([absolutePath] as Set, mWlExclude.libraryJarsProguardFile, true)
                        keepModuleClasses(absolutePath, tempClassDir, module.keepClasses)
                        return
                    }
                }

                //agp 3.x 输入jar不在build-cache目录，删除相关的依赖lib可以根据名字判断。
                //例如 agp 3.x 输入的jar路径是：/Users/fy/.gradle/caches/transforms-1/files-1.1/support-fragment-28.0.0-rc02.aar/59ce33ef0fec498f1664c7ae026d7518/jars/classes.jar
                //根据implementation获取的依赖路径是：/Users/fy/.gradle/caches/modules-2/files-2.1/com.android.support/support-fragment/28.0.0-rc02/ddd2d04e216aabc4c9342d172febfa365b171954/support-fragment-28.0.0-rc02.aar
                //路径中包含support-fragment-28.0.0-rc02.aar则可以删除
                if (agpVersion.greaterThanOrEqualTo(Constant.AGP_3_0) && null != mStripDependencies) {
                    for (DependenceInfo dependenceInfo : mStripDependencies) {
                        def suffix = dependenceInfo.dependenceType == DependenceInfo.DependenceType.AAR ? 'aar' : 'jar'
                        def filename = "${dependenceInfo.artifact}-${dependenceInfo.version}.${suffix}"
                        if (absolutePath.contains(filename)) {
                            Log.i tag, "agp 3.x exclude lib ${filename} libCachePath is ${absolutePath}"
                            if (dependenceInfo.dependenceType == DependenceInfo.DependenceType.AAR) {
                                String unzipedDir = absolutePath.substring(0, absolutePath.length() - '/jars/classes.jar'.length())
                                Log.i tag, 'agp 3.x unZipDir:' + unzipedDir
                                String packageName = parsePackageName(unzipedDir + File.separator + 'AndroidManifest.xml')
                                if (null != packageName) {
                                    excludeClasses.add(packageName + '.R.class')
                                }

                                excludeOtherFile.addAll(ExcludeFileUtils.getOtherExcludeFromDir(unzipedDir))
                            }
                            //有的jar中会包含assets等资源，一并排除掉
                            excludeOtherFile.addAll(ExcludeFileUtils.getOtherExcludeFromJar(absolutePath))
                            return
                        }
                    }
                }

                if (absolutePath.startsWith(buildCacheDir)) {
                    CacheLibInfo libInfo = getCacheFile(buildCacheDir, absolutePath)
                    PhantomPluginConfig.ExcludeConfig cfg = mWlExclude.excludeLibInfo(libInfo.name)
                    if (null != libInfo
                            && (null != cfg
                            || mRelativeLibs.contains(libInfo.path))) {
                        Log.i tag, "agp 2.x exclude lib ${libInfo.name} libCachePath is ${absolutePath}"
                        //null == cfg表示是相关联的库，只有指定不删除资源的库才保留R类
                        if (null == cfg || cfg.isExcludeRes) {
                            excludeClasses.add(libInfo.packageName + '.R.class')
                        }
                        excludeOtherFile.addAll(ExcludeFileUtils.getOtherExcludeFromDir(libInfo.cachePath))
                        //有的jar中会包含assets等资源，一并排除掉
                        excludeOtherFile.addAll(ExcludeFileUtils.getOtherExcludeFromJar(absolutePath))
                        return
                    }
                }

                for (PhantomPluginConfig.ExcludeConfig excludeJar : mWlExclude.excludeLibs) {
                    String jarPath = absolutePath
                    if (jarPath.indexOf("${File.separator}${excludeJar.artifactId}-${excludeJar.version}") >= 0
                            || jarPath.indexOf("${File.separator}${excludeJar.artifactId}${File.separator}${excludeJar.version}") >= 0
                            || mRelativeLibs.contains(jarPath)) {
                        Log.i tag, "agp old exclude lib ${excludeJar.name} libPath is ${jarPath}"
                        if (jarPath.contains("${excludeJar.artifactId}-${excludeJar.version}.aar") && jarPath.endsWith("jars${File.separator}classes.jar")) {
                            //aar
                            String unzipedDir = jarPath.substring(0, jarPath.length() - '/jars/classes.jar'.length())
                            Log.i tag, 'agp old unZipDir:' + unzipedDir
                            String packageName = parsePackageName(unzipedDir + File.separator + 'AndroidManifest.xml')
                            if (null != packageName) {
                                excludeClasses.add(packageName + '.R.class')
                            }

                            excludeOtherFile.addAll(ExcludeFileUtils.getOtherExcludeFromDir(unzipedDir))
                        }
                        parseOtherExcludeFile(excludeOtherFile, absolutePath)
                        return
                    }
                }

                //其他的jar直接输出到下一个transform
                String newFileName = "${jarInput.file.name.substring(0, jarInput.file.name.length() - 4)}_${System.currentTimeMillis()}"
                File otherJar = transformInvocation.outputProvider.getContentLocation(newFileName, TransformManager.CONTENT_JARS, TransformManager.SCOPE_FULL_PROJECT, Format.JAR)
                Log.i tag, "transform otherJar ${absolutePath} to ${otherJar.absolutePath} start"
                if (!otherJar.getParentFile().exists()) {
                    otherJar.getParentFile().mkdirs()
                }
                JarUtils.transformJar(absolutePath, otherJar, new JarVisitor() {
                    @Override
                    boolean visitEntry(ZipEntry entry, ZipInputStream inputJar, ZipOutputStream outputJar) {
                        String name = entry.getName().replace((char) '/', (char) '.')
                        for (String excludeClsName : mWlExclude.excludeClasses) {
                            if (name.startsWith(excludeClsName)) {
                                return false
                            }
                        }
                        return true
                    }
                })
                Log.i tag, "transform otherJar ${absolutePath} to ${otherJar.absolutePath} end"
            }
        }
        /**
         * 从tempClassDir目录删除需要去掉的class
         */
        excludeClasses.addAll(mWlExclude.excludeClasses)
        excludeClasses.each { String classes ->
            StringBuffer path = new StringBuffer(classes)
            int endIndex = path.size() - 1
            if (classes.endsWith(".class")) {
                endIndex = endIndex - 6
            }
            for (int i = 0; i < endIndex; i++) {
                if (path.charAt(i) == (char) '.') {
                    path.setCharAt(i, File.separatorChar)
                }
            }

            Log.i tag, "delete classes: ${path.toString()}"
            File deleteFile = new File(tempClassDir + File.separator + path.toString())
            FileUtils.deletePath(deleteFile)
            //delete R$xxx.class
            if (classes.endsWith("R.class")) {
                File[] rFiles = deleteFile.getParentFile().listFiles(new FileFilter() {
                    @Override
                    boolean accept(File file) {
                        return file.getName().startsWith("R\$")
                    }
                })
                for (File rFile : rFiles) {
                    Log.i tag, "delete classes: ${rFile.absolutePath}"
                    FileUtils.deletePath(rFile)
                }
            }
        }

        File dest = transformInvocation.outputProvider.getContentLocation("excluded", TransformManager.CONTENT_JARS, TransformManager.SCOPE_FULL_PROJECT, Format.JAR)
        Log.i tag, "makeJar ${tempClassDir} to ${dest.absolutePath}"
        JarUtils.makeJar(tempClassDir, dest.absolutePath)

        if (agpVersion.greaterThanOrEqualTo(Constant.AGP_3_0)) {
            excludeSoAndRes2(excludeOtherFile)
        } else {
            excludeSoAndRes(excludeOtherFile)
        }
    }

    /**
     * 根据输入的 module jar 获取模块的 AndroidManifest.xml 文件路径，可能为 <code>null</code>
     * @param jarPath
     * @return 模块的 AndroidManifest.xml 文件路径
     */
    static String getModuleManifestByJar(File jarFile) {
        String manifestPath = jarFile.getParentFile().absolutePath + File.separator + 'AndroidManifest.xml'
        if (new File(manifestPath).exists()) {
            return manifestPath
        }

        manifestPath = jarFile.getParentFile().getParentFile().getParentFile().absolutePath + "${File.separator}manifests${File.separator}full${File.separator}debug${File.separator}AndroidManifest.xml"
        if (new File(manifestPath).exists()) {
            return manifestPath
        }

        manifestPath = jarFile.getParentFile().getParentFile().getParentFile().absolutePath + "${File.separator}manifests${File.separator}full${File.separator}release${File.separator}AndroidManifest.xml"
        if (new File(manifestPath).exists()) {
            return manifestPath
        }

        Log.e 'getModuleManifestByJar', "exclude module not find AndroidManifest.xml for ${jarFile.absolutePath}"
        return null
    }

    /**
     * 保留被删除module中的部分类，module编译后会生成类似build\intermediates\bundles\default\classes.jar的jar，
     * 函数功能是将指定的类(包括其内部类)从classes.jar中提取出来
     *
     * @param jarPath module编译后的classes.jar路径
     * @param outPath 保留的类输出的路径
     * @param keepClasses 需要被保留的类
     */
    void keepModuleClasses(String jarPath, String outPath, Iterable<String> keepClasses) {
        if (!jarPath.endsWith("${File.separator}default${File.separator}classes.jar") || keepClasses.size() == 0) {
            return
        }
        ZipInputStream zis = null
        BufferedOutputStream bos = null
        try {
            zis = new ZipInputStream(new FileInputStream(jarPath))
            ZipEntry entry = null
            HashSet<String> proguardCfg = new HashSet<>()
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue
                }

                String entryName = entry.getName().replace((char) '/', (char) '.')
                for (String keepClass : keepClasses) {
                    if ( //保留的是某个包或类
                    entryName.startsWith(keepClass)
                            //保留内部类
                            || (keepClass.endsWith('.class') && entryName.startsWith(keepClass.substring(0, keepClass.length() - 6) + '$'))) {
                        File target = new File(outPath, entry.getName())
                        if (!target.getParentFile().exists()) {
                            // 创建文件父目录
                            target.getParentFile().mkdirs()
                        }
                        if (!target.exists()) {
                            target.createNewFile()
                        }
                        // 写入文件
                        bos = new BufferedOutputStream(new FileOutputStream(target))
                        int read = 0
                        byte[] buffer = new byte[1024 * 50]
                        while ((read = zis.read(buffer, 0, buffer.length)) != -1) {
                            bos.write(buffer, 0, read)
                        }
                        proguardCfg.add(entryName.substring(0, entryName.length() - 6))
                        bos.flush()
                        bos.close()
                    }
                }
            }
            writeKeepClassesToProguardFile(proguardCfg, mWlExclude.libraryJarsProguardFile)
            zis.closeEntry()
        } catch (IOException e) {
            throw new RuntimeException(e)
        } finally {
            IOUtils.closeQuietly(zis, bos)
        }
    }

    void excludeRes() {
        def tag = 'excludeRes'
        Set<String> excludeResKeys = new HashSet<>()
        def wlExclude = mWlExclude
        for (PhantomPluginConfig.ExcludeConfig config : wlExclude.excludeModules) {
            if (!config.isExcludeRes) {
                continue
            }
            excludeResKeys.add(project.rootProject.rootDir.absolutePath + File.separator + config.name)
        }

        for (PhantomPluginConfig.ExcludeConfig config : wlExclude.excludeLibs) {
            if (!config.isExcludeRes) {
                continue
            }
            excludeResKeys.add("${config.artifactId}-${config.version}")
            excludeResKeys.add("${config.artifactId}${File.separator}${config.version}")

        }


        project.tasks.each {
            if (it.name == 'mergeDebugResources' || it.name == 'mergeReleaseResources') {
                it.doFirst {
                    List<ResourceSet> resList = it.getInputResourceSets()

                    Log.i tag, "origin ResourceSets: $resList"
                    
                    List<ResourceSet> removedRes = new ArrayList<ResourceSet>()
                    String buildCachePath = getBuildCacheDir()
                    for (String resKey : excludeResKeys) {
                        for (ResourceSet res : resList) {
                            if (res.sourceFiles[0].absolutePath.startsWith(resKey)) {
                                removedRes.add(res)
                                break
                            }

                            if (res.sourceFiles[0].absolutePath.startsWith(buildCachePath)) {
                                //cache
                                CacheLibInfo libInfo = getCacheFile(buildCachePath, res.sourceFiles[0].absolutePath)
                                PhantomPluginConfig.ExcludeConfig cfg = wlExclude.excludeLibInfo(libInfo.name)
                                if (libInfo.name == resKey) {
                                    removedRes.add(res)
                                    break
                                }
                                if (mRelativeLibs.contains(libInfo.path) && (null == cfg || cfg.isExcludeRes)) {
                                    removedRes.add(res)
                                }
                            }

                            if (res.sourceFiles[0].absolutePath.indexOf(resKey) > 0) {
                                //exploded-aar
                                removedRes.add(res)
                                break

                            }
                        }
                    }

                    for (ResourceSet deleteItem : removedRes) {
                        resList.remove(deleteItem)
                        Log.i tag, "delete ResourcesSet: $deleteItem"
                    }

                    it.setInputResourceSets(resList)
                    Log.i tag, "new ResourceSets: $resList"
                }
            }

            if (it.getName() == 'processDebugManifest' || it.getName() == 'processReleaseManifest') {
                excludeAndroidManifests(it, wlExclude)
            }
        }
    }

    void excludeRes2() {
        Set<String> excludeResKeys = new HashSet<>()
        PhantomPluginConfig wlExclude = mWlExclude
        for (PhantomPluginConfig.ExcludeConfig config : wlExclude.excludeModules) {
            if (!config.isExcludeRes) {
                continue
            }
            excludeResKeys.add(project.rootProject.rootDir.absolutePath + File.separator + config.name)
        }

        for (PhantomPluginConfig.ExcludeConfig config : wlExclude.excludeLibs) {
            if (!config.isExcludeRes) {
                continue
            }
            excludeResKeys.add("${config.artifactId}-${config.version}")
            excludeResKeys.add("${config.artifactId}${File.separator}${config.version}")

        }


        project.tasks.each {
            if (it.name == 'mergeDebugResources' || it.name == 'mergeReleaseResources') {
                it.doFirst {
                    List<ResourceSet> resList = null
                    Field fieldPi = MergeResources.class.getDeclaredField("processedInputs")
                    fieldPi.setAccessible(true)
                    Method methodProcessor = MergeResources.class.getDeclaredMethod("getPreprocessor")
                    methodProcessor.setAccessible(true)
                    Method methodResourceSet = MergeResources.class.getDeclaredMethod("getConfiguredResourceSets", ResourcePreprocessor.class)
                    methodResourceSet.setAccessible(true)
                    ResourcePreprocessor processor = (ResourcePreprocessor) methodProcessor.invoke(it)
                    resList = (List<ResourceSet>) methodResourceSet.invoke(it, processor)

                    Log.i 'excludeRes2', "origin ResourceSets: $resList"
                    List<ResourceSet> removedRes = new ArrayList<ResourceSet>()
                    for (String resKey : excludeResKeys) {
                        for (ResourceSet res : resList) {
                            if (res.sourceFiles[0].absolutePath.startsWith(resKey)) {
                                removedRes.add(res)
                                break
                            }

                            boolean find = false

                            if (null != mStripDependencies) {
                                for (DependenceInfo dependenceInfo : mStripDependencies) {
                                    def suffix = dependenceInfo.dependenceType == DependenceInfo.DependenceType.AAR ? 'aar' : 'jar'
                                    def filename = "${dependenceInfo.artifact}-${dependenceInfo.version}.${suffix}"
                                    if (res.sourceFiles[0].absolutePath.contains(filename)) {
                                        find = true
                                        removedRes.add(res)
                                        break
                                    }
                                }

                                if (find) {
                                    continue
                                }
                            }


                            if (res.sourceFiles[0].absolutePath.indexOf(resKey) > 0) {
                                //exploded-aar
                                removedRes.add(res)
                                break

                            }
                        }
                    }

                    for (ResourceSet deleteItem : removedRes) {
                        resList.remove(deleteItem)
                        Log.i 'excludeRes2', "delete ResourcesSet: $deleteItem"
                    }

                    fieldPi.set(it, resList)

                    Log.i 'excludeRes2', "new ResourceSets: $resList"
                }
            }

            if (it.getName() == 'processDebugManifest' || it.getName() == 'processReleaseManifest') {
                excludeAndroidManifests2(it, wlExclude)
            }
        }
    }

    /**
     * mergeJniLibs和mergeJavaRes两个transform负责合并资源文件和so库，如果要排除资源文件和so库只需要修改这两个
     * transform的packagingOptions属性
     *
     * @param excludeOthers 要排除的资源和so
     */
    void excludeSoAndRes(Set<String> excludeOthers) {
        def tag = 'excludeSoAndRes'
        if (0 == excludeOthers.size()) {
            return
        }

        Log.i tag, "excludeSoAndRes: $excludeOthers"

        ((BasePlugin) project.plugins.findPlugin(AppPlugin)).variantManager.variantDataList.each {
            TransformManager transformManager = it.getScope().getTransformManager()
            Field field = TransformManager.getDeclaredField('transforms')
            field.setAccessible(true)
            List<Transform> transformList = field.get(transformManager)
            for (Transform trs : transformList) {
                if (trs.name.equalsIgnoreCase('mergeJniLibs') || trs.name.equalsIgnoreCase('mergeJavaRes')) {
                    MergeJavaResourcesTransform mjr = (MergeJavaResourcesTransform) trs
                    Field poField = MergeJavaResourcesTransform.class.getDeclaredField('packagingOptions')
                    poField.setAccessible(true)
                    ParsedPackagingOptions ppo = (ParsedPackagingOptions) poField.get(mjr)
                    PackagingOptions tmpPo = new PackagingOptions()
                    tmpPo.excludes = ImmutableSet.copyOf(ppo.excludePatterns)
                    tmpPo.merges = ImmutableSet.copyOf(ppo.mergePatterns)
                    tmpPo.pickFirsts = ImmutableSet.copyOf(ppo.pickFirstPatterns)
                    for (String excludeFile : excludeOthers) {
                        tmpPo.exclude(excludeFile)
                    }

                    ParsedPackagingOptions newPpo = new ParsedPackagingOptions(tmpPo)
                    poField.set(mjr, newPpo)
                }
            }
        }
    }

    void excludeSoAndRes2(Set<String> excludeOthers) {
        def tag = 'excludeSoAndRes2'
        if (0 == excludeOthers.size()) {
            return
        }

        Log.i tag, "excludeSoAndRes2: $excludeOthers"
        for (String excludeFile : excludeOthers) {
            project.android.packagingOptions.exclude(excludeFile)
        }

    }


    private static void parseOtherExcludeFile(Set<String> excludes, String filePath) {
        if (filePath.endsWith('.aar')) {
            excludes.addAll(ExcludeFileUtils.getOtherExcludeFromAar(filePath))
        } else if (filePath.endsWith('.jar')) {
            excludes.addAll(ExcludeFileUtils.getOtherExcludeFromJar(filePath))
        }
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    /**
     * 从 Android plugin 2.3.0开始默认开启了build cache，传递到transform的jar/aar路径可能不是原始路径，
     * 无法从路径得到库的原始名字，例如support-v4传递过来的路径为
     * .....\build-cache\d52514440b56267be453a11efa378f04007aaa3b\output\jars\classes.jar这种样式，原始路径
     * 被记录在d52514440b56267be453a11efa378f04007aaa3b目录的inputs文件里面，这些需要重inputs文件获取库的原始名字
     *
     * @param cachePath android plugin的cache目录
     * @param filePath cache目录中库的绝对路径
     * @return 返回库的原始名字，例如support-v4-21.1.1
     */
    private static CacheLibInfo getCacheFile(String cachePath, String filePath) {
        int index = filePath.indexOf(File.separator, cachePath.length() + 1)
        String rootPath = filePath.substring(0, index + 1)
        File inputsFile = new File(rootPath + 'inputs')
        if (!inputsFile.exists()) {
            return null
        }

        BufferedReader br = new BufferedReader(new FileReader(inputsFile))
        String line
        boolean findPath = false
        String pre = 'FILE_PATH='
        while ((line = br.readLine()) != null) {
            if (line.startsWith(pre)) {
                findPath = true
                break
            }
        }
        br.close()
        if (findPath) {
            CacheLibInfo libInfo = new CacheLibInfo()
            libInfo.path = line.substring(pre.length())
            libInfo.name = line.substring(line.lastIndexOf(File.separator) + 1, line.length() - 4)
            libInfo.cachePath = rootPath + 'output'
            libInfo.packageName = parsePackageName(libInfo.cachePath + File.separator + 'AndroidManifest.xml')
            return libInfo
        }

        return null
    }

    /**
     * aar库里面存在AndroidManifest文件,在编译时会生成对应的R.class，这里获取AndroidManifest里面的
     * package, 方便删除对应的R.class文件
     *
     * @param path AndroidManifest的路径
     * @return AndroidManifest的package属性值
     */
    private static String parsePackageName(String path) {
        if (path == null) {
            return null
        }

        File manifestFile = new File(path)
        if (!manifestFile.exists()) {
            return null
        }
        def xmlManifest = new XmlParser().parse(manifestFile)
        return xmlManifest.@package
    }

    /**
     * 获取一个库的依赖库，比如support-v4在版本25.3.1被拆分成多个库，在排除support-v4的时候应该将它的依赖库一起删除，在 AGP 2.x 有效
     *
     * @param excludeLibs ExcludeClasses的excludeLibs值
     * @return ExcludeClasses的excludeLibs的依赖库
     */
    private Set<String> parseRelativeLibs(List<PhantomPluginConfig.ExcludeConfig> excludeLibs) {
        Set<String> relativeLibs = new HashSet<>()

        project.rootProject.allprojects.each {
            try {
                Configuration compileConf = it.configurations.getByName('compile')
                compileConf.dependencies.each {
                    String dependencyStr = "${it.group}:${it.name}:${it.version}"
                    for (PhantomPluginConfig.ExcludeConfig excludeLib : excludeLibs) {
                        if (excludeLib.name == dependencyStr) {
                            for (File libFile : compileConf.files(it)) {
                                relativeLibs.add(libFile.absolutePath)
                            }
                        }
                    }
                }
            } catch (UnknownConfigurationException e) {
                return
            }
        }

        return relativeLibs
    }

    /**
     * 将需要删除掉的 library 绝对路径写入到 proguard -libraryjars 配置中，避免 proguard 在 shrink 过程中移除了被这些 library
     * 调用的 <b>类/成员</b>
     *
     * @param libraries 需要被删除的 library 集合
     * @param proguardFile 需要写入 -libraryjars 配置的 proguard 文件
     */
    private void writeLibraryJarsToProguardFile(Set<String> libraries, File proguardFile, boolean append = false) {
        def tag = 'writeLibraryJarsToProguardFile'
        
        Log.i tag, "E $proguardFile, append: $append"

        def writeLibraryJars = { writer ->
            libraries.each {
                def line = "-libraryjars $it"
                println(line)

                writer.write(line)
                writer.write('\n')
            }
        }

        if (append) {
            proguardFile.withWriterAppend('utf-8', writeLibraryJars)
        } else {
            proguardFile.withWriter('utf-8', writeLibraryJars)
        }

        Log.i tag, "X $proguardFile, append: $append"
    }

    /**
     * 将需要保留的类写入混淆配置文件，避免类在混淆时被删掉
     * @param classes 需要保留的类集合
     * @param proguardFile需要写入 -keep class 配置的 proguard 文件
     */
    private void writeKeepClassesToProguardFile(Set<String> classes, File proguardFile) {
        def tag = 'writeKeepClassesToProguardFile'
        
        Log.i tag, "E $proguardFile"

        def writeClasses = { writer ->
            classes.each {
                def line = "-keep class $it"
                println(line)

                writer.write(line)
                writer.write('\n')
            }
        }

        proguardFile.withWriterAppend('utf-8', writeClasses)

        Log.i tag, "X $proguardFile"
    }

    /**
     * 从processManifest Task中移除不需要的provider，provider内容类似：
     *{artifactFile=D:\works\newnearby\Map\build\outputs\aar\Map-release.aar,
     * coordinates=newnearby:Map:unspecified@aar,
     * projectPath=:Map,
     * extractedFolder=D:\works\newnearby\Map\build\intermediates\bundles\default,
     * variant=null, isSubModule=true,
     * jarsRootFolder=D:\works\newnearby\Map\build\intermediates\bundles\default}*
     * 根据coordinates来匹配需要排除的AndroidManifest.xml
     * module的coordinates类似newnearby:Map:unspecified@aar
     * 第3方库coordinates类似com.android.support:support-v4:25.3.1@aar
     * @param config
     */
    private static void excludeAndroidManifests(Task task, PhantomPluginConfig configs) {
        List<ManifestProvider> providers = task.getProviders()
        List<ManifestProvider> removedProviders = new ArrayList<>()

        for (int i = 0; i < providers.size(); i++) {
            def provider = providers.get(i)
            boolean find = false
            String coordinates = provider.coordinates
            for (PhantomPluginConfig.ExcludeConfig moduleCfg : configs.excludeModules) {
                //如果依赖是 compile project(':UIWidget:SwipeMenuListView')
                //则coordinates的值类似newnearby.UIWidget:SwipeMenuListView:unspecified@aar
                //如果依赖是 compile project(':SwipeMenuListView')
                //则coordinates的值是newnearby:SwipeMenuListView:unspecified@aar
                if (coordinates.contains(".${moduleCfg.name}:")
                        || coordinates.contains(":${moduleCfg.name}:")) {
                    removedProviders.add(provider)
                    Log.i 'excludeAndroidManifests', "exclude module AndroidManifest.xml for ${moduleCfg.name}"
                    find = true
                    break
                }
            }

            if (find) {
                continue
            }

            for (PhantomPluginConfig.ExcludeConfig libCfg : configs.excludeLibs) {
                if (coordinates.startsWith("${libCfg.name}@")) {
                    removedProviders.add(provider)
                    Log.i 'excludeAndroidManifests', "exclude lib AndroidManifest.xml for ${libCfg.name}"
                    break
                }
            }
        }

        providers.removeAll(removedProviders)
        task.setProviders(providers)
    }

    /**
     * 4.x的gradle合并manifests时输入的AndroidManifest.xml格式为：
     * module格式：Test/module1/build/intermediates/manifests/full/release/AndroidManifest.xml
     * 依赖的aar格式：.gradle/caches/transforms-1/files-1.1/appcompat-v7-28.0.0-rc02.aar/c018f2ccc4e306e4a83eeb43e732e976/AndroidManifest.xml
     * @param task
     * @param config
     */
    private static void excludeAndroidManifests2(Task task, PhantomPluginConfig config) {
        def tag = 'excludeAndroidManifests2'
        Log.i(tag, 'method E')
        MergeManifests manifestsTask = (MergeManifests) task
        Field mf = MergeManifests.class.getDeclaredField("manifests")
        mf.setAccessible(true)
        Set<ResolvedArtifactResult> artifacts = ((ArtifactCollection) mf.get(manifestsTask)).getArtifacts()

        Iterator<ResolvedArtifactResult> iterator = artifacts.iterator()
        while (iterator.hasNext()) {
            ResolvedArtifactResult manifest = iterator.next()
            boolean find = false
            for (PhantomPluginConfig.ExcludeConfig moduleCfg : config.excludeModules) {
                if (manifest.file.absolutePath.contains("${moduleCfg.name}${File.separator}build${File.separator}")) {
                    iterator.remove()
                    find = true
                    Log.i(tag, "exclude module manifest: ${manifest.file.absolutePath}")
                    break
                }
            }

            if (find) {
                continue
            }

            for (PhantomPluginConfig.ExcludeConfig libCfg : config.excludeLibs) {
                if (libCfg.name == manifest.identifier.componentId.toString()) {
                    iterator.remove()
                    Log.i(tag,  "exclude lib manifest: ${manifest.file.absolutePath}")
                }
            }
        }
        Log.i(tag, 'method X')
    }

    // only call this method for agp version 3.x
    private List<DependenceInfo> computeStripDependenceListForAgp3x() {
        def logtag = 'computeStripDependenceListForAgp3x'
        def scope = project.android.applicationVariants[0].variantData.scope

        List<DependenceInfo> stripDependencies = new ArrayList<>()
        Dependencies dependencies

        Consumer consumer = new Consumer<SyncIssue>() {
            @Override
            void accept(SyncIssue syncIssue) {
                Log.i 'computeStripDependenceListForAgp3x', "Error: ${syncIssue}"
            }
        }

        if (agpVersion.greaterThanOrEqualTo(Constant.AGP_3_1)) {
            ImmutableMap<String, String> buildMapping = ModelBuilder.computeBuildMapping(project.gradle)
            dependencies = new ArtifactDependencyGraph().createDependencies(scope, false, buildMapping, consumer)
        } else {
            // AGP 3.0.x
            dependencies = new ArtifactDependencyGraph().createDependencies(scope, false, consumer)
        }

        def excludeLibs = mWlExclude.excludeLibs.collect { "${it.groupId}:${it.artifactId}" }

        dependencies.libraries.each {
            def mavenCoordinates = it.resolvedCoordinates
            if (excludeLibs.contains("${mavenCoordinates.groupId}:${mavenCoordinates.artifactId}")) {
                Log.i logtag, "Need strip aar: ${mavenCoordinates.groupId}:${mavenCoordinates.artifactId}:${mavenCoordinates.version}"
                stripDependencies.add(
                        new AarDependenceInfo(
                                mavenCoordinates.groupId,
                                mavenCoordinates.artifactId,
                                mavenCoordinates.version,
                                it))

            }

        }
        dependencies.javaLibraries.each {
            def mavenCoordinates = it.resolvedCoordinates
            if (excludeLibs.contains("${mavenCoordinates.groupId}:${mavenCoordinates.artifactId}")) {
                Log.i logtag, "Need strip jar: ${mavenCoordinates.groupId}:${mavenCoordinates.artifactId}:${mavenCoordinates.version}"
                stripDependencies.add(
                        new JarDependenceInfo(
                                mavenCoordinates.groupId,
                                mavenCoordinates.artifactId,
                                mavenCoordinates.version,
                                it))
            }
        }

        return stripDependencies
    }

    private String getBuildCacheDir() {
        if (agpVersion.greaterThanOrEqualTo(Constant.AGP_3_0)) {
            return project.android.applicationVariants[0].variantData.scope.globalScope.buildCache
        } else {
            return AndroidGradleOptions.getBuildCacheDir(project)
        }
    }


    static class CacheLibInfo {
        //库的原始路径
        String path
        //库的原始名字
        String name
        //缓存根路径
        String cachePath
        //库的包名，aar库里面有AndroidManifest文件,从AndroidManifest获取package属性
        String packageName
    }
}
