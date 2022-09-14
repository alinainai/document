## 1.点击APP图标的启动过程

- 1.点击桌面APP图标时，Launcher的startActivity()方法，通过Binder通信，调用system_server进程中AMS服务的startActivity方法，发起启动请求
- 2.system_server进程接收到请求后，向Zygote进程发送创建进程的请求
- 3.Zygote进程fork出App进程，并执行ActivityThread的main方法，创建ActivityThread线程，初始化MainLooper，主线程Handler，同时初始化ApplicationThread用于和AMS通信交互
- 4.App进程，通过Binder向sytem_server进程发起attachApplication请求，这里实际上就是APP进程通过Binder调用sytem_server进程中AMS的attachApplication方法，上面我们已经分析过，AMS的attachApplication方法的作用是将ApplicationThread对象与AMS绑定
- 5.system_server进程在收到attachApplication的请求，进行一些准备工作后，再通过binder IPC向App进程发送handleBindApplication请求（初始化Application并调用onCreate方法）和scheduleLaunchActivity请求（创建启动Activity）
- 6.App进程的binder线程（ApplicationThread）在收到请求后，通过handler向主线程发送BIND_APPLICATION和LAUNCH_ACTIVITY消息，这里注意的是AMS和主线程并不直接通信，而是AMS和主线程的内部类
- 7.ApplicationThread通过Binder通信，ApplicationThread再和主线程通过Handler消息交互。 ( 这里猜测这样的设计意图可能是为了统一管理主线程与AMS的通信，并且不向AMS暴露主线程中的其他公开方法，大神可以来解析下)
- 8.主线程在收到Message后，创建Application并调用onCreate方法，再通过反射机制创建目标Activity，并回调Activity.onCreate()等方法

到此，App便正式启动，开始进入Activity生命周期，执行完onCreate/onStart/onResume方法，UI渲染后显示APP主界面

## ActivityThread 是主线程（UI线程）吗

系统进程的 ZygoteInit 收到某个消息之后，通过fork的方式构建一个虚拟机进程。

主线程是虚拟机启动时候默认运行的那个线程，你可以简单理解为运行java.main 函数时候所在的线程。

ActivityThread 跟线程没有任何关系，只是这个线程需要一个入口(有main函数的类)，而这个入口就是 ActivityThread 的 main 方法。

## ActivityThread的主要方法

#### ActivityThread的main方法

```java
    public static void main(String[] args) {
        ...
        
        //主线程的 Looper 初始化，主线程的 Looper不可以退出
        Looper.prepareMainLooper();
        
        ActivityThread thread = new ActivityThread();
        //在attach方法中会完成Application对象的初始化，然后调用Application的onCreate()方法
        thread.attach(false);

        //给主线程的 Handler 赋值
        if (sMainThreadHandler == null) {
            //将成员变量
            sMainThreadHandler = thread.getHandler();
        }
        ...
        Looper.loop();
        throw new RuntimeException("Main thread loop unexpectedly exited");
    }
```

该方法 创建了主线程的 Looper 对象，初始化了 ActivityThread 对象，并给主线程的 Handler 赋值。

## 主线程Looper的初始化

Looper.prepareMainLooper()
```java
    //主线程Looper的初始化
    public static void prepareMainLooper() {
        prepare(false);
        synchronized (Looper.class) {
            if (sMainLooper != null) {
                throw new IllegalStateException("The main Looper has already been prepared.");
            }
            sMainLooper = myLooper();
        }
    }
    
    //普通线程Looper的初始化
    public static void prepare() {
        prepare(true);
    }

    private static void prepare(boolean quitAllowed) {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper(quitAllowed));
    }
```
普通线程的 Prepare() 默认 quitAllowed 参数为 true，表示允许退出，而主线程也就是 ActivityThread 的 Looper 参数为 false，不允许退出。
这里的 quitAllowed 参数，最终会传递给 MessageQueue，当调用 MessageQueue 的 quit 方法时，会判断这个参数，如果是主线程，也就是 quitAllowed 参数为 false 时，会抛出异常。
```java
    //Looper的退时会判断quitAllowed
    void quit(boolean safe) {
        if (!mQuitAllowed) {
            throw new IllegalStateException("Main thread not allowed to quit.");
        }
        synchronized (this) {
            ...
        }
    }
```
我们注意到主线程Looper初始化之后，赋值给了成员变量sMainLooper，这个成员的作用就是向其他线程提供主线程的Looper对象。这下我们就应该知道为什么Looper.getMainLooper()方法能获取主线程的Looper对象了

```java
    public static Looper getMainLooper() {
        synchronized (Looper.class) {
            return sMainLooper;
        }
    }
```

## 主线程 Handler 的初始化
在 ActivityThread 的 main 方法中我们注意到一行代码：

```java
    ActivityThread thread = new ActivityThread();
    thread.attach(false);
    if (sMainThreadHandler == null) {
        sMainThreadHandler = thread.getHandler();
    }
```

初始化代码如下：

```java
    ActivityThread的成员变量
    final H mH = new H();
    
    final Handler getHandler() {
        return mH;
    }
```

