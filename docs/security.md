# Phantom 安全签名校验

## 为什么要签名校验

由于是插件化框架，需要动态加载第三方 apk，因此，为了保证第三方 apk 软件的可信赖性，将进行对引入的第三方 apk 软件进行安全与合法检测。检测分两个方面，第一是检测第三方 apk 是否经过签名认证，第二是限定宿主程序信任的某些证书的 apk 软件。

## 安全签名校验

在 Phantom 框架中，想要插件 apk 能够被宿主安装，有如下三种方式：

### 增加插件到信任列表（推荐）：

* 插件的签名在宿主声明的可信签名列表中，宿主在安装插件时会认为该插件是安全的插件，则校验通过并安装；方法是在初始化 `Phantom` 服务时调用 `PhantomCore.Config` 中的 `setCheckSignature(true)` 并调用 `addTrustedSignatures(...)` 函数，该函数支持 `String` 类型可变长参数，传递插件签名时的 `keystore` 文件的 `signature`。代码如下：

    ```java
    PhantomCore.getInstance().init(this, new PhantomCore.Config()
                    .setCheckSignature(true)
                    .addTrustedSignatures("A489D17B23B2861CE7344264BBF83D5F"));
    ```

### 插件和宿主的签名一致（推荐）：

* 推荐使用，此方法不需要配置相关内容，宿主与插件签名匹配，在企业内部多个产品时较为常用。

### 宿主不校验插件的签名（不推荐）：

* 此方法在安装插件时跳过签名校验步骤，在初始化 `Phantom` 服务时调用 `PhantomCore.Config` 中的 `setCheckSignature(false)`，参数传递 `false` 即可，代码如下：

    ```java
    PhantomCore.getInstance().init(this, new PhantomCore.Config()
                    .setCheckSignature(false));
    ```
    ***注：***不推荐使用，因为它存在一定的风险性
