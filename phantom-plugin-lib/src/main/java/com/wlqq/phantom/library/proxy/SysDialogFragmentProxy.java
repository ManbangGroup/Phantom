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
import android.app.DialogFragment;
import android.content.Context;


public class SysDialogFragmentProxy extends DialogFragment implements PhantomActivityAware {
    public Context getContext() {
        return null;
    }

    //插件中调用Fragment的getActivity方法将会被replace插件替换为getPhantomActivity方法
    @Override
    public Activity getPhantomActivity() {
        return null;
    }
}
