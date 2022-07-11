
本文RxJava源码版本为2.1.13，RxAndroid版本为2.0.2。

## 1. 基本订阅流程

在正文开始前首先我们要明确下面一点:

>RxJava 的基本流程：被观察者通过`subscribe`方法和观察者建立订阅关系，订阅关系建立的同时开始进行事件流的传递。

```java
Observable.subscribe(Observer)
```

下面我们以`Observable.create`操作符为例来分析一下源码

```kotlin
//1、调用 Observable#create(ObservableOnSubscribe) 方法创建一个 Observable 对象 
Observable.create(ObservableOnSubscribe<String> { emitter ->
        emitter.onNext("1")
        emitter.onNext("2")
        emitter.onComplete()
    }
}).map{//2、把String 转换成 Int
    it.toInt()
}.subscribe(object : Observer<Int> {//3、建立订阅关系
    override fun onSubscribe(d: Disposable) {
        System.out.print("onSubscribe\n")
    }

    override fun onNext(t: Int) {
        System.out.print("onNext $t\n")
    }
    
    override fun onComplete() {
        System.out.print("onComplete\n")
    }

    override fun onError(e: Throwable) {
        System.out.print("onError\n")
    }
})
```

咱们简单的分析一下上面的代码

- 1.通过 `Observable#create(ObservableOnSubscribe)` 方法创建一个 Observable 对象，并产生事件"1"、"2"、complete
- 2.通过 `map` 将`String`类型的事件转换为`Int`
- 3.调用 `subscribe()` 和观察者建立订阅关系

跟踪源码之前我们先简单的介绍几个主要的类

- `ObservableSource` : 一个接口，定了`subscribe(Observer)`方法，调用该方法会建立`Observable`和`Observer`的订阅关系 

- `Observable` : 被观察者，发送被`Observer`观察的数据流。`Observable`实现了`ObservableSource`接口，并重载`subscribe(Observer)`方法，在`subscribe(Observer)`方法内部会调换用自身的`subscribeActual`方法。该方法是一个抽象方法，由`Observable`的子类实现。

- `Observer` : 数据的观察者，定义了`onSubscribe(Dispose)`、 `onNext(T t)`、`onComplete()`、`onError(Excception)`方法。

- `ObservableOnSubscribe` : 接口类型，它内部只有一个 `void subscribe(@NonNull ObservableEmitter<T> emitter)` 方法。`subscribe`方法会传入一个`ObservableEmitter`对象

- `Emitter` : 定义了发射数据的方法`onNext(T t)`、`onComplete()`、`onError(Excception)`，只在`create`等操作符中才会出现。

- `ObservableEmitter` : `Emitter` 的子类，也是一个接口。相比于`Emitter`拓展了`Disposable`的能力


### 1.1 先看下 1）处 Observable.create(ObservableOnSubscribe) 的方法：

`create` 是 `Observable` 的静态方法。该方法传入一个`ObservableOnSubscribe`参数，然后返回一个`Observable`对象。代码如下

```java
@CheckReturnValue
@SchedulerSupport(SchedulerSupport.NONE)
public static <T> Observable<T> create(ObservableOnSubscribe<T> source) {
    // 检查入参source是否为null
    ObjectHelper.requireNonNull(source, "source is null");
    // 1️⃣ 尝试使用RxJavaPlugins.onObservableAssembly这个方法进行转换
    return RxJavaPlugins.onAssembly(new ObservableCreate<T>(source));
}
```
1️⃣: `RxJavaPlugins.onAssembly`尝试将一个`Observable`对象转换为另外一个`Observable`对象。当然，这取决与转换器`onObservableAssembly`是否为空；默认情况下，它是空的，但是可以通过`RxJavaPlugins`的静态方法进行`get/set`，如下面代码所示。

```java
@NonNull
public static <T> Observable<T> onAssembly(@NonNull Observable<T> source) {
    Function<? super Observable, ? extends Observable> f = onObservableAssembly;
    if (f != null) {
        return apply(f, source);
    }
    return source;
}
```

