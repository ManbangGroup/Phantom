/*
 * Copyright (C) 2017-2019 Manbang Group
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

package com.wlqq.phantom.gradle.dependency

/**
 * Represents a library in Android Project
 *
 * @author zhengtao
 */
public abstract class DependenceInfo {

    /**
     * The type of of the DependenceInfo.
     */
    public enum DependenceType{

        /**
         * Type of Android library
         */
        AAR(0x01),

        /**
         * Type of Java library
         */
        JAR(0x02)

        private final int value;

        DependenceType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Group name of dependence in a Maven repository
     */
    private String group
    /**
     * Module name of dependence in a Maven repository
     */
    private String artifact
    /**
     * Version of dependence in a Maven repository
     */
    private String version


    DependenceInfo(String group, String artifact, String version) {
        this.group = group
        this.artifact = artifact
        this.version = version
    }


    String getGroup() {
        return group
    }

    String getArtifact() {
        return artifact
    }

    String getVersion() {
        return version
    }

    abstract File getJarFile()
    abstract DependenceType getDependenceType()

    @Override
    String toString() {
        return "${group}:${artifact}:${version} -> ${jarFile} -> ${super.toString()}"
    }
}