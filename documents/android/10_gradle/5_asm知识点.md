## 1、ASM 是什么

ASM 是一个通用的 Java 字节码操作和分析框架，它可以用来修改现有的类或直接以二进制形式动态生成类。

ASM所处理对象是字节码数据，并可以对字节码:
- 分析（analysis）: 
- 生成（generation）: 
- 转换（transformation）: 



官方文档: [https://asm.ow2.io/](https://asm.ow2.io/)

可以通过 `git clone` 下载 [https://gitlab.ow2.org/asm/asm.git](https://gitlab.ow2.org/asm/asm.git)  asm 代码

IDEA 相关插件 `ASM Bytecode Outline`

## 2、ASM API

ASM库提供了两个用于生成和转换已编译类的API，一个是核心 API，以基于事件的形式来表示类；另一个是树 API，以基于对象的形式来表示类；

- 基于事件的 API 要快于基于对象的 API，所需要的内存也较少，但在使用基于事件的 API 时，类转换的实现可能要更难一些；
- 基于对象的 API 会把整个类加载到内存中

### 2.1 核心 API

- `ClassReader`: 对具体的 class 文件进行读取与解析；
- `ClassVisitor`: ClassReader 解析 class 文件过程中，解析到某个结构就会通知到 ClassVisitor 内部的相应方法（比如：解析到方法时，就会回调 ClassVisitor.visitMethod 方法）
- `ClassWriter`: 将修改后的 class 文件通过文件流的方式覆盖掉原来的 class 文件，从而实现 class 修改

我们一般通过 ClassVisitor 的子类来实现对字节码的处理。

添加 ASM 依赖

在 asm_lifecycle_plugin 的 build.gradle 中，添加对 ASM 的依赖，如下：


## 参考

- [ASM 6 开发者指南](https://github.com/dengshiwei/asm-module/blob/master/doc/ASM6%20%E5%BC%80%E5%8F%91%E8%80%85%E6%8C%87%E5%8D%97/ASM%206%20%E5%BC%80%E5%8F%91%E8%80%85%E6%8C%87%E5%8D%97.md)
- [ASM入门篇](https://segmentfault.com/a/1190000040160637)
- [深入理解Transform](https://juejin.cn/post/6844903829671002126#heading-11)
- [Android ASM快速入门](https://www.jianshu.com/p/d5333660e312)
