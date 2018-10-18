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

import android.util.Log;

public final class VLog {

    private static String sTag = "VLog";
    private static int sLevel = Log.WARN;

    private VLog() {
        // prevent instantiation
    }

    public static String getTag() {
        return sTag;
    }

    public static void setTag(String tag) {
        sTag = tag;
    }

    public static int getLevel() {
        return sLevel;
    }

    public static void setLevel(int level) {
        sLevel = level;
    }

    public static void v(String format, Object... args) {
        if (Log.VERBOSE >= sLevel) {
            Log.v(sTag, buildMsg(String.format(format, args)));
        }
    }

    public static void verbose(String tag, String format, Object... args) {
        if (Log.VERBOSE >= sLevel) {
            Log.v(tag, buildMsg(String.format(format, args)));
        }
    }

    public static void d(String format, Object... args) {
        if (Log.DEBUG >= sLevel) {
            Log.d(sTag, buildMsg(String.format(format, args)));
        }
    }

    public static void debug(String tag, String format, Object... args) {
        if (Log.DEBUG >= sLevel) {
            Log.d(tag, buildMsg(String.format(format, args)));
        }
    }

    public static void i(String format, Object... args) {
        if (Log.INFO >= sLevel) {
            Log.i(sTag, buildMsg(String.format(format, args)));
        }
    }

    public static void info(String tag, String format, Object... args) {
        if (Log.INFO >= sLevel) {
            Log.i(tag, buildMsg(String.format(format, args)));
        }
    }

    public static void w(String format, Object... args) {
        if (Log.WARN >= sLevel) {
            Log.w(sTag, buildMsg(String.format(format, args)));
        }
    }

    public static void warn(String tag, String format, Object... args) {
        if (Log.WARN >= sLevel) {
            Log.w(tag, buildMsg(String.format(format, args)));
        }
    }

    public static void w(Throwable throwable, String format, Object... args) {
        if (Log.WARN >= sLevel) {
            Log.w(sTag, buildMsg(String.format(format, args)), throwable);
        }
    }

    public static void warn(String tag, Throwable throwable, String format, Object... args) {
        if (Log.WARN >= sLevel) {
            Log.w(tag, buildMsg(String.format(format, args)), throwable);
        }
    }

    public static void e(String format, Object... args) {
        if (Log.ERROR >= sLevel) {
            Log.e(sTag, buildMsg(String.format(format, args)));
        }
    }

    public static void error(String tag, String format, Object... args) {
        if (Log.ERROR >= sLevel) {
            Log.e(tag, buildMsg(String.format(format, args)));
        }
    }

    public static void e(Throwable throwable, String msg, Object... format) {
        if (Log.ERROR >= sLevel) {
            Log.e(sTag, buildMsg(String.format(msg, format)), throwable);
        }
    }

    public static void error(String tag, Throwable throwable, String format, Object... args) {
        if (Log.ERROR >= sLevel) {
            Log.e(tag, buildMsg(String.format(format, args)), throwable);
        }
    }

    public static void e(Throwable e) {
        if (Log.ERROR >= sLevel) {
            Log.e(sTag, buildMsg(getStackTraceString(e)));
        }
    }

    public static String getStackTraceString(Throwable tr) {
        return Log.getStackTraceString(tr);
    }

    public static void printStackTrace(String tag) {
        if (Log.VERBOSE >= sLevel) {
            Log.e(tag, buildMsg(getStackTraceString(new Exception())));
        }
    }

    private static String buildMsg(String msg) {
        final StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[4];

        return new StringBuilder().append("[ (")
                .append(stackTraceElement.getFileName())
                .append(":")
                .append(stackTraceElement.getLineNumber())
                .append(")# ")
                .append(stackTraceElement.getMethodName())
                .append(" -> ")
                .append(Thread.currentThread().getName())
                .append(" ] ")
                .append(msg)
                .toString();
    }
}
