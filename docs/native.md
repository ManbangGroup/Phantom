# Native 支持

宿主加载插件中 Native，Phantom 框架自动集成。

## 插件 Native 开发

* 插件中独立开发 Native 相关代码，无需特殊处理。

* 插件中独立 `System.loadLibrary(..)` 加载，无需特殊处理。代码如下：

  ```java
  static {
       System.loadLibrary("pluginLibrary");
  }
  ```

## 宿主 Native 加载

 插件启动时，宿主将自动集成并加载插件中的 Native 相关代码，无需手动处理。