由于`f`为空，所以这里直接返回了`source`也就是刚刚`new`好的`ObservableCreate<T>(source)`。

### 1.2 跟踪 ObservableCreate 类

先看下`ObservableCreate`类的源码：该类是`Observable`的子类，并实现了`subscribeActual`方法。在`subscribeActual`方法中会生成一个`CreateEmitter`对象。然后将`CreateEmitter`传入`ObservableOnSubscribe`的`subscribe`方法中。

```java
public final class ObservableCreate<T> extends Observable<T> {
    final ObservableOnSubscribe<T> source;

    public ObservableCreate(ObservableOnSubscribe<T> source) {
        this.source = source;
    }
    
    //实现父类 Observable 的抽象方法 subscribeActual，该方法会在 observable.subcribe(observer) 调用的时候执行
    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        // 1️⃣ 创建一个 CreateEmitter 对象，CreateEmitter 继承 AtomicReference 并实现了 ObservableEmitter 和 Disposable 接口
        CreateEmitter<T> parent = new CreateEmitter<T>(observer);
        // 2️⃣ 将 parent(CreateEmitter) 作为 dispose 返回
        observer.onSubscribe(parent); //这个回调是告诉 observer 开始订阅了

        try {
            // 3️⃣ 调用 ObservableOnSubscribe 对象的 subscribe 方法，执行该方法里面的 emitter 操作
            source.subscribe(parent); //上文例子中会发送 “1”、“2”、”complete“
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            parent.onError(ex);
        }
    }
    
    // CreateEmitter 的 onNext、onError、onComplete 会直接调用 observer 的 onNext、onError、onComplete 方法
    static final class CreateEmitter<T> extends AtomicReference<Disposable> implements ObservableEmitter<T>, Disposable{
     
        final Observer<? super T> observer;
        CreateEmitter(Observer<? super T> observer) {
            this.observer = observer;
        }

        @Override
        public void onNext(T t) {
            if (!isDisposed()) {
                observer.onNext(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!tryOnError(t)) {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!isDisposed()) {
                try {
                    observer.onComplete();
                } finally {
                    dispose();
                }
            }
        }
    }
    //...
}
```

1️⃣处的`CreateEmitter` 继承自 `AtomicReference` 并实现了 `ObservableEmitter` 和 `Disposable` 接口

- 对于`ObservableEmitter`接口来说，该类就是在对应的接口方法中调用构造器入参`observer`的对应发射方法 
- 对于`Disposable`接口来说，该类向客户端返回了一个可以`dispose`操作的对象`parent`；其原理是根据`AtomicReference<Disposable>`是否为`DISPOSED`判断是不是处于`disposed`状态

因此，开头的例子中最后的链式调用部分就等价于：

```java
ObservableCreate<String>(source).map(function).subscribe(resultObserver)
```

### 1.3 下面看一下map操作符

```java
@CheckReturnValue
@SchedulerSupport(SchedulerSupport.NONE)
public final <R> Observable<R> map(Function<? super T, ? extends R> mapper) {
    ObjectHelper.requireNonNull(mapper, "mapper is null");
    return RxJavaPlugins.onAssembly(new ObservableMap<T, R>(this, mapper));
}
```
这段代码与create操作符类似，这里直接返回了一个ObservableMap。

所以，开头的例子中最后的链式调用部分再次展开为：

```java
ObservableMap<String, Int>(
    ObservableCreate<String>(source),
    function
).subscribe(resultObserver)
```
在上面，我们已经创建好了两个`Observable`，一个原始的创建数据的`ObservableCreate`以及一个用于转换的`ObservableMap`。

### 1.4 示例程序的`subscribe`操作

接下来，我们回到示例程序的`subscribe`操作中，我们知道`subscribe`是`ObservableSource`接口的方法，该方法在抽象类`Observable`中进行了重写，在重写方法中交给了抽象方法`subscribeActual`来实现，我们看看这部分代码：`Observable#subscribe(Observer)`