从以上代码中可以看到，主线程的 Handler 作为 ActivityThread 的成员变量，是在 ActivityThread 的 main 方法被执行，ActivityThread 被创建时而初始化，而接下来要说的 ApplicationThread 中的方法执行以及 Activity 的创建都依赖于主线程 Handler。至此我们也就明白了，主线程（ActivityThread）的初始化是在它的 main 方法中，主线程的 Handler 以及 MainLooper 的初始化时机都是在ActivityThread 创建的时候。

#### ActivityThread#attach() 方法
 
```java
    private void attach(boolean system) {
        ...
        if (!system) {
            final IActivityManager mgr = ActivityManager.getService();
            try {
                mgr.attachApplication(mAppThread);
            } catch (RemoteException ex) {
                throw ex.rethrowFromSystemServer();
            }else{
                ...
            }
        }
    }
```

mAppThread 是 ActivityThread 带有初始值的成员变量，是 ApplicationThread 类，ApplicationThread是ActivityThread的私有内部类，实现了IBinder接口，用于ActivityThread和ActivityManagerService的所在进程间通信。

在 ActivityThread.attach() 方法中，ActivityManagerService 通过 attachApplication方法，将 ApplicationThread 对象绑定到 ActivityManagerService，

## ActivityManagerService的主要方法

#### attachApplication 方法

在每个ActivityThread（APP）被创建的时候，都需要向ActivityManagerService绑定（或者说是向远程服务AMS注册自己），用于AMS管理ActivityThread中的所有四大组件的生命周期。

```java
    public final void attachApplication(IApplicationThread thread) {
        synchronized (this) {
            int callingPid = Binder.getCallingPid();
            final long origId = Binder.clearCallingIdentity();
            attachApplicationLocked(thread, callingPid);
            Binder.restoreCallingIdentity(origId);
        }
    }
```

#### attachApplicationLocked 方法

主要调用两个方法：

```java
private final boolean attachApplicationLocked(IApplicationThread thread, int pid) {
        ...
    //主要用于创建Application，用调用onCreate方法
    thread.bindApplication(...);
    ...
    //主要用于创建Activity
    if (mStackSupervisor.attachApplicationLocked(app)) {
            ...
    }
}
```

#### thread.bindApplication() 方法

这里的 thread 对象是 ApplicationThread 在AMS中的代理对象，
所以这里的 bindApplication 方法最终会调用 ApplicationThread.bindApplication() 方法，
该方法会向 ActivityThread 的消息对应发送 BIND_APPLICATION 的消息，
消息的处理最终会调用 Application.onCreate() 方法，
这也说明 Application.onCreate() 方法的执行时机比任何 Activity.onCreate() 方法都早。

```java
public final void bindApplication(...) {
        ...
        //该消息的处理，会调用handleBindApplication方法
        sendMessage(H.BIND_APPLICATION, data);
}
```

#### ActivityThread中的handleBindApplication方法

```java
    private void handleBindApplication(AppBindData data) {
        ...
        try {
            Application app = data.info.makeApplication(data.restrictedBackupMode, null);
            mInitialApplication = app;
            ...
            try {
                mInstrumentation.callApplicationOnCreate(app);
            } catch (Exception e) {
            }
        } finally {
        }
    }
```      

#### LoadedApk中的方法，用于创建Application

```java
    public Application makeApplication(boolean forceDefaultAppClass, Instrumentation instrumentation) {
        //如果存在Application的实例，则直接返回，这也说明Application是个单例
        if (mApplication != null) {
            return mApplication;
        }

        Application app = null;
        //...这里通过反射初始化Application

        if (instrumentation != null) {
            try {
                //调用Application的onCreate方法
                instrumentation.callApplicationOnCreate(app);
            } catch (Exception e) {
            }
        }
        return app;
    }
```

#### mStackSupervisor.attachApplicationLocked(app)方法

用于创建 Activity，mStackSupervisor 是 AMS 的成员变量，为 Activity 堆栈管理辅助类实例，
该方法最终会调用 ApplicationThread 类 的scheduleLaunchActivity 方法，该方法也是类似于第一步，
向 ActivityThread 的消息队列发送创建 Activity 的消息，最终在 ActivityThread 中完成创建Activity的操作。

```java
    boolean attachApplicationLocked(ProcessRecord app) throws RemoteException {
        ...
        if (realStartActivityLocked(hr, app, true, true)) {
            ...
        }          
        ...
    }

    final boolean realStartActivityLocked(ActivityRecord r, ProcessRecord app,
            boolean andResume, boolean checkConfig) throws RemoteException {
        ...
        try {
            //调用ApplicationThread的scheduleLaunchActivity用于启动一个Activity
            app.thread.scheduleLaunchActivity(...);
        } catch (RemoteException e) {
        }
    }
```

ApplicationThread 的 scheduleLaunchActivity 方法会向 ActivityThread 发送 LAUNCH_ACTIVITY 信息，用于启动一个 Activity，该消息的处理会调用 ActivityThread 的  handleLaunchActivity 方法，最终启动一个 Activity。

## Launcher 启动 Activity 详细代码

谈到 Activity 的启动流程就绕不开 ActivityManagerService (简称AMS)，它主要负责四大组件的启动、切换、调度以及进程的管理，是 Android 中最核心的服务，参与了所有应用程序的启动管理。Activity 的启动流程围绕 AMS，可以大致分为3个部分：

