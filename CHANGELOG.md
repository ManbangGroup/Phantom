
# v3.1.2 @2019-01-14

## Features

- 兼容 Gradle 4.4 + Android Gradle Plugin 3.0.x/3.1.x

## Behavior Changes

- Gradle 4.x + Android Gradle Plugin 3.x
  - 编译插件使用的 phantom-plugin-gradle gradle 插件提供的剔除公共库功能 `excludeLib` **不会**自动剔除指定库依赖的其它库，需要单独配
    置。详见 [phantom-sample/plugin-component/build.gradle](phantom-sample/plugin-component/build.gradle)中对 `support-v4`
    库及其依赖库的 `excludeLib` 配置
- Gradle 3.3 + Android Gradle Plugin 2.3.3
  - 编译插件使用的 phantom-plugin-gradle gradle 插件提供的剔除公共库功能 `excludeLib` **会**自动将指定库及其依赖的其它库全部剔除。

## Build Changes

- 升级到 Gradle 4.4 + Android Gradle Plugin 3.1.4

# v3.0.0 @2018-10-22

初始版本发布，详细说明见 [README](README.md)