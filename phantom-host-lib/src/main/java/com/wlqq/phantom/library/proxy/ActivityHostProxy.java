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
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentController;
import android.support.v4.app.FragmentHostCallback;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import com.wlqq.phantom.library.PhantomCore;
import com.wlqq.phantom.library.env.Constants;
import com.wlqq.phantom.library.log.LogReporter;
import com.wlqq.phantom.library.pm.PluginInfo;
import com.wlqq.phantom.library.pool.LaunchModeManager;
import com.wlqq.phantom.library.utils.ClassUtils;
import com.wlqq.phantom.library.utils.IntentUtils;
import com.wlqq.phantom.library.utils.ReflectUtils;
import com.wlqq.phantom.library.utils.TimingUtils;
import com.wlqq.phantom.library.utils.VLog;
import dalvik.system.DexClassLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

/**
 * 插件 {@link Activity} 在宿主中的坑位 {@link Activity}
 */
public class ActivityHostProxy extends FragmentActivity implements Cloneable {
    private static final Method ON_CREATE;
    private static final Method ON_POST_CREATE;
    private static final Method ON_START;
    private static final Method ON_RESUME;
    private static final Method ON_POST_RESUME;
    private static final Method ON_PAUSE;
    private static final Method ON_STOP;
    private static final Method ON_DESTROY;
    private static final Method ON_SAVE_INSTANCE_STATE;
    private static final Method ON_RESTORE_INSTANCE_STATE;
    private static final Method ON_ATTACHED_TO_WINDOW;
    private static final Method ON_DETACHED_FROM_WINDOW;
    private static final Method ON_KEY_DOWN;
    private static final Method ON_KEY_UP;
    private static final Method ON_BACK_PRESSED;
    private static final Method ON_ACTIVITY_RESULT;
    private static final Method ON_NEW_INTENT;
    private static final Method ON_REQUEST_PERMISSIONS_RESULT;

    private static final Field M_FRAGMENTS;
    private static final Field M_HOST;
    private static final Field M_CONTEXT;
    private static final Field M_ACTIVITY;

    static {
        final Class<Activity> activityClass = Activity.class;
        final Class<Bundle> bundleClass = Bundle.class;
        final Class<KeyEvent> keyEventClass = KeyEvent.class;
        final Class<Intent> intentClass = Intent.class;
        final Class<Integer> integerClass = int.class;
        final Class<FragmentHostCallback> fragmentHostCallbackClass = FragmentHostCallback.class;

        ON_CREATE = ReflectUtils.getMethod(activityClass, "onCreate", bundleClass);
        ON_POST_CREATE = ReflectUtils.getMethod(activityClass, "onPostCreate", bundleClass);
        ON_START = ReflectUtils.getMethod(activityClass, "onStart");
        ON_RESUME = ReflectUtils.getMethod(activityClass, "onResume");
        ON_POST_RESUME = ReflectUtils.getMethod(activityClass, "onPostResume");
        ON_PAUSE = ReflectUtils.getMethod(activityClass, "onPause");
        ON_STOP = ReflectUtils.getMethod(activityClass, "onStop");
        ON_DESTROY = ReflectUtils.getMethod(activityClass, "onDestroy");
        ON_SAVE_INSTANCE_STATE = ReflectUtils.getMethod(activityClass, "onSaveInstanceState", bundleClass);
        ON_RESTORE_INSTANCE_STATE = ReflectUtils.getMethod(activityClass, "onRestoreInstanceState", bundleClass);
        ON_ATTACHED_TO_WINDOW = ReflectUtils.getMethod(activityClass, "onAttachedToWindow");
        ON_DETACHED_FROM_WINDOW = ReflectUtils.getMethod(activityClass, "onDetachedFromWindow");
        ON_KEY_DOWN = ReflectUtils.getMethod(activityClass, "onKeyDown", integerClass, keyEventClass);
        ON_KEY_UP = ReflectUtils.getMethod(activityClass, "onKeyUp", integerClass, keyEventClass);
        ON_BACK_PRESSED = ReflectUtils.getMethod(activityClass, "onBackPressed");
        ON_ACTIVITY_RESULT = ReflectUtils.getMethod(activityClass, "onActivityResult",
                integerClass, integerClass, intentClass);
        ON_NEW_INTENT = ReflectUtils.getMethod(activityClass, "onNewIntent", intentClass);
        ON_REQUEST_PERMISSIONS_RESULT = ReflectUtils.getMethod(activityClass, "onRequestPermissionsResult",
                int.class, String[].class, int[].class);

        M_FRAGMENTS = ReflectUtils.getField(FragmentActivity.class, "mFragments");
        M_HOST = ReflectUtils.getField(FragmentController.class, "mHost");
        M_ACTIVITY = ReflectUtils.getField(fragmentHostCallbackClass, "mActivity");
        M_CONTEXT = ReflectUtils.getField(fragmentHostCallbackClass, "mContext");
    }

