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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wlqq.phantom.library.utils.IntentUtils;
import com.wlqq.phantom.library.utils.SuppressFBWarnings;
import com.wlqq.phantom.library.utils.VLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * 实现对插件Activity方法的拦截
 */
public class PluginInterceptActivity extends FragmentActivity {
    private ContextProxy<Activity> mContentProxy;
    private OnCreateCallback mOnCreateCallback;
    private boolean mUseCompatTheme;
    private LayoutInflater mLayoutInflater;
    private int mThemeId;
    private Resources.Theme mTheme;
    private Intent mIntent;

    public void setContextProxy(ContextProxy<Activity> contextProxy) {
        mContentProxy = contextProxy;
    }

    public ContextProxy getContextProxy() {
        return mContentProxy;
    }

    //由于setTheme的调用需要在super.onCreate()调用之前才生效，这里增加一个回调在插件Activity调用
    //super.onCreate()时通知ActivityHostProxy，ActivityHostProxy在回调中调用自己的super.onCreate()
    public void setOnCreateCallback(OnCreateCallback callback) {
        mOnCreateCallback = callback;
    }

    /**
     * 与宿主占位Activity共享window，decorview，FragmentManager(系统的和support-v4包中的)后，不再需要调用其super方法，
     * 以及相关的生命周期回调方法的super方法
     */
    @SuppressWarnings("all")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        //共享window，FragmentManager后不需要调用super方法，否则会引起一些错误
        mOnCreateCallback.onCreateCalled();
    }

    @SuppressWarnings("all")
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        //共享window，FragmentManager后不需要调用super方法，否则会引起一些错误
    }

    @SuppressWarnings("all")
    @Override
    protected void onStart() {
        //共享window，FragmentManager后不需要调用super方法，否则会引起一些错误
        if (mContentProxy.getContext() instanceof ActivityHostProxy) {
            ((ActivityHostProxy) mContentProxy.getContext()).callSuperOnStart();
        }
    }

    @SuppressWarnings("all")
    @Override
    protected void onResume() {
        //共享window，FragmentManager后不需要调用super方法，否则会引起一些错误
        if (mContentProxy.getContext() instanceof ActivityHostProxy) {
            ((ActivityHostProxy) mContentProxy.getContext()).callSuperOnResume();
        }
    }

    @SuppressWarnings("all")
    @Override
    protected void onPostResume() {
        //共享window，FragmentManager后不需要调用super方法，否则会引起一些错误
    }

    @SuppressWarnings("all")
    @Override
    protected void onPause() {
        //共享window，FragmentManager后不需要调用super方法，否则会引起一些错误
        if (mContentProxy.getContext() instanceof ActivityHostProxy) {
            ((ActivityHostProxy) mContentProxy.getContext()).callSuperOnPause();
        }
    }

    @SuppressWarnings("all")
    @Override
    protected void onStop() {
        //共享window，FragmentManager后不需要调用super方法，否则会引起一些错误
        if (mContentProxy.getContext() instanceof ActivityHostProxy) {
            ((ActivityHostProxy) mContentProxy.getContext()).callSuperOnStop();
        }
    }

    @SuppressWarnings("all")
    @Override
    protected void onDestroy() {
        //共享window，FragmentManager后不需要调用super方法，否则会引起一些错误
        if (mContentProxy.getContext() instanceof ActivityHostProxy) {
            ((ActivityHostProxy) mContentProxy.getContext()).callSuperOnDestroy();
        }
    }

    @Override
    public AssetManager getAssets() {
        return mContentProxy.getAssets();
    }

    @Override
    public Resources getResources() {
        return mContentProxy.getResources();
    }

    @Override
    public Context getApplicationContext() {
        return mContentProxy.getApplicationContext();
    }

    @Override
    public void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }

    @Override
    public ClassLoader getClassLoader() {
        return mContentProxy.getClassLoader();
    }

    @Override
    public void startActivity(Intent intent) {
        mContentProxy.startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {
        mContentProxy.startActivity(intent, options);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startActivities(Intent[] intents, @Nullable Bundle options) {
        mContentProxy.startActivities(intents, options);
    }

    @Override
    public void startActivities(Intent[] intents) {
        mContentProxy.startActivities(intents);
    }

    @Override
    public void startActivityFromFragment(Fragment fragment, Intent intent, int requestCode) {
        if (mContentProxy.getContext() instanceof FragmentActivity) {
            ((FragmentActivity) mContentProxy.getContext()).startActivityFromFragment(fragment,
                    mContentProxy.setActivityIntentExtra(intent), requestCode);
        }
    }

    @Override
    public void startActivityFromFragment(Fragment fragment, Intent intent, int requestCode, @Nullable Bundle options) {
        if (mContentProxy.getContext() instanceof FragmentActivity) {
            ((FragmentActivity) mContentProxy.getContext())
                    .startActivityFromFragment(fragment,
                            mContentProxy.setActivityIntentExtra(intent), requestCode, options);
        }
    }

    @Override
    public void startActivityFromFragment(@NonNull android.app.Fragment fragment, Intent intent, int requestCode) {
        mContentProxy.getContext().startActivityFromFragment(fragment,
                mContentProxy.setActivityIntentExtra(intent), requestCode);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startActivityFromFragment(@NonNull android.app.Fragment fragment, Intent intent,
                                          int requestCode, @Nullable Bundle options) {
        mContentProxy.getContext()
                .startActivityFromFragment(fragment,
                        mContentProxy.setActivityIntentExtra(intent), requestCode, options);
    }

    @Override
    public void startActivityFromChild(@NonNull Activity child, @RequiresPermission Intent intent, int requestCode) {
        mContentProxy.getContext().startActivityFromChild(child,
                mContentProxy.setActivityIntentExtra(intent), requestCode);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startActivityFromChild(@NonNull Activity child, @RequiresPermission Intent intent,
                                       int requestCode, @Nullable Bundle options) {
        mContentProxy.getContext().startActivityFromChild(child,
                mContentProxy.setActivityIntentExtra(intent), requestCode, options);
    }

    @Override
    public boolean startActivityIfNeeded(@RequiresPermission @NonNull Intent intent, int requestCode) {
        return mContentProxy.getContext()
                .startActivityIfNeeded(mContentProxy.setActivityIntentExtra(intent), requestCode);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean startActivityIfNeeded(@RequiresPermission @NonNull Intent intent, int requestCode,
                                         @Nullable Bundle options) {
        return mContentProxy.getContext().startActivityIfNeeded(mContentProxy.setActivityIntentExtra(intent),
                requestCode, options);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        mContentProxy.getContext().startActivityForResult(mContentProxy.setActivityIntentExtra(intent), requestCode);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startActivityForResult(Intent intent, int requestCode, @Nullable Bundle options) {
        mContentProxy.getContext()
                .startActivityForResult(mContentProxy.setActivityIntentExtra(intent), requestCode, options);
    }


    @Override
    public ComponentName startService(Intent service) {
        return mContentProxy.startService(service);
    }

    @Override
    public boolean stopService(Intent name) {
        return mContentProxy.stopService(name);
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        return mContentProxy.bindService(service, conn, flags);
    }

    @Override
    public Object getSystemService(@NonNull String name) {
        if (Context.LAYOUT_INFLATER_SERVICE.equals(name)) {
            return getLayoutInflater();
        }
        return super.getSystemService(name);
    }

    @NonNull
    @Override
    public LayoutInflater getLayoutInflater() {
        if (null == mLayoutInflater) {
            initLayoutInflater();
        }
        return mLayoutInflater;
    }

    private void initLayoutInflater() {
        mLayoutInflater = (LayoutInflater) super.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (null != mLayoutInflater) {
            mLayoutInflater.setFactory2(this);
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //共享window，FragmentManager后不需要调用super方法，否则会引起一些错误
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        //共享window，FragmentManager后不需要调用super方法，否则会引起一些错误
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //共享window，FragmentManager后不需要调用super方法，否则会引起一些错误
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        //共享window，FragmentManager后不需要调用super方法，否则会引起一些错误
    }

    @Override
    public View findViewById(@IdRes int id) {
        return mContentProxy.getContext().findViewById(id);
    }

    @Nullable
    @Override
    public View getCurrentFocus() {
        return mContentProxy.getContext().getCurrentFocus();
    }

    /**
     * 设置 Activity 切换动画
     * <p>
     * 动画资源不能放到插件中，只能使用 Android 系统提供的动画资源或将动画资源放到宿主中
     *
     * @param enterAnim A resource ID of the animation resource to use for the incoming activity.  Use 0 for no
     *                  animation.
     * @param exitAnim  A resource ID of the animation resource to use for the outgoing activity.  Use 0 for no
     *                  animation.
     */

    @Override
    public void overridePendingTransition(int enterAnim, int exitAnim) {
        mContentProxy.getContext().overridePendingTransition(enterAnim, exitAnim);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (null == inflater) {
            return;
        }
        setContentView(inflater.inflate(layoutResID, null));
    }

    @Override
    public void setContentView(View view) {
        mContentProxy.getContext().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        mContentProxy.getContext().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        mContentProxy.getContext().addContentView(view, params);
    }

    @Override
    public FragmentManager getSupportFragmentManager() {
        return mContentProxy.getContext() instanceof FragmentActivity
                ? ((FragmentActivity) mContentProxy.getContext()).getSupportFragmentManager() : null;
    }

    @Override
    public android.app.FragmentManager getFragmentManager() {
        return mContentProxy.getContext().getFragmentManager();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //按键事件由宿主处理，这里需要调用宿主的super方法，而不是本类的super方法
        if (mContentProxy.getContext() instanceof ActivityHostProxy) {
            return ((ActivityHostProxy) mContentProxy.getContext()).callSuperOnKeyDown(keyCode, event);
        } else {
            return false;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //按键事件由宿主处理，这里需要调用宿主的super方法，而不是本类的super方法
        if (mContentProxy.getContext() instanceof ActivityHostProxy) {
            return ((ActivityHostProxy) mContentProxy.getContext()).callSuperOnKeyUp(keyCode, event);
        } else {
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        //按键事件由宿主处理，这里需要调用宿主的super方法，而不是本类的super方法
        if (mContentProxy.getContext() instanceof ActivityHostProxy) {
            ((ActivityHostProxy) mContentProxy.getContext()).callSuperOnBackPressed();
        }
    }

    @SuppressWarnings("unchecked")
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
            justification = "exception would be thrown when View class is not in plugin")
    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        if (!mUseCompatTheme && !name.contains(".")) {
            return null;
        }

        try {
            Class viewCls;
            //android5.0及以上默认就是material风格
            if (mUseCompatTheme && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                viewCls = mContentProxy.getClassLoader().findClassFast(getCompatV7ViewClass(name));
            } else {
                viewCls = mContentProxy.getClassLoader().findClassFast(name);
            }
            if (null != viewCls) {
                Constructor<? extends View> constructor = viewCls.getConstructor(
                        Context.class, AttributeSet.class);
                constructor.setAccessible(true);
                return constructor.newInstance(context, attrs);
            }
        } catch (Exception e) {
            // 忽略掉该异常，杨锋的解释
            // LayoutInflater 会调用这个函数创建很多 View，但是并不是所有的 View 都能在这个函数中创
            // 建出来，当 Activity 的 onCreateView 返回 null，LayoutInflater 就会继续调用其他函数
            // 来创建 View, 比如这里的参数 viewClsStr 的值是 LinearLayout，这个时候 findClass 是
            // 会出错的，而LayoutInflater 是根据 LinearLayout 这个字符串来调用其他函数创建出 LinearLayout
        }

        return null;
    }

    /**
     * 检查当前Activity是否是使用appcompat-v7主题,
     * 判断方式参考appcompat-v7 25.3.1版本，其他版本判断方式可能不同
     *
     * @return true使用appcompat-v7主题，false其他主题
     */
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    private boolean useAppCompatTheme() {
        try {
            Class styleCls = mContentProxy.getClassLoader().findClassFast("android.support.v7.appcompat.R$styleable");
            Field themeField = styleCls.getDeclaredField("AppCompatTheme");
            themeField.setAccessible(true);
            int[] compatTheme = (int[]) themeField.get(null);
            Field actionBarField = styleCls.getDeclaredField("AppCompatTheme_windowActionBar");
            actionBarField.setAccessible(true);
            int compatActionBar = actionBarField.getInt(null);
            TypedArray a = obtainStyledAttributes(compatTheme);
            boolean res = a.hasValue(compatActionBar);
            a.recycle();
            return res;
        } catch (Exception e) {
            // 当插件中没有使用 appcompat 主题时，会进入到该异常分支。属于正常情况，不需要输出日志
            return false;
        }
    }

    /**
     * 获取基本控件对应的appcompat包中的类
     *
     * @param name 要创建控件的名字
     * @return appcompat包中对应的类名
     */
    private String getCompatV7ViewClass(String name) {
        String cls = name;
        switch (name) {
            case "TextView":
                cls = "android.support.v7.widget.AppCompatTextView";
                break;
            case "ImageView":
                cls = "android.support.v7.widget.AppCompatImageView";
                break;
            case "Button":
                cls = "android.support.v7.widget.AppCompatButton";
                break;
            case "EditText":
                cls = "android.support.v7.widget.AppCompatEditText";
                break;
            case "Spinner":
                cls = "android.support.v7.widget.AppCompatSpinner";
                break;
            case "ImageButton":
                cls = "android.support.v7.widget.AppCompatImageButton";
                break;
            case "CheckBox":
                cls = "android.support.v7.widget.AppCompatCheckBox";
                break;
            case "RadioButton":
                cls = "android.support.v7.widget.AppCompatRadioButton";
                break;
            case "CheckedTextView":
                cls = "android.support.v7.widget.AppCompatCheckedTextView";
                break;
            case "AutoCompleteTextView":
                cls = "android.support.v7.widget.AppCompatAutoCompleteTextView";
                break;
            case "MultiAutoCompleteTextView":
                cls = "android.support.v7.widget.AppCompatMultiAutoCompleteTextView";
                break;
            case "RatingBar":
                cls = "android.support.v7.widget.AppCompatRatingBar";
                break;
            case "SeekBar":
                cls = "android.support.v7.widget.AppCompatSeekBar";
                break;
            default:
        }

        return cls;
    }

    @Override
    public Intent getIntent() {
        if (mContentProxy.getContext() instanceof ActivityHostProxy) {
            if (null == mIntent) {
                mIntent = new Intent(mContentProxy.getContext().getIntent());
                final PluginClassLoader pluginClassLoader = mContentProxy.getClassLoader();
                if (pluginClassLoader == null) {
                    VLog.w("invoke getIntent, pluginClassLoader is null !!!");
                } else {
                    IntentUtils.mergeIntentExtras(mIntent, pluginClassLoader);
                }
            }

            return mIntent;
        }

        return mContentProxy.getContext().getIntent();
    }

    @Override
    public void setTheme(int themeId) {
        if (mThemeId == themeId) {
            return;
        }

        mThemeId = themeId;
        final boolean first = mTheme == null;
        if (first) {
            mTheme = getResources().newTheme();
            final Resources.Theme theme = getBaseContext().getTheme();
            if (theme != null) {
                mTheme.setTo(theme);
            }
        }
        onApplyThemeResource(mTheme, mThemeId, first);

        mUseCompatTheme = useAppCompatTheme();
        if (mContentProxy.getContext() instanceof ActivityHostProxy) {
            mContentProxy.getContext().setTheme(themeId);
        }
    }

    @Override
    public Resources.Theme getTheme() {
        return mTheme;
    }

    @Override
    public boolean isFinishing() {
        return mContentProxy.getContext().isFinishing();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public boolean isDestroyed() {
        return mContentProxy.getContext().isDestroyed();
    }

    @Override
    public boolean isChangingConfigurations() {
        return mContentProxy.getContext().isChangingConfigurations();
    }
}
