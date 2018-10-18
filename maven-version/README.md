# MavenVersion

用于

- 比较 Maven 库版本高低
- 判断 Maven 库版本是否满足依赖声明

## 比较 Maven 库版本高低

```java
Version version1 = new Version("27.0.0");
Version version2 = new Version("26.0.0");

Assert.assertTrue(version1.compareTo(version2) > 0);
```

## 判断 Maven 库版本是否满足依赖声明

支持的操作符 | 示例
---- | ----
等于 | 27.0.0
大于或等于 | >=27.0.0

```java
{
    Version version = new Version("27.0.0");
    Assert.assertTrue(version.satisfies("27"));
}

{
    Version version = new Version("27.1.0");
    Assert.assertTrue(version.satisfies(">=27"));
}
```

详细使用方法见单元测试 VersionTest, VersionUtilsTest

## 参考的开源项目

* [Maven](https://github.com/apache/maven)([Apache License](https://github.com/apache/maven/blob/master/LICENSE))
* [jsemver](https://github.com/zafarkhaja/jsemver)([MIT License](https://github.com/zafarkhaja/jsemver/blob/master/LICENSE))