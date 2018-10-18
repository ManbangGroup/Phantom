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

package com.wlqq.phantom.library.pm;


/**
 * 插件安装异常：插件中依赖的 {@link com.wlqq.phantom.communication.PhantomService} 宿主没有提供或版本太低
 */

public class PhantomServiceDependenciesMismatchException extends Exception {
    public PhantomServiceDependenciesMismatchException() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    public PhantomServiceDependenciesMismatchException(String message) {
        super(message);
    }

    public PhantomServiceDependenciesMismatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public PhantomServiceDependenciesMismatchException(Throwable cause) {
        super(cause);
    }
}
