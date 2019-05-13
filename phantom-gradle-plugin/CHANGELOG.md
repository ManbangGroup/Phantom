CHANGELOG
---------

## 3.1.3 @2019-04-28

### Bugs

* 2019-04-28 | fix(ANDROID_PHANTOM-396): 特定情况下插件依赖信息生成错误

## 3.1.1 @2019-04-03

### Bugs

* 2019-04-02 | fix(ANDROID_PHANTOM-388): 生成的 proguard-wl-exclude.pro 内容不完整

## 3.1.0 @2019-03-01

### Feature

* 2019-03-01 | feat(ANDROID_PHANTOM-380): 编译时在 build 目录下备份自动生成的依赖信息
* 2019-02-28 | feat(ANDROID_PHANTOM-379): 支持禁用自动生成插件依赖描述文件

## 3.0.1 @2019-02-26

### Bugs

* 2019-02-28 | fix(ANDROID_PHANTOM-374): 依赖库没有提前编译会导致编译失败

## 3.0.0 @2019-02-25

### Features

* 2019-01-28 | feat(ANDROID_PHANTOM-374): 将原各个插件的功能合并到一个新插件中

### Behavior Changes

#### 为了简化插件化开发，该 Gradle 插件整合了以下功能
    
- 生成宿主提供的依赖库信息(原 `BuildScript/provided_lib_properties_creator.gradle`)
- 生成插件提供的依赖库信息(原 `BuildScript/provided_lib_properties_creator.gradle`)
- 快速安装调试插件（原 PhantomDebugger）
- 剔除公共库（原 WLExcludeClasses）
- 替换插件 Activity/Service/Fragment 基类（原 PhantomReplaceGradlePlugin）

#### 新增功能

- 在 build/intermediates/phantom/debug 下生成剔除和保留的 maven 库清单

#### 测试影响

- 宿主生成其提供的 maven 库清单 `compile_dependencies.txt`
- 插件生成其剔除的 maven 库清单 `provided_dependencies_v2.txt`
- 插件编译剔除公共库插件
- 插件编译替换四大组件基类
- 插件快速安装到宿主
