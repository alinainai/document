

android官方提供了很多可用的 gradle 插件，比如
- apply plugin: 'com.android.application'
- apply plugin: 'com.android.library'

## 1、Gradle Plugins简介 

Gradle插件打包了可重用的构建逻辑，可以在不同的项目中使用。

可以使用多种语言来实现Gradle插件，其实只要最终被编译为JVM字节码的都可以，常用的有Groovy、Java、Kotlin。

通常，使用 Java/Kotlin（静态类型语言）实现的插件比使用 Groovy(动态类型语言) 实施的插件性能更好。

## 2、实现一个 Gradle 插件
新建一个 `Java or Kotlin Library` 

POM（Project Object Model）项目对象模型，包含：
- groupId	组织 / 公司的名称	com.github.bumptech.glide
- artifactId	组件的名称	glide
- version	组件的版本	4.11.0
- packaging	打包的格式	aar/jar

## 参考

[Developing Custom Gradle Plugins](https://docs.gradle.org/current/userguide/custom_plugins.html)
