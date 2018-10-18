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

import java.util.Map;

/**
 * 插件安装异常： 宿主提供的公共库与插件依赖的希望宿主提供的公共库不匹配
 */

public class SharedLibraryDependenciesMismatchException extends Exception {
    public final Map<String, String> hostCompileDependencies;
    public final Map<String, String> pluginProvidedDependencies;

    public SharedLibraryDependenciesMismatchException(String message, Map<String, String> hostCompileDependencies,
                                                      Map<String, String> pluginProvidedDependencies) {
        super(message);
        this.hostCompileDependencies = hostCompileDependencies;
        this.pluginProvidedDependencies = pluginProvidedDependencies;
    }

    @SuppressWarnings({"PMD.ConsecutiveAppendsShouldReuse", "PMD.InsufficientStringBufferDeclaration",
            "PMD.ConsecutiveLiteralAppends"})
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SharedLibraryDependenciesMismatchException{");
        sb.append("message=").append(getLocalizedMessage());
        sb.append(", hostCompileDependencies=").append(hostCompileDependencies);
        sb.append(", pluginProvidedDependencies=").append(pluginProvidedDependencies);
        sb.append('}');
        return sb.toString();
    }
}
