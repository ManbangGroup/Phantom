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

package com.wlqq.phantom.plugin.component.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.wlqq.phantom.plugin.component.MainActivity;

public class PluginService extends Service {

    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        sendMessage("onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sendMessage("onStartCommand");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        sendMessage("onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        sendMessage("onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        sendMessage("onUnbind");

        return super.onUnbind(intent);
    }

    private void sendMessage(String message) {
        Intent intent = new Intent(MainActivity.ACTION_BROADCAST_MSG);
        intent.putExtra("result","[PluginService] " + message);
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public PluginService getService() {
            return PluginService.this;
        }
    }

}
