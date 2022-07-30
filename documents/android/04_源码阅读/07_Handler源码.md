## 1.前言

`app` 的启动的入口是 `ActivityThread` 的 `main` 方法。大多数UI系统会采用消息队列维护一个 `main线程` 单独控制视图的绘制，如 `win32`、`Android`、`Java Swing`。

我们先看下在 ActivityThread.main 方法中 关于 Looper 的代码。

```java
class ActivityThread{
    public static void main(String[] args) {
        // 1、创建主线程对应的 Looper 实例
        Looper.prepareMainLooper();
	//...
	// 2、开启事件循环
        Looper.loop();
    }
    static volatile Handler sMainThreadHandler;
}
```
注意两处核心代码

## 2. Looper分析

Looper.prepareMainLooper 分析

```java
class Looper{
    //ThreadLocal 是一个线程独有的对象，可以简单理解为线程隔离
    static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<Looper>();
    @Deprecated
    public static void prepareMainLooper() {
        prepare(false); // 注意这里的传入的参数为 false，在 Looper 的构造方法中会用到
        synchronized (Looper.class) {
            sMainLooper = myLooper(); //将主线程的 Looper 赋值给静态引用 sMainLooper
        }
    }
    
    public static @Nullable Looper myLooper() {
        return sThreadLocal.get();
    }

    public static void prepare() {
        prepare(true);
    }
    
    private static void prepare(boolean quitAllowed) {
        //如果 looper 已经存在直接抛出异常，一个线程只允许有一个 Looper 对象
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
	//创建一个 lopper 实例，并赋值给到当前线程 sThreadLocal 
        sThreadLocal.set(new Looper(quitAllowed));
    }
    
    // Looper 的构造方法，Looper 创建的时候会初始化一个 MessageQueue 对象
    // main线程中 quitAllowed 为 false，表示 MessageQueue 不可以退出
    private Looper(boolean quitAllowed) {
        mQueue = new MessageQueue(quitAllowed);
        mThread = Thread.currentThread();
    }
}
```

`Looper.prepare` 主要作用是创建一个 Looper 实例并存储到当前线程的 sThreadLocal 中。sThreadLocal 是一个 ThreadLocal 对象，ThreadLocal 用于各个线程不共享变量值的操作。
在 Looper 的构造方法中会生成一个 MessageQueue 对象，继续看一下 Looper.loop 方法。

```java
class Looper{
    public static void loop() {      	
    	final Looper me = myLooper(); // 得到当前线程对应实例
    	final MessageQueue queue = me.mQueue; // 得到 Looper 对应 MessageQueue
    	boolean slowDeliveryDetected = false;
    	for (;;) {
            //取出消息队列
            Message msg = queue.next(); // might block
            if (msg == null) { //没有消息那么就退出循环
             	return;
            }
            // 通过该方法可以在 Looper 中自定义一个日志器的实例，用于统计主线程是否耗时了
            final Printer logging = me.mLogging;
            if (logging != null) {
            logging.println(">>>>> Dispatching to " + msg.target + " " 
                msg.callback + ": " + msg.what);
            }
            //调用消息分发
            msg.target.dispatchMessage(msg);
            if (observer != null) {
                observer.messageDispatched(token, msg);
            }
            if (logging != null) {
                logging.println("<<<<< Finished to " + msg.target + " " + msg.callback);
            }
        }
    }
}
```

Looper 的 loop 方法 从MessageQueue 不断取出 Message 对象，然后进行分发。

上面我们提到通过实现一个 Printer 于统计主线程耗时的技巧，下面给出一个小例子

```kotlin
class MainActivity : AppCompatActivity() {
    class MyPrinter : Printer {
        var startTime = System.currentTimeMillis()
        override fun println(x: String) {
            //事件分发处理函数开始
            if (x.contains(">>>>> Dispatching to")) {
                startTime = System.currentTimeMillis()
            }
            //事件分发处理函数开始
            if (x.contains("<<<<< Finished to ")) {
                val endTime = System.currentTimeMillis()
                //某次主线程处理耗时为duration ms
                val duration = endTime - startTime    
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //得到主线程的Looper然后挂载自己的打印
        var myLooper = Looper.getMainLooper();
        myLooper.setMessageLogging(MyPrinter())
    }
}
```
继续跟踪 MessageQueue 相关的方法

