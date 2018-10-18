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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wlqq.phantom.plugin.component.MainActivity;
import com.wlqq.phantom.plugin.component.R;

public class BroadcastFragment extends Fragment implements View.OnClickListener {

    private TextView mETLog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_broadcast, container, false);

        mETLog = (TextView) view.findViewById(R.id.tv_log);
        mETLog.setMovementMethod(ScrollingMovementMethod.getInstance());

        view.findViewById(R.id.btn_send_broadcast).setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send_broadcast:
                Intent broadcast = new Intent(MainActivity.ACTION_BROADCAST_MSG);
                broadcast.putExtra("result", "Broadcast : this is a broadcast msg.");
                getContext().sendBroadcast(broadcast);
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

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String data = intent.getStringExtra("result");

            mETLog.append(data + "\n");
        }
    };
}