- Launcher 请求 AMS的过程
- AMS 到 ApplicationThread 的调用过程
- ActivityThread 启动 Activity的过程

下面就针对这3个部分逐一进行分析。

## 1.Launcher请求AMS的过程

该过程的时序图如下：

![image](https://user-images.githubusercontent.com/17560388/125572159-779241a5-387f-44ca-9c45-2b3dc334a91d.png)

当我们点击桌面的应用快捷图标时，就会调用 Launcher#startActivitySafely() 方法：

```java
public boolean startActivitySafely(View v, Intent intent, ItemInfo item) {
    ...
    // Prepare intent
    // 注释1
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    ...
    // Could be launching some bookkeeping activity
    // 注释2
    startActivity(intent, optsBundle);
    ...
}
```

对于不太需要关注的代码省略了，主要是走调用流程，对关键代码进行分析，在注释1处将 Flag 设置为 Intent.FLAG_ACTIVITY_NEW_TASK，这样根 Activity 会在新的任务栈中启动。
在注释2处调用 startActivity 方法，这个方法在 Activity 中实现，Activity 中 startActivity 方法有好几种重载方式，但它们最终都会调用 startActivityForResult 方法：

```java
public void startActivityForResult(@RequiresPermission Intent intent, int requestCode,
            @Nullable Bundle options) {
    	// 注释1
        if (mParent == null) {
            options = transferSpringboardActivityOptions(options);
            // 注释2
            Instrumentation.ActivityResult ar =
                mInstrumentation.execStartActivity(
                    this, mMainThread.getApplicationThread(), mToken, this,
                    intent, requestCode, options);
            ...
        } else {
            ...
        }
}
```
注释1处的 mParent 是 Activity 类型的，表示当前 Activity 的父类，因为目前根 Activity 还没有创建出来，因此 mParent==null 成立，执行注释2处的逻辑，
调用 Instrumentation 的 execStartActivity 方法，Instrumentation 主要是用来监控应用程序和系统的交互，execStartActivity 方法代码如下：
```java
public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {
        ...
        try {
            intent.migrateExtraStreamToClipData();
            intent.prepareToLeaveProcess(who);
	    
            // 注释1
            int result = ActivityManager.getService()
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

核心逻辑就是注释1的代码，首先调用 ActivityManager 的 getService 方法来获取 AMS 的代理对象，接着调用 startActivity 方法。我们先进入 ActivityManager 的 getService 方法：

```java
public static IActivityManager getService() {
        return IActivityManagerSingleton.get();
}

private static final Singleton<IActivityManager> IActivityManagerSingleton =
        new Singleton<IActivityManager>() {
        	@Override
            protected IActivityManager create() {
                // 注释1
            	final IBinder b = ServiceManager.getService(Context.ACTIVITY_SERVICE);
                // 注释2
                final IActivityManager am = IActivityManager.Stub.asInterface(b);
                return am;
            }
};
```

getService 方法调用了 IActivityManagerSingleton 的 get 方法，看一下 Singleton 代码

```java
public abstract class Singleton<T> {
    private T mInstance;

    protected abstract T create();

    public final T get() {
        synchronized (this) {
            if (mInstance == null) {
                mInstance = create();
            }
            return mInstance;
        }
    }
}
```

不难发现 IActivityManagerSingleton 的 get 方法，会调用 create 方法，在注释1处得到 IBinder 类型的 AMS 引用，接着在注释2处将它转化成 IActivityManager 类型的对象，即AMS的代理对象，
这段代码采用的是 AIDL，IActivityManager.java 类是由 AIDL 工具在编译时自动生成的。
AMS继承 IActivityManager.Stub 类并实现相关方法。通过这个代理对象和 AMS( AMS 所在的进程为 SystemServer 系统服务进程)进行跨进程通信，
需要注意的 Android 8.0 之前并没有采用 AIDL，是用 AMS 的代理对象 ActivityManagerProxy 来与 AMS 进行跨进程通信的。Android8.0 去除了 ActivityManagerNative 的内部类ActivityManagerProxy，代替它的是 IActivityManager，它就是AMS的代理对象。
经过上面的分析，我们知道 execActivity 方法最终调用的是 AMS 的 startActivity 方法。补充一句，这里就由 Launcher 进程经过一系列调用到了 SystemServer 进程，可以简单概括为下图：

![image](https://user-images.githubusercontent.com/17560388/125573949-01638e54-190c-4402-ac03-1c30672b1bba.png)

## 2.AMS到ApplicationThread的调用过程

Launcher 请求 AMS 后，代码逻辑进入 AMS 中，接着是 AMS 到 ApplicationThread 的调用流程，时序图如下：

![image](https://user-images.githubusercontent.com/17560388/125573997-d5552401-e645-49b5-8711-0f5d33d88f27.png)


AMS的startActivity方法如下：

```java
@Override
public final int startActivity(IApplicationThread caller, String callingPackage,
         Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode,
         int startFlags, ProfilerInfo profilerInfo, Bundle bOptions) {
	return startActivityAsUser(caller, callingPackage, intent, resolvedType, resultTo,
            resultWho, requestCode, startFlags, profilerInfo, bOptions,
            UserHandle.getCallingUserId());
}
```

AMS 的 startActivity 方法中返回了 startActivityAsUser 方法，可以发现 startActivityAsUser 方法比 startActivity 方法多了一个参数 UserHandle.getCallingUserId(),这个方法会获得调用者的 UserId，AMS 根据这个 UserId 来确定调用者的权限。

下面进入 startActivityAsUser 方法：

```java
@Override
public final int startActivityAsUser(IApplicationThread caller, String callingPackage,
       Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode,int startFlags, ProfilerInfo profilerInfo, Bundle bOptions, int              userId) {
    // 注释1 判断调用者进程是否被隔离
    enforceNotIsolatedCaller("startActivity");
    // 注释2 检查调用者的权限
    userId = mUserController.handleIncomingUser(Binder.getCallingPid(),Binder.getCallingUid(),userId, false, ALLOW_FULL_ONLY, "startActivity", null);
    // TODO: Switch to user app stacks here.
    return mActivityStarter.startActivityMayWait(caller, -1, callingPackage, intent,
    resolvedType, null, null, resultTo, resultWho, requestCode, startFlags,profilerInfo,null, null, bOptions, false, userId, null, null,"startActivityAsUser");
}
```
在注释1处判读调用者进程是否隔离，如果没被隔离则抛出 SecurityException 异常，在注释2处检查调用者是否有权限，如果没有权限也会抛出 SecurityException 异常。最后调用了 ActivityStarter 的startActivityMayWait 方法，参数要比 startActivityAsUser 多几个，需要注意的是倒数第二个参数类型为 TaskRecord，代表启动的 Activity 所在的栈，最后一个参数 "startActivityAsUser" 代表启动的理由，接下来进入 startActivityMayWait 方法：
```java
final int startActivityMayWait(IApplicationThread caller, int callingUid,
            String callingPackage, Intent intent, String resolvedType,
            IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
            IBinder resultTo, String resultWho, int requestCode, int startFlags,
            ProfilerInfo profilerInfo, WaitResult outResult,
            Configuration globalConfig, Bundle bOptions, boolean ignoreTargetSecurity,int userId,IActivityContainer iContainer, TaskRecord inTask, String reason) {
	...
    int res = startActivityLocked(caller, intent, ephemeralIntent, resolvedType,
    	aInfo, rInfo, voiceSession, voiceInteractor,
    	resultTo, resultWho, requestCode, callingPid,
    	callingUid, callingPackage, realCallingPid, realCallingUid, startFlags,
        options, ignoreTargetSecurity, componentSpecified, outRecord, container,
        inTask, reason);
        ...
        return res;
     }
}
```
ActivityStarter 是 Android7.0 中新加入的类，它是加载 Activity 的控制类，会收集所有的逻辑来决定如何将 Intent 和 Flags 转换为 Activity，
并将 Activity 和 Task 以及 Stack 相关联。ActivityStarter 的 startActivityMayWait 方法调用了 startActivityLocked 方法：
```java
int startActivityLocked(IApplicationThread caller, Intent intent, Intent ephemeralIntent,
	String resolvedType, ActivityInfo aInfo, ResolveInfo rInfo,
    IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
    IBinder resultTo, String resultWho, int requestCode, int callingPid, int callingUid,
    String callingPackage, int realCallingPid, int realCallingUid, int startFlags,
    ActivityOptions options, boolean ignoreTargetSecurity, boolean componentSpecified,
    ActivityRecord[] outActivity, ActivityStackSupervisor.ActivityContainer container,
    TaskRecord inTask, String reason) {
	// 注释1  判断启动
    if (TextUtils.isEmpty(reason)) {
    	throw new IllegalArgumentException("Need to specify a reason.");
    }
    
    mLastStartReason = reason;
    mLastStartActivityTimeMs = System.currentTimeMillis();
    mLastStartActivityRecord[0] = null;
	// 注释2
    mLastStartActivityResult = startActivity(caller, intent, ephemeralIntent,resolvedType,aInfo, rInfo, voiceSession, voiceInteractor, resultTo, resultWho, 		requestCode,callingPid, callingUid, callingPackage, realCallingPid, realCallingUid, startFlags, options, ignoreTargetSecurity, componentSpecified,                    	     mLastStartActivityRecord,container, inTask);
    if (outActivity != null) {
        // mLastStartActivityRecord[0] is set in the call to startActivity above.
        outActivity[0] = mLastStartActivityRecord[0];
    }
    return mLastStartActivityResult;
}
```

在注释1处判断启动的理由不为空，如果为空则抛出 IllegalArgumentException 异常。紧接着在注释2处又调用了 startActivity 方法：

```java
/** DO NOT call this method directly. Use {@link #startActivityLocked} instead. */
private int startActivity(IApplicationThread caller, Intent intent, Intent ephemeralIntent,String resolvedType, ActivityInfo aInfo, ResolveInfo rInfo,
    IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
    IBinder resultTo, String resultWho, int requestCode, int callingPid, int callingUid,
    String callingPackage, int realCallingPid, int realCallingUid, int startFlags,
    ActivityOptions options, boolean ignoreTargetSecurity, boolean componentSpecified,
    ActivityRecord[] outActivity, ActivityStackSupervisor.ActivityContainer container,
    TaskRecord inTask) {
    
    int err = ActivityManager.START_SUCCESS;
    // Pull the optional Ephemeral Installer-only bundle out of the options early.
    final Bundle verificationBundle
    	= options != null ? options.popAppVerificationBundle() : null;

    ProcessRecord callerApp = null;
    // 注释1
    // caller为IApplicationThread类型
    if (caller != null) {
        // 注释2 得到Launcher进程
        callerApp = mService.getRecordForAppLocked(caller);
        if (callerApp != null) {
            // 获取Launcher进程的pid和uid
            callingPid = callerApp.pid;
            callingUid = callerApp.info.uid;
        } else {
            Slog.w(TAG, "Unable to find app for caller " + caller
                   + " (pid=" + callingPid + ") when starting: "
                   + intent.toString());
            err = ActivityManager.START_PERMISSION_DENIED;
        }
    }
    ...
    // 注释3
    // 创建即将要启动的Activity的描述类ActivityRecord
    ActivityRecord r = new ActivityRecord(mService, callerApp, callingPid, callingUid,
    	callingPackage, intent, resolvedType, aInfo, mService.getGlobalConfiguration(),
        resultRecord, resultWho, requestCode, componentSpecified, voiceSession != null,
        mSupervisor, container, options, sourceRecord);
    if (outActivity != null) {
        // 注释4
        outActivity[0] = r;
    }

    ...

    doPendingActivityLaunchesLocked(false);
    // 注释5
    return startActivity(r, sourceRecord, voiceSession, voiceInteractor, startFlags, true, options, inTask, outActivity);
}
```
ActivityStarter 的 startActivity 方法逻辑比较多，这里列出部分需要注意的代码。在注释1处判断 IApplicationThread 类型的 caller 是否为null，
这个 caller 是方法调用一路传过来的，指向的是 Launcher 所在的应用程序进程的 ApplicationThread 对象，

在注释2处调用 AMS 的g etRecordForAppLocked 方法得到的是代表 Launcher 进程的 callApp 对象，它是 ProcessRecord 类型的，ProcessRecord 用于描述一个应用程序进程。

同样的，ActivityRecord 用于描述一个 Activity ，用来记录一个Activity的所有信息。

接下来创建 ActivityRecord，用于描述将要启动的 Activity，并在注释4处将创建的 ActivityRecord 赋值给 ActivityRecord[] 类型的 outActivity ，这个 outActivity 会作为注释5处的startActivity方法的参数传递下去。

```java
private int startActivity(final ActivityRecord r, ActivityRecord sourceRecord,
	IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
    int startFlags, boolean doResume, ActivityOptions options, TaskRecord inTask,
    ActivityRecord[] outActivity) {
    
    int result = START_CANCELED;
    try {
    	mService.mWindowManager.deferSurfaceLayout();
        // 注释1
        result = startActivityUnchecked(r, sourceRecord, voiceSession, voiceInteractor,
             startFlags, doResume, options, inTask, outActivity);
    } 
    ...
    return result;
}
```

startActivity方法接着调用了startActivityUnchecked方法：

```java
private int startActivityUnchecked(final ActivityRecord r, ActivityRecord sourceRecord,
            IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
            int startFlags, boolean doResume, ActivityOptions options, TaskRecord inTask,
            ActivityRecord[] outActivity) {

        ...
        // 注释1
        if (mStartActivity.resultTo == null && mInTask == null && !mAddingToTask
                && (mLaunchFlags & FLAG_ACTIVITY_NEW_TASK) != 0) {
            newTask = true;
            // 注释2
            // 创建新的TaskRecord
            result = setTaskFromReuseOrCreateNewTask(
                    taskToAffiliate, preferredLaunchStackId, topStack);
        } else if (mSourceRecord != null) {
            result = setTaskFromSourceRecord();
        } else if (mInTask != null) {
            result = setTaskFromInTask();
        } else {
            // This not being started from an existing activity, and not part of a new task...
            // just put it in the top task, though these days this case should never happen.
            setTaskToCurrentTopOrCreateNewTask();
        }
    	...
            
        if (mDoResume) {
            final ActivityRecord topTaskActivity =
                    mStartActivity.getTask().topRunningActivityLocked();
            if (!mTargetStack.isFocusable()
                    || (topTaskActivity != null && topTaskActivity.mTaskOverlay
                    && mStartActivity != topTaskActivity)) {
 			...
            } else {
                if (mTargetStack.isFocusable() && 															!mSupervisor.isFocusedStack(mTargetStack)) {
                    	mTargetStack.moveToFront("startActivityUnchecked");
                }
                // 注释3
                mSupervisor.resumeFocusedStackTopActivityLocked(mTargetStack, 							mStartActivity,mOptions);
            }
        } else {
            mTargetStack.addRecentActivityLocked(mStartActivity);
        }
        ...
    }
