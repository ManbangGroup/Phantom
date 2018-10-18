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

package com.wlqq.phantom.library.utils;

import android.os.Handler;
import android.os.Looper;

public final class ThreadUtils {
    private static Handler sMainHandler = new Handler(Looper.getMainLooper());
    private static long sMainThreadId = sMainHandler.getLooper().getThread().getId();

    private ThreadUtils() {
    }

    public static void runOnUiThread(Runnable action) {
        if (isInUiThread()) {
            action.run();
        } else {
            sMainHandler.post(action);
        }
    }

    public static boolean isInUiThread() {
        return Thread.currentThread().getId() == sMainThreadId;
    }
}
