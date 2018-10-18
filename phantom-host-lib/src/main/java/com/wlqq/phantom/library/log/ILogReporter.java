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

package com.wlqq.phantom.library.log;

import java.util.HashMap;

/**
 * 定义基本监控接口，插件框架通过该接口记录一些监控信息，框架使用者可以实现该接口
 */

public interface ILogReporter {
    /**
     * 上报异常
     *
     * @param throwable 异常信息
     * @param message   对异常信息的额外说明。
     */
    void reportException(Throwable throwable, HashMap<String, Object> message);

    /**
     * 上报自定义事件
     *
     * @param eventId 上报事件的 id
     * @param label   上报事件的标签
     * @param params  上报的信息
     */
    void reportEvent(String eventId, String label, HashMap<String, Object> params);

    /**
     * 上报日志
     *
     * @param tag     日志 TAG
     * @param message 日志消息
     */
    void reportLog(String tag, String message);
}
