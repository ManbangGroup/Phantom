# Native SO 支持

## 包含 SO 的插件开发

与开发独立的包含 so 的 APK 相同，不需要特殊处理。

## 包含 SO 的插件加载

与不包含 so 的插件相同，不需要特殊处理。

## 包含 SO 的第三方库兼容性

理论上对于包含 so 的第三方库并不需要特殊处理，除非 so 的内部逻辑对集成的应用有特殊的限制（例如：校验所在应用的包名）。

已知兼容插件的包含 so 的第三方库清单：

* 百度地图
* 高德地图
* [flutter](https://github.com/flutter/flutter)
* [PLDroidPlayer](https://github.com/pili-engineering/PLDroidPlayer)
* [android-gif-drawable](https://github.com/koral--/android-gif-drawable)

> 如果你在项目中有使用包含 so 的第三方库并确认没有兼容性问题，请联系我们或发 PR，共同完善该清单。
