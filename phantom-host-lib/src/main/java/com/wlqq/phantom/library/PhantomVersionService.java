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

package com.wlqq.phantom.library;

import com.wlqq.phantom.communication.PhantomService;

/**
 * 提供 Phantom Core version code 信息
 * <p>
 * 插件可在其 <code>AndroidManifest.xml</code> 中声明其需要宿主集成的 Phantom Core 最低版本
 * <pre>{@code
 * <!-- 该插件对宿主 Phantom Core 的最小版本要求是 3.0.0 -->
 * <meta-data
 *     android:name="phantom.service.import.PhantomVersionService"
 *     android:value="30000" />
 * }
 * </pre>
 */
@PhantomService(name = "PhantomVersionService", version = BuildConfig.VERSION_CODE)
public class PhantomVersionService {
}
