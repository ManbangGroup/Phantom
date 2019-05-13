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
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.wlqq.phantom.communication.PhantomServiceManager;
import com.wlqq.phantom.communication.IPhantomUtils;
import com.wlqq.phantom.library.PhantomCore;
import com.wlqq.phantom.library.pool.LaunchModeManager;
import com.wlqq.phantom.library.pool.LaunchModeManager.ProxyActivityLessException;
import com.wlqq.phantom.library.utils.VLog;


public class PhantomUtilsImpl implements IPhantomUtils {
    @Override
    public Context getHostContext(Context pluginContext) {
        if (pluginContext instanceof Activity) {
            Context bastContext = ((Activity) pluginContext).getBaseContext();
            //代理activity
            if (bastContext instanceof ActivityHostProxy) {
                return ((ActivityHostProxy) bastContext).getShadow();
            }

            return bastContext;
        }

        //Application context
        if (pluginContext instanceof Application) {
            return ((Application) pluginContext).getBaseContext();
        }

        return pluginContext;
    }

    @Override
    public void startActivity(Context hostContext, Intent intent) {
        PhantomCore.getInstance().startActivity(hostContext, intent);
    }

    @Override
    public @Nullable Intent resolveActivity(Intent originIntent, int launchMode) {
        ComponentName originComponent = originIntent.getComponent();
        try {
            String activity = LaunchModeManager.getInstance()
                    .resolveFixedActivity(originComponent.flattenToString(), launchMode);
            Intent intent = new Intent(originIntent);
            if (null != intent.getExtras()) {
                //清空extras
                intent.replaceExtras(new Bundle());
            }
            intent.setComponent(new ComponentName(PhantomServiceManager.getHostPackage(), activity));
            intent.putExtra("origin_intent", originIntent);
            return intent;
        } catch (ProxyActivityLessException e) {
            VLog.w(e, "no available Proxy Activity found");
        }

        return null;
    }
}