```java
@SchedulerSupport(SchedulerSupport.NONE)
@Override
public final void subscribe(Observer<? super T> observer) {
    // 检查入参observer是否为空
    ObjectHelper.requireNonNull(observer, "observer is null");
    try {
        //默认情况下返回入参参数observer
        observer = RxJavaPlugins.onSubscribe(this, observer);
        // 检查转换后的observer是否为空
        ObjectHelper.requireNonNull(observer, "Plugin returned null Observer");
        // 调用抽象方法subscribeActual进行真正的订阅
        subscribeActual(observer);
    } catch (NullPointerException e) { // NOPMD
        throw e;
    } catch (Throwable e) {
        Exceptions.throwIfFatal(e);
        // can't call onError because no way to know if a Disposable has been set or not
        // can't call onSubscribe because the call might have set a Subscription already
        RxJavaPlugins.onError(e);

        NullPointerException npe = new NullPointerException("Actually not, but can't throw other exceptions due to RS");
        npe.initCause(e);
        throw npe;
    }
}
```

由于先执行的是最近的`Observable`也就是`ObservableMap`，我们先看看其`subscribeActual`方法：

```java
public final class ObservableMap<T, U> extends AbstractObservableWithUpstream<T, U> {
    final Function<? super T, ? extends U> function;

    public ObservableMap(ObservableSource<T> source, Function<? super T, ? extends U> function) {
        super(source);
        this.function = function;
    }

    @Override
    public void subscribeActual(Observer<? super U> t) {
        source.subscribe(new MapObserver<T, U>(t, function));
    }

    static final class MapObserver<T, U> extends BasicFuseableObserver<T, U> {
        ...
    }
}
```
`MapObserver`简单来说就是在`onNext`方法中会将原始值`t`用转换方法`mappper`进行转换，然后调用构造器入参`actual`这个`Observer`的`onNext`方法

2️⃣:调用`observer.onSubscribe(parent)`，因为`observer.actual`为`resultObserver`，所以通知我们写的消费者开始进行订阅了。

3️⃣:调用`source.subscribe(parent)`，正式开始订阅。 
  - 这里的`source`就是最原始的数据源，这里将`parent`作为参数`emitter`传入到`source`的`subscribe`方法中，然后在该方法中我们调用了其`onNext`、`onComplete`方法。实际上调用的就是`CreateEmitter`的对应的方法 
  - 在`CreateEmitter.onNext`中会调用`observer.onNext`，这里的`observer`就是`MapObserver` 
  - 在`MapObserver.onNext`中会将原始值`t`用转换方法`mapper`进行转换，然后调用构造器入参`actual`这个`Observer`的`onNext`方法。最后的`actual`实际上就是`resultObserver`。 
  - 这样，原始数据"1"经过`CreateEmitter`的发射后，在`MapObserver`中经过`mapper`转换最后到了`resultObserver`中

几个基本类之间的关系：

<img width="700" alt="以create为例，相关类的UML图" src="https://user-images.githubusercontent.com/17560388/177450216-091c09fe-2b56-4cb5-b039-abab98fdc02e.png">

## 2. 线程切换

本节以下面的示例为例，和上边的例子相比，增加了`subscribeOn`和`observeOn`操作符

```kotlin
Observable.create(ObservableOnSubscribe<String> { emitter ->
    Log.e("TAG", "subscribe(): " + Thread.currentThread().name)
    emitter.onNext("1")
    emitter.onNext("2")
    emitter.onComplete()
}).subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(object : Observer<String> {
        override fun onComplete() {
            Log.e("TAG", "onComplete(): " + Thread.currentThread().name)
        }
        
        override fun onSubscribe(d: Disposable) {
            Log.e("TAG", "onSubscribe(): " + Thread.currentThread().name)
        }
        
        override fun onNext(t: String) {
            Log.e("TAG", "onNext(): " + Thread.currentThread().name)
        }
        
        override fun onError(e: Throwable) {
            Log.e("TAG", "onError(): " + Thread.currentThread().name)
        }
    })
```
运行结果：
```shell
E/TAG: onSubscribe(): main
E/TAG: subscribe(): RxCachedThreadScheduler-2
E/TAG: onNext(): main
E/TAG: onNext(): main
E/TAG: onComplete(): main
```
注意: 
- `onSubscribe`发生在当前线程，与`subscribeO`n和`observeOn`无关；
- `subscribeOn`决定了最上游数据产生的线程；
- `observeOn`决定了下游的订阅发生的线程。

