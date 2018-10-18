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

package com.wlqq.phantom.communication;

/**
 * 调用 {@link IService#call(String, Object...)} 找不到指定方法时，抛出该异常
 *
 * @see IService#call(String, Object...)
 */
public class MethodNotFoundException extends Exception {
    public MethodNotFoundException(String detailMessage) {
        super(detailMessage);
    }

    public MethodNotFoundException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public MethodNotFoundException(Throwable throwable) {
        super(throwable);
    }
}
