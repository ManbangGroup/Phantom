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

package com.wlqq.phantom.library.pm;


import android.os.Build;
import android.support.annotation.NonNull;

import com.wlqq.phantom.library.utils.FileUtils;
import com.wlqq.phantom.library.utils.VLog;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * 用于在安装插件时拷贝 so 库的工具类
 */
final class NativeLibraryUtils {
    // see https://developer.android.com/ndk/guides/abis.html#sa
    static final String ABI_ARMEABI = "armeabi";
    static final String ABI_ARMEABI_V7A = "armeabi-v7a";
    static final String ABI_ARM64_V8A = "arm64-v8a";
    static final String ABI_X86 = "x86";
    static final String ABI_X86_64 = "x86_64";
    static final String ABI_MIPS = "mips";
    static final String ABI_MIPS64 = "mips64";

    private NativeLibraryUtils() {
        // prevent instantiation
    }

    /**
     * Copies native binaries to a shared library directory.
     *
     * @param apkFile          APK file to scan for native libraries
     * @param sharedLibraryDir directory for libraries to be copied to
     * @throws CopyNativeSoException 拷贝 so 失败
     */
    static void copyNativeBinaries(@NonNull File apkFile, @NonNull File sharedLibraryDir) throws CopyNativeSoException {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(apkFile);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (String abi : Build.SUPPORTED_ABIS) {
                    if (copyNativeBinaries(zipFile, sharedLibraryDir, abi)) {
                        VLog.i("copyNativeBinaries ok, abi: %s", abi);
                        return;
                    }
                }
            }

            if (copyNativeBinaries(zipFile, sharedLibraryDir, Build.CPU_ABI)) {
                VLog.i("copyNativeBinaries ok, abi: %s", Build.CPU_ABI);
                return;
            }

            if (copyNativeBinaries(zipFile, sharedLibraryDir, Build.CPU_ABI2)) {
                VLog.i("copyNativeBinaries ok, abi: %s", Build.CPU_ABI2);
                return;
            }

            if (copyNativeBinaries(zipFile, sharedLibraryDir, ABI_ARMEABI)) {
                VLog.i("copyNativeBinaries ok, abi: %s", ABI_ARMEABI);
                return;
            }

            VLog.i("copyNativeBinaries not found");

        } catch (IOException e) {
            final String msg = "copyNativeBinaries error, create ZipFile error";
            VLog.w(e, msg);
            FileUtils.cleanDir(sharedLibraryDir);
            throw new CopyNativeSoException(msg, e);
        } finally {
            FileUtils.closeZipFileQuietly(zipFile);
        }
    }

    /**
     * Copies native binaries to a shared library directory.
     *
     * @param zipFile          APK file to scan for native libraries
     * @param sharedLibraryDir directory for libraries to be copied to
     * @param abi              device abi type
     * @return copy so successfully
     * @throws CopyNativeSoException 拷贝 so 失败
     * @see #ABI_ARMEABI
     * @see #ABI_ARMEABI_V7A
     * @see #ABI_ARM64_V8A
     * @see #ABI_X86
     * @see #ABI_X86_64
     * @see #ABI_MIPS
     * @see #ABI_MIPS64
     */
    static boolean copyNativeBinaries(@NonNull ZipFile zipFile, @NonNull File sharedLibraryDir, @NonNull String abi)
            throws CopyNativeSoException {
        int copiedSoCount = 0;
        try {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.contains("../")) {
                    continue;
                }
                if (!entry.isDirectory() && name.startsWith("lib/" + abi + "/") && name.endsWith(".so")) {
                    String soName = name.substring(name.lastIndexOf('/') + 1);
                    final File destination = new File(sharedLibraryDir, soName);
                    VLog.w("copy from %s to %s", name, destination);
                    FileUtils.copyInputStreamToFile(zipFile.getInputStream(entry), destination);
                    copiedSoCount++;
                }
            }
            return copiedSoCount > 0;
        } catch (IOException e) {
            final String msg = "copyNativeBinaries error, abi: " + abi;
            VLog.w(e, msg);
            FileUtils.cleanDir(sharedLibraryDir);
            throw new CopyNativeSoException(msg, e);
        }
    }
}
