package com.wlqq.phantom.gradle.plugin.utils;

public class Log {
    private Log() {

    }

    public static int i(String tag, String msg) {
        System.out.println("[INFO][" + tag + "] " + msg);
        return 0;
    }

    public static int e(String tag, String msg) {
        System.err.println("[ERROR][" + tag + "] " + msg);
        return 0;
    }
}
