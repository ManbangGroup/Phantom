# PhantomCore 初始化配置

在宿主程序的 `Application#onCreate()` 方法中调用 `PhantomCore#init(...)` 方法进行初始化

```java
public class PhantomApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PhantomCore.getInstance().init(this, new PhantomCore.Config());
    }
}
```

**示例代码**：

* [Host](../phantom-sample/host)

## PhantomCore.Config 配置参数说明

* `setDebug(..)`

    ```java
    /**
      * 设置调试模式：
      *   在调试模式下，插件生命周期方法调用中的异常不会被捕获，应用会崩溃。
      *   支持插件快速部署到宿主。
      *
      * 默认 false
      *
      * @param debug 是否开启调试模式
      * @return 该配置对象
      */
    ```

* `setLogLevel(..)`

    ```java
    /**
      * 设置日志级别；默认 {@link android.util.Log#WARN}。
      *
      * @param int：logLevel 见 {@link android.util.Log}
      * @return 该配置对象
      */
    ```

* `setCheckVersion(..)`

    ```java
    /**
      * 安装插件时是否检查版本号；若为 true，则仅支持升级安装；默认 true
      *
      * @param boolean：checkVersion 是否检查版本号
      * @return 该配置对象
      */
    ```

* `setTurboDexEnabled(..)`

    ```java
    /**
      * 是否优化首次加载插件速度；若为 true, 首次加载插件禁用 dex2oat ，而以解释执行的方式运行
      * NOTE：由于是以 hack 的方式实现，可能在部分设备上存在兼容性问题
      *
      * 默认 true
      *
      * @param boolean：enabled 是否启用快速加载插件
      * @return 该配置对象
      */
    ```

* `setLogReport(..)`

    ```java
    /**
      * 设置数据统计上报类对象
      *
      * @param ILogReport：logReport 数据统计上报类对象
      * @return 该配置对象
      */
    ```

* `setPreloadAsync(..)`

    ```java
    /**
      * 设置是否在 SDK 初始化时 异步执行 解析已安装的插件 apk 信息
      * true - 异步加载，SDK 初始化时间会减小，但后续首次调用插件管理的 API (安装/卸载/查询) 可能会 block (毫秒级)
      * false - 同步加载，SDK 初始化时间会增大（随已安装的插件 apk 数量线性增长），不会影响后续调用插件管理的 API (安装/卸载/查询)
      *
      * @param boolean：preloadAsync 是否异步加载，默认为 true
      * @return 该配置对象
      */
    ```

* `setCheckSignature(..)`

    ```java
    /**
      * 安装插件时是否校验签名；若为 true, 则插件与宿主签名一致才能安装；默认 true
      *
      * @param boolean：checkSignature 是否校验签名
      * @return 该配置对象
      */
    ```

* `addTrustedSignatures(..)`

    ```java
    /**
      * 添加信任的插件签名 MD5 列表。插件安装时，检查插件安装包签名 MD5 是否在信任的列表中。若不在，安装插件会失败。
      *
      * @param String：signatures 信任的插件签名 MD5 列表
      * @return 该配置对象
      * @see InstallResult#ERR_SIGNATURE_MISMATCH
      */
    ```

* `addPhantomService(..)`

    ```java
    /**
      * 添加宿主提供的供插件调用的服务对象，服务对象类必须使用 {@link PhantomService} 注解
      *
      * @param Object：phantomService 服务对象
      * @return 该配置对象
      */
    ```

* `addPhantomServiceIndex(..)`

    ```java
    /**
      * 添加宿主提供的供插件调用的服务索引
      *
      * @param PhantomServiceIndex：服务索引
      * @return 该配置对象
      */
    ```

* `setPhantomEventCallback(..)`

    ```java
    /**
      * 设置 Phantom 事件通知回调
      *
      * @param PhantomEventCallback：Phantom 事件通知回调
      * @return 该配置对象
      */
    ```
