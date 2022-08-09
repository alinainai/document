## 1、Gradle 构建生命周期

- 1.初始化阶段: 执行 settings.gradle，确定主 project 和⼦ project 
- 2.配置阶段: 执⾏每个 project 的 bulid.gradle（将每个 build.gradle 文件实例化为一个 Gradle 的 project 对象），确定出所有 task 所组成的有向无环图
- 3.执行阶段: 按照上一阶段所确定出的有向⽆环图来执行指定的 task

一张完整的 Gradle 生命周期图

<img width="400" alt="Gradle 构建生命周期" src="https://user-images.githubusercontent.com/17560388/183627888-cba4352d-c45b-41c8-8b42-cbc77688b37b.png">


注意：Gradle 执行脚本文件的时候会生成对应的实例，主要有如下三种对象：

- 1、Gradle 对象：在项目初始化时构建，全局单例存在，只有这一个对象
- 2、Project 对象：每一个 build.gradle 都会转换成一个 Project 对象
- 3、Settings 对象：Seetings.gradle 会转变成一个 Seetings 对象

Gradle 在各个阶段都提供了生命周期回调，在添加监听器的时候需要注意：监听器要在生命周期回调之前添加，否则会导致有些回调收不到

### 1.1 Gradle 初始化阶段

在 settings.gradle 执行完后，会回调 Gradle 对象的 settingsEvaluated 方法
在构建所有工程 build.gradle 对应的 Project 对象后，也就是初始化阶段完毕，会回调 Gradle 对象的 projectsLoaded 方法

### 1.2 Gradle 配置阶段：

Gradle 会循环执行每个工程的 build.gradle 脚本文件

- 在执行当前工程 build.gradle 前，会回调 Gradle 对象的 beforeProject 方法和当前 Project 对象的 beforeEvaluate 方法
- 在执行当前工程 build.gradle 后，会回调 Gradle 对象的 afterProject 方法和当前 Project 对象的 afterEvaluate 方法
- 在所有工程的 build.gradle 执行完毕后，会回调 Gradle 对象的 projectsEvaluated 方法
- 在构建 Task 依赖有向无环图后，也就是配置阶段完毕，会回调 TaskExecutionGraph 对象的 whenReady 方法

注意： Gradle 对象的 beforeProject，afterProject 方法和 Project 对象的 beforeEvaluate ，afterEvaluate 方法回调时机是一致的，区别在于：

- 1、Gradle 对象的 beforeProject，afterProject 方法针对项目下的所有工程，即每个工程的 build.gradle 执行前后都会收到这两个方法的回调
- 2、Project 对象的 beforeEvaluate ，afterEvaluate 方法针对当前工程，即当前工程的 build.gradle 执行前后会收到这两个方法的回调

### 1.3 执行阶段：

Gradle 会循环执行 Task 及其依赖的 Task
- 在当前 Task 执行之前，会回调 TaskExecutionGraph 对象的 beforeTask 方法
- 在当前 Task 执行之后，会回调 TaskExecutionGraph 对象的 afterTask 方法

### 1.4 当所有的 Task 执行完毕后，会回调 Gradle 对象的 buildFinish 方法

### 1.5 打印 Gradle 构建各个阶段及各个任务的耗时
在 settings.gradle 添加如下代码：
```groovy
//初始化阶段开始时间
long beginOfSetting = System.currentTimeMillis()
//配置阶段开始时间
def beginOfConfig
//配置阶段是否开始了，只执行一次
def configHasBegin = false
//存放每个 build.gradle 执行之前的时间
def beginOfProjectConfig = new HashMap()
//执行阶段开始时间
def beginOfTaskExecute
//初始化阶段执行完毕
gradle.projectsLoaded {
    println "初始化总耗时 ${System.currentTimeMillis() - beginOfSetting} ms"
}

//build.gradle 执行前
gradle.beforeProject {Project project ->
    if(!configHasBegin){
        configHasBegin = true
        beginOfConfig = System.currentTimeMillis()
    }
    beginOfProjectConfig.put(project,System.currentTimeMillis())
}

//build.gradle 执行后
gradle.afterProject {Project project ->
    def begin = beginOfProjectConfig.get(project)
    println "配置阶段，$project 耗时：${System.currentTimeMillis() - begin} ms"
}

//配置阶段完毕
gradle.taskGraph.whenReady {
    println "配置阶段总耗时：${System.currentTimeMillis() - beginOfConfig} ms"
    beginOfTaskExecute = System.currentTimeMillis()
}

//执行阶段
gradle.taskGraph.beforeTask {Task task ->
    task.doFirst {
        task.ext.beginOfTask = System.currentTimeMillis()
    }

    task.doLast {
        println "执行阶段，$task 耗时：${System.currentTimeMillis() - task.ext.beginOfTask} ms"
    }
}

//执行阶段完毕
gradle.buildFinished {
    println "执行阶段总耗时：${System.currentTimeMillis() - beginOfTaskExecute}"
}
```
日志
```shell
//执行 Gradle 命令
./gradlew clean

//打印结果如下：
初始化总耗时 140 ms

> Configure project :
配置阶段，root project 'GradleDemo' 耗时：1181 ms

> Configure project :app
配置阶段，project ':app' 耗时：1122 ms
配置阶段总耗时：2735 ms

> Task :clean
执行阶段，task ':clean' 耗时：0 ms

> Task :app:clean
执行阶段，task ':app:clean' 耗时：1 ms
执行阶段总耗时：325
```

## 2、Project 和 Tasks

每次构建（build）至少由一个 project 构成，一个 project 由一到多个 task 构成。
项目结构中的每个 build.gradle 文件代表一个 project ，在这些编译脚本文件中可以定义一系列的task；

### 2.1 Project API


task 本质上又是由一组被顺序执行的 Action 对象构成，Action其实是一段代码块，类似于Java中的方法。

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

使用 gradlew 执行命令的时候，就是执行 gradle-wrapper 的指令。每个 gradle 版本有差异性，gradle-wrapper 对 gradle 进行封装。

```shell
./gradlew taskName1 taskName2
```
### 1.3 doFirst() doLast() 和普通代码段的区别:

普通代码段: 在 task 创建过程中就会被执行，发在 configuration 阶段

doFirst() 和 doLast(): 在 task 执⾏过程中被执行，发⽣在 execution 阶段。如果⽤用户没有直接或间接执⾏ task，那么它的 doLast() doFirst() 代码不会被执行，其中 doFirst() 是往队列的前⾯插入代码，doLast() 是往队列的后面插⼊代码

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

## 参考：
- [Android Gradle 自定义Task 详解](https://blog.csdn.net/zhaoyanjun6/article/details/76408024)
- [Android Gradle使用总结](https://blog.csdn.net/zhaoyanjun6/article/details/77678577)
- [Gradle 使用指南 -- Gradle 生命周期](https://www.heqiangfly.com/2016/03/18/development-tool-gradle-lifecycle/)
- [Gradle execute command lines in custom task](https://stackoverflow.com/questions/38250735/gradle-execute-command-lines-in-custom-task)
