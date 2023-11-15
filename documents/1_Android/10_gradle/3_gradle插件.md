## 一、Gradle插件简介 

`Gradle插件`帮助我们`封装可重用的构建逻辑，方便在不同的项目中使用`。

我们可以使用不同的语言来编写`Gradle插件`，比如 Groovy、Java、Kotlin。
通常，使用 `Java/Kotlin（静态类型语言`）实现的插件比使用 `Groovy(动态类型语言)` 实施的插件性能更好。

`android`官方提供了很多可用的 gradle 插件，如:
- apply plugin: 'com.android.application'
- apply plugin: 'com.android.library'

## 二、实现一个Gradle插件

有三种方式构建插件：`单独项目`、`脚本` 和 `buildSrc` 。

### 2.1 在单独项目中创建插件

以`单独项目`的方式去构建插件，然后发布到本地 `maven_repo` 仓库，并在 app 中依赖它。我们的 demo 基于 `gradle 7.3.3` 开发。

首先在 Android 项目中新建一个 `Java or Kotlin Library` module，名字任意，如 `gradleplugin` ，并配置该 `module` 的 `build.gradle` 。

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

// 添加 DSL 依赖
dependencies {
    implementation localGroovy() // Groovy DSL
    implementation gradleApi()  // Gradle DSL
}

// 以源码的形式生成插件
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
            // gradle 插件 发布的地址
            url = "$rootDir/maven-repo"  
        }
    }
}
```
注意一下 `url = "$rootDir/maven-repo"` 是 gradle 插件发布的地址，`demo` 会将 `gradle 插件`发布到根目录的` maven-repo` 文件夹中。

### 2.2 实现 CustomPlugin

我们创建一个简单 CustomPlugin 类，该类需要继承 Plugin 类。我们在 CustomPlugin 类中添加一个打印日志的代码，如下

```kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project

class CustomPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        println("Hello CustomPlugin")
    }
}
```

在项目的 `gradle task` 列表中找到 `pulish` 并执行，将插件发布到本地。

<img width="279" alt="image" src="https://user-images.githubusercontent.com/17560388/190604721-d09c1136-3fc8-45be-9d7a-a6869d0bcf89.png">

如果 `gradle task` 列表中没有 task 任务，可以先执行一下 `File -> Sync Project with Gradle Files`

## 三、使用插件

在项目的 setting.gradle 中添加本地 maven 仓库的依赖

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
在根目录的 build.gradle 中添加 classpath 的依赖
```groovy
buildscript {
    dependencies {
        classpath "com.gas.gradleplugin:custom:1.1"
    }
}

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



## 参考

- [Developing Custom Gradle Plugins](https://docs.gradle.org/current/userguide/custom_plugins.html)
- [Maven Publish Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:tasks)
- [Gradle 系列（2）手把手带你自定义 Gradle 插件](https://juejin.cn/post/7098383560746696718#heading-13)
- [Gradle 系列 （四）、Gradle 插件实战应用](https://juejin.cn/post/6989877607126794247#heading-3)