```
startActivityUnchecked 方法主要处理与栈管理相关的逻辑。

在前面 Launcher 的 startActivitySafely 中，将 Intent的Flag 设置为 FLAG_ACTIVITY_NEW_TASK，这样注释1处的条件判断就会满足，
接着执行注释2处的 setTaskFromReuseOrCreateNewTask 方法内部会创建一个新的 Activity 任务栈。
在注释3处会调用 ActivityStackSupervisor 的 resumeFocusedStackTopActivityLocked 方法：
```java
boolean resumeFocusedStackTopActivityLocked(
    	ActivityStack targetStack, ActivityRecord target, ActivityOptions targetOptions){
    if (targetStack != null && isFocusedStack(targetStack)) {
        return targetStack.resumeTopActivityUncheckedLocked(target, targetOptions);
    }
    // 注释1
    // 获取要启动的Activity所在的栈的栈顶的不是处于停止状态的ActivityRecord
    final ActivityRecord r = mFocusedStack.topRunningActivityLocked();
    // 注释2
    if (r == null || r.state != RESUMED) {
        // 注释3
        mFocusedStack.resumeTopActivityUncheckedLocked(null, null);
    } else if (r.state == RESUMED) {
        // Kick off any lingering app transitions form the MoveTaskToFront operation.
        mFocusedStack.executeAppTransition(targetOptions);
    }
    return false;
}
```

注释1处调用 ActivityStack 的 topRunningActivityLocked 方法获取要启动的 Activity 所在栈的栈顶并且不是停止状态的 ActivityRecord。
在注释2处，如果 ActivityRecord 不为 null，或者要启动的 Activity 的状态不是 RESUMED 状态，就会调用注释3处的 ActivityStack 的 resumeTopActivityUncheckedLocked 方法，
对于即将启动的 Activity，注释2处条件判断是肯定满足的，接下来查看 ActivityStack 的 resumeTopActivityUnchecked 方法：

```java
boolean resumeTopActivityUncheckedLocked(ActivityRecord prev, ActivityOptions options) {
    if (mStackSupervisor.inResumeTopActivity) {
        // Don't even start recursing.
        return false;
    }

    boolean result = false;
    try {
        // Protect against recursion.
        mStackSupervisor.inResumeTopActivity = true;
        // 注释1
        result = resumeTopActivityInnerLocked(prev, options);
    } 
    ...
}
```

紧接着查看注释1处的 ActivityStack 的 resumeTopActivityInnerLocked 方法：

```java
private boolean resumeTopActivityInnerLocked(ActivityRecord prev, ActivityOptions options) { 
    ...
        // 注释1
            mStackSupervisor.startSpecificActivityLocked(next, true, true);
        }

        if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
        return true;
    }
