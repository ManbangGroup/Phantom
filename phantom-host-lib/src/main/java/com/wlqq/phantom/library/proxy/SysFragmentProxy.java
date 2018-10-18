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
import android.app.Fragment;
import android.content.Context;

/**
 * 由于 final 方法不能重写，对于需要改变行为的 final 方法需要进行替换。
 * 目前的使用场景是，在插件中调用 fragment 的 getActivity 和 getContext 方法
 * 更希望得到的结果可能是插件 Activity 而不是宿主的代理 Activity（ActivityHostProxy）
 */
public class SysFragmentProxy extends Fragment implements PhantomActivityAware {
    @Override
    public Context getContext() {
        Context context = super.getContext();
        return context instanceof ActivityHostProxy
                ? ((ActivityHostProxy) context).getClientActivity() : context;
    }

    //插件中调用Fragment的getActivity方法将会被replace插件替换为getPhantomActivity方法
    @Override
    public Activity getPhantomActivity() {
        Activity activity = super.getActivity();
        return activity instanceof ActivityHostProxy
                ? ((ActivityHostProxy) activity).getClientActivity() : activity;
    }
}
