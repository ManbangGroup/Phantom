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


import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class FileUtilsTest {
    @Test
    public void testCalculateMd5ForInputStream() throws Exception {
        Assert.assertEquals("2a66a876a40630539e220890dc897550",
                FileUtils.calculateMd5(getInputStream("com.wlqq.phantom.plugin.sample1_1.0.0.apk")));
    }

    private InputStream getInputStream(String filename) throws IOException {
        return getClass().getClassLoader().getResourceAsStream(filename);
    }
}