```
接着进入注释1处，ActivityStackSupervisor 的 startSpecificActivityLocked 方法
```java
void startSpecificActivityLocked(ActivityRecord r,boolean andResume, boolean checkConfig) {
    // Is this activity's application already running?
    // 注释1
    // 获取即将启动的Activity的所在的应用进程
    ProcessRecord app = mService.getProcessRecordLocked(r.processName,
                        r.info.applicationInfo.uid, true);

    r.getStack().setLaunchTime(r);
	// 注释2
    // 判断要启动的Activity所在的应用程序进程如果已经运行的话，就会调用注释3处
    if (app != null && app.thread != null) {
        try {
            if ((r.info.flags&ActivityInfo.FLAG_MULTIPROCESS) == 0
                || !"android".equals(r.info.packageName)) {
                    
                app.addPackage(r.info.packageName, r.info.applicationInfo.versionCode,
                            mService.mProcessStats);
                }
                // 注释3
                // 这个方法第二个参数是代表要启动的Activity所在的应用程序进程的ProcessRecord
                realStartActivityLocked(r, app, andResume, checkConfig);
                return;
            } catch (RemoteException e) {
                Slog.w(TAG, "Exception when starting activity "
                        + r.intent.getComponent().flattenToShortString(), e);
            }

            // If a dead object exception was thrown -- fall through to
            // restart the application.
        }

        mService.startProcessLocked(r.processName, r.info.applicationInfo, true, 0,
                "activity", r.intent.getComponent(), false, false, true);
    }