### 2.1 observeOn

`observeOn`用来指定观察者回调的线程，该方法执行后会返回一个`ObservableObserveOn`对象。
```java
@CheckReturnValue
@SchedulerSupport(SchedulerSupport.CUSTOM)
public final Observable<T> observeOn(Scheduler scheduler) {
    return observeOn(scheduler, false, bufferSize());
}

@CheckReturnValue
@SchedulerSupport(SchedulerSupport.CUSTOM)
public final Observable<T> observeOn(Scheduler scheduler, boolean delayError, int bufferSize) {
    ObjectHelper.requireNonNull(scheduler, "scheduler is null");
    ObjectHelper.verifyPositive(bufferSize, "bufferSize");
    return RxJavaPlugins.onAssembly(new ObservableObserveOn<T>(this, scheduler, delayError, bufferSize));
}
```
在我们的例子中，ObservableObserveOn的四个参数为：
```java
source = this
scheduler = AndroidSchedulers.mainThread()
delayError = false
bufferSize = Math.max(1, Integer.getInteger("rx2.buffer-size", 128)) = 128
```
下面看看其subscribeActual方法：

```java
@Override
protected void subscribeActual(Observer<? super T> observer) {
    // 如果传入的scheduler是Scheduler.trampoline()的情况
    // 该线程的意义是传入当前线程，也就是不做任何线程切换操作
    if (scheduler instanceof TrampolineScheduler) {
        source.subscribe(observer);
    } else {
        Scheduler.Worker w = scheduler.createWorker();
        // 将Scheduler创建的Worker传入了ObserveOnObserver
        // 这里直接调用了上游的subscribe方法，因此observeOn操作也不会影响上游线程执行环境
        source.subscribe(new ObserveOnObserver<T>(observer, w, delayError, bufferSize));
    }
}
```
`scheduler.createWorker()`创建了一个主线程`handler`的`Worker`，这样我们到了`ObserveOnObserver`中，首先看看其`onSubscribe`方法：

```java
@Override
public void onSubscribe(Disposable s) {
    if (DisposableHelper.validate(this.s, s)) {
        this.s = s;
        if (s instanceof QueueDisposable) {
            ...
        }
        // 创建一个单生产者单消费者的队列
        queue = new SpscLinkedArrayQueue<T>(bufferSize);
        // 直接调用上游的onSubscribe方法
        actual.onSubscribe(this);
    }
}
```
从上面的分析我们看出，`observeOn`不会影响上游线程执行环境，也不会影响下游的`onSubscribe`回调的线程。

接着看`ObserveOnObserver#onNext`方法：

```java
@Override
public void onNext(T t) {
    // done标志位在onComplete以及onError中被设置为true
    if (done) {
        return;
    }
    // sourdeMode本例中为0，所以会将t加入到queue中
    if (sourceMode != QueueDisposable.ASYNC) {
        queue.offer(t);
    }
    schedule();
}
```

`onNext`会调用`ObserveOnObserver#schedule`方法：

```java
void schedule() {
    // ObserveOnObserver 类间接继承了 AtomicInteger
    // 第一个执行该方法肯定返回0，执行后就自增为1了
    // 也就意味着 worker.schedule(this) 只会执行一次
    if (getAndIncrement() == 0) {
        worker.schedule(this);
    }
}
```
`Worker.schedule(Runnable)`方法直接调用了重载方法`schedule(Runnable run, long delay, TimeUnit unit)`，后面的两个参数为`0L`, `TimeUnit.NANOSECONDS`，这就意味着立刻马上执行`run`。

### 2.1.1 RxAndroid

RxAndroid 库总共就4个文件，其中两个文件比较重要：HandlerScheduler以及封装了该类的`AndroidSchedulers`。

AndroidSchedulers提供了两个公有静态方法来切换线程：

