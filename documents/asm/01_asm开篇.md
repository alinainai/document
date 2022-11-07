## 一、ASM 是什么

ASM 是一个通用的 Java 字节码操作和分析框架，它可以用来修改现有的类或直接以二进制形式动态生成类。

ASM所处理对象是字节码数据，并可以对字节码:

- 分析（analysis）: find potential bugs、detect unused code、reverse engineer code
- 生成（generation）: 从无到有创建新的Class
- 转换（transformation）: optimize programs、obfuscate programs、insert perfomance monitoring code

官方文档: [https://asm.ow2.io/](https://asm.ow2.io/)

可以通过 [https://gitlab.ow2.org/asm/asm.git](https://gitlab.ow2.org/asm/asm.git) 下载 asm 代码

IDEA 相关插件 `ASM Bytecode Outline` （AS不能使用）,可以将 Java 代码转成 ASM 使用的字节码来帮助开发者。

JDK 中的使用到的 ASM : Java 8 Lambda 技术

## 二、ASM API

ASM库提供了两个用于生成和转换已编译类的API，

- Core API: 以基于事件的形式来表示类，包括 `asm.jar`、`asm-util.jar`和`asm-commons.jar`；
- Tree API: 以基于对象的形式来表示类，包括 `asm-tree.jar` 和` asm-analysis.jar`

基于事件的 API 要快于基于对象的 API，所需要的内存也较少，但在使用基于事件的 API 时，类转换的实现可能要更难一些。基于对象的 API 会把整个类加载到内存中

### 1、asm.jar中的核心类

- `ClassReader`: 对具体的 class 文件进行读取与解析；
- `ClassVisitor`: ClassReader 解析 class 文件过程中，解析到某个结构就会通知到 ClassVisitor 内部的相应方法；
- `ClassWriter`: 将修改后的 class 文件通过文件流的方式覆盖掉原来的 class 文件，从而实现 class 修改；



## 四、ASM 应用
我们以一个简单的例子先来使用 ASM 并结合 gradle 插件 来添加 Activity 生命周期的日志。

一、配置相关参数
首先在 build.gradle 中添加对 ASM 的依赖


## 参考

- [Java ASM系列一：Core API](https://lsieun.github.io/java/asm/java-asm-season-01.html)
- [ASM 6 开发者指南](https://github.com/dengshiwei/asm-module/blob/master/doc/ASM6%20%E5%BC%80%E5%8F%91%E8%80%85%E6%8C%87%E5%8D%97/ASM%206%20%E5%BC%80%E5%8F%91%E8%80%85%E6%8C%87%E5%8D%97.md)
- [ASM入门篇](https://segmentfault.com/a/1190000040160637)
- [深入理解Transform](https://juejin.cn/post/6844903829671002126#heading-11)
- [Android ASM快速入门](https://www.jianshu.com/p/d5333660e312)
- [ASM框架学习(二)-ClassVisitor](https://www.jianshu.com/p/dcc9ffcf9c8e)