```

​ 在注释1处获取即将启动的Activity所在的应用程序进程，在注释2处判断要启动的Activity所在的应用程序进程如果已经运行的话，就会调用注释3处的realStartActivityLocked方法，这个方法的第二个参数是代表要启动的Activity所在的应用程序进程的ProcessRecord。接下来进入realStartActivityLocked方法：
```java
final boolean realStartActivityLocked(ActivityRecord r, ProcessRecord app,
      boolean andResume, boolean checkConfig) throws RemoteException {
    ...
    // 注释1
    // app是ProcessRecord类型的，thread是IApplicationThread类型的 
	app.thread.scheduleLaunchActivity(new Intent(r.intent), r.appToken,
    	System.identityHashCode(r), r.info,
        mergedConfiguration.getGlobalConfiguration(),
        mergedConfiguration.getOverrideConfiguration(), r.compat,
        r.launchedFromPackage, task.voiceInteractor, app.repProcState, r.icicle,
        r.persistentState, results, newIntents, !andResume,
        mService.isNextTransitionForward(), profilerInfo);
        ...
        return true;
}
```
注释1处的 app.thread 指的是 IApplicationThread，其中 ApplicationThread 继承了 IApplicationThread.Stub 。app 指的是传入的要启动的 Activity 所在的应用程序进程，
因此这段代码指的就是要在目标应用程序进程启动 Activity。当前代码逻辑运行在 AMS 所在的进程( SystemServer 系统服务进程)中，通过 ApplicationThread 来与应用程序进程进程 Binder 通信(跨进程通信)，也就是说 ApplicationThread 是 AMS 和应用程序进程的通信桥梁

![image](https://user-images.githubusercontent.com/17560388/125584585-dfaca878-5865-4e32-91a2-7137253871b7.png)

## 3.ActivityThread启动Activity的过程
通过前面的分析，我们知道代码逻辑目前运行到应用程序进程中，ActivityThread启动Activity过程时序图如下：


![image](https://user-images.githubusercontent.com/17560388/125588057-39c5c7ed-d6c4-4bbf-bdf5-179cec024c47.png)


接着查看 ApplicationThread 的 scheduleLaunchActivity 方法，ApplicationThread 是 ActivityThread 的内部类，ActivityThread 负责管理当前应用程序进程的主线程，scheduleLaunchActivity 方法代码如下：
```java
@Override
public final void scheduleLaunchActivity(Intent intent, IBinder token, int ident,
		ActivityInfo info, Configuration curConfig, Configuration overrideConfig,
		CompatibilityInfo compatInfo, String referrer, IVoiceInteractor voiceInteractor,
		int procState, Bundle state, PersistableBundle persistentState,
		List<ResultInfo> pendingResults, List<ReferrerIntent> pendingNewIntents,
		boolean notResumed, boolean isForward, ProfilerInfo profilerInfo) {

	updateProcessState(procState, false);
	// 此处的r就是保存要启动activity的信息，作为参数通过sendMessage发送出去
	ActivityClientRecord r = new ActivityClientRecord();

    r.token = token;
    r.ident = ident;
    r.intent = intent;
    r.referrer = referrer;
    ...
    updatePendingConfiguration(curConfig);
    sendMessage(H.LAUNCH_ACTIVITY, r);
}
```
scheduleLaunchActivity 方法将启动 Activity 的参数封装成 ActivityClientRecord。sendMessage 方法向 H 类发送类型为 LAUNCH_ACTIVITY 的消息，并将 ActivityClientRecord 传递出去，sendMessage 方法有多个重载，最终调用的 sendMessage 方法如下：
```java
private void sendMessage(int what, Object obj, int arg1, int arg2, boolean async) {
    if (DEBUG_MESSAGES) Slog.v(
        TAG, "SCHEDULE " + what + " " + mH.codeToString(what)
        + ": " + arg1 + " / " + obj);
    Message msg = Message.obtain();
    msg.what = what;
    msg.obj = obj;
    msg.arg1 = arg1;
    msg.arg2 = arg2;
    if (async) {
        msg.setAsynchronous(true);
    }
    mH.sendMessage(msg);
}
```
这里的mH指的是H，它是 ActivityThread 的内部类并继承 Handler，是应用程序进程中主线程的消息管理类。因为 ApplicationThread 是一个Binder，它的调用逻辑运行在 Binder 线程池(子线程)中，所以这里需要用 H 将代码的逻辑切换到主线程中。H 类中处理消息的 handleMessage 方法如下：
```java
public void handleMessage(Message msg) {
    if (DEBUG_MESSAGES) Slog.v(TAG, ">>> handling: " + codeToString(msg.what));
    switch (msg.what) {
        case LAUNCH_ACTIVITY: {
            Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "activityStart");
            // 注释1
            // 取出之前保存的要启动的Activity的信息
            // 这些信息封装在ActivityClientRecord这个类中
            final ActivityClientRecord r = (ActivityClientRecord) msg.obj;
			// 注释2
            // pachageinfo 指的是LoadedApk
            r.packageInfo = getPackageInfoNoCheck(
                r.activityInfo.applicationInfo, r.compatInfo);
            handleLaunchActivity(r, null, "LAUNCH_ACTIVITY");
            Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
        } break;
        case RELAUNCH_ACTIVITY: {
            Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "activityRestart");
            ActivityClientRecord r = (ActivityClientRecord)msg.obj;
            handleRelaunchActivity(r);
            Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
        } break;
        ...
    }
    ...
}
```
查看 H 类中 handleMessage 方法中对 LAUNCH_ACTIVITY 的处理，在注释1处将传过来的 msg 的成员变量 obj 转换为 ActivityClientRecord。在注释2处通过 getPackageInfoNoCheck 方法获得LoadedApk 类型的对象并赋值给 ActivityClientRecord 的成员变量 packageInfo。应用程序要启动 Activity 时需要将该 Activity 所属的 APK 加载进来，而 LoadedApk 就是用来描述已加载的 APK 文件的。接着调用 handleLaunchActivity 方法：
```java
private void handleLaunchActivity(ActivityClientRecord r, Intent customIntent, String reason) {
	...   

    // Initialize before creating the activity
    WindowManagerGlobal.initialize();
    // 注释1 启动Activity
    Activity a = performLaunchActivity(r, customIntent);

    if (a != null) {
        r.createdConfig = new Configuration(mConfiguration);
        reportSizeConfigurations(r);
        Bundle oldState = r.state;
        // 注释2 将Activity的状态置为Resume
        handleResumeActivity(r.token, false, r.isForward,
              !r.activity.mFinished && !r.startsNotResumed, r.lastProcessedSeq, reason);

        if (!r.activity.mFinished && r.startsNotResumed) {

            performPauseActivityIfNeeded(r, reason);

            if (r.isPreHoneycomb()) {
                r.state = oldState;
            }
        }
    } else {
        // If there was an error, for any reason, tell the activity manager to stop us.
        try {
            // 注释3 停止Activity启动
            ActivityManager.getService()
                .finishActivity(r.token, Activity.RESULT_CANCELED, null,
                                Activity.DONT_FINISH_TASK_WITH_ACTIVITY);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }
}
```
注释1处的 performLaunchActivity 方法用来启动 Activity，
注释2处的代码用来将 Activity 的状态设置为 Resume，如果该 Activity 为 null，就会执行注释3处的逻辑，
通知AMS停止启动 Activity，接下来进入 performLaunchActivity：
```java
private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) 
	// 注释1 获取ActivityInfo类
    ActivityInfo aInfo = r.activityInfo;
    if (r.packageInfo == null) {
        // 注释2 获取APK文件的描述类
        r.packageInfo = getPackageInfo(aInfo.applicationInfo, r.compatInfo,
                                       Context.CONTEXT_INCLUDE_CODE);
    }
	// 注释3
	// 获取要启动Activity的ComponentName类，它保存了该Activity的包名和类名
    ComponentName component = r.intent.getComponent();
    ...
    // 注释4
    // 创建要启动Activity的上下文
    ContextImpl appContext = createBaseContextForActivity(r);
    Activity activity = null;
    try {
        java.lang.ClassLoader cl = appContext.getClassLoader();
        // 注释5
        // 通过类加载器来创建要启动的Activity的实例
        activity = mInstrumentation.newActivity(
            cl, component.getClassName(), r.intent);
        ...
        }
    } catch (Exception e) {
        ...
    }

    try {
        // 注释6
        // 创建Application
        Application app = r.packageInfo.makeApplication(false, mInstrumentation);
        ...
        if (activity != null) {
            ...
            // 注释7
            // 初始化Activity
            activity.attach(appContext, this, getInstrumentation(), r.token,
                            r.ident, app, r.intent, r.activityInfo, title, r.parent,
                            r.embeddedID, r.lastNonConfigurationInstances, config,
                            r.referrer, r.voiceInteractor, window, r.configCallback);

            ...
            if (r.isPersistable()) {
                // 注释8
          	// 顾名思义下一步就是去调用Activity的onCreate方法
                mInstrumentation.callActivityOnCreate(activity, r.state, 								r.persistentState);
            } else {
                mInstrumentation.callActivityOnCreate(activity, r.state);
            }
            ...
        r.paused = true;
        mActivities.put(r.token, r);

    } catch (SuperNotCalledException e) {
        throw e;
    } catch (Exception e) {
        ...
    }
    return activity;
    }
