## 前提知识点

本文基于 Android 9 （SDK 28）代码

### 1.系统的启动流程 

先简单的了解下 Android 系统的启动流程

>加载BootLoader --> 初始化内核 --> 启动init进程 --> init进程fork出Zygote进程 --> Zygote进程fork出SystemServer进程

- 系统中的所有进程都是由 Zygote 进程 fork 出来的
- SystemServer 进程是系统进程，很多系统服务，例如 ActivityManagerService、PackageManagerService、WindowManagerService…都是在该进程被创建后启动

### 2.前提知识点
先大概讲几个知识点和相关类的概念

- ActivityManagerServices（AMS）：是一个服务端进程，负责管理所有的 Activity 的生命周期，AMS 通过 Binder与Activity 通信，而 AMS 与 Zygote 之间是通过 Socket 通信
- ActivityThread：可以理解为我们常说的 `UI线程/主线程`，它的 main() 方法是 APP 的真正入口
- ApplicationThread：一个实现了 IBinder 接口的 ActivityThread 内部类，用于 ActivityThread 和 AMS 的所在进程间通信
- Instrumentation：可以理解为 ActivityThread 的一个工具类，在 ActivityThread 中初始化，一个进程只存在一个 Instrumentation 对象，在每个 Activity 初始化时，会通过 Activity 的 Attach 方法，将该引用传递给 Activity。Activity 所有生命周期的方法都有该类来执行。

## 1、点击 Launcher 启动 App

点击 Launcher 启动 App 的大概流程如下：
>Launcher 进程 --请求启动 App 的 launcher Activity--> system_server(AMS) --请求创建应用--> Zygote

Launcher 本身也是一个应用程序，点击 icon 启动 app 也是调用 Activity.startActivity 方法。

所以我们先从 startActivity 开始。

### 1.1 Activity 的 startActivity

这里假设从进程 A 启动进程 B 中的 Activity 对象，Activity.startActivity 方法最终会走到 startActivityForResult 方法

```java
public void startActivityForResult(@RequiresPermission Intent intent, int requestCode,
        @Nullable Bundle options) {
    if (mParent == null) {
        options = transferSpringboardActivityOptions(options);
        Instrumentation.ActivityResult ar = mInstrumentation.execStartActivity( this, mMainThread.getApplicationThread(), mToken, this,
                intent, requestCode, options);
        if (ar != null) {
            mMainThread.sendActivityResult(
                mToken, mEmbeddedID, requestCode, ar.getResultCode(),
                ar.getResultData());
        }
        if (requestCode >= 0) {
            mStartedActivity = true;
        }
        cancelInputsAndStartExitTransition(options);
    } else {
        ...
    }
}
```

而该方法最终调用 mInstrumentation.execStartActivity 方法

- mMainThread 就是上面我们说过的 ActivityThread 类型的对象，在这里 ActivityThread 可以理解为一个进程，就是 A 所在的进程。
- mMainThread.getApplicationThread() 的返回类型是 ApplicationThread，ApplicationThread 是 ActivityThread 的内部类，继承 IApplicationThread.Stub，也是个Binder 对象。ApplicationThread 用来实现进程间通信，具体来说就是 AMS 所在系统进程通知应用程序进程进行的一系列操作

### Instrumentation 的 execStartActivity 方法

方法如下：
```java
public ActivityResult execStartActivity(
        Context who, IBinder contextThread, IBinder token, Activity target,
        Intent intent, int requestCode, Bundle options) {
    IApplicationThread whoThread = (IApplicationThread) contextThread;
    Uri referrer = target != null ? target.onProvideReferrer() : null;
    if (referrer != null) {
        intent.putExtra(Intent.EXTRA_REFERRER, referrer);
    }
    ...
    try {
        intent.migrateExtraStreamToClipData();
        intent.prepareToLeaveProcess(who);
        int result = ActivityTaskManager.getService()
            .startActivity(whoThread, who.getBasePackageName(), intent,
                    intent.resolveTypeIfNeeded(who.getContentResolver()),
                    token, target != null ? target.mEmbeddedID : null,
                    requestCode, 0, null, options);
        checkStartActivityResult(result, intent);
    } catch (RemoteException e) {
        throw new RuntimeException("Failure from system", e);
    }
    return null;
}
```
在 Instrumentation 中，会通过 ActivityManager.getService 获取 AMS 的实例，然后调用其 startActivity 方法，实际上这里就是通过 AIDL 来调用 AMS 的 startActivity 方法，至此，startActivity 的工作重心成功地从进程 A 转移到了系统进程 ATMS 中。

我们看一下继续看一下获取 ActivityTaskManager 相关代码

```java
public static IActivityTaskManager getService() {
    return IActivityTaskManagerSingleton.get();
}

private static final Singleton<IActivityTaskManager> IActivityTaskManagerSingleton =
        new Singleton<IActivityTaskManager>() {
            @Override
            protected IActivityTaskManager create() {
                final IBinder b = ServiceManager.getService(Context.ACTIVITY_TASK_SERVICE);
                return IActivityTaskManager.Stub.asInterface(b);
            }
        };
```

