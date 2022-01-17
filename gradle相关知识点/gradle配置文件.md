## 1.Android 的 Gradle 是啥

Gradle 是 Android 项目的构建工具，目前支持 Java、Groovy、Kotlin和Scala等语言。

按照 gradle 的规则(build.gradle、settings.gradle、gradle-wrapper、gradle 语法) 去构建项目。

### 1.1 Android Gradle 的 Project 和 Tasks

每次构建（build）至少由一个 project 构成，一个 project 由一到多个 task 构成。项目结构中的每个 build.gradle 文件代表一个 project ，在这编译脚本文件中可以定义一系列的task；

task 本质上又是由一组被顺序执行的 Action 对象构成，Action其实是一段代码块，类似于Java中的方法。

闭包相当于可以被传递的代码块

### 1.2 Android Gradle 构建生命周期
 
compile, implementation 和 api
implementation:不不会传递依赖
compile / api:会传递依赖;api 是 compile 的替代品，效果完全等同 当依赖被传递时，⼆二级依赖的改动会导致 0 级项⽬目重新编译;当依赖不不传递时，⼆二级依赖的改动 不不会导致 0 级项⽬目重新编译
项⽬目结构
单 project:build.gradle
多 project:由 settings.gradle 配置多个 查找 settings 的顺序:
1. 当前⽬目录
2. 兄弟⽬目录 master 3. ⽗父⽬目录
task
使⽤用⽅方法: ./gradlew taskName task 的结构:
   
 
task taskName { 初始化代码
doFirst { task 代码
}
doLast {
task 代码 }
}
doFirst() doLast() 和普通代码段的区别:
普通代码段:在 task 创建过程中就会被执⾏行行，发⽣生在 configuration 阶段
doFirst() 和 doLast():在 task 执⾏行行过程中被执⾏行行，发⽣生在 execution 阶段。如果⽤用户没有 直接或间接执⾏行行 task，那么它的 doLast() doFirst() 代码不不会被执⾏行行
doFirst() 和 doLast() 都是 task 代码，其中 doFirst() 是往队列列的前⾯面插⼊入代码，doLast() 是往队列列的后⾯面插⼊入代码
task 的依赖:可以使⽤用 task taskA(dependsOn: b) 的形式来指定依赖。指定依赖后，task 会在⾃自⼰己执⾏行行前先执⾏行行⾃自⼰己依赖的 task。
gradle 执⾏行行的⽣生命周期 三个阶段:
初始化阶段:执⾏行行 settings.gradle，确定主 project 和⼦子 project 定义阶段:执⾏行行每个 project 的 bulid.gradle，确定出所有 task 所组成的有向⽆无环图 执⾏行行阶段:按照上⼀一阶段所确定出的有向⽆无环图来执⾏行行指定的 task
在阶段之间插⼊入代码:
  ⼀一⼆二阶段之间:
settings.gradle 的最后 ⼆二三阶段之间:
   afterEvaluate { 插⼊入代码
}
