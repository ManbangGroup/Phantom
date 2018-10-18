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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * 注：不推荐宿主中使用插件中 Fragment，因为此时需要宿主提供 Context 对象，系统会警告非空构造的 Fragment
 * （插件内部 Fragment 使用除外）
 */
@SuppressLint("ValidFragment")
public class PluginFragment extends Fragment {

    Context mContext;

    @Deprecated
    public PluginFragment() {
    }

    public PluginFragment(Context context) {
        mContext = context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        if (mContext != null) {
            inflater = LayoutInflater.from(mContext);
        }
        return createView(inflater);
    }

    private View createView(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.view_partial, null);

        ((TextView) view.findViewById(R.id.textview)).setText("PluginFragment in Plugin-View!");

        view.findViewById(R.id.btn_dialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                        .setTitle("PluginFragment")
                        .setMessage("Dialog in Plugin-View!")
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                alertDialog.show();
            }
        });

        return view;
    }
}
