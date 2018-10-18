# phantom-sample

示例工程，帮助开发者快速将 Phantom 接入到已有项目。

## 工程目录

* **host**：宿主工程，从插件中获取 View/Fragment ，并嵌入宿主
* **plugin-component**：插件工程 —— Android 四大组件中 Activity、Service、Broadcast 使用
* **plugin-view**：插件工程 —— View 使用，Toast、Notification、WebView 使用，宿主与插件 View 通信

## 如何编译

### 编译插件

```
./gradlew phantom-sample:plugin-component:assembleDebug
./gradlew phantom-sample:plugin-view:assembleDebug
```

### 编译 宿主

```
./gradlew phantom-sample:host:assembleDebug
```