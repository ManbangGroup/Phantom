

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

package com.wlqq.phantom.gradle

class PhantomPluginConfig {
    /** 插件在编译时生成其需要宿主提供的 maven 库清单 provided_dependencies_v2.txt，并打包到 APK assets 目录 */
    boolean genProvidedDeps = true

    /** 插件包名 */
    String pluginApplicationId

    /** 插件版本名 */
    String pluginVersionName

    /** 手机存储目录,默认"/sdcard/" */
    String phoneStorageDir = "/sdcard/"

    /** 宿主包名,默认null */
    String hostApplicationId

    /** 宿主launcherActivity,默认null */
    String hostAppLauncherActivity

    /**
     * 添加要删除的module，可配置在打包时是否排除module的res目录
     *
     * @param name       module名字
     * @param excludeRes false不排除res目录，true排除res目录, 默认为true
     */
    public void excludeModule(String name, boolean excludeRes, Iterable<String> keepClasses) {
        excludeModules.add(new ExcludeConfig(name, excludeRes, keepClasses));
    }

    /**
     * 添加要删除的module，可配置在打包时是否排除module的res目录
     *
     * @param name       module名字
     * @param excludeRes false不排除res目录，true排除res目录, 默认为true
     */
    public void excludeModule(String name, boolean excludeRes) {
        excludeModule(name, excludeRes, Collections.emptyList());
    }

    /**
     * 添加要删除的module，可配置在打包时是否排除module的res目录
     *
     * @param name       module名字
     * @param excludeRes false不排除res目录，true排除res目录, 默认为true
     */
    public void excludeModule(String name) {
        excludeModule(name, true, Collections.emptyList());
    }

    /**
     * 添加要删除的jar/aar，可配置在打包时是否排除jar/aar的res目录
     *
     * @param name               jar/aar在maven上的坐标
     * @param excludeRes         false不排除res目录，true排除res目录, 默认为 <code>true</code>
     * @param versionRequirement 版本约束，支持 等于，如: 4.12 大于或等于，如: >=4.12 默认值为 <code>null</code> ，即等于 name 指定的版本
     */
    public void excludeLib(String name, boolean excludeRes, String versionRequirement) {
        String[] items = name.split(":");
        excludeLibs.add(new ExcludeConfig(items[0], items[1], items[2], excludeRes, versionRequirement));
    }

    /**
     * 添加要删除的jar/aar，可配置在打包时是否排除jar/aar的res目录
     *
     * @param name               jar/aar在maven上的坐标
     * @param excludeRes         false不排除res目录，true排除res目录, 默认为 <code>true</code>
     * @param versionRequirement 版本约束，支持 等于，如: 4.12 大于或等于，如: >=4.12 默认值为 <code>null</code> ，即等于 name 指定的版本
     */
    public void excludeLib(String name, boolean excludeRes) {
        excludeLib(name, excludeRes, null);
    }

    /**
     * 添加要删除的jar/aar，可配置在打包时是否排除jar/aar的res目录
     *
     * @param name               jar/aar在maven上的坐标
     * @param excludeRes         false不排除res目录，true排除res目录, 默认为 <code>true</code>
     * @param versionRequirement 版本约束，支持 等于，如: 4.12 大于或等于，如: >=4.12 默认值为 <code>null</code> ，即等于 name 指定的版本
     */
    public void excludeLib(String name) {
        excludeLib(name, true, null);
    }

    /**
     * 添加要删除的class文件
     *
     * @param name 要删除的class，可以是包名或者class完整名字，比如android.support.v4.R.class
     */
    public void excludeClass(String name) {
        excludeClasses.add(name);
    }

    public boolean isExcludeLib(String name) {
        for (ExcludeConfig config : excludeLibs) {
            if (config.getArtifactId() + "-" + config.getVersion().equals(name)) {
                return true;
            }

        }


        return false;
    }

    public ExcludeConfig excludeLibInfo(String name) {
        for (ExcludeConfig config : excludeLibs) {
            if (config.getArtifactId() + "-" + config.getVersion().equals(name)) {
                return config;
            }

        }


        return null;
    }

    public ArrayList<ExcludeConfig> getExcludeModules() {
        return excludeModules;
    }

    public void setExcludeModules(List<ExcludeConfig> excludeModules) {
        this.excludeModules = excludeModules;
    }

    public ArrayList<ExcludeConfig> getExcludeLibs() {
        return excludeLibs;
    }

    public void setExcludeLibs(List<ExcludeConfig> excludeLibs) {
        this.excludeLibs = excludeLibs;
    }

    public ArrayList<String> getExcludeClasses() {
        return excludeClasses;
    }

    public void setExcludeClasses(List<String> excludeClasses) {
        this.excludeClasses = excludeClasses;
    }

    public File getLibraryJarsProguardFile() {
        return libraryJarsProguardFile;
    }

    public void setLibraryJarsProguardFile(File libraryJarsProguardFile) {
        this.libraryJarsProguardFile = libraryJarsProguardFile;
    }

    private List<ExcludeConfig> excludeModules = new ArrayList<ExcludeConfig>();
    private List<ExcludeConfig> excludeLibs = new ArrayList<ExcludeConfig>();
    private List<String> excludeClasses = new ArrayList<String>();
    private File libraryJarsProguardFile;

    /**
     * 删除module或jar/aar的配置，默认排除res目录里的资源
     */
    public static class ExcludeConfig {
        public ExcludeConfig(String name) {
            this.name = name;
        }

        public ExcludeConfig(String name, boolean excludeRes) {
            this.name = name;
            this.isExcludeRes = excludeRes;
        }

        public ExcludeConfig(String name, boolean excludeRes, Iterable<String> keepClasses) {
            this.name = name;
            this.isExcludeRes = excludeRes;
            this.keepClasses = keepClasses;
        }

        /**
         * 构造 exclude maven artifact 配置
         *
         * @param groupId            maven group id
         * @param artifactId         maven artifact id
         * @param version            maven version
         * @param excludeRes         是否排除资源
         * @param versionRequirement 版本约束，支持 等于，如: 4.12; 大于或等于，如: >=4.12; 若为 <code>null</code> ，则等于 <code>version</code>
         *                           指定的版本
         */
        public ExcludeConfig(String groupId, String artifactId, String version, boolean excludeRes,
                String versionRequirement) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.isExcludeRes = excludeRes;
            if (versionRequirement == null) {
                this.versionRequirement = this.version;
            } else {
                this.versionRequirement = versionRequirement;
            }

            this.name = this.groupId + ":" + this.artifactId + ":" + this.version;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public boolean getIsExcludeRes() {
            return isExcludeRes;
        }

        public boolean isIsExcludeRes() {
            return isExcludeRes;
        }

        public void setIsExcludeRes(boolean isExcludeRes) {
            this.isExcludeRes = isExcludeRes;
        }

        public String getVersionRequirement() {
            return versionRequirement;
        }

        public void setVersionRequirement(String versionRequirement) {
            this.versionRequirement = versionRequirement;
        }

        public Iterable<String> getKeepClasses() {
            return keepClasses;
        }

        public void setKeepClasses(Iterable<String> keepClasses) {
            this.keepClasses = keepClasses;
        }

        private String name;
        private String groupId;
        private String artifactId;
        private String version;
        private boolean isExcludeRes = true;
        private String versionRequirement;
        private Iterable<String> keepClasses;
    }
}
