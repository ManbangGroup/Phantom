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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wlqq.phantom.plugin.component.MainActivity;
import com.wlqq.phantom.plugin.component.R;
import com.wlqq.phantom.plugin.component.service.PluginIntentService;
import com.wlqq.phantom.plugin.component.service.PluginService;

public class ServiceFragment extends Fragment implements View.OnClickListener {

    private TextView mETLog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_service, container, false);

        mETLog = (TextView) view.findViewById(R.id.tv_log);
        mETLog.setMovementMethod(ScrollingMovementMethod.getInstance());

        view.findViewById(R.id.btn_start_plugin_intent_service).setOnClickListener(this);
        view.findViewById(R.id.btn_start_plugin_service).setOnClickListener(this);
        view.findViewById(R.id.btn_stop_plugin_service).setOnClickListener(this);
        view.findViewById(R.id.btn_bind_plugin_service).setOnClickListener(this);
        view.findViewById(R.id.btn_unbind_plugin_service).setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        final Intent intent = new Intent();
        switch (v.getId()) {
            case R.id.btn_start_plugin_intent_service:
                intent.setClass(getActivity(), PluginIntentService.class);
                getContext().startService(intent);
                break;
            case R.id.btn_start_plugin_service:
                intent.setClass(getActivity(), PluginService.class);
                getContext().startService(intent);
                break;
            case R.id.btn_stop_plugin_service:
                intent.setClass(getActivity(), PluginService.class);
                getContext().stopService(intent);
                break;
            case R.id.btn_bind_plugin_service:
                intent.setClass(getActivity(), PluginService.class);
                getContext().bindService(intent, mServiceConnection, Service.BIND_AUTO_CREATE);
                break;
            case R.id.btn_unbind_plugin_service:
                unbindServiceSafe(mServiceConnection);
                break;
            default:
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getContext().registerReceiver(mBroadcastReceiver, new IntentFilter(MainActivity.ACTION_BROADCAST_MSG));
    }

    @Override
    public void onPause() {
        super.onPause();
        getContext().unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onDestroy() {
        unbindServiceSafe(mServiceConnection);
        super.onDestroy();
    }

    private void unbindServiceSafe(ServiceConnection serviceConnection) {
        try {
            getContext().unbindService(serviceConnection);
        } catch (Exception e) {
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PluginService.LocalBinder binder = (PluginService.LocalBinder) service;
            mETLog.append(name + ": onServiceConnected\n");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mETLog.append(name +": onServiceDisconnected\n");
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String data = intent.getStringExtra("result");

            mETLog.append(data + "\n");
        }
    };
}
