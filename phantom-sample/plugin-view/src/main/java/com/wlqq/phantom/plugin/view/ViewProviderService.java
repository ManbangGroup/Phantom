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

package com.wlqq.phantom.plugin.view;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.view.View;

import com.wlqq.phantom.communication.PhantomService;
import com.wlqq.phantom.communication.RemoteMethod;

@PhantomService(name = BuildConfig.APPLICATION_ID + "/ViewProviderService", version = 1)
public class ViewProviderService {
    /**
     * @param context 宿主传递过来的 {@link Context}
     * @return 插件提供的 View
     *
     * @since 1
     */
    @RemoteMethod(name = "getPluginView")
    public View getPluginView(final Context context) {
        return new PluginView(context);
    }

    /**
     * @param context 宿主传递过来的 {@link Context}
     * @return 插件提供的 Fragment
     *
     * @since 1
     */
    @RemoteMethod(name = "getPluginFragment")
    public Fragment getPluginFragment(Context context) {
        return new PluginFragment(context);
    }
}
