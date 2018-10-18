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

import android.os.SystemClock;

import java.util.HashMap;


public final class TimingUtils {
    public static final int SECTION_DURATION_10_MS = 10;
    public static final int SECTION_DURATION_20_MS = 20;
    public static final int SECTION_DURATION_50_MS = 50;
    public static final int SECTION_DURATION_100_MS = 100;
    public static final int SECTION_DURATION_500_MS = 500;

    public static final int MAX_SECTION_10 = 10;
    public static final int MAX_SECTION_20 = 20;
    public static final int MAX_SECTION_40 = 40;
    public static final int MAX_SECTION_50 = 50;
    public static final int MAX_SECTION_100 = 100;

    private static HashMap<String, Long> sTimeRecords = new HashMap<>();

    private TimingUtils() {
        // prevent instantiation
    }

    public static void startTime(String tag) {
        sTimeRecords.put(tag, SystemClock.elapsedRealtime());
    }

    public static long elapsedTime(String tag) {
        Long start = sTimeRecords.remove(tag);
        if (null == start) {
            return 0;
        }

        final long duration = SystemClock.elapsedRealtime() - start;

        if (duration < 0) {
            return 0;
        }

        return duration;
    }

    public static String getNormalizedDuration(String tag, int sectionDurationMs, int maxSection) {
        final long duration = elapsedTime(tag);
        return normalizeDuration(duration, sectionDurationMs, maxSection);
    }

    public static String normalizeDuration(long elapsedDuration, int sectionDurationMs, int maxSection) {
        long normalizedDuration;
        if (elapsedDuration % sectionDurationMs == 0) {
            normalizedDuration = elapsedDuration;
        } else {
            normalizedDuration = (elapsedDuration + sectionDurationMs) / sectionDurationMs * sectionDurationMs;
        }

        final int maxDuration = sectionDurationMs * maxSection;

        if (normalizedDuration <= maxDuration) {
            return "<=" + normalizedDuration;
        } else {
            return ">" + maxDuration;
        }
    }
}
