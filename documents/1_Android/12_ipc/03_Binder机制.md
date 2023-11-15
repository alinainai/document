## 一、Binder 概述

Binder 是 Android 系统的一种 IPC，基于开源的 OpenBinder 实现；
Android 四大组件之间的通信依赖于 Binder 机制。应用层的各种服务如：AMS、PMS 等也基于 Binder 机制。

Android 系统是基于 Linux 内核的，Linux 已经提供了管道、消息队列、共享内存和 Socket 等 IPC 机制。为什么还要搞一套 Binder 来实现 IPC 呢？我们从三个方面来分析一下

1、从性能上来看

- Socket 作为一款通用接口，其传输效率低，开销大，主要用在跨网络的进程间通信和本机上进程间的低速通信。
- 消息队列和管道采用`存储-转发`方式，即数据先从发送方缓存区拷贝到内核开辟的缓存区中，然后再从内核缓存区拷贝到接收方缓存区，至少有两次拷贝过程。
- 共享内存虽然无需拷贝，但`控制复杂，难以使用`。Binder 只需要一次数据拷贝，性能上仅次于共享内存。

| IPC方式              | 数据拷贝次数 |
| -------------------- | ------------ |
| 共享内存             | 0            |
| Binder               | 1            |
| Socket/管道/消息队列 | 2            |

2、从稳定性上来看

Binder 基于 C/S 架构，客户端（Client）有什么需求就丢给服务端（Server）去完成，架构清晰、职责明确又相互独立，自然稳定性更好。
共享内存虽然无需拷贝，但是控制复杂，难以使用。从稳定性的角度讲，Binder 机制是优于内存共享的。

3、从安全性上来看

Android 为每个安装好的 APP 分配了自己的 UID，UID 是鉴别进程身份的重要标志。

传统的 IPC 只能由用户在数据包中填入 UID/PID，完全依赖上层协议来确保。容易被恶意程序利用。可靠的身份标识只有由 IPC 机制在内核中添加。

其次传统的 IPC 访问接入点是开放的，只要知道这些接入点的程序都可以和对端建立连接，不管怎样都无法阻止恶意程序通过猜测接收方地址获得连接。

同时 Binder 既支持实名 Binder，又支持匿名 Binder，安全性高。

**最后用一张表格来总结下 Binder 的优势：**

| 优势   | 描述                                                     |
| ------ | -------------------------------------------------------- |
| 性能   | 只需要一次数据拷贝，性能上仅次于共享内存                 |
| 稳定性 | 基于 C/S 架构，职责明确、架构清晰，因此稳定性好          |
| 安全性 | 为每个 APP 分配 UID，进程的 UID 是鉴别进程身份的重要标志 |

## 二、Binder 跨进程通信原理



### 2.1 可加载内核模块/内存映射

跨进程通信是需要内核空间做支持的。

传统的 IPC 机制如管道、Socket 都是内核的一部分，因此通过内核支持来实现进程间通信自然是没问题的。

但是 Binder 不是 Linux 系统内核的一部分，那怎么办呢？这就得益于 Linux 的可加载内核模块（Loadable Kernel Module，LKM）的机制；**模块是具有独立功能的程序，它可以被单独编译，但是不能独立运行。它在运行时被链接到内核作为内核的一部分运行**。Android 系统通过动态添加一个内核模块（也就是Binder Dirver）运行在内核空间，通过这个内核模块作为桥梁来实现通信。

Linux 中的**内存映射（mmap）**

内存映射简单的讲就是将用户空间的一块内存区域映射到内核空间。映射关系建立后，用户对这块内存区域的修改可以直接反应到内核空间；反之内核空间对这段区域的修改也能直接反应到用户空间。
内存映射能减少数据拷贝次数，实现用户空间和内核空间的高效互动。两个空间各自的修改能直接反映在映射的内存区域，从而被对方空间及时感知。也正因为如此，内存映射能够提供对进程间通信的支持。

### 2.2 Binder的实现原理

Binder 正是基于内存映射（mmap）来实现的，但是 mmap() 通常是用在有物理介质的文件系统上的。比如进程中的用户区域是不能直接和物理设备打交道的，如果想要把磁盘上的数据读取到进程的用户区域，需要两次拷贝（磁盘-->内核空间-->用户空间）；通常在这种场景下 mmap() 就能发挥作用，通过在物理介质和用户空间之间建立映射，减少数据的拷贝次数，用内存读写取代I/O读写，提高文件读取效率。而 Binder 并不存在物理介质，因此 Binder 驱动使用 mmap() 并不是为了在物理介质和用户空间之间建立映射，而是用来在内核空间创建数据接收的缓存空间。

