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

package com.wlqq.phantom.sample;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;

import com.wlqq.phantom.communication.MethodNotFoundException;
import com.wlqq.phantom.communication.PhantomServiceManager;
import com.wlqq.phantom.communication.IService;
import com.wlqq.phantom.library.PhantomCore;

public class EmbedPluginViewActivity extends AppCompatActivity {
    private FrameLayout mFlEmbedView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_embed_plugin_view);

        mFlEmbedView = findViewById(R.id.frl_embed_view);

        initViews();
    }


    private void initViews() {
        final Context pluginContext = PhantomCore.getInstance().createPluginContext(this,
                "com.wlqq.phantom.plugin.view");

        IService iService = PhantomServiceManager.getService("com.wlqq.phantom.plugin.view", "ViewProviderService");
        if (iService != null) {
            try {
                View view = (View) iService.call("getPluginView", pluginContext);

                if (view != null) {
                    mFlEmbedView.addView(view);
                }

            } catch (MethodNotFoundException e) {
                e.printStackTrace();
            }

            try {
                Fragment fragment = (Fragment) iService.call("getPluginFragment", pluginContext);

                if (fragment != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .add(R.id.frl_embed_fragment, fragment)
                            .commit();
                }

            } catch (MethodNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
