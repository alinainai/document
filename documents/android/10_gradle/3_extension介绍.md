## 1、Extension 介绍

Extension 拓展项

使用 Extension 可以为插件提供配置项，比如 `android{}` 就是 `Android Gradle Plugin` 的扩展。我们可以在 `android{}`中配置项目所依赖的SDK版本和一些其他的配置

```groovy
android {
    compileSdk 32

    defaultConfig {
        applicationId "com.egas.demo"
        minSdk 23
```

### 1.1 创建一个 Extension

我们可以借助 ExtensionContainer 来创建我们自定义的 Extension。ExtensionContainer 是管理 Extension 的一个容器，我们可以通过 ExtensionContainer 去对 Extension 进行相应的操作。

我们可以通过下面代码获取 Project 中的 ExtensionContainer 对象。

```groovy
//当前在 app 的 build.gradle 文件中
extensions //方式1
project.extensions //方式2
getExtensions() //方式3
project.getExtensions() //方式4
```
ExtensionContainer 可以通过 add/create 方法添加 Extension

我们先看一下 两种方法的 API 说明

```groovy

```


