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

import org.junit.Assert;
import org.junit.Test;

public class VersionTest {
    @Test
    public void testCreateVersion() throws Exception {
        // org.infinispan:infinispan-directory-provider:9.2.0.Alpha2
        Version version = new Version("9.2.0.Alpha2");
        // junit:junit:4.12
        version = new Version("4.12");
        // org.json:json:20171018
        version = new Version("20171018");
        // io.reactivex.rxjava2:rxjava:2.1.6
        version = new Version("2.1.6");
        // io.reactivex.rxjava2:rxjava:2.1.6-SNAPSHOT
        version = new Version("2.1.6-SNAPSHOT");
    }

    @Test
    public void testCompareVersionEquals() throws Exception {
        {
            Version version1 = new Version("27.0.0");
            Version version2 = new Version("27.0.0");

            Assert.assertTrue(version1.compareTo(version2) == 0);
        }

        {
            Version version1 = new Version("27.0");
            Version version2 = new Version("27.0.0");

            Assert.assertTrue(version1.compareTo(version2) == 0);
        }

        {
            Version version1 = new Version("27");
            Version version2 = new Version("27.0.0");

            Assert.assertTrue(version1.compareTo(version2) == 0);
        }

        {
            Version version1 = new Version("27");
            Version version2 = new Version("27.0");

            Assert.assertTrue(version1.compareTo(version2) == 0);
        }

        {
            Version version1 = new Version("27.0");
            Version version2 = new Version("27.0.0");

            Assert.assertTrue(version1.compareTo(version2) == 0);
        }
    }

    @Test
    public void testCompareVersionEquals2() throws Exception {
        {
            Version version1 = new Version("27.0.0");
            Version version2 = new Version("27.0.0");

            Assert.assertTrue(version1.equals(version2));
        }

        {
            Version version1 = new Version("27.0");
            Version version2 = new Version("27.0.0");

            Assert.assertTrue(version1.equals(version2));
        }

        {
            Version version1 = new Version("27");
            Version version2 = new Version("27.0.0");

            Assert.assertTrue(version1.equals(version2));
        }

        {
            Version version1 = new Version("27");
            Version version2 = new Version("27.0");

            Assert.assertTrue(version1.equals(version2));
        }

        {
            Version version1 = new Version("27.0");
            Version version2 = new Version("27.0.0");

            Assert.assertTrue(version1.equals(version2));
        }
    }

    @Test
    public void testCompareVersionGreater() throws Exception {
        {
            Version version1 = new Version("27.0.0");
            Version version2 = new Version("26.0.0");

            Assert.assertTrue(version1.compareTo(version2) > 0);
        }

        {
            Version version1 = new Version("26.0.1");
            Version version2 = new Version("26.0.0");

            Assert.assertTrue(version1.compareTo(version2) > 0);
        }

        {
            Version version1 = new Version("26.0.1");
            Version version2 = new Version("26.0");

            Assert.assertTrue(version1.compareTo(version2) > 0);
        }

        {
            Version version1 = new Version("26.0.1");
            Version version2 = new Version("26");

            Assert.assertTrue(version1.compareTo(version2) > 0);
        }

        {
            Version version1 = new Version("26.1");
            Version version2 = new Version("26");

            Assert.assertTrue(version1.compareTo(version2) > 0);
        }

        {
            Version version1 = new Version("20171218");
            Version version2 = new Version("20171018");
            Assert.assertTrue(version1.compareTo(version2) > 0);
        }
    }

    @Test
    public void testSatisfiesEqual() throws Exception {
        {
            Version version = new Version("27.0.0");
            Assert.assertTrue(version.satisfies("27.0.0"));
        }

        {
            Version version = new Version("27.0.0");
            Assert.assertTrue(version.satisfies("27.0"));
        }

        {
            Version version = new Version("27.0.0");
            Assert.assertTrue(version.satisfies("27"));
        }

        {
            Version version = new Version("27.0.0");
            Assert.assertFalse(version.satisfies("27.0.1"));
        }

        {
            Version version = new Version("27.0.0-SNAPSHOT");
            Assert.assertTrue(version.satisfies("27.0.0-SNAPSHOT"));
            Assert.assertFalse(version.satisfies("27.0.1-SNAPSHOT"));
            Assert.assertFalse(version.satisfies("27.0.0"));
        }
    }

    @Test
    public void testSatisfiesGreaterOrEqual() throws Exception {
        {
            Version version = new Version("27.0.0");
            Assert.assertTrue(version.satisfies(">=27.0.0"));
        }

        {
            Version version = new Version("27.0.1");
            Assert.assertTrue(version.satisfies(">=27.0"));
        }

        {
            Version version = new Version("27.1.0");
            Assert.assertTrue(version.satisfies(">=27"));
        }

        {
            Version version = new Version("27.0.0");
            Assert.assertFalse(version.satisfies(">=27.1.0"));
        }

        {
            Version version = new Version("27");
            Assert.assertTrue(version.satisfies(">=26.9999999.9999999.99999999.99999999999"));
        }

        {
            Version version = new Version("20171018");
            Assert.assertTrue(version.satisfies(">=20161018"));
            Assert.assertFalse(version.satisfies("20161018"));
        }

        {
            Version version = new Version("27.0.0");
            Assert.assertFalse(version.satisfies("27.0.0-SNAPSHOT"));
            Assert.assertTrue(version.satisfies(">=27.0.0-SNAPSHOT"));
        }

        {
            Version version = new Version("28.0.0");
            Assert.assertFalse(version.satisfies("27.0.0-SNAPSHOT"));
            Assert.assertTrue(version.satisfies(">=27.0.0-SNAPSHOT"));
        }

        {
            Version version = new Version("28.0.0");
            Assert.assertTrue(version.satisfies(">=28.0.0-beta.2"));
        }

        {
            Version version = new Version("28.0.0");
            Assert.assertTrue(version.satisfies(">=28.0.0-beta.2+build.3"));
        }

        {
            Version version = new Version("28.0.0");
            Assert.assertTrue(version.satisfies(">=28.0.0-alpha1"));
        }

        {
            Version version = new Version("28.0.0");
            Assert.assertTrue(version.satisfies(">=28.0.0-beta01"));
        }

        {
            Version version = new Version("28.0.0");
            Assert.assertTrue(version.satisfies(">=28.0.0-rc02"));
        }

        {
            Version version = new Version("28.0.0-rc02");
            Assert.assertTrue(version.satisfies(">=25.3.1"));
        }
    }

    @Test
    public void testInvalidExpressionSatisfiesShouldReturnFalse() throws Exception {
        {
            Version version = new Version("27.0.0");
            Assert.assertFalse(version.satisfies("=27.0.0"));
        }

        {
            Version version = new Version("27.0.0");
            Assert.assertFalse(version.satisfies("==27.0.0"));
        }

        {
            Version version = new Version("27.0.0");
            Assert.assertFalse(version.satisfies(">27.0.0"));
        }

        {
            Version version = new Version("27.0.0");
            Assert.assertFalse(version.satisfies("<27.0.0"));
        }

        {
            Version version = new Version("27.0.0");
            Assert.assertFalse(version.satisfies("<=27.0.0"));
        }

        {
            Version version = new Version("27.0.0");
            Assert.assertFalse(version.satisfies("!=27.0.0"));
        }

        {
            Version version = new Version("27.0.0");
            Assert.assertFalse(version.satisfies("27.0.0-prerelease"));
        }
    }
}
