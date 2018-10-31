# Phantom 通信服务

## 什么是 Phantom 通信服务？

Phantom 通信服务（简称：Phantom Service）是由 Phantom 开源框架为宿主程序与插件程序之间能进行交互提供的通信服务；采用了代理、反射机制，宿主程序和插件程序根据约束规则定义对外接口供外部调用。

## Phantom Service 使用

在 Phantom Service 的使用过程中，可分如下三步：

### 定义 Phantom Service：

定义 Phantom Service 时，需要做的就是创建一个 `class` 类，并定义供外部调用接口。

* 定义 Phantom Service 类，在 `class` 头部添加注解 `@PhantomService(name = "${serviceName}", version = ${serviceVersion})`，则该 `class` 将被标识为一个 Phantom Service。
  * 注解参数 `${serviceName}`：该参数支持 `String` 类型，由 `'CATEGORY'`（类别名，通常以包名命名）和 `'NAME'`（服务名称，通常以类文件名命名）两部分组成，以 `'/'` 隔开；例如：`'com.wlqq.phantom.plugin.view/ViewProviderService'`，该参数值最终作为 key 来标识 Phantom Service 对象并注册，外部调用方在调用时将使用该 key 来获取 Phantom Service 的代理实例对象。
  * 注解参数 `${serviceVersion}`：该参数支持 `int` 类型，代表当前 Phantom Service 版本号，通常在新增接口并发布时递增（例如：v1 时仅支持 1 个接口，v2 时支持多个接口）。

* 定义 Phantom Service 接口，在定义 Phantom Service 接口时需要在其函数体头部添加注解 `@RemoteMethod(name = "${methodName}")`，则该接口将被标识为可被外部调用。
  * 注解参数 `${methodName}`：该参数支持 `String` 类型，代表该函数接口名称，通常与函数名相同，例如：`'@RemoteMethod(name = "getPluginView")'`。
  * 接口参数：支持传参，例如：`getPluginView(Context context)`。
  * 接口返回值：支持返回值。

* 示例：定义 Phantom Service 类 `ViewProviderService.java`，代码如下：

    ```java
    @PhantomService(name = BuildConfig.APPLICATION_ID + "/ViewProviderService", version = 1)
    public class ViewProviderService {
        /**
         * @param context 宿主传递过来的 {@link Context}
         * @return 插件提供的 View
         *
         * @since 1
         */
        @RemoteMethod(name = "getPluginView")
        public View getPluginView(final Context context) {
            return new PluginView(context);
        }

        /**
         * @param context 宿主传递过来的 {@link Context}
         * @return 插件提供的 Fragment
         *
         * @since 1
         */
        @RemoteMethod(name = "getPluginFragment")
        public Fragment getPluginFragment(Context context) {
            return new PluginFragment(context);
        }
    }
    ```
    > 注：  
    > 1. 如代码所示，创建 `View` 和 `Fragment` 类型控件时需要传递宿主封装后的 `PluginContext` 类型的 `Context` 对象，见本文『Phantom Service 调用』。  
    > 2. 具体可参考 Sample 源代码 [Plugin-View](../phantom-sample/plugin-view)

### Phantom Service 注册：

Phantom Service 被注册后才能被外部调用，注册一个 Phantom Service 有两种方案：

* 方案一、手动注册，可用于宿主程序和插件程序中；需要在其所在的项目中调用 `PhantomServiceManager.registerService(..)` 接口进行，注册接口参数为 Phantom Service 的实例对象。建议在项目的 `Application` 的 `onCreate()` 中进行，示例代码如下：

    ```java
    public class PluginIPCApplication extends Application {

        @Override
        public void onCreate() {
            super.onCreate();
            PhantomServiceManager.registerService(new PluginIpcService());
        }
    }
    ```

* 方案二、宿主初始化时注册，只能在宿主程序中使用；宿主初始化 Phantom 框架时，可以通过 `PhantomCore.Config` 中 `addPhantomService(..)` 接口注册 Phantom Service，参数是一个 Phantom Service，代码如下：

    ```java
    PhantomCore.getInstance().init(this, new PhantomCore.Config().addPhantomService(new HostIpcService()));
    ```

### Phantom Service 调用：

Phantom Service 的调用，可简单分两步：

* 获取 Phantom Service 代理：通过 Phantom Service 注解 `@PhantomService(name = "${serviceName}"，version = ${serviceVersion})` 中的 `${serviceName}` 值作为 key，调用 `PhantomServiceManager` 中的 `getService(${key})` 可获取其 Phantom Service 的代理 `IService`。

* 通过 `IService` 执行代理事件：通过上一步骤中获取的代理对象 `IService`，调用其 `call(...)` 即可远程调用 Phantom Service 中接口，`call(...)` 接口中：
  * 参数 1：`String` 类型，该参数是待调用 Phantom Service 中的接口中注解 `@RemoteMethod(name = "getPluginView")` 中的配置值（此处为 `getPluginView`），也是需要执行的对应的接口函数名称。  
  * 参数 2：`Object...` 类型， 该参数是一个可变长参数，是 Phantom Service 中需要执行的接口所需要的参数。
  * 返回值：返回 `Object` 类型。
* 宿主获取插件中 View 示例，代码在宿主 `EmbedPluginViewActivity.java` 中执行，代码如下:

    ```java
    /**
     * 宿主获取 Plugin-View 插件中 View
     */
    private View getPluginView() {
        // 插件 Phantom Service 的 'CATEGORY'
        String pluginPackageName = "com.wlqq.phantom.plugin.view";
        // 插件 Phantom Service 的 'NAME'
        String serviceName = "ViewProviderService";
        PluginInfo pluginInfo = PhantomCore.getInstance().findPluginInfoByPackageName(pluginPackageName);

        if (pluginInfo == null) {
            Toast.makeText(this, "Please install and start plugin first: " + pluginPackageName , Toast.LENGTH_SHORT).show();
            return null;
        }
        Context pluginContext = new PluginContext(this, pluginInfo).createContext();

        // 插件 Phantom Service 代理对象
        IService service = PhantomServiceManager.getService(pluginPackageName, serviceName);
        if (service == null) {
            Toast.makeText(this, "Service not exist: " + serviceName, Toast.LENGTH_SHORT).show();
            return null;
        }

        // 待调用的插件 Phantom Service 对应接口名称
        String methodName = "getPluginView";
        try {
            // 通过反射调用插件 Phantom Service 对应接口
            View view = (View) service.call(methodName, pluginContext);
            return view;
        } catch (MethodNotFoundException e) {
            Toast.makeText(this, "Method not exist: " + methodName, Toast.LENGTH_SHORT).show();
        }
        return null;
    }
    ```
    > 注：详细参考 Sample 源代码 [Host](../phantom-sample/host)

## 混淆配置

### 宿主

宿主依赖的 `phantom-host-lib` 已配置相应的 `consumerProguardFiles`，因此不需要额外的配置。

### 插件

在插件的 `proguard` 配置中加入代码

```
# Phantom Service method
-keepclassmembers class * {
    @com.wlqq.phantom.communication.RemoteMethod <methods>;
}
```
