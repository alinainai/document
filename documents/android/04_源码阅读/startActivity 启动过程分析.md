本课时我们来看一下 startActivity 过程的具体流程，课程中引用的源码版本是 android-28。

在手机桌面应用中点击某一个 icon 之后，实际上最终就是通过 startActivity 去打开某一个 Activity 页面。我们知道 Android 中的一个 App 就相当于一个进程，所以 startActivity 操作中还需要判断，目标 Activity 的进程是否已经创建，如果没有，则在显示 Activity 之前还需要将进程 Process 提前创建出来。

假设是从 ActivityA 跳转到另一个 App 中的 ActivityB，过程如下图所示：

![image](https://user-images.githubusercontent.com/17560388/168716141-b60ef11f-b649-421c-9563-88ac64b99d6a.png)

整个 startActivity 的流程分为 3 大部分，也涉及 3 个进程之间的交互：

1. ActivityA --> ActivityManagerService（简称 AMS）
2. ActivityManagerService --> ApplicationThread
3. ApplicationThread --> Activity

## ActivityA --> ActivityManagerService 阶段

这一过程并不复杂，用一张图表示具体过程如下：

![image](https://user-images.githubusercontent.com/17560388/168716226-e6a97fc7-df2a-480d-8a9a-0232c4968b51.png)


接下来看下源码中做了哪些操作。

### Activity 的 startActivity

![image](https://user-images.githubusercontent.com/17560388/168716252-2f284cac-c133-42e9-8ba8-9fc80da5303a.png)


最终调用了 startActivityForResult 方法，传入的 -1 表示不需要获取 startActivity 的结果。

### Activity 的 startActivityForResult

具体代码如下所示：

![image](https://user-images.githubusercontent.com/17560388/168716282-fe9d63d4-d9c5-4462-8aa7-905e79aea6f2.png)


startActivityForResult 也很简单，调用 Instrumentation.execStartActivity 方法。剩下的交给 Instrumentation 类去处理。

解释说明：

- Instrumentation 类主要用来监控应用程序与系统交互。
- 蓝框中的 mMainThread 是 ActivityThread 类型，ActivityThread 可以理解为一个进程，在这就是 A 所在的进程。
- 通过 mMainThread 获取一个 ApplicationThread 的引用，这个引用就是用来实现进程间通信的，具体来说就是 AMS 所在系统进程通知应用程序进程进行的一系列操作，稍后会再介绍。

### Instrumentation 的 execStartActivity

方法如下：

![image](https://user-images.githubusercontent.com/17560388/168716311-9f940e3b-39d4-4152-a8a4-c5207a34997b.png)

在 Instrumentation 中，会通过 ActivityManger.getService 获取 AMS 的实例，然后调用其 startActivity 方法，实际上这里就是通过 AIDL 来调用 AMS 的 startActivity 方法，至此，startActivity 的工作重心成功地从进程 A 转移到了系统进程 AMS 中。

## ActivityManagerService --> ApplicationThread

接下来就看下在 AMS 中是如何一步一步执行到 B 进程的。

> 这里先剧透一下：刚才在看 Instrumentation 的时候，我们讲过一个 ApplicationThread 类，这个类是负责进程间通信的，这里 AMS 最终其实就是调用了 B 进程中的一个 ApplicationThread 引用，从而间接地通知 B 进程进行相应操作。

相比于 startActivity-->AMS，AMS-->ApplicationThread 流程看起来复杂好多了，实际上这里面就干了 2 件事：

1. 综合处理 launchMode 和 Intent 中的 Flag 标志位，并根据处理结果生成一个目标 Activity B 的对象（ActivityRecord）。
2. 判断是否需要为目标 Activity B 创建一个新的进程（ProcessRecord）、新的任务栈（TaskRecord）。

接下来就从 AMS 的 startActivity 方法开始看起：

## AMS 的 startActivity

![image](https://user-images.githubusercontent.com/17560388/168716566-eb040ef6-9538-42bf-9900-31304aad33a0.png)

从上图可以看出，经过多个方法的调用，最终通过 obtainStarter 方法获取了 ActivityStarter 类型的对象，然后调用其 execute 方法。在 execute 方法中，会再次调用其内部的 startActivityMayWait 方法。

### ActivityStarter 的 startActivityMayWait

ActivityStarter 这个类看名字就知道它专门负责一个 Activity 的启动操作。它的主要作用包括解析 Intent、创建 ActivityRecord、如果有可能还要创建 TaskRecord。startActivityMayWait 方法的部分实现如下：

![image](https://user-images.githubusercontent.com/17560388/168716889-8cb90658-822d-4f3e-b2e7-8c4ab958a6d1.png)

> 从上图可以看出获取目标 Activity 信息的操作由 mSupervisor 来实现，它是 ActivityStackSupervisor 类型，从名字也能猜出它主要是负责 Activity 所处栈的管理类。

在上图中的 resolveIntent 中实际上是调用系统 PackageManagerService 来获取最佳 Activity。有时候我们通过隐式 Intent 启动 Activity 时，系统中可能存在多个 Activity 可以处理 Intent，此时会弹出一个选择框让用户选择具体需要打开哪一个 Activity 界面，就是此处的逻辑处理结果。

在 startActivityMayWait 方法中调用了一个重载的 startActivity 方法，而最终会调用的 ActivityStarter 中的 startActivityUnchecked 方法来获取启动 Activity 的结果。

ActivityStarter 的 startActivityUnchecked



解释说明：

- 图中 1 处计算启动 Activity 的 Flag 值。
- 注释 2 处处理 Task 和 Activity 的进栈操作。
- 注释 3 处启动栈中顶部的 Activity。

computeLaunchingTaskFlags 方法具体如下：

![image](https://user-images.githubusercontent.com/17560388/168717200-8e021e62-0b18-4981-a9d3-93b7052b5db8.png)

这个方法的主要作用是计算启动 Activity 的 Flag，不同的 Flag 决定了启动 Activity 最终会被放置到哪一个 Task 集合中。

- 图中 1 处 mInTask 是 TaskRecord 类型，此处为 null，代表 Activity 要加入的栈不存在，因此需要判断是否需要新建 Task。
- 图中 2 处的 mSourceRecord 的类型 ActivityRecord 类型，它是用来描述“初始 Activity”，什么是“初始 Activity”呢？比如 ActivityA 启动了ActivityB，ActivityA 就是初始 Activity。当我们使用 Context 或者 Application 启动 Activity 时，此 SourceRecord 为 null。
- 图中 3 处表示初始 Activity 如果是在 SingleInstance 栈中的 Activity，这种需要添加 NEW_TASK 的标识。因为 SingleInstance 栈只能允许保存一个 Activity。
- 图中 4 处表示如果 Launch Mode 设置了 singleTask 或 singleInstance，则也要创建一个新栈。

### ActivityStackSupervisor 的 startActivityLocked

方法中会调用 insertTaskAtTop 方法尝试将 Task 和 Activity 入栈。如果 Activity 是以 newTask 的模式启动或者 TASK 堆栈中不存在该 Task id，则 Task 会重新入栈，并且放在栈的顶部。需要注意的是：Task 先入栈，之后才是 Activity 入栈，它们是包含关系。

这里一下子涌出了好几个概念 Stack、Task、Activity，其实它们都是在 AMS 内部维护的数据结构，可以用一张图来描述它们之间的关系。

![image](https://user-images.githubusercontent.com/17560388/168717305-b9e58b4e-477c-4343-a143-10a0cb4bb851.png)

关于它们之间实际操作过程可以参考 [Android 8.0 Activity启动流程](https://mp.weixin.qq.com/s/Z14PtsmQXgIuTrbC6VVLiw) 这篇文章，不过需要注意这篇文章中分析的是基于 android-27 版本。

### ActivityStack 的 resumeFocusedStackTopActivityLocked

![image](https://user-images.githubusercontent.com/17560388/168718185-0e6fd100-efb6-4a62-926c-42d36df2aab5.png)


经过一系列调用，最终代码又回到了 ActivityStackSupervisor 中的 startSpecificActivityLocked 方法。

ActivityStackSupervisor 的 startSpecificActivityLocked

![image](https://user-images.githubusercontent.com/17560388/168718217-6027770e-3b28-48dc-954c-d87a82e5aa08.png)

解释说明：

- 图中 1 处根据进程名称和 Application 的 uid 来判断目标进程是否已经创建，如果没有则代表进程未创建。
- 图中 2 处调用 AMS 创建 Activity 所在进程。

不管是目标进程已经存在还是新建目标进程，最终都会调用图中红线标记的 realStartActivityLocked 方法来执行启动 Activity 的操作。

### ActivityStackSupervisor 的 realStartActivityLocked

![image](https://user-images.githubusercontent.com/17560388/168718270-41d35daf-d006-4350-9aa8-87002bcef17f.png)

这个方法在 android-27 和 android-28 版本的区别很大，从 android-28 开始 Activity 的启动交给了事务（Transaction）来完成。

- 图中 1 处创建 Activity 启动事务，并传入 app.thread 参数，它是 ApplicationThread 类型。在上文 startActivity 阶段已经提过 ApplicationThread 是为了实现进程间通信的，是 ActivityThread 的一个内部类。
- 图中 2 处执行 Activity 启动事务。

Activity 启动事务的执行是由 ClientLifecycleManager 来完成的，具体代码如下：

![image](https://user-images.githubusercontent.com/17560388/168718341-5304cfaf-c9c5-4e3f-87c7-0bead2018ee8.png)

可以看出实际上是调用了启动事务 ClientTransaction 的 schedule 方法，而这个 transaction 实际上是在创建 ClientTransaction 时传入的 app.thread 对象，也就是 ApplicationThread。如下所示：

![image](https://user-images.githubusercontent.com/17560388/168718371-649239e7-1248-48f0-9bc0-4fcb9083ecc5.png)

解释说明：

- 这里传入的 app.thread 会赋值给 ClientTransaction 的成员变量 mClient，ClientTransaction 会调用 mClient.scheduleTransaction(this) 来执行事务。
- 这个 app.thread 是 ActivityThread 的内部类 ApplicationThread，所以事务最终是调用 app.thread 的 scheduleTransaction 执行。

到这为止 startActivity 操作就成功地从 AMS 转移到了另一个进程 B 中的 **ApplicationThread **中，剩下的就是 AMS 通过进程间通信机制通知 ApplicationThread 执行 ActivityB 的生命周期方法。

## ApplicationThread -> Activity
刚才我们已近分析了 AMS 将启动 Activity 的任务作为一个事务 ClientTransaction 去完成，在 ClientLifecycleManager 中会调用 ClientTransaction的schedule() 方法，如下：

![image](https://user-images.githubusercontent.com/17560388/168718489-9c69c427-02c2-4dab-8b9a-3b0b4a131bd8.png)

而 mClient 是一个 IApplicationThread 接口类型，具体实现是 ActivityThread 的内部类 ApplicationThread。因此后续执行 Activity 生命周期的过程都是由 ApplicationThread 指导完成的，scheduleTransaction 方法如下：

![image](https://user-images.githubusercontent.com/17560388/168718514-72ee11d7-daed-47c9-9b7e-931e278c85d5.png)

可以看出，这里还是调用了ActivityThread 的 scheduleTransaction 方法。但是这个方法实际上是在 ActivityThread 的父类 ClientTransactionHandler 中实现，具体如下：

![image](https://user-images.githubusercontent.com/17560388/168718537-596fb07e-7c60-447d-b07a-96f15bb18fd3.png)

调用 sendMessage 方法，向 Handler 中发送了一个 EXECUTE_TRANSACTION 的消息，并且 Message 中的 obj 就是启动 Activity 的事务对象。而这个 Handler 的具体实现是 ActivityThread 中的 mH 对象。具体如下：

![image](https://user-images.githubusercontent.com/17560388/168718563-b9a787cf-d2f5-4c4e-b172-7de406f53393.png)

最终调用了事务的 execute 方法，execute 方法如下：

![image](https://user-images.githubusercontent.com/17560388/168718591-10942a03-4af0-45d5-9660-f345c2d2d638.png)

在 executeCallback 方法中，会遍历事务中的 callback 并执行 execute 方法，这些 callbacks 是何时被添加的呢？

还记得 ClientTransaction 是如何创建被创建的吗？重新再看一遍：

![image](https://user-images.githubusercontent.com/17560388/168718650-5f41f165-b686-45bd-b8a6-6650359a9ec7.png)


在创建 ClientTransaction 时，通过 addCallback 方法传入了 Callback 参数，从图中可以看出其实是一个 LauncherActivityItem 类型的对象。

LaunchActivityItem 的 execute()

![image](https://user-images.githubusercontent.com/17560388/168718675-e7be46bc-d6d8-472c-9cb7-c1c314aee2a7.png)

终于到了跟 Activity 生命周期相关的方法了，图中 client 是 ClientTransationHandler 类型，实际实现类就是 ActivityThread。因此最终方法又回到了 ActivityThread。

ActivityThread 的 handleLaunchActivity
这是一个比较重要的方法，Activity 的生命周期方法就是在这个方法中有序执行，具体如下：

![image](https://user-images.githubusercontent.com/17560388/168718706-38e2e44c-37bb-4b26-9e19-4bbc51c35f52.png)


解释说明：

- 图中 1 处初始化 Activity 的 WindowManager，每一个 Activity 都会对应一个“窗口”，下一节会详细讲解。
- 图中 2 处调用 performLaunchActivity 创建并显示 Activity。
- 图中 3 处通过反射创建目标 Activity 对象。
- 图中 4 处调用 attach 方法建立 Activity 与 Context 之间的联系，创建 PhoneWindow 对象，并与 Activity 进行关联操作，下节会详细讲解。
- 图中 5 处通过 Instrumentation 最终调用 Activity 的 onCreate 方法。

至此，目标 Activity 已经被成功创建并执行生命周期方法。

## 总结
这节课我带着你详细查看了 Activity 的启动在源码中的实现流程。这一过程主要涉及 3 个进程间的通信过程：

- 首先进程 A 通过 Binder 调用 AMS 的 startActivity 方法。
- 然后 AMS 通过一系列的计算构造目标 Intent，然后在 ActivityStack 与 ActivityStackSupervisor 中处理 Task 和 Activity 的入栈操作。
- 最后 AMS 通过 Binder 机制，调用目标进程中 ApplicationThread 的方法来创建并执行 Activity 生命周期方法，实际上 ApplicationThread 是 ActivityThread 的一个内部类，它的执行最终都调用到了 ActivityThread 中的相应方法。
 
