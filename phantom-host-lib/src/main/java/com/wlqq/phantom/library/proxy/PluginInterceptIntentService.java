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

import android.app.IntentService;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;


public abstract class PluginInterceptIntentService extends IntentService {
    private ContextProxy<Service> mContentProxy;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public PluginInterceptIntentService(String name) {
        super(name);
    }

    public void setContextProxy(ContextProxy<Service> contextProxy) {
        mContentProxy = contextProxy;
    }

    @Override
    public void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public AssetManager getAssets() {
        return mContentProxy.getAssets();
    }

    @Override
    public Resources getResources() {
        return mContentProxy.getResources();
    }

    @Override
    public Context getApplicationContext() {
        return mContentProxy.getApplicationContext();
    }

    @Override
    public ClassLoader getClassLoader() {
        return mContentProxy.getClassLoader();
    }

    @Override
    public void startActivity(Intent intent) {
        mContentProxy.startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {
        mContentProxy.startActivity(intent, options);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startActivities(Intent[] intents, @Nullable Bundle options) {
        mContentProxy.startActivities(intents, options);
    }

    @Override
    public void startActivities(Intent[] intents) {
        mContentProxy.startActivities(intents);
    }

    @Override
    public ComponentName startService(Intent service) {
        return mContentProxy.startService(service);
    }

    @Override
    public boolean stopService(Intent name) {
        return mContentProxy.stopService(name);
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        return mContentProxy.bindService(service, conn, flags);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mContentProxy.setActivityIntentExtra(intent);
        return mContentProxy.getContext().onBind(intent);
    }
}
