
本文RxJava源码版本为2.1.13，RxAndroid版本为2.0.2。

## 1. 基本订阅流程

在正文开始前首先我们要明确一点 RxJava 的基本流程：被观察者通过`subscribe`方法和观察者建立订阅关系，订阅关系建立的同时开始进行事件流的传递。

```java
Observable.subscribe(Observer)
```

下面我们以`Observable.create`操作符为例来跟踪一下源码

```kotlin
//1) 调用 Observable#create(ObservableOnSubscribe) 方法创建一个 Observable 对象 
Observable.create(ObservableOnSubscribe<String> { emitter ->
        emitter.onNext("1")
        emitter.onNext("2")
        emitter.onComplete()
    }
}).map{
    it.toInt()
}.subscribe(object : Observer<Int> {
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

看流程之前我们先简单的介绍几个主要的类

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

