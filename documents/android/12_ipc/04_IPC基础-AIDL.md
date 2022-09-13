## 1、AIDL 简单介绍

AIDL 的全称是 Android 接口自定义语言，和其他接口语言 (IDL) 类似。利用它定义客户端与服务均认可的编程接口，以便二者使用进程间通信 (IPC) 进行相互通信。

简单点说 AIDL 就是 Android 提供的一种方便定义 IPC 的技术。SDK 会将 .aidl 文件编译为 .java 文件，我们将在下面的分析中结合例子讲解一下 .java 文件中的方法。

AIDL 技术也是基于 Binder 技术实现的。

## 2、使用

我们新建一个 Android项目，然后 new 一个 .aidl 文件。我们参考 《Android 开发艺术探索》的例子，在我们的 demo 中也实现一套。


## 参考

- [你真的理解AIDL中的in，out，inout么？](https://www.jianshu.com/p/ddbb40c7a251)
- [Android进程间通信 深入浅出AIDL](https://zhuanlan.zhihu.com/p/338093696)
- [AIDL实现的音乐播放器](https://github.com/naman14/Timber)


