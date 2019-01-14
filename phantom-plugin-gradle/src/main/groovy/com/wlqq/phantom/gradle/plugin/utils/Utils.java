package com.wlqq.phantom.gradle.plugin.utils;

import org.joor.Reflect;
import org.joor.ReflectException;

import java.util.Locale;

public class Utils {
    // <= 3.0
    private static final String VERSION_3_ZERO_FIELD = "com.android.builder.Version";
    // > 3.1
    private static final String VERSION_3_ONE_FIELD = "com.android.builder.model.Version";
    private static final String AGP_VERSION_FIELD = "ANDROID_GRADLE_PLUGIN_VERSION";

    private Utils() {
        // prevent instantiation
    }

    public static String getAgpVersion() {

        String gradlePluginVersion = "";
        try {
            gradlePluginVersion = Reflect.on(VERSION_3_ZERO_FIELD).get(AGP_VERSION_FIELD);
        } catch (ReflectException e) {
        }

        try {
            gradlePluginVersion = Reflect.on(VERSION_3_ONE_FIELD).get(AGP_VERSION_FIELD);
        } catch (ReflectException e) {
        }

        return gradlePluginVersion;
    }

    public static void log(String fmt, Object... args) {
        System.out.println(String.format(Locale.US, fmt, args));
    }
}
