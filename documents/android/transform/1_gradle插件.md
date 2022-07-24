## 1、Gradle Plugins简介 

Gradle插件打包了可重用的构建逻辑，可以在不同的项目中使用。

可以使用多种语言来实现Gradle插件，其实只要最终被编译为JVM字节码的都可以，常用的有Groovy、Java、Kotlin。

通常，使用 Java/Kotlin（静态类型语言）实现的插件比使用 Groovy(动态类型语言) 实施的插件性能更好。

android官方提供了很多可用的 gradle 插件，比如:
- apply plugin: 'com.android.application'
- apply plugin: 'com.android.library'

## 2、实现一个 Gradle 插件

本文 demo 基于 gradle 7.3.3 开发

### 2.1 新建一个 `anndroid` 项目 `TransformDemo` 
### 2.2 新建一个 `Java or Kotlin Library` 名字任意，如 `gradleplugin` 
并配置 gradleplugin 的 build.gradle 

```groove
plugins {
    id 'java-library'
    id 'java-gradle-plugin'
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
            implementationClass = 'com.gas.gradleplugin.CustomPlugin'
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


## 参考

- [Developing Custom Gradle Plugins](https://docs.gradle.org/current/userguide/custom_plugins.html)
- [Maven Publish Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:tasks)
