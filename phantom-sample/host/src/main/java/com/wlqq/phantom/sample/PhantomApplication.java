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

import android.app.Application;

import com.wlqq.phantom.library.PhantomCore;
import com.wlqq.phantom.library.log.ILogReporter;

import java.util.HashMap;

public class PhantomApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        initPhantom();
    }

    private void initPhantom() {
        PhantomCore.getInstance().init(this, new PhantomCore.Config()
                .setDebug(BuildConfig.DEBUG)
                .setLogLevel(BuildConfig.DEBUG ? android.util.Log.VERBOSE : android.util.Log.WARN)
                .addPhantomService(new HostInfoService())
                .setLogReporter(new LogReporterImpl()));
    }


    private static final class LogReporterImpl implements ILogReporter {

        @Override
        public void reportException(Throwable throwable, HashMap<String, Object> message) {
            // 使用 Bugly 或其它异常监控平台上报 Phantom 内部捕获的异常
        }

        @Override
        public void reportEvent(String eventId, String label, HashMap<String, Object> params) {
            // 使用 talkingdata 或其它移动统计平台上报 Phantom 内部自定义事件
        }

        @Override
        public void reportLog(String tag, String message) {
            // 使用 Bugly 或其它异常监控平台上报 Phantom 内部输出的上下文相关日志
        }
    }


}