## 2、ActivityManagerService --> ApplicationThread

接下来就看下 AMS 是如何一步一步执行到 B 进程的。

>上面我们说过 ApplicationThread 类是负责进程间通信的，AMS 会调用 B 进程中的 ApplicationThread 引用，从而间接地通知 B 进程进行相应操作。

相比于 startActivity-->ATMS，ATMS-->ApplicationThread 流程看起来复杂好多了，实际上这里面就干了 2 件事：

1. 综合处理 launchMode 和 Intent 中的 Flag 标志位，并根据处理结果生成一个目标 Activity B 的对象（ActivityRecord）。
2. 判断是否需要为目标 Activity B 创建一个新的进程（ProcessRecord）、新的任务栈（TaskRecord）。

接下来就从 AMS 的 startActivity 方法开始看起：

### 2.1 ATMS 的 startActivity

![image](https://user-images.githubusercontent.com/17560388/168716566-eb040ef6-9538-42bf-9900-31304aad33a0.png)

从上图可以看出，经过多个方法的调用，最终通过 obtainStarter 方法获取了 ActivityStarter 类型的对象，然后调用其 execute 方法。在 execute 方法中，会再次调用其内部的 startActivityMayWait 方法。

### 2.2 ActivityStarter 的 startActivityMayWait

ActivityStarter 这个类看名字就知道它专门负责一个 Activity 的启动操作。它的主要作用包括解析 Intent、创建 ActivityRecord、如果有可能还要创建 TaskRecord。startActivityMayWait 方法的部分实现如下：

![image](https://user-images.githubusercontent.com/17560388/168716889-8cb90658-822d-4f3e-b2e7-8c4ab958a6d1.png)

> 从上图可以看出获取目标 Activity 信息的操作由 mSupervisor 来实现，它是 ActivityStackSupervisor 类型，从名字也能猜出它主要是负责 Activity 所处栈的管理类。

在上图中的 resolveIntent 中实际上是调用系统 PackageManagerService 来获取最佳 Activity。有时候我们通过隐式 Intent 启动 Activity 时，系统中可能存在多个 Activity 可以处理 Intent，此时会弹出一个选择框让用户选择具体需要打开哪一个 Activity 界面，就是此处的逻辑处理结果。

在 startActivityMayWait 方法中调用了一个重载的 startActivity 方法，而最终会调用的 ActivityStarter 中的 startActivityUnchecked 方法来获取启动 Activity 的结果。

### 2.3 ActivityStarter 的 startActivityUnchecked

![image](https://user-images.githubusercontent.com/17560388/182028262-67ba6306-aec1-4779-bfce-515eebec8fb2.png)

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

### 2.4 ActivityStackSupervisor 的 startActivityLocked

方法中会调用 insertTaskAtTop 方法尝试将 Task 和 Activity 入栈。如果 Activity 是以 newTask 的模式启动或者 TASK 堆栈中不存在该 Task id，则 Task 会重新入栈，并且放在栈的顶部。需要注意的是：Task 先入栈，之后才是 Activity 入栈，它们是包含关系。

这里一下子涌出了好几个概念 Stack、Task、Activity，其实它们都是在 AMS 内部维护的数据结构，可以用一张图来描述它们之间的关系。

![image](https://user-images.githubusercontent.com/17560388/168717305-b9e58b4e-477c-4343-a143-10a0cb4bb851.png)

关于它们之间实际操作过程可以参考 [Android 8.0 Activity启动流程 (基于 android-27 版本)](https://mp.weixin.qq.com/s/Z14PtsmQXgIuTrbC6VVLiw) 

### 2.5 ActivityStack 的 resumeFocusedStackTopActivityLocked

![image](https://user-images.githubusercontent.com/17560388/168718185-0e6fd100-efb6-4a62-926c-42d36df2aab5.png)

经过一系列调用，最终代码又回到了 ActivityStackSupervisor 中的 startSpecificActivityLocked 方法。

### 2.6 ActivityStackSupervisor 的 startSpecificActivityLocked

![image](https://user-images.githubusercontent.com/17560388/168718217-6027770e-3b28-48dc-954c-d87a82e5aa08.png)

解释说明：

- 图中 1 处根据进程名称和 Application 的 uid 来判断目标进程是否已经创建，如果没有则代表进程未创建。
- 图中 2 处调用 AMS 创建 Activity 所在进程。

不管是目标进程已经存在还是新建目标进程，最终都会调用图中红线标记的 realStartActivityLocked 方法来执行启动 Activity 的操作。

### 2.7 ActivityStackSupervisor 的 realStartActivityLocked

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

## 3、ApplicationThread -> Activity

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

## 4、ActivityThread 的 handleLaunchActivity
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
 