## 3、MessageQueue 分析

我们先看一下 MessageQueue 这个类的主要代码，主要还是 next() 方法

```java
class MessageQueue{ 

    private long mPtr; // used by native code //一个用于标识状态的类，这个标志位被native代码所使用
    
    Message next() {
        // Return here if the message loop has already quit and been disposed. 如果标志位为 0，那么证明消息循环已经结束，那么返回 null 即可
        final long ptr = mPtr;
        if (ptr == 0) {
            return null;
        }	
        int pendingIdleHandlerCount = -1; // -1 only during first iteration
        // 下次轮询的时间
        int nextPollTimeoutMillis = 0;
        for (;;) {
            // 如果现在最近的一个消息需要若干时间之后才执行
            if (nextPollTimeoutMillis != 0) {
            	// 刷新所有的binder命令到内核，这里不需要关心
                Binder.flushPendingCommands();
            }
            // 调用 linux 的 epoll 函数进行休眠
	    // epoll 是 linux的 下的 api，这里不需要深究只需要认为 object.wait 和 object.notify 的作用即可
	    // 当有信息要处理或者休眠到期或被内核唤醒
            nativePollOnce(ptr, nextPollTimeoutMillis);
            synchronized (this) {
                // Try to retrieve the next message.  Return if found.
                final long now = SystemClock.uptimeMillis();
                Message prevMsg = null;
                //消息队列头
                Message msg = mMessages;
                //同步屏障，稍后在讲解先跳过
                if (msg != null && msg.target == null) {
                    // Stalled by a barrier.  Find the next asynchronous message in the queue.
                    do {
                        prevMsg = msg;
                        msg = msg.next;
                    } while (msg != null && !msg.isAsynchronous());
                }
                if (msg != null) {
                    //当前消息的所预期的运行时间未到那么 设置一个休眠时间
                    if (now < msg.when) {
                        nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);
                    } else {
                        // 链表断链后返回
                        mBlocked = false;
                        if (prevMsg != null) {
                            prevMsg.next = msg.next;
                        } else {
                            mMessages = msg.next;
                        }
                        msg.next = null;
                        if (false) Log.v("MessageQueue", "Returning message: " + msg);
                        return msg;
                    }
                } else {
                    // No more messages.
                    nextPollTimeoutMillis = -1;
                }
               //...
        }
    }
}
```

Message 作为队列节点的数据结构，我们看下他的属性：

```java
public final class Message implements Parcelable {
   	
    public int what; // 用户多定义的消息标识号
    /**
     * 目标消息要送达的时间。这个时间是基于{@link SystemClock#uptimeMillis}.
     */
    @UnsupportedAppUsage
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public long when;
	
    // 所对应的 handler，如果这个为空那么标识这个消息作为同步屏障
    @UnsupportedAppUsage
    /*package*/ Handler target;
    
    // 如果你希望直接回调自定义的函数那么赋值这个即可
    @UnsupportedAppUsage
    /*package*/ Runnable callback;

    // 链表的下一个节点
    @UnsupportedAppUsage
    /*package*/ Message next;
}
```

Message 在 MessageQueue 中按照时间戳排序，when越小越靠前

