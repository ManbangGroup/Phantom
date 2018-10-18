# 插件 meta-data

在 Phantom 开源框架使用中，插件通常会根据需求做一些配置，例如：插件需要配置其宿主程序提供的 Phantom 框架最小版本号。

## 插件 meta-data 配置

关于插件 `meta-data` 的配置，主要为插件本身提供一些向宿主程序「显示」说明。

* `phantom.service.import.` 类型：其 `value` 值为 `int` 类型，通常用于定义 Phantom 插件程序依赖宿主程序某版本号的服务配置，代表当前插件程序需要宿主程序所提供对应或以上的版本号的该服务，具体配置代码如下：

    ```xml
    <!-- 该插件依赖宿主提供的高德地图服务（版本 2），宿主提供的高徳地图服务版本必须大于等于版本 2，该插件才能安装成功 -->
    <meta-data
            android:name="phantom.service.import.amap"
            android:value="2"/>
    ```

    > ***注：***  
    > 1. 典型示例见本文章节 `PhantomVersionService` 配置。  
    > 2. Phantom 通信服务相关内容参考 [Phantom 通信服务](./phantom-service.md)。

* `phantom.service.export.` 类型：其 `value` 值为 `int` 类型，通常用于定义 Phantom 插件程序内某服务版本号配置，代表当前插件程序提供的该版本号的服务，具体配置代码如下：

    ```xml
    <!-- 提供的高德地图服务（版本 2） -->
    <meta-data
            android:name="phantom.service.export.amap"
            android:value="2"/>
    ```
    > ***注：***Phantom 通信服务相关内容参考『[./phantom-service.md)』。

* `phantom.hidden` 类型：其 `value` 值为 `boolean` 类型，用于配置当前插件程序是否需要在插件管理页中隐藏该插件，具体配置代码如下：

    ```xml
    <!-- 该插件不需要在插件管理器中展示，声明其 phantom.hidden 属性为 true -->
    <meta-data
            android:name="phantom.hidden"
            android:value="true"/>
    ```

## `PhantomVersionService` 配置

### 插件 `PhantomVersionService` 的定义

插件程序中 `meta-data` 为 `PhantomVersionService` 的声明的内容是插件程序依赖的宿主插件 Phantom 框架的最小版本号（可以理解为插件程度为了检查宿主程序的 Phantom 框架版本而存在）。

### 插件 `PhantomVersionService` 的作用  

为保证插件兼容性，当宿主程序在安装插件时会读取插件程序依赖的 Phantom 框架版本号（即 `PhantomVersionService` 配置值），如果宿主提供的 Phantom 框架版本号 小于 插件声明其依赖的 Phantom 框架版本号，则认为宿主程序 Phantom 框架版本过低，不予安装。

### 插件 `PhantomVersionService` 配置

* 插件程序中声明依赖 Phantom 框架版本号，需要在 `AndroidManifest.xml` 中设置 `meta-data`，其中`android:name` 为 `phantom.service.import.PhantomVersionService`，`android:value` 为 `${phantom_version_code}`，代码如下：

    ```xml
    <!-- 3.x 的插件需要运行在 Phantom 3.0.0 及以上版本 -->
    <meta-data
        android:name="phantom.service.import.PhantomVersionService"
        android:value="30000"/>
    ```
