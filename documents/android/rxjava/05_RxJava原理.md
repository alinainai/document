
本文RxJava源码版本为2.1.13，RxAndroid版本为2.0.2。RxAndroid这个库只提供了一个调度器，所以没有单独拎出来说。

## 1. 基本订阅流程

在正文开始前，我们以Observable.create操作符为例，先看一下下面几个基本类之间的关系：

<img width="400" alt="以create为例，相关类的UML图" src="https://user-images.githubusercontent.com/17560388/177450216-091c09fe-2b56-4cb5-b039-abab98fdc02e.png">

- `ObservableSource`:数据源，会被`Observer`消耗。这是一个接口，其实现类为抽象类`Observable`，接口中的`subscribe`方法会由抽象类`Observable`的抽象方法s`ubscribeActual`方法进行实现。
- `Emitter`:决定如何发射数据，只在`create`等操作符中才会出现
- `Observer`:数据的消耗端

本文会以下面的例子进行源码的解读：

```java
Observable.create(object : ObservableOnSubscribe<String> {
    override fun subscribe(emitter: ObservableEmitter<String>) {
        emitter.onNext("1")
        emitter.onNext("2")
        emitter.onComplete()
    }
}).map(object : Function<String, Int> {
    override fun apply(t: String): Int {
        return t.toInt() * 10
    }
}).subscribe(object : Observer<Int> {
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

先看看Observable.create的方法：

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

因此，开头的例子中最后的链式调用部分就等价于：

```java
ObservableCreate<String>(source).map(function).subscribe(resultObserver)
```

下面看一下map操作符：

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

所有的操作符都将上一个`Observable`作为一个参数传入构造函数，这就是`RxJava`中数据会依次经过这些`Observable`的原因。
同时，值得注意的是，rxjava中每个操作符都会在内部创建一个`Observable`对象。

接下来，我们回到示例程序的`subscribe`操作中，我们知道`subscribe`是`ObservableSource`接口的方法，该方法在抽象类`Observable`中进行了重写，在重写方法中交给了抽象方法`subscribeActual`来实现，我们看看这部分代码：

```java
@SchedulerSupport(SchedulerSupport.NONE)
@Override
public final void subscribe(Observer<? super T> observer) {
    // 检查入参observer是否为空
    ObjectHelper.requireNonNull(observer, "observer is null");
    try {
        // 同1️⃣处的注释，默认情况下返回入参参数observer
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
现在，我们正式开始看看例子是怎么执行的。
由于先执行的是最近的Observable也就是ObservableMap，我们先看看其subscribeActual方法：

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
我们先不看具体的实现，先尝试把所有的代码全部展开，然后在研究例子的结果。目前可以展开如下：

```java
ObservableCreate<String>(source).subscribe(MapObserver<String, Int>(resultObserver, function))
```
现在轮到展开ObservableCreate了：

```java
public final class ObservableCreate<T> extends Observable<T> {
    final ObservableOnSubscribe<T> source;

    public ObservableCreate(ObservableOnSubscribe<T> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        // 1️⃣ 创建一个 CreateEmitter 对象，CreateEmitter 继承 AtomicReference 并实现了 ObservableEmitter 和 Disposable 接口
        CreateEmitter<T> parent = new CreateEmitter<T>(observer);
        // 2️⃣ 将 parent(CreateEmitter) 作为 dispose 返回
        observer.onSubscribe(parent);
        try {
            // 3️⃣ 调用 ObservableOnSubscribe 对象的 subscribe 方法，执行该方法里面的 emitter 操作
            source.subscribe(parent);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            parent.onError(ex);
        }
    }

    static final class CreateEmitter<T>
    extends AtomicReference<Disposable>
    implements ObservableEmitter<T>, Disposable {
        ...
    }
    ...
}
```
1️⃣处的`CreateEmitter` 继承自 `AtomicReference` 并实现了 `ObservableEmitter` 和 `Disposable` 接口

- 对于`ObservableEmitter`接口来说，该类就是在对应的接口方法中调用构造器入参`observer`的对应发射方法 
- 对于`Disposable`接口来说，该类向客户端返回了一个可以`dispose`操作的对象`parent`；其原理是根据`AtomicReference<Disposable>`是否为`DISPOSED`判断是不是处于`disposed`状态

MapObserver简单来说就是在onNext方法中会将原始值t用转换方法mappper进行转换，然后调用构造器入参actual这个Observer的onNext方法

2️⃣:调用observer.onSubscribe(parent)，因为observer.actual为resultObserver，所以通知我们写的消费者开始进行订阅了。

3️⃣:调用source.subscribe(parent)，正式开始订阅。 
  - 这里的source就是最原始的数据源，这里将parent作为参数emitter传入到source的subscribe方法中，然后在该方法中我们调用了其onNext、onComplete方法。实际上调用的就是CreateEmitter的对应的方法 
  - 在CreateEmitter.onNext中会调用observer.onNext，这里的observer就是MapObserver 
  - 在MapObserver.onNext中会将原始值t用转换方法mapper进行转换，然后调用构造器入参actual这个Observer的onNext方法。最后的actual实际上就是resultObserver。 
  - 这样，原始数据"1"经过CreateEmitter的发射后，在MapObserver中经过mapper转换最后到了resultObserver中