- mainThread()指定主线程;
- from(Looper looper)指定别的线程。

`AndroidSchedulers.mainThread()` 最终会返回一个 `HandlerScheduler(new Handler(Looper.getMainLooper())`对象

```java
/** Android-specific Schedulers. */
public final class AndroidSchedulers {

    private static final class MainHolder {
        // mainThread() 会返回一个 HandlerScheduler 对象
        static final Scheduler DEFAULT = new HandlerScheduler(new Handler(Looper.getMainLooper()));
    }

    private static final Scheduler MAIN_THREAD = RxAndroidPlugins.initMainThreadScheduler(
            new Callable<Scheduler>() {
                @Override public Scheduler call() throws Exception {
                    return MainHolder.DEFAULT;
                }
            });

    /** A {@link Scheduler} which executes actions on the Android main thread. */
    public static Scheduler mainThread() {
        return RxAndroidPlugins.onMainThreadScheduler(MAIN_THREAD);
    }

    /** A {@link Scheduler} which executes actions on {@code looper}. */
    public static Scheduler from(Looper looper) {
        if (looper == null) throw new NullPointerException("looper == null");
        return new HandlerScheduler(new Handler(looper));
    }

    private AndroidSchedulers() {
        throw new AssertionError("No instances.");
    }
}
```
再说说另外一个关键文件`HandlerScheduler`，该类的作用就是将`Runnable`使用指定的`Handler`来执行。

该类的两个公共方法：
- `scheduleDirect`方法直接执行`Runnable`；
- 或者通过`createWorker()`创建一个`HandlerWorker`对象，稍后通过该对象的`schedule`方法执行`Runnable`。


现在回到`ObserveOnObserver.schedule`方法中，这里调用了`worker.schedule(this)`方法。这里已经通过`HandlerScheduler`回到主线程了。

接着看`ObserveOnObserver.run`方法。
```java
@Override
public void run() {
    if (outputFused) {
        drainFused();
    } else {
        drainNormal();
    }
}
```
由于`outputFused`在本例中为`false`（打断点可知），所以我们看看`drainNormal()`方法。
```java
void drainNormal() {
    int missed = 1;

    // queue在onSubscribe方法中被创建，且在onNext中放入了一个值
    final SimpleQueue<T> q = queue;
    // actual就是下游的observer
    final Observer<? super T> a = actual;

    for (;;) {
        if (checkTerminated(done, q.isEmpty(), a)) {
            return;
        }

        for (;;) {
            boolean d = done;
            T v;

            // 取值
            try {
                v = q.poll();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                s.dispose();
                q.clear();
                a.onError(ex);
                worker.dispose();
                return;
            }
            boolean empty = v == null;

            if (checkTerminated(d, empty, a)) {
                return;
            }

            if (empty) {
                break;
            }

            // 调用下游observer的onNext方法
            a.onNext(v);
        }

        missed = addAndGet(-missed);
        if (missed == 0) {
            break;
        }
    }
}
```
在上面代码中在一些关键点写了一些注释，需要注意的是，调用该方法的`run`方法已经被切换到主线程中执行了，这样此方法也是在主线程中执行的。

至此，`observeOn`工作原理已经解释完毕，我们已经知道了`observeOn`是如何决定了下游订阅发生的线程的：

>将`Runnable`抛给指定的线程池来执行，`Runnable`里面会调用下游`observer`的`onNext`方法。

下面看看`subscribeOn`。

### 2.2 subscribeOn 操作符

`subscribeOn`切换原理和`observeOn`非常相似。

在`Observable.subscribeOn`方法中，创建了一个`ObservableSubscribeOn`对象，我们看一下其`subscribeActual`方法：

