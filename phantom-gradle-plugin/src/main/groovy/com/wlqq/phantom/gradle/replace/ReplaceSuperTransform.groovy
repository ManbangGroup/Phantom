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

package com.wlqq.phantom.gradle.replace

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.wlqq.phantom.gradle.utils.JarUtils
import com.wlqq.phantom.gradle.utils.JarVisitor
import com.wlqq.phantom.gradle.utils.Log
import javassist.ClassPool
import javassist.CtClass
import javassist.NotFoundException
import javassist.bytecode.*
import org.gradle.api.Project

import java.lang.reflect.Modifier
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ReplaceSuperTransform extends Transform {
    static final TAG = "__ReplaceSuperClass__"

    Project project
    private static final String INTERCEPT_ACTIVITY = 'com.wlqq.phantom.library.proxy.PluginInterceptActivity'
    private static final String INTERCEPT_APPLICATION = 'com.wlqq.phantom.library.proxy.PluginInterceptApplication'
    private static final String INTERCEPT_SERVICE = 'com.wlqq.phantom.library.proxy.PluginInterceptService'
    private static final String INTERCEPT_INTENT_SERVICE = 'com.wlqq.phantom.library.proxy.PluginInterceptIntentService'
    private static final String INTERCEPT_FRAGMENT = 'com.wlqq.phantom.library.proxy.SysFragmentProxy'
    private static final String INTERCEPT_DIALOG_FRAGMENT = 'com.wlqq.phantom.library.proxy.SysDialogFragmentProxy'
    private static final String INTERCEPT_LIST_FRAGMENT = 'com.wlqq.phantom.library.proxy.SysListFragmentProxy'
    private static
    final String INTERCEPT_PREFERENCE_FRAGMENT = 'com.wlqq.phantom.library.proxy.SysPreferenceFragmentProxy'


    private Set<String> mClassPaths = new HashSet<>()


    ReplaceSuperTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return TAG
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        project.android.applicationVariants.each { variant ->
            variant.javaCompiler.classpath.files.each { file ->
                mClassPaths.add(file.absolutePath)
            }
        }
        mClassPaths.add(getPlatform())

        String transformsDir = "${project.buildDir.toString()}${File.separator}intermediates${File.separator}transforms${File.separator}${getName()}"
        File tempFile = new File(transformsDir)
        if (tempFile.exists() && tempFile.isDirectory()) {
            FileUtils.deletePath(tempFile)
        }

        String tempClassDir = "${project.buildDir.toString()}${File.separator}tmp${File.separator}tmpPluginClasses"
        tempFile = new File(tempClassDir)
        if (tempFile.exists() && tempFile.isDirectory()) {
            FileUtils.deletePath(tempFile)
        }

        transformInvocation.inputs.each { TransformInput input ->
            /**
             * 遍历输入目录并拷贝到tempClassDir
             */
            input.directoryInputs.each { DirectoryInput directoryInput ->
                Log.i TAG, "Copying ${directoryInput.file.absolutePath} to ${tempClassDir} start"
                FileUtils.copyDirectory(directoryInput.file, new File(tempClassDir))
                //替换父类
                replaceComponentSuperClass(tempFile)

                File dest = transformInvocation.outputProvider.getContentLocation("replacedsuper", TransformManager.CONTENT_JARS, TransformManager.SCOPE_FULL_PROJECT, Format.JAR)
                Log.i TAG, "makeJar ${tempClassDir} to ${dest.absolutePath}"
                JarUtils.makeJar(tempClassDir, dest.absolutePath)
                Log.i TAG, "Copying ${directoryInput.file.absolutePath} to ${tempClassDir} end"
            }

            /**
             * 遍历输入jar并解压到tempClassDir
             */
            input.jarInputs.each { JarInput jarInput ->
                String newFileName = "${jarInput.file.name.substring(0, jarInput.file.name.length() - 4)}_${jarInput.file.hashCode()}_${jarInput.file.length()}"
                File otherJar = transformInvocation.outputProvider.getContentLocation(newFileName, TransformManager.CONTENT_JARS, TransformManager.SCOPE_FULL_PROJECT, Format.JAR)
                if (!otherJar.getParentFile().exists()) {
                    otherJar.getParentFile().mkdirs()
                }
                Log.i TAG, "DealJar ${jarInput.file.absolutePath} to ${otherJar.absolutePath} start"
                ClassPool pool = new ClassPool()
                mClassPaths.add(jarInput.file.absolutePath)
                for (String clsPath : mClassPaths) {
                    try {
                        pool.insertClassPath(clsPath)
                    } catch (NotFoundException e) {
                        // ignore not found path
                    }
                }
                JarUtils.transformJar(jarInput.file.absolutePath, otherJar, new JarVisitor() {
                    @Override
                    boolean visitEntry(ZipEntry entry, ZipInputStream inputJar, ZipOutputStream outputJar) {
                        String entryName = entry.getName()
                        if (!entryName.endsWith('.class') || entryName.startsWith("android/")) {
                            //非class文件保持原样输出
                            return true
                        }

                        //println("----dealJar=${jarInput.file.name},entry=${entryName}")
                        String clsName = entryName.substring(0, entryName.length() - 6)
                        clsName = clsName.replace((char) '/', (char) '.')
                        CtClass replacedClass = pool.get(clsName)
                        boolean supperChanged = replaceSuperClass(replacedClass)
                        boolean methodChanged = ReplaceMethodCall.replaceFragmentMethods(replacedClass)
                        if (!supperChanged && !methodChanged) {
                            //没有替换的class 原样输出
                            return true
                        }

                        //输出替换过的class到jar
                        byte[] classData = replacedClass.toBytecode()
                        JarUtils.saveEntry(entryName, classData, outputJar)
                        replacedClass.defrost()

                        return false
                    }
                })
                Log.i TAG, "DealJar ${jarInput.file.absolutePath} to ${otherJar.absolutePath} end"
            }
        }
    }

    /**
     * 替换自定义Activity的父类为{@link ReplaceSuperTransform#INTERCEPT_ACTIVITY}
     * 替换规则：
     * 1. 包名以android开头的类不替换，即support包中的activity子类不替换
     * 2. 替换父类为Activity和FragmentActivity的类（我们目前实际使用情况是Activity都继承自系统Activity和FragmentActivity）
     *
     * @param dir 工程class所在目录
     */
    private void replaceComponentSuperClass(File dir) {
        Pattern pattern = Pattern.compile('.*\\.class$')
        List<File> allClasses = FileUtils.find(dir, pattern)
        ClassPool pool = new ClassPool()
        mClassPaths.add(dir.getAbsolutePath())
        for (String clsPath : mClassPaths) {
            try {
                pool.insertClassPath(clsPath)
            } catch (NotFoundException e) {
                // ignore not found path
            }
        }

        allClasses.each { classFile ->
            String packagePath = classFile.absolutePath.substring(dir.absolutePath.length() + 1)
            String className = packagePath.substring(0, packagePath.length() - 6).replace(File.separatorChar, (char) '.')
            //android.开头的包里面的class不替换,这里主要是排除support包中的class
            if (className.startsWith('android.')) {
                return
            }
            CtClass replacedClass = pool.get(className)
            boolean superChanged = replaceSuperClass(replacedClass)
            boolean methodChanged = ReplaceMethodCall.replaceFragmentMethods(replacedClass)
            if (superChanged || methodChanged) {
                replacedClass.writeFile(dir.absolutePath)
            }
        }
    }

    private boolean replaceSuperClass(CtClass replacedClass) {

        if (replacedClass.isEnum()
                || replacedClass.isInterface()
                || replacedClass.isAnnotation()
                || Modifier.isStatic(replacedClass.getModifiers())) {
            return false
        }
        replacedClass.getModifiers()

        String superFullName = replacedClass.classFile.superclass
        if (null == superFullName) {
            return false
        }

        boolean res = false;
        //只替换父类是FragmentActivity和Activity的类
        if (superFullName == 'android.support.v4.app.FragmentActivity'
                || superFullName ==~ /android\.app\.\w*Activity/
                || superFullName == 'android.support.v7.app.AppCompatActivity'
                || superFullName == 'android.preference.PreferenceActivity') {
            //只替换父类是FragmentActivity和Activity的类

            Log.i TAG, "replace activity class:${replacedClass.getName()}"
            replacedClass.defrost()
            replaceSuperCall(replacedClass.classFile, superFullName, INTERCEPT_ACTIVITY)
            //替换构造函数super调用
            replacedClass.classFile.setSuperclass(INTERCEPT_ACTIVITY)
            res = true
            Log.i TAG, "replace activity class:${replacedClass.getName()} end"
        } else if (superFullName == 'android.app.Application') {
            //替换Application类的父类

            Log.i TAG, "replace application class:${replacedClass.getName()}"
            replacedClass.defrost()
            replaceSuperCall(replacedClass.classFile, superFullName, INTERCEPT_APPLICATION)
            //替换构造函数super调用
            replacedClass.classFile.setSuperclass(INTERCEPT_APPLICATION)
            res = true
            Log.i TAG, "replace application class:${replacedClass.getName()} end"
        } else if (superFullName == 'android.app.Service') {
            //替换Service类的父类

            Log.i TAG, "replace service class:${replacedClass.getName()}"
            replacedClass.defrost()
            replaceSuperCall(replacedClass.classFile, superFullName, INTERCEPT_SERVICE)
            //替换构造函数super调用
            replacedClass.classFile.setSuperclass(INTERCEPT_SERVICE)
            res = true
            Log.i TAG, "replace service class:${replacedClass.getName()} end"
        } else if (superFullName == 'android.app.IntentService') {
            //替换 IntentService 类的父类

            Log.i TAG, "replace intent service class:${replacedClass.getName()}"
            replacedClass.defrost()
            replaceSuperCall(replacedClass.classFile, superFullName, INTERCEPT_INTENT_SERVICE)
            //替换构造函数super调用
            replacedClass.classFile.setSuperclass(INTERCEPT_INTENT_SERVICE)
            res = true
            Log.i TAG, "replace intent service class:${replacedClass.getName()} end"
        } else if (superFullName == 'android.app.Fragment') {
            //替换系统Fragment
            Log.i TAG, "replace Sys Framgment class:${replacedClass.getName()}"
            replacedClass.defrost()
            replaceSuperCall(replacedClass.classFile, superFullName, INTERCEPT_FRAGMENT)
            //替换构造函数super调用
            replacedClass.classFile.setSuperclass(INTERCEPT_FRAGMENT)
            res = true
            Log.i TAG, "replace Sys Framgment class:${replacedClass.getName()} end"
        } else if (superFullName == 'android.app.DialogFragment') {
            //替换系统DialogFragment
            Log.i TAG, "replace Sys DialogFramgment class:${replacedClass.getName()}"
            replacedClass.defrost()
            replaceSuperCall(replacedClass.classFile, superFullName, INTERCEPT_DIALOG_FRAGMENT)
            //替换构造函数super调用
            replacedClass.classFile.setSuperclass(INTERCEPT_DIALOG_FRAGMENT)
            res = true
            Log.i TAG, "replace Sys DialogFramgment class:${replacedClass.getName()} end"
        } else if (superFullName == 'android.app.ListFragment') {
            //替换系统ListFragment
            Log.i TAG, "replace Sys ListFragment class:${replacedClass.getName()}"
            replacedClass.defrost()
            replaceSuperCall(replacedClass.classFile, superFullName, INTERCEPT_LIST_FRAGMENT)
            //替换构造函数super调用
            replacedClass.classFile.setSuperclass(INTERCEPT_LIST_FRAGMENT)
            res = true
            Log.i TAG, "replace Sys ListFragment class:${replacedClass.getName()} end"
        } else if (superFullName == 'android.preference.PreferenceFragment') {
            //替换系统PreferenceFragment
            Log.i TAG, "replace Sys PreferenceFragment class:${replacedClass.getName()}"
            replacedClass.defrost()
            replaceSuperCall(replacedClass.classFile, superFullName, INTERCEPT_PREFERENCE_FRAGMENT)
            //替换构造函数super调用
            replacedClass.classFile.setSuperclass(INTERCEPT_PREFERENCE_FRAGMENT)
            res = true
            Log.i TAG, "replace Sys PreferenceFragment class:${replacedClass.getName()} end"
        }

        return res
    }

    //获取当前编译使用的android.jar，在加载Activity的子类的时候需要
    private String getPlatform() {
        return "${project.android.getSdkDirectory()}${File.separator}platforms${File.separator}" +
                "${project.android.getCompileSdkVersion()}${File.separator}android.jar"
    }

    /**
     * 由于CtClass#setSuperclass方法只替换构造函数中的super调用，在android5.0以上手机会出现问题。其他方法
     * 中的super调用也需要替换。例如super.onCreate(), super.setContentView()等
     *
     * @param targetClass 需要替换的class文件
     * @param oldSuper 原来的父类
     * @param newSuper 新的父类
     */
    private void replaceSuperCall(ClassFile targetClass, String oldSuper, String newSuper) {
        List methodList = targetClass.getMethods()
        for (MethodInfo mi : methodList) {
            if (mi.isConstructor()) {
                continue
            }

            CodeAttribute ca = mi.getCodeAttribute()
            if (null == ca) {
                continue
            }
            byte[] code = ca.getCode();
            for (int i = 0; i < code.length - 2; i++) {
                //获取操作码
                int optCode = code[i] & 0xff
                //如果操作码是指定的函数调用则可以进行替换，
                //183 对应javassist.bytecode.Opcode#INVOKESPECIAL = 183
                if (optCode == 183) {
                    ConstPool cp = ca.getConstPool()
                    int mRef = ByteArray.readU16bit(code, i + 1)
                    try {
                        //getTag中调用了getItem可能发生NPE
                        if (cp.getTag(mRef) != ConstPool.CONST_Methodref) {
                            //不是方法调用
                            continue
                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace()
                        continue
                    }
                    //super 方法所在的类
                    String clsRef = cp.getMethodrefClassName(mRef)
                    //如果super调用的方法所在的类是原来的父类，就进行替换
                    if (clsRef == (oldSuper)) {
                        int nt = cp.getMethodrefNameAndType(mRef)
                        int sc = cp.addClassInfo(newSuper)
                        int mRef2 = cp.addMethodrefInfo(sc, nt)
                        ByteArray.write16bit(mRef2, code, i + 1)
                    }
                }
            }
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
}
