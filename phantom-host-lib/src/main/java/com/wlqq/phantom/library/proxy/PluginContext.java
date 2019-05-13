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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.TimingLogger;
import android.view.WindowManager;

import com.wlqq.phantom.library.env.Constants;
import com.wlqq.phantom.library.log.LogReporter;
import com.wlqq.phantom.library.pm.PluginInfo;
import com.wlqq.phantom.library.utils.VLog;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;


public class PluginContext {
    private final Activity mBaseContext;
    private final PluginInfo mPluginInfo;
    private Class mTargetClass;
    private final ContextProxy<Activity> mContextProxy;

    private static final Field[] ACTIVITY_FIELDS;

    static {
        ACTIVITY_FIELDS = Activity.class.getDeclaredFields();
    }

    public PluginContext(Activity baseContext, @NonNull PluginInfo pluginInfo) {
        this.mBaseContext = baseContext;
        this.mPluginInfo = pluginInfo;
        this.mContextProxy = new ContextProxy<>(pluginInfo, baseContext);
    }

    void setTargetClass(Class cls) {
        this.mTargetClass = cls;
    }

    @SuppressWarnings("unused")
    @Keep
    public Activity getBaseContext() {
        return mBaseContext;
    }

    @SuppressWarnings("unused")
    @Keep
    public PluginInfo getPluginInfo() {
        return this.mPluginInfo;
    }

    @Nullable
    public Context createContext() {

        if (null == mTargetClass) {
            mTargetClass = PluginInterceptActivity.class;
        }

        try {
            TimingLogger logger = new TimingLogger(Constants.TAG, "PluginContext#createContext");

            PluginInterceptActivity pluginActivity = (PluginInterceptActivity) mTargetClass.newInstance();
            pluginActivity.setContextProxy(mContextProxy);
            logger.addSplit("create proxy object");

            attachStatus(pluginActivity);
            logger.addSplit("attachStatus");

            setTheme(pluginActivity);
            logger.addSplit("setTheme");

            logger.dumpToLog();

            return pluginActivity;
        } catch (Exception e) {
            final String msg = "createContext error, targetClass: " + mTargetClass;
            VLog.w(e, msg);
            LogReporter.reportUsableSpaceMegabytes();
            LogReporter.reportException(new PluginContextCreateException(msg, e), null);
        }
        return null;
    }

    /**
     * 将宿主Activity的一些变量值赋值给插件Activity，使插件Activity与宿主占位Activity具有相同的状态
     */
    private void attachStatus(PluginInterceptActivity pluginActivity) throws IllegalAccessException {
        for (Field field : ACTIVITY_FIELDS) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers)
                    || Modifier.isFinal(modifiers)
                    || Modifier.isVolatile(modifiers)
                    || Modifier.isTransient(modifiers)) {
                continue;
            }

            field.setAccessible(true);
            Object fieldsValue = field.get(mBaseContext);
            field.set(pluginActivity, fieldsValue);
        }

        pluginActivity.attachBaseContext(mBaseContext);

        //设置输入法模式
        if (!mTargetClass.getName().equals(PluginInterceptActivity.class.getName())) {
            ActivityInfo info = mPluginInfo.getActivityInfo(
                    new ComponentName(mPluginInfo.packageName, mTargetClass.getName()));
            if (null != info && info.softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED) {
                mBaseContext.getWindow().setSoftInputMode(info.softInputMode);
            }
        }
    }

    private int getThemeId(PluginInterceptActivity pluginActivity) {
        // Activity 主题选择顺序
        // 1. 先检查插件 Activity 是否有设置 Theme
        // 2. 然后检查插件 Application 是否有设置 Theme
        // 3. 若都没有设置，默认设置为 android:style/Theme.Light.NoTitleBar
        int theme = 0;
        final ActivityInfo activityInfo = mPluginInfo.getActivityInfo(
                new ComponentName(mPluginInfo.packageName, pluginActivity.getClass().getName()));
        if (null != activityInfo) {
            theme = activityInfo.theme;
        }
        if (theme != 0) {
            return theme;
        }

        final ApplicationInfo applicationInfo = mPluginInfo.getApplicationInfo();
        if (applicationInfo != null) {
            theme = applicationInfo.theme;
        }

        if (0 == theme) {
            theme = android.R.style.Theme_Holo_Light_NoActionBar;
        }

        return theme;
    }

    private void setTheme(PluginInterceptActivity obj) throws Exception {
        int theme = getThemeId(obj);
        obj.setTheme(theme);
    }
}