    // 含有fragment的标记
    static final String FRAGMENTS_TAG = "android:support:fragments";
    static final String NEXT_CANDIDATE_REQUEST_INDEX_TAG = "android:support:next_request_index";
    static final String ALLOCATED_REQUEST_INDICIES_TAG = "android:support:request_indicies";
    static final String REQUEST_FRAGMENT_WHO_TAG = "android:support:request_fragment_who";

    private PluginInterceptActivity mClientActivity;
    private DexClassLoader mClassLoader;
    private PluginInfo mPluginInfo;

    private String mTargetComponentStr;
    private String mTargetPackageName;
    private String mTargetClassName;

    // extras key-value items to report
    private final HashMap<String, Object> mExtMsg = new HashMap<>();

    private boolean mCanCallBackPressed;

    private boolean mIsOnCreateSuccess;

    private ComponentName mPluginComponentName;

    private boolean mUseHostTheme;

    private Resources.Theme mHostTheme;
    private int mHostThemeId = -1;
    private Intent mNewIntent;
    private boolean mHasSuperCalled = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        initPluginBundle();

        if (mPluginInfo == null) {
            super.onCreate(savedInstanceState);
            mHasSuperCalled = true;
            String msg = String.format("bundle for activity %s is null", mPluginComponentName);
            HashMap<String, Object> params = new HashMap<>(1);
            params.put(LogReporter.Key.MESSAGE, msg);

            LogReporter.reportState(LogReporter.EventId.PLUGIN_ACTIVITY_ON_CREATE, false, params);
            LogReporter.reportException(new ActivityOnCreateException(msg));

            finish();
            return;
        }
        VLog.i("onCreate, originIntent.getComponent: %s", mPluginComponentName);
        mTargetComponentStr = mPluginComponentName.flattenToString();
        mTargetPackageName = mPluginComponentName.getPackageName();
        mTargetClassName = mPluginComponentName.getClassName();
        final String targetActivity = mTargetClassName;
        try {
            TimingUtils.startTime(targetActivity);

            Class targetCls = loadTargetActivity(mPluginComponentName);

            mExtMsg.put(LogReporter.Key.TARGET_ACTIVITY, targetActivity);
            mExtMsg.put(LogReporter.Key.PACKAGE_NAME, mPluginInfo.packageName);
            mExtMsg.put(LogReporter.Key.VERSION_NAME, mPluginInfo.versionName);

            //proxy target activity
            PluginContext pluginContext = new PluginContext(this, mPluginInfo);
            pluginContext.setTargetClass(targetCls);
            mClientActivity = (PluginInterceptActivity) pluginContext.createContext();
            if (mClientActivity == null) {
                throw new Exception("create mClientActivity return null");
            }
            replaceSupportFragmentContext();

            //对插件Activity调用setTheme()函数设置主题的支持,setTheme()需要在super.onCreate()之前调用
            //当插件Activity调用super.onCreate()的时候才调用这里的super.onCreate()
            mClientActivity.setOnCreateCallback(new OnCreateCallback() {
                @Override
                public void onCreateCalled() {
                    requestWindowFeature(Window.FEATURE_NO_TITLE);
                    ActivityHostProxy.super.onCreate(savedInstanceState);
                    mHasSuperCalled = true;
                }
            });

            final ActivityInfo activityInfo = PhantomCore.getInstance().findActivityInfo(mPluginComponentName);
            if (activityInfo != null) {
                setRequestedOrientation(activityInfo.screenOrientation);
            }

            Bundle savedData = getPluginSavedData(savedInstanceState);

            if (ON_CREATE != null) {
                ON_CREATE.invoke(mClientActivity, savedData);
            } else {
                // should not happen
                throw new IllegalStateException("onCreate method not found");
            }

            trackActivityLoadTime(targetActivity,
                    TimingUtils.getNormalizedDuration(targetActivity, TimingUtils.SECTION_DURATION_50_MS,
                            TimingUtils.MAX_SECTION_20));

            LogReporter.reportState(LogReporter.EventId.PLUGIN_ACTIVITY_ON_CREATE, true, mPluginInfo.packageName,
                    mExtMsg);

            logActivityEvent("onCreate", true);

            mIsOnCreateSuccess = true;
        } catch (Throwable e) {
            if (!mHasSuperCalled) {
                super.onCreate(savedInstanceState);
            }

            logActivityEvent("onCreate", false);

            mCanCallBackPressed = true;

            final String msg = "ActivityHostProxy onCreate error: " + targetActivity;
            VLog.w(e, msg);

            mExtMsg.put(LogReporter.Key.MESSAGE, e.toString());
            if (null == mPluginInfo) {
                LogReporter.reportState(LogReporter.EventId.PLUGIN_ACTIVITY_ON_CREATE, false, mExtMsg);
            } else {
                LogReporter.reportState(LogReporter.EventId.PLUGIN_ACTIVITY_ON_CREATE, false, mPluginInfo.packageName,
                        mExtMsg);
            }

            LogReporter.reportException(new ActivityOnCreateException(msg, e));

            finish();
        }
    }

    private void trackActivityLoadTime(String targetActivity, String time) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(LogReporter.Key.TIME, time);
        params.putAll(mExtMsg);
        LogReporter.reportEvent(LogReporter.EventId.PLUGIN_ACTIVITY_LOAD, targetActivity, params);
    }


    // 替换support-v4 25.3.1版本Fragment的activity.由于Fragment的getActivity()方法是final的，不能被复写，
    // 只能用反射进行替换
    private void replaceSupportFragmentContext() throws ReplaceSupportFragmentContextException {
        try {
            // class FragmentActivity
            // final FragmentController mFragments;
            Object mFragments = ReflectUtils.readField(M_FRAGMENTS, this);
            // class FragmentController
            // private final FragmentHostCallback<?> mHost;
            Object mHost = ReflectUtils.readField(M_HOST, mFragments);
            // class FragmentHostCallback
            // private final Activity mActivity;
            // final Context mContext;
            ReflectUtils.writeField(M_ACTIVITY, mHost, mClientActivity);
            ReflectUtils.writeField(M_CONTEXT, mHost, mClientActivity);
        } catch (Exception e) {
            throw new ReplaceSupportFragmentContextException(e);
        }
    }

    /**
     * 主要用于微信回调，微信回调的 Activity 在宿主中。实现微信回调时，复写该方法，
     * 指定需要启动的插件 Activity
     *
     * @return 原始 {@link Intent}，非坑位 {@link Intent}
     */
    public Intent getOriginIntent() {
        return null;
    }

    @Override
    public AssetManager getAssets() {
        initPluginBundle();
        if (mUseHostTheme) {
            return super.getAssets();
        }
        return null == mPluginInfo ? super.getAssets() : mPluginInfo.getPluginAssetManager();
    }

    @Override
    public Resources getResources() {
        initPluginBundle();
        if (mUseHostTheme) {
            return super.getResources();
        }
        if (mPluginInfo == null || !mPluginInfo.isStarted()) {
            // initBundle 之后 bundle不一定是started
            // bug detail: https://bugly.qq.com/v2/crash-reporting/crashes/4ff3d1676d/73617?pid=1
            return super.getResources();
        } else {
            return mPluginInfo.getPluginResources();
        }
    }

    public PluginInterceptActivity getClientActivity() {
        return mClientActivity;
    }

    public void useHostTheme() {
        this.mUseHostTheme = true;
    }

    /**
     * 插件中有时可能希望得到宿主的占位Activity，传递给其他组件使用，而其他组件希望使用这个Activity实例来访问宿主的资源。
     * 例如：在插件中弹出通知，通知的图标希望使用宿主的图标，这时就需要访问宿主资源。
     *
     * @return 占位Activity实例
     */
    @Keep
    public Activity getShadow() {
        try {
            ActivityHostProxy shadow = (ActivityHostProxy) this.clone();
            shadow.useHostTheme();
            return shadow;
        } catch (CloneNotSupportedException e) {
            VLog.w(e, "clone ActivityHostProxy exception");

            String msg = String.format("proxy %s for plugin activity %s cannot clone",
                    this.getClass().getSimpleName(), mPluginComponentName);
            HashMap<String, Object> params = new HashMap<>(1);
            params.put(LogReporter.Key.MESSAGE, msg);
            LogReporter.reportState(LogReporter.EventId.PROXY_ACTIVITY_CANNOT_CLONE, false, params);
        }

        return null;
    }

    @Override
    public void setTheme(int themeId) {
        super.setTheme(themeId);
        if (mHostThemeId == -1) {
            mHostThemeId = themeId;
        }
    }

    @Override
    public Object getSystemService(@NonNull String name) {
        Object service = super.getSystemService(name);
        if (name.equals(Context.LAYOUT_INFLATER_SERVICE) && mUseHostTheme) {
            LayoutInflater inflater = (LayoutInflater) service;
            //当通过插件context获取宿主context的时候，layoutInflater的context需要设置mUseHostTheme标志
            ((ActivityHostProxy) inflater.getContext()).useHostTheme();
            return inflater;
        }

        return service;
    }

    private void getPluginComponentName() {
        if (null == getIntent()) {
            return;
        }
        Intent originIntent = getIntent().getParcelableExtra(Constants.ORIGIN_INTENT);
        if (null == originIntent) {
            originIntent = getOriginIntent();
        }

        if (null == originIntent) {
            final String msg = "onCreate, originIntent is null";
            VLog.w(msg);
            LogReporter.reportException(new ActivityOnCreateException(msg), null);

            HashMap<String, Object> params = new HashMap<>(1);
            params.put(LogReporter.Key.MESSAGE, msg);
            LogReporter.reportState(LogReporter.EventId.PLUGIN_ACTIVITY_ON_CREATE, false, params);

            finish();
            return;
        }

        mPluginComponentName = originIntent.getComponent();
        if (mPluginComponentName == null) {
            final String msg = "onCreate, originIntent.getComponent is null";
            VLog.w(msg);
            LogReporter.reportException(new ActivityOnCreateException(msg), null);

            HashMap<String, Object> params = new HashMap<>(1);
            params.put(LogReporter.Key.MESSAGE, msg);
            LogReporter.reportState(LogReporter.EventId.PLUGIN_ACTIVITY_ON_CREATE, false, params);

            finish();
        }
    }

    private void initPluginBundle() {
        if (null != mPluginInfo && mPluginInfo.isStarted()) {
            // start过后的pluginBundle才return
            return;
        }
        if (mPluginInfo != null) {
            mPluginInfo.start();
            return;
        }

        if (null == mPluginComponentName) {
            getPluginComponentName();
        }

        if (null != mPluginComponentName) {
            mPluginInfo = PhantomCore.getInstance().findPluginInfoByActivityName(mPluginComponentName);

            //分享到微信，从微信界面返回后，插件可能停止了
            if (mPluginInfo == null) {
                final String msg = String.format(Locale.ENGLISH, "initPluginBundle findPluginByActivityName fail: %s",
                        mPluginComponentName.toShortString());
                VLog.w(msg);
                LogReporter.reportException(new FindPluginInfoException(msg));
                return;
            }

            if (!mPluginInfo.isStarted()) {
                mPluginInfo.start();
            }
        }
    }

    private Class<?> loadTargetActivity(@NonNull ComponentName componentName) throws Exception {
        if (!mPluginInfo.isStarted()) {
            boolean started = mPluginInfo.start();
            if (!started) {
                throw new Exception(
                        String.format("start bundle %s from ActivityHostProxy fault", mPluginInfo.packageName));
            }
        }


        mClassLoader = mPluginInfo.getPluginClassLoader();
        if (mClassLoader == null) {
            throw new Exception(String.format("classloader for bundle %s is null", mPluginInfo.packageName));
        }

        return mClassLoader.loadClass(componentName.getClassName());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        callTargetActivityMethod(ON_POST_CREATE, "onPostCreate", savedInstanceState);
    }


    @SuppressWarnings("all")
    @Override
    protected void onStart() {
        callTargetActivityMethod(ON_START, "onStart");
    }

    public void callSuperOnStart() {
        super.onStart();
    }

    @SuppressWarnings("all")
    @Override
    protected void onResume() {
        callTargetActivityMethod(ON_RESUME, "onResume");
    }

    public void callSuperOnResume() {
        super.onResume();
    }


    @Override
    protected void onPostResume() {
        super.onPostResume();
        callTargetActivityMethod(ON_POST_RESUME, "onPostResume");
    }

    @SuppressWarnings("all")
    @Override
    protected void onPause() {
        callTargetActivityMethod(ON_PAUSE, "onPause");
    }

    public void callSuperOnPause() {
        super.onPause();
    }

    @SuppressWarnings("all")
    @Override
    protected void onStop() {
        callTargetActivityMethod(ON_STOP, "onStop");
    }

    public void callSuperOnStop() {
        super.onStop();
    }

    @SuppressWarnings("all")
    @Override
    protected void onDestroy() {
        if (mIsOnCreateSuccess) {
            callTargetActivityMethod(ON_DESTROY, "onDestroy");
        } else {
            callSuperOnDestroy();
        }

        if (this.isFinishing()) {
            LaunchModeManager.getInstance().unrefActivity(this.getClass().getName(), mTargetComponentStr);
        }
    }

    public void callSuperOnDestroy() {
        super.onDestroy();
    }

    @Override
    public void startActivityFromFragment(@NonNull android.app.Fragment fragment, Intent intent, int requestCode) {
        super.startActivityFromFragment(fragment, setPluginFlag(intent), requestCode);
    }

    @Override
    public void startActivityFromFragment(@NonNull android.app.Fragment fragment, Intent intent, int requestCode,
            @Nullable Bundle options) {
        super.startActivityFromFragment(fragment, setPluginFlag(intent), requestCode, options);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(setPluginFlag(intent));
    }

    @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {
        super.startActivity(setPluginFlag(intent), options);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(setPluginFlag(intent), requestCode);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startActivityForResult(Intent intent, int requestCode, @Nullable Bundle options) {
        super.startActivityForResult(setPluginFlag(intent), requestCode, options);
    }

    @Override
    public void startActivities(Intent[] intents, @Nullable Bundle options) {
        super.startActivities(setPluginFlag(intents), options);
    }

    @Override
    public void startActivities(Intent[] intents) {
        super.startActivities(setPluginFlag(intents));
    }

    @Override
    public void startActivityFromFragment(Fragment fragment, Intent intent, int requestCode) {
        super.startActivityFromFragment(fragment, setPluginFlag(intent), requestCode);
    }

    @Override
    public void startActivityFromFragment(Fragment fragment, Intent intent, int requestCode, @Nullable Bundle options) {
        super.startActivityFromFragment(fragment, setPluginFlag(intent), requestCode, options);
    }

    @Override
    public void startActivityFromChild(@NonNull Activity child, @RequiresPermission Intent intent, int requestCode) {
        super.startActivityFromChild(child, setPluginFlag(intent), requestCode);
    }

    @Override
    public void startActivityFromChild(@NonNull Activity child, @RequiresPermission Intent intent, int requestCode,
            @Nullable Bundle options) {
        super.startActivityFromChild(child, setPluginFlag(intent), requestCode, options);
    }

    @Override
    public boolean startActivityIfNeeded(@RequiresPermission @NonNull Intent intent, int requestCode) {
        return super.startActivityIfNeeded(setPluginFlag(intent), requestCode);
    }

    @Override
    public boolean startActivityIfNeeded(@RequiresPermission @NonNull Intent intent, int requestCode,
            @Nullable Bundle options) {
        return super.startActivityIfNeeded(setPluginFlag(intent), requestCode, options);
    }

    @Override
    public ComponentName startService(Intent service) {
        return super.startService(setPluginServiceFlag(service));
    }

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(setPluginServiceFlag(name));
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        return super.bindService(setPluginServiceFlag(service), conn, flags);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle saveData = new Bundle();
        callTargetActivityMethod(ON_SAVE_INSTANCE_STATE, "onSaveInstanceState", saveData);
        outState.putBundle(Constants.PLUGIN_SAVED_DATA, saveData);
        removeFragmentState(outState);
    }

    /**
     * 清除Fragment状态相关的key
     *
     * @param outState
     */
    private void removeFragmentState(Bundle outState) {
        outState.remove(FRAGMENTS_TAG);
        outState.remove(NEXT_CANDIDATE_REQUEST_INDEX_TAG);
        outState.remove(ALLOCATED_REQUEST_INDICIES_TAG);
        outState.remove(REQUEST_FRAGMENT_WHO_TAG);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        savedInstanceState.setClassLoader(mPluginInfo.getPluginClassLoader());
        Set<String> keys = savedInstanceState.keySet();
        for (String key : keys) {
            Bundle internalBundle = savedInstanceState.getBundle(key);
            if (null != internalBundle) {
                internalBundle.setClassLoader(mPluginInfo.getPluginClassLoader());
            }
        }

        try {
            super.onRestoreInstanceState(savedInstanceState);
        } catch (Exception e) {
            VLog.w(e, "super.onRestoreInstanceState() error");
            LogReporter.reportException(e);
        }

        Bundle savedData = getPluginSavedData(savedInstanceState);
        callTargetActivityMethod(ON_RESTORE_INSTANCE_STATE, "onRestoreInstanceState", savedData);
    }

    @Nullable
    private Bundle getPluginSavedData(Bundle savedInstanceState) {
        Bundle savedData = null;
        if (null != savedInstanceState) {
            savedData = savedInstanceState.getBundle(Constants.PLUGIN_SAVED_DATA);
            if (null != savedData) {
                savedData.setClassLoader(mClassLoader);
            }
        }

        return savedData;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        callTargetActivityMethod(ON_ATTACHED_TO_WINDOW, "onAttachedToWindow");
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        callTargetActivityMethod(ON_DETACHED_FROM_WINDOW, "onDetachedFromWindow");
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return (boolean) callTargetActivityMethod(ON_KEY_DOWN, "onKeyDown", keyCode, event);
    }

    public boolean callSuperOnKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && checkFragmentManagerState()) {
            return true;
        }
        boolean res = (boolean) callTargetActivityMethod(ON_KEY_UP, "onKeyUp", keyCode,
                event);
        if (mCanCallBackPressed) {
            return super.onKeyUp(keyCode, event);
        }
        return res;
    }

    public boolean callSuperOnKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    public void onBackPressed() {
        if (mCanCallBackPressed) {
            super.onBackPressed();
        }

        callTargetActivityMethod(ON_BACK_PRESSED, "onBackPressed");
    }


    public void callSuperOnBackPressed() {
        super.onBackPressed();
    }

    /**
     * keyUp事件时检查FragmentManager状态，如果backStack栈存在fragment则弹出fragment，
     * 在某些比较慢的手机上可能出现FragmentManager状态为保存，Activity确响应返回键的情况。
     *
     * @return true表示keyUp事件被消费，事件不能继续传递下去。false表示keyUp事件可以继续传递
     */
    private boolean checkFragmentManagerState() {
        try {
            return getFragmentManager().popBackStackImmediate();
        } catch (IllegalStateException e) {
            VLog.w(e, "FragmentManager#popBackStackImmediate exception");
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (null != data) {
            data.setExtrasClassLoader(mClassLoader);
        }
        super.onActivityResult(requestCode, resultCode, data);
        callTargetActivityMethod(ON_ACTIVITY_RESULT, "onActivityResult", requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        callTargetActivityMethod(ON_REQUEST_PERMISSIONS_RESULT, "onRequestPermissionsResult",
                requestCode, permissions, grantResults);
    }

    private Object callTargetActivityMethod(Method method, String methodName, Object... args) {
        VLog.d("callTargetActivityMethod: %s, args: %s", methodName, Arrays.toString(args));

        try {
            if (mClientActivity == null) {
                throw new IllegalStateException("target activity is null");
            }

            if (method == null) {
                throw new IllegalArgumentException("method is null: " + methodName);
            }

            Object result = method.invoke(mClientActivity, args);

            logActivityEvent(methodName, true);

            return result;
        } catch (Throwable e) {
            logActivityEvent(methodName, false);

            if ("onKeyUp".equals(methodName)) {
                mCanCallBackPressed = true;
            }
            VLog.w(e, "callTargetActivityMethod: %s error", methodName);

            mExtMsg.put(LogReporter.Key.MESSAGE, e.toString());
            LogReporter.reportException(e, mExtMsg);
            LogReporter.reportState(LogReporter.EventId.PLUGIN_ACTIVITY, false, mExtMsg);
            return false;
        }
    }

    /**
     * 获取系统调用 {@linkplain #onNewIntent(Intent)} 传递过来的原始 {@linkplain Intent}
     *
     * @return 原始 {@linkplain Intent}
     */
    @Keep
    @Nullable
    public Intent getNewIntent() {
        return mNewIntent;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //记录intent，如果IntentUtils.mergeIntentExtras出现问题，插件可以取得mNewIntent来临时解决问题
        mNewIntent = new Intent(intent);
        LaunchModeManager.getInstance().unrefActivity(this.getClass().getName(), mTargetComponentStr);
        if (null != mClientActivity) {
            IntentUtils.mergeIntentExtras(intent, mClientActivity.getClassLoader());
        }
        callTargetActivityMethod(ON_NEW_INTENT, "onNewIntent", intent);
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        if (mUseHostTheme) {
            return super.onCreateView(parent, name, context, attrs);
        }
        // 部分手机在插件Activity转场动画时mClientActivity还未实例化导致空指针
        // crash stacktrace: https://bugly.qq.com/v2/crash-reporting/crashes/4ff3d1676d/65600?pid=1
        if (mClientActivity != null) {
            return mClientActivity.onCreateView(parent, name, context, attrs);
        }
        return null;
    }

    @NonNull
    @Override
    public LayoutInflater getLayoutInflater() {
        if (mUseHostTheme) {
            return super.getLayoutInflater();
        }
        return null == mClientActivity ? super.getLayoutInflater() : mClientActivity.getLayoutInflater();
    }

    @Override
    public Resources.Theme getTheme() {
        if (mUseHostTheme) {
            initHostTheme();
            return mHostTheme;
        }
        return null == mClientActivity ? super.getTheme() : mClientActivity.getTheme();
    }

    private void initHostTheme() {
        if (null == mHostTheme && mHostThemeId != -1) {
            mHostTheme = getApplication().getTheme();
            onApplyThemeResource(mHostTheme, mHostThemeId, true);
        }
    }


    private Intent setPluginFlag(Intent intent) {
        if (mUseHostTheme || intent.hasExtra(Constants.ORIGIN_INTENT)) {
            return intent;
        }
        return mClientActivity.getContextProxy().setActivityIntentExtra(intent);
    }

    @SuppressWarnings("PMD.UseVarargs")
    private Intent[] setPluginFlag(Intent[] intents) {
        if (mUseHostTheme || intents[0].hasExtra(Constants.ORIGIN_INTENT)) {
            return intents;
        }

        return mClientActivity.getContextProxy().setIntentExtra(intents);
    }


    private Intent setPluginServiceFlag(Intent intent) {
        if (mUseHostTheme || intent.hasExtra(Constants.ORIGIN_INTENT)) {
            return intent;
        }
        return mClientActivity.getContextProxy().setServiceIntentExtra(intent);
    }

    private void logActivityEvent(String eventName, boolean success) {
        if (mTargetPackageName != null && mTargetClassName != null && mPluginInfo != null) {
            LogReporter.reportLog(mTargetPackageName + "_" + mPluginInfo.versionName
                    + "/" + ClassUtils.getSimpleName(mTargetClassName)
                    + " " + eventName + (success ? " success" : " fail"));
        }
    }

    // 透明主题的占位  Activity 1 个, launch mode: standard
    public static class ActivityProxyTranslucent extends ActivityHostProxy {
    }

    //single top 占位activity 50个
    public static class ActivityProxySingleTop0 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop1 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop2 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop3 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop4 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop5 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop6 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop7 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop8 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop9 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop10 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop11 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop12 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop13 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop14 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop15 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop16 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop17 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop18 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop19 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop20 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop21 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop22 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop23 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop24 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop25 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop26 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop27 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop28 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop29 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop30 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop31 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop32 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop33 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop34 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop35 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop36 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop37 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop38 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop39 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop40 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop41 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop42 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop43 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop44 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop45 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop46 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop47 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop48 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTop49 extends ActivityHostProxy {
    }

    //single task 占位activity 50个
    public static class ActivityProxySingleTask0 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask1 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask2 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask3 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask4 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask5 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask6 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask7 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask8 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask9 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask10 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask11 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask12 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask13 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask14 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask15 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask16 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask17 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask18 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask19 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask20 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask21 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask22 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask23 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask24 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask25 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask26 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask27 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask28 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask29 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask30 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask31 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask32 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask33 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask34 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask35 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask36 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask37 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask38 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask39 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask40 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask41 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask42 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask43 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask44 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask45 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask46 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask47 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask48 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleTask49 extends ActivityHostProxy {
    }

    //single instance 占位activity 30个
    public static class ActivityProxySingleInstance0 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance1 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance2 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance3 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance4 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance5 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance6 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance7 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance8 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance9 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance10 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance11 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance12 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance13 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance14 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance15 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance16 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance17 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance18 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance19 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance20 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance21 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance22 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance23 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance24 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance25 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance26 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance27 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance28 extends ActivityHostProxy {
    }

    public static class ActivityProxySingleInstance29 extends ActivityHostProxy {
    }
}