## 4. MessageQueue入队的操作函数
```java
class MessageQueue{

    // Indicates whether next() is blocked waiting in pollOnce() with a non-zero timeout.
    private boolean mBlocked;
    
    final long ptr = mPtr; //当 mPtr 为0时退出消息循环，在构造 MessageQueue 的时候会初始化一个非0数值
    // when 表示此消息要送达的时间
    boolean enqueueMessage(Message msg, long when) {
        synchronized (this) {       
            msg.markInUse();
            msg.when = when;
            Message p = mMessages;
            boolean needWake;
            // 当前队列是空的还未初始化 那么进行初始化
            if (p == null || when == 0 || when < p.when) {
                // New head, wake up the event queue if blocked.
                msg.next = p;
                mMessages = msg;
                needWake = mBlocked;
            } else {
                // 插入消息在队列中，在多数情况我们不需要唤醒队列，除非有一个同步屏障在头队列头且消息是一个最早的异步消息，这里可以简单理解为了快速响应异步消息
                needWake = mBlocked && p.target == null && msg.isAsynchronous();     
                // 根据消息的优先级插入到队列中
                Message prev;
                for (;;) {
                    prev = p;
                    p = p.next;
                  
                    if (p == null || when < p.when) {
                        break;
                    }
                    if (needWake && p.isAsynchronous()) {
                        needWake = false;
                    }
                }
                msg.next = p; // invariant: p == prev.next
                prev.next = msg;
            }
            // We can assume mPtr != 0 because mQuitting is false.
            // 因为之前判断过当前消息队列没有退出( mQuitting is false),所以我们假设
            //mPtr!=0
            if (needWake) {
           	//mPtr==0 标识已经退出
           	//nativeWake 使用 linux 底层的 epool 唤醒队列，并且使用 mPtr 告知唤醒的消息队列状态
                nativeWake(mPtr);
            }
        }
        return true;
    }
}
```

## 5. Handler分析

为了构建一个和其他线程通信的桥梁，Android 提供 Handler，方便我们使用 Looper 完成线程通信和切换.

Handler 的构造函数
```java
class Handler{
    //callback: 当消息对象 Message 没有设置特有回调时，就先会先用 Handler.CallBack，然后再根据返回值为false再去调用Handler自身的handleMessage函数
    //async: 是否允许开启同步屏障 后文在讲叙
    public Handler(Callback callback, boolean async) 
}

public interface Callback {
    public boolean handleMessage(Message msg);
}
```

消息通过下面的方法进行分发：

```java
class Looper{
    public static void loop() {
	// 其中 msg 是 Message 类型, target 是 Handler 类型
        msg.target.dispatchMessage(msg);      
    }
}
```

消息最终调用 Handler 的 dispatchMessage(msg) 方法

```java
class Handler{
    public void dispatchMessage(Message msg) { 
        if (msg.callback != null) { //1. 如果 Message 有指定回调，直接调用 Message 的 callback
            handleCallback(msg);
        } else {
            //构造函数传入callback如果设置了根据返回值确定是否继续调用自己的handleMessage
            if (mCallback != null) { //2. 如果 handler 构造传入 callback 不为空，优先响应 mCallback。如果 mCallback 返回值为 true，结束回调。
                if (mCallback.handleMessage(msg)) 
                    return;
                }
            }
            handleMessage(msg); //3. 回调给 handler 的 handleMessage 方法
        }
    }
    //调用Message定义的回调然后直接返回
    private static void handleCallback(Message message) {
        message.callback.run();
    }
}
```
回调流程图：

<img width="500" alt="msg回调流程" src="https://user-images.githubusercontent.com/17560388/154253375-a203c5d7-6a30-46e3-9841-983abc3bd84a.png">

## 6. 同步屏障

Android 的 main线程 负责更新 ui，一般会每 16ms 重绘一次屏幕。为了防止 `UI绘制事件` 延迟，我们将 Message 分为 同步消息 和 异步消息，默认情况我们的消息都是同步消息。只需要调用 Message.setAsynchronous 即可成为异步消息.

```java
class Message{
    public boolean isAsynchronous() {
        return (flags & FLAG_ASYNCHRONOUS) != 0;
    }
    public void setAsynchronous(boolean async) {
        if (async) {
            flags |= FLAG_ASYNCHRONOUS;
        } else {
            flags &= ~FLAG_ASYNCHRONOUS;
        }
    }
}
```

成为异步消息后，还需要插入同步屏障，插入同步屏障后会优先执行异步消息。

我们回过头看下MessageQueue的异步处理部分