一次完整的 Binder IPC 通信过程通常是这样：

1. 首先 Binder 驱动在内核空间创建一个`数据接收缓存区`；
2. 接着在内核空间开辟一块`内核缓存区`，建立**内核缓存区和内核中数据接收缓存区之间的映射关系**，以及**内核中数据接收缓存区和接收进程用户空间地址的映射关系**；
3. 发送方进程通过系统调用 **copy_from_user()** 将数据 copy 到内核中的内核缓存区，由于内核缓存区和接收进程的用户空间存在内存映射，因此也就相当于把数据发送到了接收进程的用户空间，这样便完成了一次进程间的通信。

如下图：

<img width="600" alt="Binder IPC 原理" src="https://user-images.githubusercontent.com/17560388/182274245-0f1547cf-1806-4748-bae1-3928a9f1d32c.png">

## 三、Binder 通信模型

一次完整的进程间通信必然至少包含两个进程，通常我们称通信的双方分别为客户端进程（Client）和服务端进程（Server），由于进程隔离机制的存在，通信双方必然需要借助 Binder 来实现。

### 3.1 Client/Server/ServiceManager/Binder驱动

前面我们介绍过，Binder 是基于 C/S 架构的。由一系列的组件组成，包括 Client、Server、ServiceManager、Binder 驱动。其中 Client、Server、Service Manager 运行在用户空间，Binder 驱动运行在内核空间。

其中 Service Manager 和 Binder 驱动由系统提供，而 Client、Server 由应用程序来实现。Client、Server 和 ServiceManager 均是通过系统调用 open、mmap 和 ioctl 来访问设备文件 /dev/binder，从而实现与 Binder 驱动的交互来间接的实现跨进程通信。

<img width="600" alt="CS驱动模型" src="https://user-images.githubusercontent.com/17560388/182274318-ac65fc47-a434-43ab-b0f6-5859caff4afa.png">

1、Binder 驱动

和路由器一样，Binder 驱动是整个通信的核心；它工作于内核态，提供open()，mmap()，poll()，ioctl()等标准文件操作，用户通过/dev/binder访问该它。

Binder 驱动负责进程之间 Binder 通信的建立，Binder 在进程之间的传递，Binder 引用计数管理，数据包在进程之间的传递和交互等一系列底层支持。Binder驱动的代码位于 linux 目录的 drivers/misc/binder.c 中。

Binder驱动和应用程序之间定义了一套接口协议，主要功能由 ioctl() 接口实现，不提供 read()，write() 接口，因为 ioctl() 灵活方便，且能够一次调用实现先写后读以满足同步交互，而不必分别调用 write() 和 read()。

2、ServiceManager 与实名 Binder

ServiceManager 和 DNS 类似，将字符形式的 Binder 名字转化成 Client 中对该 Binder 的引用，使得 Client 能够通过 Binder 的名字获得对 Binder 实体的引用。注册了名字的 Binder 叫**实名 Binder**，就像网站一样除了除了有 IP 地址意外还有自己的网址。

Server 创建了 Binder，并为它起一个字符形式，可读易记得名字，将这个 Binder 实体连同名字一起以数据包的形式通过 Binder 驱动发送给 ServiceManager ，通知 ServiceManager 注册一个名为 “张三” 的 Binder，它位于某个 Server 中。

Binder驱动为这个穿越进程边界的 Binder 创建位于内核中的实体节点以及 ServiceManager 对实体的引用，将名字以及新建的引用打包传给 ServiceManager。ServiceManger 收到数据后从中取出名字和引用填入查找表。

3、ServiceManager 和 Server

细心的读者可能会发现其中的蹊跷：ServiceManager 是一个进程，Server 是另一个进程，Server向ServiceManager注册Binder必然会涉及进程间通信。

当前实现的是进程间通信却又要用到进程间通信，这就好象蛋可以孵出鸡前提却是要找只鸡来孵蛋。Binder的实现比较巧妙：预先创造一只鸡来孵蛋：ServiceManager 和其它进程同样采用 Binder 通信，ServiceManager 是Server 端，有自己的 Binder 对象（实体），其它进程都是 Client，需要通过这个 Binder 的引用来实现 Binder 的注册，查询和获取。

ServiceManager 提供的 Binder 比较特殊，它没有名字也不需要注册，当一个进程使用 `BINDER_SET_CONTEXT_MGR`命令将自己注册成 ServiceManager 时 Binder 驱动会自动为它创建 Binder 实体（这就是那只预先造好的鸡）。其次这个 Binder 的引用在所有 Client 中都固定为0而无须通过其它手段获得。也就是说，一个 Server 若要向 ServiceManager 注册自己 Binder 就必需通过0这个引用号和 ServiceManager的Binder 通信。类比网络通信，0号引用就好比域名服务器的地址，你必须预先手工或动态配置好。要注意这里说的Client是相对SMgr而言的，一个应用程序可能是个提供服务的Server，但对 ServiceManager 来说它仍然是个 Client。

