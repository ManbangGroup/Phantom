# 插件安装、升级、卸载 与 启动

## 插件安装

### 安装方法：

* 插件安装时调用 `PhantomCore` 中的 `installPlugin(..)` 函数即可安装一个插件，参数为一个 apk 文件绝对路径。代码如下：

    ```java
    PhantomCore.getInstance().installPlugin("/mnt/sdcard/plugins/com.wlqq.phantom.plugin.view_1.0.0.apk")
    ```

* 同时支持从 `assets` 目录安装，代码如下：：

    ```java
    PhantomCore.getInstance().installPluginFromAssets("plugins/com.wlqq.phantom.plugin.view_1.0.0.apk")
    ```

### 安装结果：

安装完插件后无论是成功或者失败，都会返回一个结果对象 `InstallResult`，其中 `status` 为状态码，`message` 为描述，可以进行 logcat 打印查看。

## 插件升级

* 插件升级 与 插件安装相同，调用 `PhantomCore` 中的 `installPlugin(..)` 函数即可覆盖旧版本插件，参数为一个 apk 文件绝对路径。代码如下：

    ```java
    PhantomCore.getInstance().installPlugin("/mnt/sdcard/plugins/com.wlqq.phantom.plugin.view_1.0.0.apk")
    ```

## 插件卸载

* 卸载插件方式较为简单，调用 `PhantomCore` 中的 `uninstallPlugin(..)` 函数即可安装一个插件，参数为插件的 PackageName。代码如下：

    ```java
    PhantomCore.getInstance().uninstallPlugin("com.wlqq.phantom.plugin.view")
    ```

## 启动插件

* 插件安装完之后，启动插件代表该插件将被立即加载到内存中，调用 `PluginInfo` 接口中 `start()` 即可，代码如下：

    ```java
    InstallResult installResult = PhantomCore.getInstance().installPlugin("/mnt/sdcard/plugins/com.wlqq.phantom.plugin.view_1.0.0.apk");
    PluginInfo pluginInfo = installResult.plugin;
    if (installResult.isSuccess() && pluginInfo != null) {
          pluginInfo.start();
    }
    ```