```java
class MessageQueue{
    Message next() {
                //...
                //如果当前的头节点是 target 是 null 那么证明插入了一个同步屏障标志
                //那么一直轮询到一个异步消息然后返回处理
                if (msg != null && msg.target == null) {
                    // Stalled by a barrier. Find the next asynchronous message in the queue.
                    do {
                        prevMsg = msg;
                        msg = msg.next;
                    } while (msg != null && !msg.isAsynchronous());
                }
               //...
        }
    }
}
```

## 7. UI更新机制
```java
class ViewRootImpl{
    //这个函数开始便利根view致性各种绘制操作
    void scheduleTraversals() {
        if (!mTraversalScheduled) {
            mTraversalScheduled = true;
            //放入一个屏障
            mTraversalBarrier = mHandler.getLooper().postSyncBarrier();
            mChoreographer.postCallback(
                    Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
            if (!mUnbufferedInputDispatch) {
                scheduleConsumeBatchedInput();
            }
            notifyRendererOfFramePending();
        }
    }
}
```

具体插入屏障代码，由于只是简单的链表操作不在解释

```java
class Looper{
    public int postSyncBarrier() {
        return mQueue.enqueueSyncBarrier(SystemClock.uptimeMillis());
    }
}

class MessageQueue(){
    int enqueueSyncBarrier(long when) {
        synchronized (this) {
            final int token = mNextBarrierToken++;
            final Message msg = Message.obtain();
            msg.markInUse();
            msg.when = when;
            msg.arg1 = token;

            Message prev = null;
            Message p = mMessages;
            if (when != 0) {
                while (p != null && p.when <= when) {
                    prev = p;
                    p = p.next;
                }
            }
            if (prev != null) { // invariant: p == prev.next
                msg.next = p;
                prev.next = msg;
            } else {
                msg.next = p;
                mMessages = msg;
            }
            return token;
        }
    }
    //移除
    void removeSyncBarrier(int token) {
        // Remove a sync barrier token from the queue.
        // If the queue is no longer stalled by a barrier then wake it.
        synchronized (this) {
            Message prev = null;
            Message p = mMessages;
            while (p != null && (p.target != null || p.arg1 != token)) {
                prev = p;
                p = p.next;
            }
            if (p == null) {
                throw new IllegalStateException("The specified message queue synchronization "
                        + " barrier token has not been posted or has already been removed.");
            }
            final boolean needWake;
            if (prev != null) {
                prev.next = p.next;
                needWake = false;
            } else {
                mMessages = p.next;
                needWake = mMessages == null || mMessages.target != null;
            }
            p.recycleUnchecked();

            // If the loop is quitting then it is already awake.
            // We can assume mPtr != 0 when mQuitting is false.
            if (needWake && !mQuitting) {
                nativeWake(mPtr);
            }
        }
    }
}    
```
IdleHandler
MessageQueue我们重新看下这个类的next函数
```java
//MessageQueue.java
Class MessageQueue{
    private IdleHandler[] mPendingIdleHandlers;
    Message next() {
        final long ptr = mPtr;
        if (ptr == 0) {
            return null;
        }
        int pendingIdleHandlerCount = -1; // -1 only during first iteration
        int nextPollTimeoutMillis = 0;
        for (;;) {
            if (nextPollTimeoutMillis != 0) {
                Binder.flushPendingCommands();
            }
            nativePollOnce(ptr, nextPollTimeoutMillis);
            synchronized (this) {
                final long now = SystemClock.uptimeMillis();
                Message prevMsg = null;
                Message msg = mMessages;
                if (msg != null && msg.target == null) { //如果有消息就分发 然后返回
 		
 		}

             //如果没有消息那么就调用IdleHandler集合中的对象
            for (int i = 0; i < pendingIdleHandlerCount; i++) {
                final IdleHandler idler = mPendingIdleHandlers[i];
                mPendingIdleHandlers[i] = null; // release the reference to the handler
                boolean keep = false;
        	//调用函数
                keep = idler.queueIdle();
                //如果函数返回false，那么会将其移除
                if (!keep) {
                    synchronized (this) {
                        mIdleHandlers.remove(idler);
                    }
                }
            }
            pendingIdleHandlerCount = 0;
            nextPollTimeoutMillis = 0;
        }
    }
}
```