4、Client 获得实名 Binder 

Server 向 ServiceManager 中注册了 Binder 以后， Client 就能通过名字获得 Binder 的引用了。Client 也利用保留的 0 号引用向 ServiceManager 请求访问某个 Binder:  我申请访问名字叫 张三 的 Binder 引用。

ServiceManager 收到这个请求后从请求数据包中取出 Binder 名称，在查找表里找到对应的条目，取出对应的 Binder 引用作为回复发送给发起请求的 Client。从面向对象的角度看，Server 中的 Binder 实体现在有两个引用：一个位于 ServiceManager 中，一个位于发起请求的 Client 中。如果接下来有更多的 Client 请求该 Binder，系统中就会有更多的引用指向该 Binder ，就像 Java 中一个对象有多个引用一样。

5、匿名 Binder

并不是所有 Binder 都需要注册给 ServiceManager 广而告之的。Server 端可以通过已经建立的 Binder 连接将创建的 Binder 实体传给 Client，当然这条已经建立的 Binder 连接必须是通过实名 Binder 实现。

由于这个 Binder 没有向 ServiceManager 注册名字，所以是个匿名Binder。Client 将会收到这个匿名 Binder 的引用，通过这个引用向位于 Server 中的实体发送请求。匿名 Binder 为通信双方建立一条私密通道，只要 Server没有把匿名 Binder 发给别的进程，别的进程就无法通过穷举或猜测等任何方式获得该 Binder 的引用，向该Binder 发送请求。

Binder 整个通信过程如下：



<img width="600" alt="Binder 通信模型" src="https://user-images.githubusercontent.com/17560388/182274364-75d5f165-1bc4-4929-9f08-a791f615b57d.png">

### 3.2 Binder 通信中的代理模式

我们已经解释清楚 Client、Server 借助 Binder 驱动完成跨进程通信的实现机制了，但是还有个问题会让我们困惑。A 进程想要 B 进程中某个对象（object）是如何实现的呢？毕竟它们分属不同的进程，A 进程没法直接使用 B 进程中的 object。

前面我们介绍过跨进程通信的过程都有 Binder 驱动的参与，因此在数据流经 Binder 驱动的时候驱动会对数据做一层转换。当 A 进程想要获取 B 进程中的 object 时，驱动并不会真的把 object 返回给 A，而是返回了一个跟 object 看起来一模一样的代理对象 objectProxy，这个 objectProxy 具有和 object 一摸一样的方法，但是这些方法并没有 B 进程中 object 对象那些方法的能力，这些方法只需要把请求参数交给驱动即可。对于 A 进程来说和直接调用 object 中的方法是一样的。

当 Binder 驱动接收到 A 进程的消息后，发现这是个 objectProxy 就去查询自己维护的表单，一查发现这是 B 进程 object 的代理对象。于是就会去通知 B 进程调用 object 的方法，并要求 B 进程把返回结果发给自己。当驱动拿到 B 进程的返回结果后就会转发给 A 进程，一次通信就完成了。

<img width="600" alt="Binder 通信过程" src="https://user-images.githubusercontent.com/17560388/182274378-4b44bc5f-4096-411d-a6ab-64a868ebfd2c.png">

### 3.3 Binder 的含义

现在我们可以对 Binder 做个更加全面的定义了：

- 从进程间通信的角度看，Binder 是一种进程间通信的机制；
- 从 Server 进程的角度看，Binder 指的是 Server 中的 Binder 实体对象；
- 从 Client 进程的角度看，Binder 指的是对 Binder 代理对象，是 Binder 实体对象的一个远程代理
- 从传输过程的角度看，Binder 是一个可以跨进程传输的对象；Binder 驱动会对这个跨越进程边界的对象对一点点特殊处理，自动完成代理对象和本地对象之间的转换。

## 参考资料如下：

- [写给 Android 应用工程师的 Binder 原理剖析](https://mp.weixin.qq.com/s/NBm5lh8_ZLfodOXT8Ph5iA)
- [Android Binder 设计与实现 - 设计篇](https://blog.csdn.net/universus/article/details/6211589)
- Android 进程间通信（IPC）机制 Binder 简要介绍和学习计划
- 《Android 系统源代码情景分析》
- [Binder 学习指南](https://weishu.me/2016/01/12/binder-index-for-newer/)
- Android 深入浅出之 Binder 机制
- 认真分析 mmap ：是什么 为什么 怎么用
