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

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AndroidGradleOptions
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.internal.dsl.PackagingOptions
import com.android.build.gradle.internal.packaging.ParsedPackagingOptions
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.transforms.MergeJavaResourcesTransform
import com.android.build.gradle.tasks.MergeManifests
import com.android.build.gradle.tasks.MergeResources
import com.android.ide.common.res2.ResourcePreprocessor
import com.android.ide.common.res2.ResourceSet
import com.android.manifmerger.ManifestProvider
import com.android.utils.FileUtils
import com.google.common.collect.ImmutableSet
import com.wlqq.phantom.gradle.plugin.Constant
import com.wlqq.phantom.gradle.plugin.PhantomPluginConfig
import com.wlqq.phantom.gradle.plugin.utils.IOUtils
import com.wlqq.phantom.gradle.plugin.utils.JarUtils
import com.wlqq.phantom.gradle.plugin.utils.JarVisitor
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.internal.component.external.model.DefaultModuleComponentSelector
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.SAXException

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ExcludeClassesTransform extends Transform {

    Project project
    PhantomPluginConfig mWlExclude
    Set<String> mRelativeLibs

    ExcludeClassesTransform(Project project) {
        this.project = project
        project.afterEvaluate {
            mWlExclude = project.extensions.getByName(Constant.USER_CONFIG)
            mRelativeLibs = isGradle4x() ? parseRelativeLibs2(mWlExclude.excludeLibs) : parseRelativeLibs(mWlExclude.excludeLibs)
            if (isGradle4x()) {
                excludeRes2()
            } else {
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

        Set<String> excludeOtherFile = new HashSet<>()

        transformInvocation.inputs.each { TransformInput input ->
            /**
             * 遍历输入目录并拷贝到tempClassDir
             */
            input.directoryInputs.each { DirectoryInput directoryInput ->
                println "Copying ${directoryInput.file.absolutePath} to ${tempClassDir} start"
                FileUtils.copyDirectory(directoryInput.file, new File(tempClassDir))
                println "Copying ${directoryInput.file.absolutePath} to ${tempClassDir} end"
            }

            /**
             * 遍历输入jar并解压到tempClassDir
             */
            input.jarInputs.each { JarInput jarInput ->
                for (PhantomPluginConfig.ExcludeConfig module : mWlExclude.excludeModules) {
                    if (jarInput.file.absolutePath.indexOf(File.separator + module.name + File.separator) >= 0) {
                        println "exclude module ${module.name} jarPath is ${jarInput.file.absolutePath}"
                        String modulePackage = parsePackageName(getModuleManifestByJar(jarInput.file))
                        if (null != modulePackage && module.isExcludeRes) {
                            excludeClasses.add(modulePackage + '.R.class')
                        }
                        excludeOtherFile.addAll(ExcludeFileUtils.getOtherExcludeFromDir(jarInput.file.getParentFile().absolutePath))
                        excludeOtherFile.addAll(ExcludeFileUtils.getOtherExcludeFromJar(jarInput.file.absolutePath))

                        writeLibraryJarsToProguardFile([jarInput.file.absolutePath] as Set, mWlExclude.libraryJarsProguardFile, true)
                        keepModuleClasses(jarInput.file.absolutePath, tempClassDir, module.keepClasses)
                        return
                    }
                }

                //gradle4.x输入jar不在build-cache目录，删除相关的依赖lib可以根据名字判断。
                //例如gradle4.x 输入的jar路径是：/Users/fy/.gradle/caches/transforms-1/files-1.1/support-fragment-28.0.0-rc02.aar/59ce33ef0fec498f1664c7ae026d7518/jars/classes.jar
                //根据implementation获取的依赖路径是：/Users/fy/.gradle/caches/modules-2/files-2.1/com.android.support/support-fragment/28.0.0-rc02/ddd2d04e216aabc4c9342d172febfa365b171954/support-fragment-28.0.0-rc02.aar
                //路径中包含support-fragment-28.0.0-rc02.aar则可以删除
                if (isGradle4x() && null != mRelativeLibs) {
                    for (String relativeLibName : mRelativeLibs) {
                        String inputFilePath = jarInput.file.absolutePath
                        if (inputFilePath.contains(relativeLibName)) {
                            println "exclude lib ${relativeLibName} libCachePath is ${inputFilePath}"
                            if (relativeLibName.endsWith('.aar')) {
                                String unzipedDir = inputFilePath.substring(0, inputFilePath.length() - '/jars/classes.jar'.length())
                                println 'unZipDir:' + unzipedDir
                                String packageName = parsePackageName(unzipedDir + File.separator + 'AndroidManifest.xml')
                                if (null != packageName) {
                                    excludeClasses.add(packageName + '.R.class')
                                }

                                excludeOtherFile.addAll(ExcludeFileUtils.getOtherExcludeFromDir(unzipedDir))
                            }
                            //有的jar中会包含assets等资源，一并排除掉
                            excludeOtherFile.addAll(ExcludeFileUtils.getOtherExcludeFromJar(inputFilePath))
                            return
                        }
                    }
                }

                if (jarInput.file.absolutePath.startsWith(buildCacheDir)) {
                    CacheLibInfo libInfo = getCacheFile(buildCacheDir, jarInput.file.absolutePath)
                    PhantomPluginConfig.ExcludeConfig cfg = mWlExclude.excludeLibInfo(libInfo.name)
                    if (null != libInfo
                            && (null != cfg
                            || mRelativeLibs.contains(libInfo.path))) {
                        println "exclude lib ${libInfo.name} libCachePath is ${jarInput.file.absolutePath}"
                        //null == cfg表示是相关联的库，只有指定不删除资源的库才保留R类
                        if (null == cfg || cfg.isExcludeRes) {
                            excludeClasses.add(libInfo.packageName + '.R.class')
                        }
                        excludeOtherFile.addAll(ExcludeFileUtils.getOtherExcludeFromDir(libInfo.cachePath))
                        //有的jar中会包含assets等资源，一并排除掉
                        excludeOtherFile.addAll(ExcludeFileUtils.getOtherExcludeFromJar(jarInput.file.absolutePath))
                        return
                    }
                }

                for (PhantomPluginConfig.ExcludeConfig excludeJar : mWlExclude.excludeLibs) {
                    String jarPath = jarInput.file.absolutePath
                    if (jarPath.indexOf("${File.separator}${excludeJar.artifactId}-${excludeJar.version}") >= 0
                            || jarPath.indexOf("${File.separator}${excludeJar.artifactId}${File.separator}${excludeJar.version}") >= 0
                            || mRelativeLibs.contains(jarPath)) {
                        println "exclude lib ${excludeJar.name} libPath is ${jarPath}"
                        if (jarPath.contains("${excludeJar.artifactId}-${excludeJar.version}.aar") && jarPath.endsWith("jars${File.separator}classes.jar")) {
                            //aar
                            String unzipedDir = jarPath.substring(0, jarPath.length() - '/jars/classes.jar'.length())
                            println 'unZipDir:' + unzipedDir
                            String packageName = parsePackageName(unzipedDir + File.separator + 'AndroidManifest.xml')
                            if (null != packageName) {
                                excludeClasses.add(packageName + '.R.class')
                            }

                            excludeOtherFile.addAll(ExcludeFileUtils.getOtherExcludeFromDir(unzipedDir))
                        }
                        parseOtherExcludeFile(excludeOtherFile, jarInput.file.absolutePath)
                        return
                    }
                }

                //其他的jar直接输出到下一个transform
                String newFileName = "${jarInput.file.name.substring(0, jarInput.file.name.length() - 4)}_${System.currentTimeMillis()}"
                File otherJar = transformInvocation.outputProvider.getContentLocation(newFileName, TransformManager.CONTENT_JARS, TransformManager.SCOPE_FULL_PROJECT, Format.JAR)
                println "transform otherJar ${jarInput.file.absolutePath} to ${otherJar.absolutePath} start"
                if (!otherJar.getParentFile().exists()) {
                    otherJar.getParentFile().mkdirs()
                }
                JarUtils.transformJar(jarInput.file.absolutePath, otherJar, new JarVisitor() {
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
                println "transform otherJar ${jarInput.file.absolutePath} to ${otherJar.absolutePath} end"
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

            println("delete classes: ${path.toString()}")
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
                    println("delete classes: ${rFile.absolutePath}")
                    FileUtils.deletePath(rFile)
                }
            }
        }

        File dest = transformInvocation.outputProvider.getContentLocation("excluded", TransformManager.CONTENT_JARS, TransformManager.SCOPE_FULL_PROJECT, Format.JAR)
        println "makeJar ${tempClassDir} to ${dest.absolutePath}"
        JarUtils.makeJar(tempClassDir, dest.absolutePath)

        if (isGradle4x()) {
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
    String getModuleManifestByJar(File jarFile) {
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

        println "exclude module not find AndroidManifest.xml for ${jarFile.absolutePath}"
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

                    println "origin ResourceSets:" + resList
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
                        println "delete ResourcesSet:" + deleteItem
                    }

                    it.setInputResourceSets(resList)
                    println "new ResourceSets:" + resList
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
                    ResourcePreprocessor processor = (ResourcePreprocessor)methodProcessor.invoke(it)
                    resList = (List<ResourceSet>)methodResourceSet.invoke(it, processor)

                    println "origin ResourceSets:" + resList
                    List<ResourceSet> removedRes = new ArrayList<ResourceSet>()
                    for (String resKey : excludeResKeys) {
                        for (ResourceSet res : resList) {
                            if (res.sourceFiles[0].absolutePath.startsWith(resKey)) {
                                removedRes.add(res)
                                break
                            }

                            boolean find = false

                            if (null != mRelativeLibs) {
                                for (String relativeLibName : mRelativeLibs) {
                                    if (res.sourceFiles[0].absolutePath.contains(relativeLibName)) {
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
                        println "delete ResourcesSet:" + deleteItem
                    }

                    fieldPi.set(it, resList)

                    println "new ResourceSets:" + resList
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
        if (0 == excludeOthers.size()) {
            return
        }

        println "excludeSoAndRes: " + excludeOthers

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
        if (0 == excludeOthers.size()) {
            return
        }

        println "excludeSoAndRes2: " + excludeOthers
        for (String excludeFile : excludeOthers) {
            project.android.packagingOptions.exclude(excludeFile)
        }

    }


    private void parseOtherExcludeFile(Set<String> excludes, String filePath) {
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
    private CacheLibInfo getCacheFile(String cachePath, String filePath) {
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
     * @param path AndroidManifest的路径
     * @return AndroidManifest的package属性值
     */
    private String parsePackageName(String path) {
        if (path == null) {
            return null
        }

        File manifestFile = new File(path)
        if (!manifestFile.exists()) {
            return null
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance()
            DocumentBuilder db = dbf.newDocumentBuilder()
            Document doc = db.parse(path)
            Element manifestNode = doc.getDocumentElement()
            return manifestNode.getAttribute('package')
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace()
        } catch (SAXException se) {
            se.printStackTrace()
        } catch (IOException ioe) {
            ioe.printStackTrace()
        }

        return null
    }

    /**
     * 获取一个库的依赖库，比如support-v4在版本25.3.1被拆分成多个库，在排除support-v4的时候应该将它的依赖库一起删除
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
     * 适配gradle4.x获取一个库的依赖库，比如support-v4在版本25.3.1被拆分成多个库，在排除support-v4的时候应该将它的依赖库一起删除
     * 这里是获取lib的直接依赖包，比如support-v4依赖了support-fragment，support-fragment又依赖了support-runtime, 这里就只获取support-fragment
     * @param excludeLibs ExcludeClasses的excludeLibs值
     * @return ExcludeClasses的excludeLibs的依赖库
     */
    private Set<String> parseRelativeLibs2(List<PhantomPluginConfig.ExcludeConfig> excludeLibs) {
        Set<String> relativeLibs = new HashSet<>()

        project.rootProject.allprojects.each {
            try {
                Configuration compileConf = it.configurations.getByName('releaseCompileClasspath')
                Set<String> directDependencies = new HashSet<>()
                //获取lib的直接依赖库
                compileConf.getIncoming().getResolutionResult().getRoot().getDependencies().each {
                    if (!(it instanceof UnresolvedDependencyResult)) {
                        ResolvedDependencyResult rr = (ResolvedDependencyResult)it
                        if (rr.requested instanceof DefaultModuleComponentSelector) {
                            directDependencies.add("${it.requested.module}-${it.requested.version}".toString())
                            rr.selected.dependencies.each {
                                directDependencies.add("${it.requested.module}-${it.requested.version}".toString())
                            }
                        }
                    }
                }
                //获取lib直接依赖包对应的文件
                compileConf.allDependencies.each {
                    String dependencyStr = "${it.group}:${it.name}:${it.version}"
                    for (PhantomPluginConfig.ExcludeConfig excludeLib : excludeLibs) {
                        if (excludeLib.name == dependencyStr) {
                            for (File libFile : compileConf.files(it)) {
                                String libName = libFile.name
                                if (directDependencies.contains(libName.substring(0, libName.length() - 4))) {
                                    relativeLibs.add(libName)
                                }
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
        printf("[writeLibraryJarsToProguardFile] E %s, append: %s\n", proguardFile, append)

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

        printf("[writeLibraryJarsToProguardFile] X %s, append: %s\n", proguardFile, append)
    }

    /**
     * 将需要保留的类写入混淆配置文件，避免类在混淆时被删掉
     * @param classes 需要保留的类集合
     * @param proguardFile需要写入 -keep class 配置的 proguard 文件
     */
    private void writeKeepClassesToProguardFile(Set<String> classes, File proguardFile) {
        printf("[writeKeepClassesToProguardFile] E %s", proguardFile)

        def writeClasses = { writer ->
            classes.each {
                def line = "-keep class $it"
                println(line)

                writer.write(line)
                writer.write('\n')
            }
        }

        proguardFile.withWriterAppend('utf-8', writeClasses)

        printf("[writeKeepClassesToProguardFile] X %s", proguardFile)
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
    private void excludeAndroidManifests(Task task, PhantomPluginConfig configs) {
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
                    println "exclude module AndroidManifest.xml for ${moduleCfg.name}"
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
                    println "exclude lib AndroidManifest.xml for ${libCfg.name}"
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
    private void excludeAndroidManifests2(Task task, PhantomPluginConfig config) {
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
                    println("exclude manifest:" + manifest.file.absolutePath)
                    break
                }
            }

            if (find) {
                continue
            }

            for (PhantomPluginConfig.ExcludeConfig libCfg : config.excludeLibs) {
                if (libCfg.name == manifest.identifier.componentId.toString()) {
                    iterator.remove()
                    println("exclude manifest:" + manifest.file.absolutePath)
                }
            }
        }
    }

    // Android Gradle Plugin 的版本获取比较麻烦，这里使用 Gradle 版本替代
    // Gradle 4.x 配置使用 Android Gradle Plugin 3.x
    private boolean isGradle4x() {
        return project.gradle.gradleVersion.startsWith('4.')
    }

    private String getBuildCacheDir() {
        if (isGradle4x()) {
            return project.android.applicationVariants[0].variantData.scope.globalScope.buildCache
        }

        return AndroidGradleOptions.getBuildCacheDir(project)
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