```
在注释1处获取 ActivityInfo，用于存储代码以及 AndroidManifest 设置的 Activity 和 Receiver 节点信息，比如 Activity 的 theme 和 launchMode。

在注释2处获取 APK 文件的描述类 LoadedApk。

在注释3处获取要启动的 Activity 的 ComponentName 类，在该类中保存了 Activity 的包名和类名。

注释4处用来创建要启动 Activity 的上下文环境。

注释5处根据 ComponentName 中存储的 Activity 类名，用类加载器来创建该 Activity 的实例。

注释6处用来创建 Application，makeApplication 方法内部会调用 Application 的 onCreate 方法。

注释7处调用 Activity 的 attach 方法初始化 Activity，在 attach 方法中会创建 Window 对象( PhoneWindow )并与 Activity 自身进行关联。

在注释8处调用 Instrumentation 的 callActivityOnCreate 方法：
```java
public void callActivityOnCreate(Activity activity, Bundle icicle,
            PersistableBundle persistentState) {
    prePerformCreate(activity);
    activity.performCreate(icicle, persistentState);
    postPerformCreate(activity);
}
```
进入 activity 的 performCreate 方法：
```java
final void performCreate(Bundle icicle, PersistableBundle persistentState) {
    restoreHasCurrentPermissionRequest(icicle);
    // 注释1
    // 正式进入Activity的生命周期方法
    onCreate(icicle, persistentState);
    mActivityTransitionState.readState(icicle);
    performCreateCommon();
}
```
如注释1处代码所示，正式进入 Activity 的生命周期方法，根 Activity 的启动流程大致分析完了，下面对大致流程进行小结一下。

## 4.总结
根 Activit y的启动过程涉及4个进程，分别是 Launcher 进程、AMS 所在进程( SystemServer 进程)、Zygote 进程、应用程序进程，首先 Launcher 进程向 AMS 请求创建根 Activity，AMS 会判断根Activity 所需要的应用程序进程是否存在并启动，如果不存储就会请求 Zygote 进程创建应用程序进程。应用程序进程启动后，AMS 会请求应用程序进程启动根 Activity。




