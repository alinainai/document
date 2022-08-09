## 1、Gradle Plugins简介 

`Gradle插件`打包了可重用的构建逻辑，方便在不同的项目中使用。我们可以使用多种语言来实现`Gradle插件`，只要最终能被编译为JVM字节码的就可以，常用的有`Groovy、Java、Kotlin`。

通常，使用 `Java/Kotlin（静态类型语言`）实现的插件比使用 `Groovy(动态类型语言)` 实施的插件性能更好。

`android`官方提供了很多可用的 gradle 插件，比如:
- apply plugin: 'com.android.application'
- apply plugin: 'com.android.library'

## 2、实现一个 Gradle 插件

本文 `demo` 基于 `gradle 7.3.3` 开发，实现方式是通过`单独的项目`去`实现 gradle 插件`并发布到本地 `maven_repo` 仓库。

### 2.1 新建一个 `anndroid` 项目 `TransformDemo` 

### 2.2 新建一个 `Java or Kotlin Library` 名字任意，如 `gradleplugin` 
并配置 gradleplugin 的 build.gradle 

```groovy
plugins {
    id 'java-library'
    id 'java-gradle-plugin' // 根据 gradlePlugin 中的配置自动生成插件的配置文件
    id 'org.jetbrains.kotlin.jvm'
    id 'com.gradle.plugin-publish' version '0.21.0'
    id 'maven-publish'
    id 'groovy'
    //dokka 生成 Java 文档的库
}

dependencies {
    implementation localGroovy() // Groovy DSL
    implementation gradleApi()  // Gradle DSL
}

task sourcesJar(type: Jar, dependsOn: classes) {
    group = 'Publications'
    description = 'Create jar of sources.'
    archiveClassifier = 'sources'
    from sourceSets.main.allSource
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

gradlePlugin {
    plugins {
        simplePlugin {
            id = 'com.gas.gradleplugin'
            implementationClass = 'com.gas.gradleplugin.CustomPlugin' // 自定义的 Plugin<Project> 
        }
    }
}

publishing {
    publications {
        maven(MavenPublication) {
            groupId = 'com.gas.gradleplugin' // 组织/公司的名称，如 com.github.bumptech.glide
            artifactId = 'custom' // 组件的名称如，glide
            version = '1.1'
            from components.java
            // 上传source，可以看到方法的注释
            artifact sourcesJar
        }
    }
    repositories {
        maven {
            url = "$rootDir/maven-repo"  
        }
    }
}
```
注意一下 `url = "$rootDir/maven-repo"` ,这个是 `gradle 插件` 发布的地址，`demo` 会将 `gradle 插件`发布到根目录的` maven-repo` 文件夹中

### 2.3 实现 CustomPlugin

```kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project

class CustomPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        println("Hello CustomPlugin")
    }
}
```

在项目的 `gradle task` 列表中执行 `pulish` task 将插件发布到本地。如果 `gradle task` 列表中没有 task 任务，可以先执行一下 `File -> Sync Project with Gradle Files`

### 2.4 在 app 添加依赖

在 setting.gradle 中添加 maven 本地仓库
```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven {
            url = uri("$rootDir/maven-repo")
        }

    }
}
```
在项目的 build.gradle 中添加 classpath 的依赖
```groovy
buildscript {
    dependencies {
        classpath "com.gas.gradleplugin:custom:1.1"
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id 'com.android.application' version '7.2.1' apply false
    id 'com.android.library' version '7.2.1' apply false
    id 'org.jetbrains.kotlin.android' version '1.6.10' apply false
    id 'org.jetbrains.kotlin.jvm' version '1.6.10' apply false
}
```
在 app 的 build.gradle 中添加插件的依赖
```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'com.gas.gradleplugin'
}
```
这样在执行 `app build` 过程的时候可以看到 `Hello CustomPlugin` 的日志

### 3.gradle插件的拓展

## 参考

- [Developing Custom Gradle Plugins](https://docs.gradle.org/current/userguide/custom_plugins.html)
- [Maven Publish Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:tasks)
