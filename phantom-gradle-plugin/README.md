# Phantom Gradle Plugin

用于构建 Phantom 宿主和插件的 Gradle 插件

## 背景说明

为了简化插件化开发，将以下功能整合到同一个项目中

- 生成宿主提供的依赖库信息(原 `BuildScript/provided_lib_properties_creator.gradle`)
- 生成插件需要的依赖库信息(原 `BuildScript/provided_lib_properties_creator.gradle`)
- 快速安装调试插件（原 [PhantomDebugger](http://git.56qq.com/Android-Team/PhantomDebugger)）
- 剔除公共库（原 [WLExcludeClasses](http://git.56qq.com/Android-Team/WLExcludeClasses)）
- 替换插件 Activity/Service/Fragment 基类（原 [PhantomReplaceGradlePlugin](http://git.56qq.com/Android-Team/PhantomReplaceGradlePlugin)）

同时增加了以下功能

- 插件端
  - 编译时在 `<application module>/build/intermediates/phantom/debug` 下生成剔除和保留的 maven 库清单
  - 支持**禁用**自动生成插件需要的依赖库信息
  - 备份自动生成的插件需要的依赖库信息文件 `provided_dependencies_v2.txt` 到 `<application module>/build/intermedidates/phantom/<build variant>` 目录
- 宿主端
  - 备份自动生成的宿主提供的依赖库信息文件 `compile_dependencies.txt` 到 `<application module>/build/intermedidates/phantom/<build variant>` 目录

## 插件 maven 坐标

`com.wlqq.android:phantom-gradle-plugin`

## 最新版本

见[发布说明](CHANGELOG.md)

## 宿主端

### 功能说明

- 编译时生成宿主依赖的 maven 库坐标和版本清单文件 `compile_dependencies.txt`，并打包到 APK 的 assets 目录中。同时备份该文件到编译中间目录 `<application module>/build/intermedidates/phantom/<build variant>`

### 如何接入

在宿主 `<application module>/build.gradle` 应用该插件，需要放到 `apply plugin: 'com.android.application'` 后面

```groovy
apply plugin: 'com.wlqq.phantom.host'
```

### 常见问题

1. Q: 如何禁用自动生成 `compile_dependencies.txt` ？

   A: 不接入该插件

## 插件端

### 功能说明

- 剔除不需要打包到插件中的 maven 库/源码模块/具体的类或包，可以指定是否保留资源(原 [WLExcludeClasses](http://git.56qq.com/Android-Team/WLExcludeClasses))。
- 生成插件需要宿主提供的 maven 库坐标和版本约束清单文件 `provided_dependencies_v2.txt`,并打包到 APK 的 assets 目录中。同时备份该文件到编译中间目录 `<application module>/build/intermedidates/phantom/<build variant>`
- 替换插件中的 Activity/Service/Fragment 组件的基类（原 [PhantomReplaceGradlePlugin](http://git.56qq.com/Android-Team/PhantomReplaceGradlePlugin)）
- 快速安装调试插件（原 [PhantomDebugger](http://git.56qq.com/Android-Team/PhantomDebugger)）

### 如何接入

在插件 `<application module>/build.gradle` 应用该插件，需要放到 `apply plugin: 'com.android.application'` 之后，在 `apply from: file("${rootDir}/BuildScript/aop_plugin.gradle")` 之前（如果存在）

```groovy
apply plugin: 'com.wlqq.phantom.plugin'
```

配置插件，主要包含以下三个部分

1. 剔除不需要打包到插件中的 maven 库/源码模块/具体的类或包

    ```groovy
    /**
     * 剔除指定 maven 库里面的所有类和资源，要求宿主提供版本相同的库
     *
     * @param mavenLib 指定的 maven 库坐标及版本，例如：<code>com.android.support:support-v4:25.3.1</code>
     */
    excludeLib(String mavenLib)
    /**
     * 剔除指定 maven 库里面的所有类，设置是否剔除资源，要求宿主提供版本相同的库
     *
     * @param mavenLib   指定的 maven 库坐标及版本，例如：<code>com.android.support:support-v4:25.3.1</code>
     * @param excludeRes 是否剔除资源
     */
    excludeLib(String mavenLib, boolean excludeRes)
    /**
     * 剔除指定 maven 库里面的所有类，设置是否剔除资源，设置对宿主提供库的版本要求
     *
     * @param mavenLib   指定的 maven 库坐标及版本，例如：<code>com.android.support:support-v4:25.3.1</code>
     * @param excludeRes 是否剔除资源
     * @param requrement 对宿主提供库的版本要求，两种格式：1. 具体的某个版本，如：<code>25.3.1</code> 2. 大于或等于某个版本，如：<code>>=25.3.1</code>
     */
    excludeLib(String mavenLib, boolean excludeRes, String requirement)

    // 例子：
    // 剔除指定 maven 库里面的所有类和 res 目录资源，要求宿主提供版本一致的库，即等于 25.3.1
    excludeLib 'com.android.support:support-v4:25.3.1'
    // 剔除指定 maven 库里面的所有类，但不剔除 res 目录资源，要求宿主提供版本一致的库，即等于 25.3.1
    excludeLib 'com.android.support:support-v4:25.3.1', false
    // 剔除指定 maven 库里面的所有类，但不剔除 res 目录资源，要求宿主提供等于 25.3.1 的库
    excludeLib 'com.android.support:support-v4:25.3.1', false, '25.3.1'
    // 剔除指定 maven 库里面的所有类，但不剔除 res 目录资源，要求宿主提供等于大于或等于 25.3.1 的库
    excludeLib 'com.android.support:support-v4:25.3.1', false, '>=25.3.1'

    /**
     * 剔除指定 module 里的所有类，设置是否剔除资源，以及需要保留的类或包
     *
     * @param name        module 名字
     * @param excludeRes  是否剔除资源
     * @param keepClasses 需要保留的类或包(只支持 module 源码中的类或包)
     */
    excludeModule(String name, boolean excludeRes, Iterable<String> keepClasses)
    /**
     * 剔除指定 module 里的所有类，设置是否剔除资源
     *
     * @param name       module 名字
     * @param excludeRes 是否剔除资源
     */
    excludeModule(String name, boolean excludeRes)
    /**
     * 剔除指定 module 里的所有类和资源
     *
     * @param name module 名字
     */
    excludeModule(String name)

    // 例子：
    // 剔除 WLUtils 模块中的类以及 WLUtils/libs 目录里面的 jar 包中的类以及资源
    excludeModule 'WLUtils'
    // 剔除 Map 模块中的类，不剔除 Map 模块的资源。如果依赖于 Map 的模块直接引用了 Map 里面的资源，就不能剔除 Map 的资源，建议开发模块时尽量不要出现资源交叉引用情况
    excludeModule 'Map', false
    // 剔除 Map 模块时，也可以将其中一些类保留下来，比如下面的例子
    // MapView 和 PoiSearch 类(包括它们的内部类)以及 utils 包下的所有类都会被保留下来，该配置对第三方 jar 中的类无效
    excludeModule 'Map', false, ['com.wlqq.mapapi.map.MapView.class', 'com.wlqq.mapapi.search.PoiSearch.class', 'com.wlqq.utils']

    /**
     * 剔除指定的类或包
     *
     * @param name 要剔除的类名或包名，比如: <code>android.support.v4.R.class</code> 或 <code>android.support.v4</code>
     */
    public void excludeClass(String name)

    // 例子：
    // 剔除 android.support.v4 包中所有类
    excludeClass 'android.support.v4'
    // 剔除类 android.support.v4.R
    excludeClass 'android.support.v4.R.class'

    /**
     * 生成因剔除代码而需要的额外 proguard 配置文件
     *
     * @param proguard 配置文件
     */
    libraryJarsProguardFile(File libraryJarsProguardFile)
    ````

2. 生成插件需要宿主提供的 maven 库坐标和版本约束清单文件

    ```groovy
    /**
     * 是否生成插件需要宿主提供的 maven 库坐标和版本约束清单文件 provided_dependencies_v2.txt
     *
     * @param enabled 是否开启（默认 true）
     */
    genProvidedDeps(boolean enabled)
    ```

3. 快速安装插件到宿主

    ```groovy
    // 宿主包名
    phantomPluginConfig.hostApplicationId = "com.wlqq"
    // 宿主 launcher Activity full class name
    phantomPluginConfig.hostAppLauncherActivity = "com.wlqq.activity.HomeActivity"
    // 插件包名
    phantomPluginConfig.pluginApplicationId = android.defaultConfig.applicationId
    // 插件版本号
    phantomPluginConfig.pluginVersionName = android.defaultConfig.versionName
    ```

示例代码

```groovy
phantomPluginConfig {
    //=== BEGIN: 剔除不需要打包到插件中的 maven 库/源码模块/具体的类或包 ===
    // 剔除 support-v4 及其传递依赖的其它 maven 库
    excludeLib "com.android.support:support-v4:${ANDROID_SUPPORT_LIB_VERSION}", true, ">=${MIN_ANDROID_SUPPORT_LIB_VERSION}"
    excludeLib "com.android.support:support-fragment:${ANDROID_SUPPORT_LIB_VERSION}", true, ">=${MIN_ANDROID_SUPPORT_LIB_VERSION}"
    excludeLib "com.android.support:support-media-compat:${ANDROID_SUPPORT_LIB_VERSION}", true, ">=${MIN_ANDROID_SUPPORT_LIB_VERSION}"
    excludeLib "com.android.support:support-core-utils:${ANDROID_SUPPORT_LIB_VERSION}", true, ">=${MIN_ANDROID_SUPPORT_LIB_VERSION}"
    excludeLib "com.android.support:support-core-ui:${ANDROID_SUPPORT_LIB_VERSION}", true, ">=${MIN_ANDROID_SUPPORT_LIB_VERSION}"
    excludeLib "com.android.support:support-compat:${ANDROID_SUPPORT_LIB_VERSION}", true, ">=${MIN_ANDROID_SUPPORT_LIB_VERSION}"
    excludeLib "com.android.support:support-annotations:${ANDROID_SUPPORT_LIB_VERSION}", true, ">=${MIN_ANDROID_SUPPORT_LIB_VERSION}"
    excludeLib "com.android.support:support-vector-drawable:${ANDROID_SUPPORT_LIB_VERSION}", true, ">=${MIN_ANDROID_SUPPORT_LIB_VERSION}"
    excludeLib "com.android.support:animated-vector-drawable:${ANDROID_SUPPORT_LIB_VERSION}", true, ">=${MIN_ANDROID_SUPPORT_LIB_VERSION}"
    excludeLib "com.google.code.gson:gson:${GSON_VERSION}", true, ">=${MIN_GSON_VERSION}"
    excludeLib "org.apache.httpcomponents:httpmime:${HTTP_MIME_VERSION}", true, ">=${MIN_HTTP_MIME_VERSION}"
    excludeLib "com.wlqq.android:ImageLoader:${IMAGE_LOADER_VERSION}", true, ">=${MIN_IMAGE_LOADER_VERSION}"

    excludeModule("PluginCardView")

    libraryJarsProguardFile file('proguard-wl-exclude.pro')
    //=== END: 剔除不需要打包到插件中的 maven 库/源码模块/具体的类或包 ===

    // 禁用自动生成 provided_dependencies_v2.txt
    genProvidedDeps false   // 默认 true

    //=== BEGIN: 快速安装插件
    // 宿主包名
    phantomPluginConfig.hostApplicationId = "com.wlqq"
    // 宿主 launcher Activity full class name
    phantomPluginConfig.hostAppLauncherActivity = "com.wlqq.activity.HomeActivity"
    // 插件包名
    phantomPluginConfig.pluginApplicationId = android.defaultConfig.applicationId
    // 插件版本号
    phantomPluginConfig.pluginVersionName = android.defaultConfig.versionName
    //=== END: 快速安装插件
}
```

### 编译插件

```bash
./gradlew assemble<build variant>
```

#### 编译并安装插件到宿主

```bash
./gradlew phInstallPlugin<build variant>
```

### 常见问题

1. Q: 使用 `excludeLib` 剔除 maven 库没有生效

   升级到 Gradle 4.4 + AGP 3.1.4 之后，为了更精确的控制剔除的库，
不再支持剔除 maven 库的传递依赖库。需要显式剔除传递依赖库。可以使用网站 [Maven Repository](https://mvnrepository.com/) 查询 maven 库的依赖库

2. Q: 希望手动声明需要宿主提供的 maven 库及版本

   1. 手动在插件 `<application module>/src/main/assets` 目录中添加 `provided_dependencies_v2.txt` 文件，添加需要宿主提供的 maven  库及版本约束
   2. 在 `phantomPluginConfig` 配置块中设置 `genProvidedDeps = false` 禁用自动生成依赖描述信息功能

3. Q: 快速安装插件到宿主没有生效

   1. 检查插件是否编译成功；
   2. 检查插件是否安装成功(logcat 日志过滤关键字 `/Phantom:`)
