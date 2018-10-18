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

package com.wlqq.mavenversion;


import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class VersionUtilsTest {

    private Map<String, String> mVersions;

    @Before
    public void setup() {
        mVersions = new HashMap<>();
        mVersions.put("junit:junit", "4.12");
        mVersions.put("com.android.support:support-v4", "25.3.1");
    }

    @After
    public void tearDown() {
        mVersions.clear();
        mVersions = null;
    }

    @Test
    public void testSatisfiesStrictShouldSuccess1() throws Exception {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("junit:junit", "4.12");
        requirements.put("com.android.support:support-v4", "25.3.1");

        Assert.assertTrue(VersionVerifier.satisfies(mVersions, requirements).success);
    }

    @Test
    public void testSatisfiesStrictShouldSuccess2() throws Exception {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("junit:junit", "4.12");

        Assert.assertTrue(VersionVerifier.satisfies(mVersions, requirements).success);
    }

    @Test
    public void testSatisfiesStrictShouldFail1() throws Exception {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("junit:junit", "4.12");
        // version mismatch
        requirements.put("com.android.support:support-v4", "25.3.0");

        final VersionVerifier.Result satisfies = VersionVerifier.satisfies(mVersions, requirements);
        System.out.println(satisfies);
        Assert.assertFalse(satisfies.success);
        Assert.assertEquals("com.android.support:support-v4", satisfies.lib);
    }

    @Test
    public void testSatisfiesStrictShouldFail2() throws Exception {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("junit:junit", "4.12");
        requirements.put("com.android.support:support-v4", "25.3.1");
        // this lib is missing
        requirements.put("com.android.support:appcompat-v7", "25.3.1");

        final VersionVerifier.Result satisfies = VersionVerifier.satisfies(mVersions, requirements);
        System.out.println(satisfies);
        Assert.assertFalse(satisfies.success);
        Assert.assertEquals("com.android.support:appcompat-v7", satisfies.lib);
    }

    @Test
    public void testSatisfiesLooseShouldSuccess1() throws Exception {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("junit:junit", "4.12");
        requirements.put("com.android.support:support-v4", ">=25.3.1");

        final VersionVerifier.Result satisfies = VersionVerifier.satisfies(mVersions, requirements);
        Assert.assertTrue(satisfies.success);
    }

    @Test
    public void testSatisfiesLooseShouldSuccess2() throws Exception {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("junit:junit", "4.12");
        requirements.put("com.android.support:support-v4", ">=25");

        final VersionVerifier.Result satisfies = VersionVerifier.satisfies(mVersions, requirements);
        Assert.assertTrue(satisfies.success);
    }

    @Test
    public void testSatisfiesLooseShouldSuccess3() throws Exception {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("junit:junit", "4.12");
        requirements.put("com.android.support:support-v4", ">=24");

        final VersionVerifier.Result satisfies = VersionVerifier.satisfies(mVersions, requirements);
        Assert.assertTrue(satisfies.success);
    }

    @Test
    public void testSatisfiesLooseShouldFail1() throws Exception {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("junit:junit", "4.12");
        requirements.put("com.android.support:support-v4", ">=26"); // version not satisfies

        final VersionVerifier.Result satisfies = VersionVerifier.satisfies(mVersions, requirements);
        System.out.println(satisfies);
        Assert.assertFalse(satisfies.success);
        Assert.assertEquals("com.android.support:support-v4", satisfies.lib);
    }

    @Test
    public void testSatisfiesLooseShouldFail2() throws Exception {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("junit:junit", "4.12");
        requirements.put("com.android.support:support-v4", ">=25.3.2"); // version not satisfies

        final VersionVerifier.Result satisfies = VersionVerifier.satisfies(mVersions, requirements);
        System.out.println(satisfies);
        Assert.assertFalse(satisfies.success);
        Assert.assertEquals("com.android.support:support-v4", satisfies.lib);
    }

    @Test
    public void testSatisfiesLooseShouldFail3() throws Exception {
        Map<String, String> requirements = new HashMap<>();
        requirements.put("junit:junit", "4.12");
        requirements.put("com.android.support:support-v4", ">=25");
        requirements.put("com.android.support:appcompat-v7", ">=25");   // lib not exist

        final VersionVerifier.Result satisfies = VersionVerifier.satisfies(mVersions, requirements);
        System.out.println(satisfies);
        Assert.assertFalse(satisfies.success);
        Assert.assertEquals("com.android.support:appcompat-v7", satisfies.lib);
    }
}
