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

package com.wlqq.phantom.library.utils;

import android.os.Build;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class VmUtils {
    /**
     * 是否运行阿里云 OS
     */
    private static final boolean IS_YUN_OS = isYunOS();
    private static final int VM_WITH_ART_VERSION_MAJOR = 2;
    private static final int VM_WITH_ART_VERSION_MINOR = 1;
    /**
     * 是否运行 ART 虚拟机
     */
    public static final boolean IS_VM_ART = isVmArt();

    private VmUtils() {
        // prevent instantiation
    }

    /**
     * Java 虚拟机是否支持 TurboDex
     * <p>
     * 前提条件：
     * <ul>
     * <li>ART 虚拟机</li>
     * <li>非阿里云 OS</li>
     * <li>系统小于 9</li>
     * </ul>
     *
     * @return true 若支持 TurboDex
     */
    public static boolean isVmSupportTurboDex() {
        // TurboDex 目前还不支持 Android 9 (API Level 28)
        return IS_VM_ART && !IS_YUN_OS && Build.VERSION.SDK_INT < 28;
    }

    private static boolean isVmArt() {
        boolean isArt = false;
        String versionString = System.getProperty("java.vm.version");
        if (versionString != null) {
            Matcher matcher = Pattern.compile("(\\d+)\\.(\\d+)(\\.\\d+)?").matcher(versionString);
            if (matcher.matches()) {
                try {
                    int major = Integer.parseInt(matcher.group(1));
                    int minor = Integer.parseInt(matcher.group(2));
                    isArt = major > VM_WITH_ART_VERSION_MAJOR
                            || major == VM_WITH_ART_VERSION_MAJOR && minor >= VM_WITH_ART_VERSION_MINOR;
                } catch (NumberFormatException e) {
                    VLog.w(e, "error parse: %s", versionString);
                }
            }
        }

        VLog.i("VM with version: " + versionString + (isArt ? " has ART support"
                : " does not have ART support"));
        return isArt;
    }

    private static boolean isYunOS() {
        String yunOsVersion = null;
        String javaVmName = null;

        try {
            final Method get = ReflectUtils.getMethod(Class.forName("android.os.SystemProperties"), "get",
                    String.class);
            yunOsVersion = (String) get.invoke(null, "ro.yunos.version");
            javaVmName = (String) get.invoke(null, "java.vm.name");
        } catch (Exception e) {
            VLog.w(e, "error call SystemProperties get");
        }

        return javaVmName != null && javaVmName.toLowerCase(Locale.ENGLISH).contains("lemur") || hasText(yunOsVersion);
    }

    private static boolean hasText(CharSequence str) {
        if (!hasLength(str)) {
            return false;
        }
        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLength(CharSequence str) {
        return (str != null && str.length() > 0);
    }
}
