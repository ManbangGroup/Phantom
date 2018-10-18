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
 * 由于final方法不能重写，对于需要改变行为的final方法需要进行代理。
 * 目前的使用场景是，在插件中调用fragment的getActivity和getContext方法
 * 更希望得到的结果可能是插件Activity而不是宿主的代理Activity（ActivityHostProxy）
 */
public class SysFragmentProxy extends Fragment implements PhantomActivityAware {
    public Context getContext() {
        return null;
    }

    //代理宿主getActivity方法
    @Override
    public Activity getPhantomActivity() {
        return null;
    }
}
