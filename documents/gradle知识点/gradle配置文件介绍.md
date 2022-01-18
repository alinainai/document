## 1. Android 中的 Gradle 

Gradle 是 Android 项目的构建工具，目前支持 Java、Groovy、Kotlin和Scala等（还会拓展）语言。

按照 gradle 的规则(build.gradle、settings.gradle、gradle-wrapper、gradle 语法) 去构建项目。

使用 gradlew 执行命令的时候，就是执行 gradle-wrapper 的指令。每个 gradle 版本有差异性，gradle-wrapper 对 gradle 进行封装。

### 1.1 Gradle 的 Project 和 Tasks

每次构建（build）至少由一个 project 构成，一个 project 由一到多个 task 构成。

项目结构中的每个 build.gradle 文件代表一个 project ，在这些编译脚本文件中可以定义一系列的task；

task 本质上又是由一组被顺序执行的 Action 对象构成，Action其实是一段代码块，类似于Java中的方法。

闭包（Closure）相当于可以被传递的代码块

```groovy
task taskName { 
    //初始化代码，初始化话的时候会执行
    doFirst { //Closure
     //初始化的时候不会执行
    }
    doLast {
    //初始化的时候不会执行
    }
}
```
### 1.2 task的执行: 

```shell
./gradlew taskName1 taskName2 //顺序执行 taskName1 taskName2
```
### 1.3 doFirst() doLast() 和普通代码段的区别:

普通代码段:在 task 创建过程中就会被执行，发在 configuration 阶段

doFirst() 和 doLast(): 在 task 执⾏过程中被执行，发⽣在 execution 阶段。如果⽤用户没有直接或间接执⾏ task，那么它的 doLast() doFirst() 代码不会被执行

doFirst() 和 doLast() 都是 task 代码，其中 doFirst() 是往队列的前⾯插入代码，doLast() 是往队列的后面插⼊代码

### 1.4 task 的依赖:

可以使⽤ task taskA(dependsOn: b) 的形式来指定依赖。指定依赖后，task 会在⾃⼰执行前先执行⾃己依赖的 task。
```groovy
task task1 {
    doLast { println "执行task1----"}
}
task task2 {
    doLast { println "执行task2----"}
}
//task2 依赖 task1, 执行task2之前会先执行task1
task2.dependsOn task1
```

执行和日志

```shell
./gradlew task2

> Task :task1
执行task1----

> Task :task2
执行task2----
```

### 1.5 Android Gradle 构建生命周期

- 初始化阶段: 执行 settings.gradle，确定主 project 和⼦ project 
- 定义阶段: 执⾏每个 project 的 bulid.gradle（将每个build.gradle文件实例化为一个Gradle的project对象），确定出所有 task 所组成的有向无环图
- 执行阶段: 按照上一阶段所确定出的有向⽆环图来执行指定的 task

### 1.6 在阶段之间插⼊入代码:

⼀二阶段之间:
```groovy
settings.gradle 的最后 
```
⼆三阶段之间:
```groovy
afterEvaluate { 
    插⼊入代码
}
```

## 2. Android Gradle 配置
 
### 2.1 gradle 项⽬结构

- 单 project: build.gradle
- 多 project: 由 settings.gradle 配置多个 
 
查找 settings 的顺序:

1. 当前⽬录
2. 兄弟目录 master 
3. ⽗目录

### 2.2 根目录下的 build.gradle

```groovy
buildscript {//配置构建脚本所用到的代码库和依赖关系}

allprojects {//所有模块需要用到的一些公共属性}

task clean(type: Delete) {
    delete rootProject.buildDir
}
```

### 2.3 module 下的 build.gradle

android{} 是 pluagins {id 'com.android.application'} 中的方法 

常用的依赖:

compile, implementation 和 api

implementation: 不会传递依赖

compile / api: 会传递依赖; api 是 compile 的替代品，效果完全等同。

当依赖被传递时，⼆级依赖的改动会导致 0 级项⽬重新编译;
当依赖不传递时，⼆级依赖的改动不会导致 0 级项⽬重新编译


## 3. 在 build.gradle 中自定义 task

### 3.1 依赖 copy
```groovy
task copyImage(type: Copy) {
    from 'C:\\Users\\yiba_zyj\\Desktop\\gradle\\copy'
    into 'C:\\Users\\yiba_zyj\\Desktop'
    include "*.jpg"
    exclude "image1.jpg"
    rename("image2.jpg","123.jpg")
}
```
### 3.2 依赖 delete
```groovy
task deleteFile(type: Delete) {
    //删除系统桌面 delete 
    delete "path"
}
```
### 3.3 执行 shell

1. Use Gradle [Exec](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Exec.html) task type
```groovy
task fooExec(type: Exec) {
    workingDir "${buildDir}/foo"
    commandLine 'echo', 'Hello world!'
    doLast {
        println "Executed!"
    }
}
```
2. Use Gradle [Project.exec](https://docs.gradle.org/current/javadoc/org/gradle/api/Project.html#exec-groovy.lang.Closure-) method
```groovy
task execFoo {
    doLast {
        exec {
            workingDir "${buildDir}/foo"
            executable 'echo'
            args 'Hello world!'
        }
        println "Executed!"
    }
}
```

## 4. 将 task 放到单独的文件

```groovy
// ../代表根目录
apply from:"../utils.gradle"
```




参考：

[Android Gradle 自定义Task 详解](https://blog.csdn.net/zhaoyanjun6/article/details/76408024)

[Android Gradle使用总结](https://blog.csdn.net/zhaoyanjun6/article/details/77678577)

[Gradle 使用指南 -- Gradle 生命周期](https://www.heqiangfly.com/2016/03/18/development-tool-gradle-lifecycle/)

[Gradle execute command lines in custom task](https://stackoverflow.com/questions/38250735/gradle-execute-command-lines-in-custom-task)