```java
@Override
public void subscribeActual(final Observer<? super T> s) {
    // 将下游observer包装成为SubscribeOnObserver
    final SubscribeOnObserver<T> parent = new SubscribeOnObserver<T>(s);
    // 调用下游的onSubscribe方法
    s.onSubscribe(parent);
    // 1. SubscribeTask是一个Runnable对象，其run方法为：source.subscribe(parent)
    // 2. 调用scheduler.scheduleDirect开始执行Runnable
    parent.setDisposable(scheduler.scheduleDirect(new SubscribeTask(parent)));
}
```
和上面分析的`observeOn`类似，`scheduler.scheduleDirect`肯定起到一个线程切换的过程，线程切换之后就会执行`source.subscribe(parent)`。就这样`subscribe`会一直向上传递到数据发射的位置，发射数据的方法的线程自然也会发生改变。

回过头来看一下`scheduler.scheduleDirect`干了些什么，这里的`scheduler`是`IoScheduler`，该方法是其基类`Scheduler`的方法：

```java
@NonNull
public Disposable scheduleDirect(@NonNull Runnable run) {
    return scheduleDirect(run, 0L, TimeUnit.NANOSECONDS);
}

@NonNull
public Disposable scheduleDirect(@NonNull Runnable run, long delay, @NonNull TimeUnit unit) {
    final Worker w = createWorker();

    final Runnable decoratedRun = RxJavaPlugins.onSchedule(run);
    // DisposeTask的run方法就是调用了decoratedRun的run方法
    DisposeTask task = new DisposeTask(decoratedRun, w);
    // w是IoScheduler创建的EventLoopWorker
    w.schedule(task, delay, unit);

    return task;
}
```
我们接着看一下`EventLoopWorker.schedule`方法：

```java
@NonNull
@Override
public Disposable schedule(@NonNull Runnable action, long delayTime, @NonNull TimeUnit unit) {
    if (tasks.isDisposed()) {
        // don't schedule, we are unsubscribed
        return EmptyDisposable.INSTANCE;
    }
    return threadWorker.scheduleActual(action, delayTime, unit, tasks);
}
```
这里的`threadWorker`实际上是一个`NewThreadWorker`，直接看`scheduleActual`方法：

```java
@NonNull
public ScheduledRunnable scheduleActual(final Runnable run, long delayTime, @NonNull TimeUnit unit, @Nullable DisposableContainer parent) {
    Runnable decoratedRun = RxJavaPlugins.onSchedule(run);
    // 就相当于一个Runnable
    ScheduledRunnable sr = new ScheduledRunnable(decoratedRun, parent);

    if (parent != null) {
        if (!parent.add(sr)) {
            return sr;
        }
    }

    Future<?> f;
    try {
        // 根据延迟时间执行这个runnable
        if (delayTime <= 0) {
            f = executor.submit((Callable<Object>)sr);
        } else {
            f = executor.schedule((Callable<Object>)sr, delayTime, unit);
        }
        sr.setFuture(f);
    } catch (RejectedExecutionException ex) {
        if (parent != null) {
            parent.remove(sr);
        }
        RxJavaPlugins.onError(ex);
    }

    return sr;
}
```
这样，开始的`SubscribeTask`就会在指定的`io线程池`中进行运行了。

为什么`subscribeOn()`只有第一次切换有效？

因为RxJava最终能影响`ObservableOnSubscribe`这个匿名实现接口的运行环境的只能是最后一次`subscribe`操作，又因为`RxJava`订阅的时候是从下往上订阅，所以从上往下第一个`subscribeOn()`就是最后运行的。

举个例子：

```java
Observable.create(ObservableOnSubscribe<String> { emitter ->
    emitter.onNext("1")
    emitter.onNext("2")
    emitter.onComplete()
}).subscribeOn(Schedulers.io())
    .map(...)
    .subscribeOn(Schedulers.newThread())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(...)
```

数据发射时所在的线程可以这样理解：

```java
// 伪代码
Thread("newThread()") {
    Thread("io()") {
        emitter.onNext("1")
        emitter.onNext("2")
        emitter.onComplete()
    }
}
```
### 2.3 线程切换小结¶
最后，线程切换特点可以从下图例子中体现出来：

<img width="700" alt="RxJava线程转化关系图" src="https://user-images.githubusercontent.com/17560388/178108221-01d8724b-bed1-4b6d-9f6c-0f6405407a0d.png">




