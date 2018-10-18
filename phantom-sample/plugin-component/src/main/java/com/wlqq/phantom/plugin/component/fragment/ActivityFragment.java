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

package com.wlqq.phantom.plugin.component.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.wlqq.phantom.plugin.component.LaunchModeActivity;
import com.wlqq.phantom.plugin.component.R;
import com.wlqq.phantom.plugin.component.SingleInstanceActivity;
import com.wlqq.phantom.plugin.component.SingleTaskActivity;
import com.wlqq.phantom.plugin.component.SingleTopActivity;
import com.wlqq.phantom.plugin.component.StandardActivity;

public class ActivityFragment extends Fragment implements View.OnClickListener {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);

        view.findViewById(R.id.btn_start_singleinstance_activity).setOnClickListener(this);
        view.findViewById(R.id.btn_start_singletask_activity).setOnClickListener(this);
        view.findViewById(R.id.btn_start_singletop_activity).setOnClickListener(this);
        view.findViewById(R.id.btn_start_stantard_activity).setOnClickListener(this);
        view.findViewById(R.id.btn_start_host_activity).setOnClickListener(this);
        view.findViewById(R.id.btn_start_system_activity).setOnClickListener(this);
        view.findViewById(R.id.btn_start_activity_in_another_plugin).setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start_stantard_activity:
                startLaunchModeActivity(StandardActivity.class);
                break;
            case R.id.btn_start_singleinstance_activity:
                startLaunchModeActivity(SingleInstanceActivity.class);
                break;
            case R.id.btn_start_singletop_activity:
                startLaunchModeActivity(SingleTopActivity.class);
                break;
            case R.id.btn_start_singletask_activity:
                startLaunchModeActivity(SingleTaskActivity.class);
                break;
            case R.id.btn_start_activity_in_another_plugin:
                startOtherPluginActivity("com.wlqq.phantom.plugin.view", "com.wlqq.phantom.plugin.view.MainActivity");
                break;
            case R.id.btn_start_host_activity:
                startHostActivity();
                break;
            case R.id.btn_start_system_activity:
                startSystemActivity();
                break;
            default:
                break;
        }
    }

    private void startLaunchModeActivity(Class<? extends Activity> clazz) {
        Intent intent = new Intent();
        intent.setClass(getActivity(), clazz);
        intent.putExtra(LaunchModeActivity.KEY_LABEL, clazz.getSimpleName());
        intent.putExtra(LaunchModeActivity.KEY_INDEX, 1);
        startActivity(intent);
    }

    private void startHostActivity() {
        Intent intent = new Intent();
        intent.setClassName("com.wlqq.phantom.sample", "com.wlqq.phantom.sample.MainActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void startSystemActivity() {
        Uri webpage = Uri.parse("http://m.baidu.com");
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        startActivity(intent);
    }

    private void startOtherPluginActivity(String packageName, String className) {
        Intent intent = new Intent();
        intent.setClassName(packageName, className);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getActivity(), "启动错误，请检查插件是否安装", Toast.LENGTH_SHORT).show();
        }
    }
}
