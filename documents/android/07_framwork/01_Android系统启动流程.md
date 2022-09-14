Android 系统是基于 Linux 内核开发的。

这里我们大致讲一下启动流程，简单的了解下即可。

>加载BootLoader --> 初始化内核 --> 启动init进程 --> init进程fork出Zygote进程 --> Zygote进程fork出SystemServer进程

<img width="1241" alt="image" src="https://user-images.githubusercontent.com/17560388/190057191-880c3ff0-5695-45ef-8de4-75f0fb10374a.png">

1.系统的启动过程

**知识点：**

- 系统中的所有进程都是由 Zygote 进程 fork 出来的
- SystemServer 进程是系统进程，很多系统服务，例如 ActivityManagerService、PackageManagerService、WindowManagerService…都是在该进程被创建后启动
- ActivityManagerServices（AMS）：是一个服务端对象，负责所有的 Activity 的生命周期，AMS 通过 Binder与Activity 通信，而 AMS 与 Zygote 之间是通过 Socket 通信
- ActivityThread：本篇的主角，UI线程/主线程，它的 main() 方法是 APP 的真正入口
- ApplicationThread：一个实现了 IBinder 接口的 ActivityThread 内部类，用于 ActivityThread 和 AMS 的所在进程间通信
- Instrumentation：可以理解为 ActivityThread 的一个工具类，在 ActivityThread 中初始化，一个进程只存在一个 Instrumentation 对象，在每个 Activity 初始化时，会通过 Activity 的 Attach 方法，将该引用传递给 Activity。Activity 所有生命周期的方法都有该类来执行。

