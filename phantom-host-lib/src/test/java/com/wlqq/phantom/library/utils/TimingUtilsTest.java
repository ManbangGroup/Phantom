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

import junit.framework.Assert;

import org.junit.Test;


public class TimingUtilsTest {

    @Test
    public void normalizeDuration_isCorrect() throws Exception {
        Assert.assertEquals("<=50",
                TimingUtils.normalizeDuration(49, TimingUtils.SECTION_DURATION_50_MS, TimingUtils.MAX_SECTION_20));
    }

    @Test
    public void normalizeDuration_isCorrect_1() throws Exception {
        Assert.assertEquals("<=50",
                TimingUtils.normalizeDuration(50, TimingUtils.SECTION_DURATION_50_MS, TimingUtils.MAX_SECTION_20));
    }

    @Test
    public void normalizeDuration_isCorrect_2() throws Exception {
        Assert.assertEquals("<=100",
                TimingUtils.normalizeDuration(51, TimingUtils.SECTION_DURATION_50_MS, TimingUtils.MAX_SECTION_20));
    }

    @Test
    public void normalizeDuration_isCorrect_3() throws Exception {
        Assert.assertEquals("<=200",
                TimingUtils.normalizeDuration(199, TimingUtils.SECTION_DURATION_50_MS, TimingUtils.MAX_SECTION_20));
    }

    @Test
    public void normalizeDuration_isCorrect_4() throws Exception {
        Assert.assertEquals(">1000",
                TimingUtils.normalizeDuration(1001, TimingUtils.SECTION_DURATION_50_MS, TimingUtils.MAX_SECTION_20));
    }

    @Test
    public void normalizeDuration_isCorrect_5() throws Exception {
        Assert.assertEquals(">1000",
                TimingUtils.normalizeDuration(1001, TimingUtils.SECTION_DURATION_50_MS, TimingUtils.MAX_SECTION_20));
    }
}
