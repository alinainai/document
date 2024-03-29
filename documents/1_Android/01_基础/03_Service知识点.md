## 1.简单介绍

Service 是一种可在后台执行长时间运行操作而不提供界面的应用组件。

服务可由其他应用组件启动，而且即使用户切换到其他应用，服务仍将在后台继续运行。

此外，组件可通过绑定到服务与之进行交互，甚至是执行进程间通信 (IPC)。例如，服务可在后台处理网络事务、播放音乐，执行文件 I/O 或与内容提供程序进行交互。

### 1.1 三种不同的服务类型

前台服务执行一些用户能注意到的操作。例如，音频应用会使用前台服务来播放音频曲目。前台服务必须显示通知。即使用户停止与应用的交互，前台服务仍会继续运行。


## 参考

[服务概览](https://developer.android.com/guide/components/services?hl=zh-cn#Lifecycle)
