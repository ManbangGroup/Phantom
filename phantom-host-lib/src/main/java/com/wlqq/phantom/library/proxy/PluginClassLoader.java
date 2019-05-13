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

package com.wlqq.phantom.library.proxy;

import android.os.Build;
import android.util.Log;
import android.util.TimingLogger;

import dalvik.system.DexClassLoader;

import com.wlqq.phantom.library.log.LogReporter;
import com.wlqq.phantom.library.pm.PluginInfo;
import com.wlqq.phantom.library.utils.FileUtils;
import com.wlqq.phantom.library.utils.IoUtils;
import com.wlqq.phantom.library.utils.ReflectUtils;
import com.wlqq.phantom.library.utils.SuppressFBWarnings;
import com.wlqq.phantom.library.utils.VLog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PluginClassLoader extends DexClassLoader {
    private static final String TAG = "PluginClassLoader";
    // how to enable detail log
    // $ adb shell setprop log.tag.PluginClassLoader DEBUG
    private static final boolean PRINT_DETAIL_LOG = Log.isLoggable(TAG, Log.VERBOSE);

    /**
     * We look for additional dex files named {@code classes2.dex},
     * {@code classes3.dex}, etc.
     */
    private static final String DEX_PREFIX = "classes";
    private static final String DEX_SUFFIX = ".dex";
    // 用于记录额外的 dex 数量
    public static final String EXTRA_DEX_COUNT_FILE = "multidex.count";

    public PluginClassLoader(PluginInfo pluginInfo, ClassLoader parent) {
        super(pluginInfo.apkPath, pluginInfo.odexDir, pluginInfo.libPath, parent);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            installMultiDexBeforeLollipop(pluginInfo, parent);
        }
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        Class<?> clazz = null;

        try {
            clazz = findClassFast(className);
        } catch (ClassNotFoundException e) {
            // ignore it on purpose
        }

        if (clazz != null) {
            return clazz;
        }

        return super.loadClass(className);
    }

    Class<?> findClassFast(String className) throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(className);
        if (clazz != null) {
            return clazz;
        }

        return findClass(className);
    }

    /**
     * install extra dexes for Android 4.x
     *
     * @see <a href="https://github.com/Qihoo360/RePlugin/pull/264/files">
     *     support multidex feature in plugin application for the ROM below LOLLIPOP</a>
     * @deprecated apply to ROM before Lollipop,may be deprecated
     */
    private void installMultiDexBeforeLollipop(PluginInfo pi, ClassLoader parent) {
        VLog.e("for plugin: %s", pi.packageName);
        TimingLogger logger = new TimingLogger(TAG, "installMultiDexBeforeLollipop");
        try {
            List<File> files = loadSecondaryDexes(pi, false);
            logger.addSplit("loadSecondaryDexes, forceReload: false");
            try {
                installSecondaryDexes(pi, parent, files);
                logger.addSplit("installSecondaryDexes first");
                // Some IOException causes may be fixed by a clean extraction.
            } catch (Exception e) {
                VLog.e(e, "Failed to install extracted secondary dex files, retrying with "
                        + "forced extraction");
                files = loadSecondaryDexes(pi, true);
                logger.addSplit("loadSecondaryDexes, forceReload: true");

                installSecondaryDexes(pi, parent, files);
                logger.addSplit("installSecondaryDexes second");
            }
        } catch (Exception e) {
            VLog.e(e, "error install multidex");
            LogReporter.reportException(new MultiDexInstallException(e));
        } finally {
            logger.dumpToLog();
        }

    }

    private void installSecondaryDexes(PluginInfo pi, ClassLoader parent, List<File> dexFiles)
            throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException {
        if (dexFiles != null && dexFiles.size() > 0) {

            List<Object[]> allElements = new LinkedList<>();

            // get dexElements of main dex
            Class<?> clz = Class.forName("dalvik.system.BaseDexClassLoader");
            Object pathList = ReflectUtils.readField(clz, this, "pathList");
            Object[] mainElements = (Object[]) ReflectUtils.readField(pathList.getClass(), pathList, "dexElements");
            allElements.add(mainElements);

            // get dexElements of extra dex (need to load dex first)
            String optimizedDirectory = pi.getExtraOdexesDir().getAbsolutePath();

            for (File file : dexFiles) {
                if (PRINT_DETAIL_LOG) {
                    VLog.e("dex file: %s", file.getName());
                }

                DexClassLoader dexClassLoader = new DexClassLoader(file.getAbsolutePath(), optimizedDirectory,
                        optimizedDirectory, parent);

                Object obj = ReflectUtils.readField(clz, dexClassLoader, "pathList");
                Object[] dexElements = (Object[]) ReflectUtils.readField(obj.getClass(), obj, "dexElements");
                allElements.add(dexElements);
            }

            // combine Elements
            Object combineElements = combineArray(allElements);

            // rewrite Elements combined to classLoader
            ReflectUtils.writeField(pathList.getClass(), pathList, "dexElements", combineElements);

            // Test whether the Extra Dex is installed
            if (PRINT_DETAIL_LOG) {
                Object object = ReflectUtils.readField(pathList.getClass(), pathList, "dexElements");
                int length = Array.getLength(object);
                VLog.e("dexElements length: %d", length);
            }
        }
    }

    /**
     * combine dexElements Array
     *
     * @param allElements all dexElements of dexes
     * @return the combined dexElements
     */
    private static Object combineArray(List<Object[]> allElements) {

        int startIndex = 0;
        int arrayLength = 0;
        Object[] originalElements = null;

        for (Object[] elements : allElements) {

            if (originalElements == null) {
                originalElements = elements;
            }

            arrayLength += elements.length;
        }

        Object[] combined = (Object[]) Array.newInstance(
                originalElements.getClass().getComponentType(), arrayLength);

        for (Object[] elements : allElements) {

            System.arraycopy(elements, 0, combined, startIndex, elements.length);
            startIndex += elements.length;
        }

        return combined;
    }

    private static List<File> loadSecondaryDexes(PluginInfo pi, boolean forceReload) throws IOException {
        List<File> files;
        if (forceReload) {
            if (PRINT_DETAIL_LOG) {
                VLog.e("Forced extraction must be performed.");
            }
            files = performExtractions(pi);
            putSecondaryDexesCount(pi, files);
        } else {
            try {
                files = loadExistingExtractions(pi);
            } catch (IOException ioe) {
                VLog.e(ioe, "Failed to reload existing extracted secondary dex files, "
                        + "falling back to fresh extraction");
                files = performExtractions(pi);
                putSecondaryDexesCount(pi, files);
            }
        }
        if (PRINT_DETAIL_LOG) {
            VLog.e("load found %d secondary dex files", files.size());
        }
        return files;
    }

    private static List<File> loadExistingExtractions(PluginInfo pi) throws IOException {
        if (PRINT_DETAIL_LOG) {
            VLog.e("loadExistingExtractions");
        }

        final File extraDexesDir = pi.getExtraDexesDir();

        final int totalDexNumber = getSecondaryDexesCount(pi);
        List<File> files = new ArrayList<>(totalDexNumber - 1);

        for (int secondaryNumber = 2; secondaryNumber <= totalDexNumber; secondaryNumber++) {
            File dexFile = new File(extraDexesDir, DEX_PREFIX + secondaryNumber + DEX_SUFFIX);
            if (dexFile.isFile()) {
                files.add(dexFile);
            } else {
                throw new IOException("Missing extracted secondary dex file '" + dexFile.getPath() + "'");
            }
        }

        return files;
    }

    private static List<File> performExtractions(PluginInfo pi) throws IOException {
        if (PRINT_DETAIL_LOG) {
            VLog.e("performExtractions");
        }

        final File extraDexesDir = pi.getExtraDexesDir();
        FileUtils.cleanDir(extraDexesDir);

        List<File> files = new ArrayList<>();

        final ZipFile apk = new ZipFile(pi.apkPath);

        try {
            int secondaryNumber = 2;
            ZipEntry dexFile = apk.getEntry(DEX_PREFIX + secondaryNumber + DEX_SUFFIX);
            while (dexFile != null) {
                final File outFile = new File(extraDexesDir, dexFile.getName());
                extractFile(apk, dexFile, outFile);
                files.add(outFile);

                secondaryNumber++;
                dexFile = apk.getEntry(DEX_PREFIX + secondaryNumber + DEX_SUFFIX);
            }
        } finally {
            FileUtils.closeZipFileQuietly(apk);
        }

        return files;
    }

    private static int getSecondaryDexesCount(PluginInfo pi) throws IOException {
        return FileUtils.readFileToInt(getDexesCountFile(pi));
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private static void putSecondaryDexesCount(PluginInfo pi, List<File> extractedDexes) {
        final File dexesCountFile = getDexesCountFile(pi);
        try {
            FileUtils.writeIntToFile(dexesCountFile, extractedDexes.size() + 1);
        } catch (IOException e) {
            VLog.e(e, "error putSecondaryDexesCount");
            dexesCountFile.delete();
        }
    }

    private static File getDexesCountFile(PluginInfo pi) {
        return new File(pi.installDir, EXTRA_DEX_COUNT_FILE);
    }

    private static void extractFile(ZipFile zipFile, ZipEntry ze, File outFile) throws IOException {
        InputStream in = null;
        try {
            in = zipFile.getInputStream(ze);
            FileUtils.copyInputStreamToFile(in, outFile);
            if (PRINT_DETAIL_LOG) {
                VLog.e("extractFile(): Success! fn: %s", outFile.getName());
            }
        } finally {
            IoUtils.closeQuietly(in);
        }
    }
}
