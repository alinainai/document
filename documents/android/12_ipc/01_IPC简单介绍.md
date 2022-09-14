## 一、IPC简单介绍

IPC 进程间通信技术。

因为有进程隔离，进程间的数据是不共享的。所以需要进程间通信技术来实现不同进程间数据的交互。

Android 是基于 Linux Kernel 开发的，我们先讲一下 Linux 中的 IPC

## 二、Linux 下传统的进程间通信原理

### 2.1 基本概念介绍

我们先看一下Liunx跨进程通信涉及到几个基本概念

<img width="400" alt="Linux 背景知识" src="https://user-images.githubusercontent.com/17560388/182273725-ec3e14ca-9c9e-4f7e-8d55-5363ae25dde8.png">

- 进程隔离
简单的说就是操作系统中，进程与进程间内存是不共享的。两个进程就像两个平行的世界，A 进程没法直接访问 B 进程的数据，这就是进程隔离的通俗解释。A 进程和 B 进程之间要进行数据交互就得采用特殊的通信机制：进程间通信（IPC）。

- 进程空间划分：用户空间(User Space)/内核空间(Kernel Space)
现在操作系统都是采用的虚拟存储器，对于 32 位系统而言，它的寻址空间（虚拟存储空间）就是 2 的 32 次方，也就是 4GB。操作系统的核心是内核，独立于普通的应用程序，可以访问受保护的内存空间，也可以访问底层硬件设备的权限。为了保护用户进程不能直接操作内核，保证内核的安全，操作系统从逻辑上将虚拟空间划分为用户空间（User Space）和内核空间（Kernel Space）。针对 Linux 操作系统而言，将最高的 1GB 字节供内核使用，称为内核空间；较低的 3GB 字节供各进程使用，称为用户空间。

>简单的说就是，内核空间（Kernel）是系统内核运行的空间，用户空间（User Space）是用户程序运行的空间。为了保证安全性，它们之间是隔离的。

<img width="400" alt="内存分配" src="https://user-images.githubusercontent.com/17560388/182273864-eef93519-bfb5-4d45-bb4d-364e16bc92be.png">

- 系统调用：用户态与内核态
虽然从逻辑上进行了用户空间和内核空间的划分，但不可避免的用户空间需要访问内核资源，比如文件操作、访问网络等等。为了突破隔离限制，就需要借助**系统调用**来实现。系统调用是用户空间访问内核空间的唯一方式，保证了所有的资源访问都是在内核的控制下进行的，避免了用户程序对系统资源的越权访问，提升了系统安全性和稳定性。

Linux 使用两级保护机制：0 级供系统内核使用，3 级供用户程序使用。

当一个任务（进程）执行系统调用而陷入内核代码中执行时，称进程处于内核运行态（内核态）。此时处理器处于特权级最高的（0级）内核代码中执行。当进程处于内核态时，执行的内核代码会使用当前进程的内核栈。每个进程都有自己的内核栈。

当进程在执行用户自己的代码的时候，我们称其处于用户运行态（用户态）。此时处理器在特权级最低的（3级）用户代码中运行。

系统调用主要通过如下两个函数来实现：
```c
copy_from_user() //将数据从用户空间拷贝到内核空间
copy_to_user() //将数据从内核空间拷贝到用户空间
```
### 2.2 Linux 下的传统 IPC 通信原理

理解了上面的几个概念，我们再来看看传统的 IPC 方式中，进程之间是如何实现通信的。

通常的做法是消息发送方将要发送的数据存放在内存缓存区中，通过系统调用进入内核态。然后内核程序在内核空间分配内存，开辟一块内核缓存区，调用 copy_from_user() 函数将数据从用户空间的内存缓存区拷贝到内核空间的内核缓存区中。同样的，接收方进程在接收数据时在自己的用户空间开辟一块内存缓存区，然后内核程序调用 copy_to_user() 函数将数据从内核缓存区拷贝到接收进程的内存缓存区。这样数据发送方进程和数据接收方进程就完成了一次数据传输，我们称完成了一次进程间通信。如下图：

<img width="600" alt="传统 IPC 通信原理" src="https://user-images.githubusercontent.com/17560388/182273944-7127a9e6-b2a9-4bfd-a5a8-1ecc61398293.png">

这种传统的 IPC 通信方式有两个问题：

- 1. 性能低下，一次数据传递需要经历：`内存缓存区 --> 内核缓存区 --> 内存缓存区`，需要 2 次数据拷贝；
- 2. 接收数据的缓存区由数据接收进程提供，但是接收进程并不知道需要多大的空间来存放将要传递过来的数据，因此只能开辟尽可能大的内存空间或者先调用 API 接收消息头来获取消息体的大小，这两种做法不是浪费空间就是浪费时间。

Android 中的 Binder 我们下篇文章再讲，我们先看下如何在开发的时候开启多线程。

## 三、Android 多进程

### 1.如何开启多进程

1. 通过 JNI 在 native 层 fork 一个新的进程。忽略掉
2. 给四大组件在 Manifest 指定 android:process。

咱们以 Activity 为例子，3个 Activity 指定不同的进程，如下：
```html
<activity
    android:name=".MainActivity"
    android:exported="true" />
<activity android:name=".Remote1Activity"
    android:exported="false"
    android:process=":remote" />
<activity android:name=".Remote2Activity"
    android:exported="false"
    android:process="com.egas.demo.remote" />
```

使用 adb 指令查看当前 app 所运行的线程

```shell 
adb shell ps | grep yourpackagename
```
```shell 
u0_a911   26215   701 5307828  78088 0  0 S com.egas.demo
u0_a911   26216   701 5307852  79988 0  0 S com.egas.demo:remote
u0_a911   26217   701 5272876  77828 0  0 S com.egas.demo.remote
```

`:remote` 表示进程全路径为 `包名:remote` 
- 以 ":" 开头的进程属于当前应用的私有进程，其他进程不可以和它跑在同一个进程中
- 不以 ":" 开头的进程属于全局进程，其他应用通过 ShareUID 方式和它跑在同一个进程中。

>Android 会为每个应用分配一个唯一的UID，具有相同UID的应用才能共享数据。
>两个应用有相同的ShareUID和签名才能跑在同一个进程，他们才能共享data 目录、组件信息还有内存数据。

### 2.多进程带来的问题
1. 静态成员和单例模式完全失效
2. 线程同步机制完全失效
3. SharedPreferences 的可靠性下降
4. Application 会多次创建

### 3 解决 Application 会多次创建的问题

```java
class App:Application() {

    override fun onCreate() {
        super.onCreate()
        val processName = getProcessName(this, android.os.Process.myPid())
        if (!TextUtils.isEmpty(processName) && processName.equals(this.packageName)) {
            Log.e("app","app main") //判断进程名，保证只有主进程运行
        }
    }

    private fun getProcessName(cxt: Context, pid: Int): String? {
        val am = cxt.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningApps = am.runningAppProcesses ?: return null
        for (procInfo in runningApps) {
            if (procInfo.pid == pid) {
                return procInfo.processName
            }
        }
        return null
    }
```







