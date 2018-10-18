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

import android.app.AlertDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

public class PluginView extends FrameLayout implements View.OnClickListener {

    public PluginView(@NonNull Context context) {
        super(context);

        LayoutInflater.from(context).inflate(R.layout.view_partial, this, true);
        findViewById(R.id.btn_dialog).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_dialog:
                AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                        .setTitle("PluginView")
                        .setMessage("Dialog in Plugin-View!")
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                alertDialog.show();
                break;
            default:
                break;
        }
    }
}
