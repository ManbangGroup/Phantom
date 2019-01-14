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
 * 供远程调用的服务模块描述
 */
public interface IService extends ServiceInfo {
    /**
     * 调用服务提供的方法
     *
     * @param method 方法名，见 {@link RemoteMethod#name()}
     * @param args   调用参数列表
     * @return 调用方法的返回值
     * @throws MethodNotFoundException 若方法名不存在
     * @see RemoteMethod#name()
     */
    Object call(String method, Object... args) throws MethodNotFoundException;
}
