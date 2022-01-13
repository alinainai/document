
官方文档
[http://reactivex.io/documentation/observable.html](http://reactivex.io/documentation/observable.html)

## RxJava2 gradle集成：

>implementation "io.reactivex.rxjava2:rxjava:2.2.8"
>
>implementation 'io.reactivex.rxjava2:rxkotlin:2.0.0'

注意这里为了方便使用到了[RxKotlin](https://github.com/ReactiveX/RxKotlin)，一个非常不错的RxJava Kotlin扩展库，也是reactivex出品。


## 1.生产者

### 1.1 Flowable 操作符

Flowable: 响应式流和背压

```kotlin
Flowable.create(FlowableOnSubscribe<String> { emitter ->
    emitter.onNext("test1");
    emitter.onNext("test2");
    emitter.onComplete();
}, BackpressureStrategy.BUFFER)
.subscribe { t -> debug(t) }
```
### 1.2 Observable 操作符

Observable: 无背压 (被观察者)，最常用的一个

官方的建议：

使用Observable 

- 不超过1000个元素、随着时间流逝基本不会出现OOM 
- GUI事件或者1000Hz频率以下的元素 
- 平台不支持Java Steam(Java8新特性) 
- Observable开销比Flowable低

使用Flowable 

- 超过10k+的元素(可以知道上限) 
- 读取硬盘操作（可以指定读取多少行） 
- 通过JDBC读取数据库 - 网络（流）IO操作

### 1.3 Single 操作符

Single: 只有一个元素或者错误的流

Single只包含两个事件，一个是正常处理成功的onSuccess，另一个是处理失败的onError，它只发送一次消息

```kotlin
Single.create(SingleOnSubscribe<Int> { emitter -> emitter.onSuccess(1) })
    .subscribe({ t -> debug("onSuccess+$t") }, { e -> debug("onError+$e") })
```
### 1.4 Completable 操作符 

Completable: 没有任何元素，只有一个完成或者错误信号的流，onComplete 和 onError 两个事件

```kotlin
Completable.create { e -> e.onComplete() }
    .subscribe( { debug("onComplete") },{ e -> debug("onError+$e") })
```

### 1.5 Maybe 操作符 

Maybe: 没有任何元素或者只有一个元素或者只有一个错误的流

如果你有一个需求是可能发送一个数据或者不会发送任何数据，这时候你就需要 Maybe，它类似于 Single 和 Completable 的混合体。

Maybe可能会调用以下其中一种情况（也就是所谓的Maybe）：
- onSuccess 或者 onError
- onComplete 或者 onError

```kotlin
Maybe.create(MaybeOnSubscribe<Int> {e->
//    e.onSuccess(1)
      e.onComplete()
}).subscribe({ t -> debug("onSuccess+$t")},{ e -> debug("onError+$e") },{debug("onComplete")})
```

### 什么是背压
背压就是生产者（被观察者）的生产速度大于消费者（观察者）消费速度从而导致的问题。

举一个简单点的例子，如果被观察者快速发送消息，但是观察者处理消息的很缓慢，如果没有特定的流（Flow）控制，就会导致大量消息积压占用系统资源，最终导致十分缓慢。


## 2.调度器

 * Schedulers.computation() 用于计算任务，如事件循环或和回调处理，默认线程数等于处理器的数量
 * Schedulers.from(executor)使用指定的Executor作为调度器
 * Schedulers.single()      该调度器的线程池只能同时执行一个线程
 * Schedulers.io()          用于IO密集型任务，如异步阻塞IO操作，这个调度器的线程池会根据需要增长；默认是一个CachedThreadScheduler，很像一个有线程缓存的新线程调度器
 * Schedulers.newThread()   为每个任务创建一个新线程
 * Schedulers.trampoline()  当其它排队的任务完成后，在当前线程排队开始执行。
 * AndroidSchedulers.mainThread() 主线程，UI线程，可以用于更新界面

## 3.创建事件序列

### 3.1 create() 操作符

`create() `方法创建

```kotlin
Observable.create<String> { emitter ->
        with(emitter) {
        onNext("Hello")
        onNext("Handsome")
        onNext("Kotlin")
        onComplete()
    }
}
```
### 3.2 interval() 操作符

使用`interval()`方法创建事件序列间隔发射

```kotlin
//每隔1秒发送一个整数，从0开始 (默认执行无数次 使用`take(int)`方法限制执行次数)
Observable.interval(0, 1, TimeUnit.SECONDS)
    .take(5)
    .subscribe { print("$it  ") }
/*
其他重载方法：
`initialDelay`参数用来指示开始发射第一个整数的之前要停顿的时间，时间的单位与period一样，都是通过unit参数来指定的；
`period`参数用来表示每个发射之间停顿多少时间；
`unit`表示时间的单位，是TimeUnit类型的；
`scheduler`参数指定数据发射和等待时所在的线程。
*/
public static Observable<Long> interval(long initialDelay, long period, TimeUnit unit, Scheduler scheduler)
public static Observable<Long> interval(long period, TimeUnit unit, Scheduler scheduler)
public static Observable<Long> intervalRange(long start, long count, long initialDelay, long period, TimeUnit unit, Scheduler scheduler)
```
### 3.3 defer() 创建事件序列

`defer`直到有观察者订阅时才创建Observable，并且为每个观察者创建一个刷新的Observable

`defer`操作符会一直等待直到有观察者订阅它，然后它使用 Observable 工厂方法生成一个 Observable
  
```kotlin
val observable = Observable.defer { Observable.just(System.currentTimeMillis()) }
observable.subscribe { print("$it ") }   // 454  订阅时才产生了Observable
print("   ")
observable.subscribe { print("$it ") }   // 459  订阅时才产生了Observable
```

### 3.4 使用`empty()` `never()` `error()`方法创建事件序列

`empty()`：创建一个不发射任何数据但是正常终止的Observable

```kotlin
// empty()只会调用onComplete方法
Observable.empty<String>().subscribeBy(
    onNext = { print(" next ") },
    onComplete = { print(" complete ") },
    onError = { print(" error ") }
)
```

`never()`：创建一个不发射数据也不终止的Observable
 
```kotlin
// 什么也不会做
Observable.never<String>().subscribeBy(
    onNext = { print(" next ") },
    onComplete = { print(" complete ") },
    onError = { print(" error ") }
)
```

`error(Throwable exception)`：创建一个不发射数据以一个错误终止的

```kotlin
// `error()`只会调用onError方法
Observable.error<Exception>(Exception()).subscribeBy(
    onNext = { print(" next ") },
    onComplete = { print(" complete ") },
    onError = { print(" error ") }
)
```
### 3.5 repeat() 操作符

使用`repeat()`方法创建事件序列，表示指定的序列要发射多少次

```kotlin
// 重载方法
public final Observable<T> repeat(long times)
public final Observable<T> repeatUntil(BooleanSupplier stop)
public final Observable<T> repeatWhen(Function<? super Observable<Object>, ? extends ObservableSource<?>> handler)

// 不指定次数即无限次发送(内部调用有次数的重载方法并传入Long.MAX_VALUE) 
Observable.range(5, 10).repeat().subscribe { print("$it  ") }
Observable.range(5, 10).repeat(1).subscribe { print("$it  ") }

// repeatUntil在满足指定要求的时候停止重复发送，否则会一直发送
// 这里当随机产生的数字`<10`时停止发送 否则继续  (这里始终为true(即停止重复) 省的疯了似的执行)
val numbers = arrayOf(0, 1, 2, 3, 4)
numbers.toObservable().repeatUntil {
    Random(10).nextInt() < 10
}.subscribe { print("$it  ") }    
```

### 3.6 timer() 创建事件序列

创建一个在给定的时间段之后返回一个特殊值的 Observable ，它在延迟一段给定的时间后发射一个简单的数字 0

```kotlin
// 在500毫秒之后输出一个数字0
val disposable = Observable.timer(500, TimeUnit.MILLISECONDS).subscribe { print("$it  ") }
if (!disposable.isDisposed) disposable.dispose()
```
### 3.7 from 操作符系列

- `from()`
- `fromArray`
- `fromCallable`
- `fromIterable` 

```kotlin
val names = arrayOf("ha", "hello", "yummy", "kt", "world", "green", "delicious")
// 注意：使用`*`展开数组
val disposable = Observable.fromArray(*names).subscribe { print("$it  ") }

// 可以在Callable内执行一段代码 并返回一个值给观察者
Observable.fromCallable { 1 }.subscribe { print("$it  ") }
```
### 3.8 just() 操作符

使用`just()`方法快捷创建事件队列，将传入的参数依次发送出来(最少1个 最多10个)

```kotlin
val disposable = Observable.just("Just1", "Just2", "Just3")
    .subscribe { print("$it  ") }
// 将会依次调用：onNext("Just1"); onNext("Just2"); onNext("Just3");  onCompleted();
```

### 3.9 range() 操作符

使用`range()`方法快捷创建事件队列，创建一个序列

```kotlin
// 用Observable.range()方法产生一个序列
// 用map方法将该整数序列映射成一个字符序列
// 最后将得到的序列输出来 forEach 内部也是调用的 subscribe(Consumer<? super T> onNext)
Observable.range(0, 10)
.map { item -> "range$item" }
//.forEach { print("$it  ") }
.subscribeBy(
    onNext = { print("$it  ") },
    onComplete = { print("range complete !!! ") }
)
```
### 4. 变换操作

### 4.1 mapCast() 操作符

- `map`操作符对原始Observable发射的每一项数据应用一个函数，然后返回一个发射这些结果的Observable。默认不在任何特定的调度器上执行
- `cast`操作符将原始Observable发射的每一项数据都强制转换为一个指定的类型`（多态）`，然后再发射数据，它是map的一个特殊版本
 
```kotlin
Observable.range(1, 5).map { item -> "to String $item" }.subscribe { print("$it  ") }
// 将`Date`转换为`Any` (如果前面的Class无法转换成第二个Class就会出现ClassCastException)
Observable.just(Date()).cast(Any::class.java).subscribe { print("$it  ") }
```
### 4.2 flatMap()、contactMap() 操作符

`map`与`flatMap`的区别:

- `map`是在一个 item 被发射之后，到达 map 处经过转换变成另一个 item ，然后继续往下走；
- `flapMap`是 item 被发射之后，到达 flatMap 处经过转换变成一个 Observable，而这个 Observable 并不会直接被发射出去，而是会立即被激活，然后把它发射出的每个 item 都传入流中，再继续走下去。

使用`flatMap()` `contactMap()` 做变换操作

`flatMap`将一个发送事件的上游 Observable 变换为多个发送事件的 Observables，然后将它们发射的事件合并后放进一个单独的Observable里

两者区别：`flatMap`不保证顺序  `contactMap()`保证顺序

### 4.3 flatMap() 的原理是这样的：

- 1. 使用传入的事件对象创建一个 Observable 对象；
- 2. 并不发送这个 Observable, 而是将它激活，于是它开始发送事件；
- 3. 每一个创建出来的 Observable 发送的事件，都被汇入同一个 Observable ，而这个 Observable 负责将这些事件统一交给 Subscriber 的回调方法。

这三个步骤，把事件拆成了两级，通过一组新创建的 Observable 将初始的对象『铺平』之后通过统一路径分发了下去。

 而这个『铺平』就是 flatMap() 所谓的 flat。
```kotlin
Observable.range(1, 5).flatMap { Observable.just("$it to flat")}.subscribe { print("$it  ") }
Observable.range(1, 5).concatMap { Observable.just("$it to concat") }.subscribe { print("$it  ") }
//latMap`挺重要的，再举一个例子,依次打印Person集合中每个元素中Plan的action
arrayListOf(
    Person("KYLE", arrayListOf(Plan(arrayListOf("Study RxJava", "May 1 to go home")))),
    Person("WEN QI", arrayListOf(Plan(arrayListOf("Study Java", "See a Movie")))),
    Person("LU", arrayListOf(Plan(arrayListOf("Study Kotlin", "Play Game")))),
    Person("SUNNY", arrayListOf(Plan(arrayListOf("Study PHP", "Listen to music"))))
).toObservable().flatMap {
    Observable.fromIterable(it.planList)
}.flatMap {
    Observable.fromIterable(it.actionList)
}.subscribeBy(
    onNext = { print("$it  ") }
)
```

### 4.4 flatMapIterable() 操作符

使用`flatMapIterable()`做变换操作，将上流的任意一个元素转换成一个Iterable对象
```kotlin
Observable.range(1, 5)
    .flatMapIterable { integer -> Collections.singletonList("$integer") }
    .subscribe { print("$it  ") }
```
### 4.5 buffer() 操作符

使用`buffer(count,skip)`，将事件缓冲至列表中

```kotlin
// 生成一个7个整数构成的流，然后使用`buffer`之后，这些整数会被3个作为一组进行输出
// count 是一个buffer的最大值
// skip 是指针后移的距离(不定义时就为count)
// 例如 1 2 3 4 5 buffer(3) 的结果为：[1,2,3] [4,5] ( buffer(3) 也就是 buffer(3,3))
// 例如 1 2 3 4 5 buffer(3,2) 的结果为：[1,2,3] [3,4,5] [5]
Observable.range(1, 5).buffer(3)
    .subscribe {
        print("${Arrays.toString(it.toIntArray())}  ")
    }
```
### 4.6 groupBy()

使用`groupBy()`做变换操作，用于分组元素(根据groupBy()方法返回的值进行分组)

将每个元素都赋予一个组ID，然后将组ID相同元素放在一个 Observable 内发射出来

```kotlin
List<Dict> lst = new ArrayList<>();
lst.add(new Dict("1", "A"));
lst.add(new Dict("2", "B"));
lst.add(new Dict("1", "B"));
lst.add(new Dict("2", "A"));
lst.add(new Dict("3", "B"));
lst.add(new Dict("3", "A"));
Observable.fromIterable(lst)
        .groupBy(new Function<Dict, String>() {
            @Override
            public String apply(Dict dict) throws Exception {
                return dict.getValue()+"group";
            }
        })
        .subscribe(grp -> grp.subscribe( d -> System.out.println(d)));
```

### 4.7 scan() 操作符

使用`scan()`做变换操作将数据以一定的逻辑聚合起来，scan() 会接收两个参数:上一次生成的值(也被称为累加器)以及上游 Observable 的当前值。

```kotlin 
Observable.just(10, 14, 12, 13, 14, 16) //progress
    .scan {total, chunk -> total + chunk }
    .subscribe { print("$it  ") }
//10, 24, 36, 49, 63, 79
```
在第一轮迭代中，total 就是来自 progress 的第一个条目;而在第二次迭代中，它变成了上一次 scan() 操作的结果值。

重载方法：可以在 scan 中添加 初始值。如下，我们将初始值设置为1，结果会先把初始值 1 发送。

```kotlin
Observable.just(10, 14, 12, 13, 14, 16) //progress
    .scan(1) { total, chunk -> total + chunk }
    .subscribe { print("$it  ") }
//1 11 25 37 50 64 80  会先把1（初始值）发送，然后再和 progress 数据项累加。
```

### 4.8 window() 操作符

使用`window()`做变换操作将事件分组 参数`count`就是分的组数

 `window`和`buffer`类似，但不是发射来自原始Observable的数据包，它发射的是Observable， 这些Observables中的每一个都发射原始Observable数据的一个子集，最后发射一个onCompleted通知。

```kotlin
Observable.range(1, 10).window(3)
   .subscribeBy(
       onNext = { it.subscribe { int -> print("{${it.hashCode()} : $int} ") } },
       onComplete = { print("onComplete ") }
   )
```

## 5. 过滤操作/条件操作符

### 5.1 filter() 操作符

使用`filter()`做过滤操作对源做过滤

```kotlin
// 过滤掉 <=5 的数据源 只有 >5 的数据源会发送出去
Observable.range(1, 10).filter { it > 5 }.subscribe { print("$it  ") }
```
### 5.2 element() 操作符

使用`element()`获取源中指定位置的数据
 * `elementAt`  指定位置
 * `firstElement`  第一个
 * `lastElement`   最后一个
 
```kotlin
// `elementAt` 在输入的 index 超出事件序列的总数就不会出现任何结果
// `elementAtOrError` 则会报异常
// `first...` 和 `last...` 都类似
// 只有 index=0 的数据源会被发射
Observable.range(1, 10).elementAt(0).subscribe { print("$it  ") }
print("[firstElement]: ")
Observable.range(1, 19).firstElement().subscribe { print("$it  ") }
print("[lastElement]: ")
Observable.range(34, 2).lastElement().subscribe { print("$it  ") }
```
### 5.3 distinct() 去重

使用`distinct()`对源中相同的数据进行过滤

```kotlin
Observable.just(1, 1, 1, 2, 3, 4, 1, 5, 5, 6)
      .distinct()
      .subscribe { print("$it  ") } // 1  2  3  4  5  6

Observable.just(1, 1, 1, 2, 3, 4, 1, 5, 5, 6)
      .distinctUntilChanged()
      .subscribe { print("$it  ") } //1  2  3  4  1  5  6
```
### 5.4 skip() 过滤掉数据的前n项

使用`skip()` 过滤掉数据的前n项
 * `skip`         过滤掉数据的前n项 参数count代表跳过事件的数量
 * `skipLast`     与`skip` 功能相反 过滤掉数据的后n项
 * `skipUntil`    当 skipUntil() 中的 Observable 发送事件了，原来的 Observable 才会发送事件给观察者。
 * `skipWhile`    可以设置条件，当某个数据满足条件时不发送该数据，反之则发送。
 
```kotlin
Observable.just(1, 2, 3, 4, 5, 6)
        .skip(2)
        .subscribe { print("$it  ") } // [skip]: 3  4  5  6

Observable.just(6)
        .skipUntil<Int> { Observable.just(2).delay(2, TimeUnit.SECONDS).subscribe { print("$it  ") } }
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .subscribe { print("$it  ") }
	
Observable.just(1, 2, 3, 4)
        .skipWhile {  it < 3 }.subscribe { print("$it  ") } // [skipWhile]: 3  4
    
```
### 5.5 take() 取数据的前n项

使用`take()` 取数据的前n项
 * `take`          取数据的前n项 参数count代表取的事件的数量
 * `takeLast`      与`take`功能相反 取数据的后n项
 * `takeUntil` 与`skip...`对应
 * `takeWhile`与`skip...`对应
```kotlin
Observable.range(1, 5).take(2).subscribe { print("$it  ") } // 1  2
```
### 5.6 ignoreElements() 过滤所有源Observable产生的结果

使用`ignoreElements()` 过滤所有源Observable产生的结果，只会把Observable的`onComplete`和`onError`事件通知给订阅者

```kotlin
Observable.just(1, 1, 2, 3, 4)
    .ignoreElements()
    .subscribeBy(
        onComplete = { print(" onComplete ") },
        onError = { print(" onError ") }
            // 没有`onNext`)
```
### 5.7 debounce() 限制发射频率过快

使用`debounce()` 限制发射频率过快，如果两件事件发送的时间间隔小于设定的时间间隔则`前一件`事件就不会发送给观察者

```kotlin
Observable.create<Int> { emitter ->
    emitter.onNext(1)
    Thread.sleep(900)
    emitter.onNext(2)
}.debounce(1, TimeUnit.SECONDS)
.subscribe { print("$it  ") } // 2
```
### 5.8 ofType() 过滤不符合该类型事件

使用`ofType()` 过滤不符合该类型事件
```kotlin
Observable.just(1, 2, 3, "k", "Y")
    .ofType(String::class.java)
    .subscribe { print("$it  ") } // [ofType]: k  Y
```

### 5.9 all() 判断事件序列是否全部满足某个事件

all 判断事件序列是否全部满足某个事件，如果都满足则返回 true，反之则返回 false
```kotlin
Observable.just(1, 2, 3, 4)
    .all { it < 5 }
    .subscribe(Consumer {
        print("$it  ")
    }) // [all]: true
```
### 5.10 contains() 判断事件序列中是否含有某个元素

判断事件序列中是否含有某个元素，如果有则返回 true，如果没有则返回 false。

```kotlin
Observable.just(1, 2, 3, 4)
    .contains(3)
    .subscribe(Consumer {
        print("$it  ")
    })  // [contains]: true
```
### 5.11 isEmpty() 判断事件序列是否为空

判断事件序列是否为空  是返回true  否返回false

```kotlin
Observable.create<String> { emitter ->
    emitter.onComplete()
}.isEmpty()
.subscribe(Consumer {
            print("$it  ")
}) 
// [isEmpty]: true
```
### 5.12 defaultIfEmpty() 操作符

如果观察者只发送一个 onComplete() 事件，则可以利用这个方法发送一个值。

```kotlin
Observable.create<Int> { emitter ->
    emitter.onComplete()
}.defaultIfEmpty(666)
.subscribe { print("$it  ") }
// [defaultIfEmpty]: 666
```

### 5.13 amb() 操作符

amb()， 它会订阅上游其所操控的所有 Observable 并等待第一个事件的发布。其中有一个 Observable 发布第一个事件之后， amb() 会丢弃所有其他的流，接下来只跟踪第一个发布事件的 Observable。

```kotlin
val list = ArrayList<Observable<Long>>()
list.add(Observable.intervalRange(1, 5, 2, 1, TimeUnit.SECONDS))
list.add(Observable.intervalRange(6, 5, 0, 1, TimeUnit.SECONDS))
Observable.amb(list)
    .subscribe { print("$it  ") }
// [amb]:  6  7  8  9  10
```
## 6. 组合操作

### 6.1 concat() 组合操作

将多个Observable拼接起来，但是它会严格按照传入的Observable的顺序进行发射，一个 Observable 没有发射完毕之前不会发射另一个 Observable 里面的数据

`concat()`方法内部还是调用的`concatArray(source1, source2)`方法，只是在调用前对传入的参数做了`null`判断

与 `merge()` 作用基本一样，只是 `merge()` 是并行发送事件，而 concat() 串行发送事件

```kotlin
Observable.concat(Observable.range(1, 5), Observable.range(6, 5))
    .subscribe { print("$it  ") }
// [concat]: 1  2  3  4  5  6  7  8  9  10
```
### 6.2 merge() 做组合操作

让多个数据源的数据合并起来进行发射(merge后的数据可能会交错发射)

内部实际操作为调用了`fromArray()+flatMap`方法 只是在调用前对数据源参数做了`null`判断

与 `concat()` 作用基本一样，只是 `concat()` 是串行发送事件，而 merge() 并行发送事件

**与`mergeError`的比较**

`mergeError`方法与`merge`方法的表现一致，
* 只是在处理由`onError`触发的错误的时候有所不同。
* `mergeError`方法会等待所有的数据发射完毕之后才把错误发射出来，即使多个错误被触发，该方法也只会发射出一个错误信息。
* 而如果使用`merger`方法，那么当有错误被触发的时候，该错误会直接被抛出来，并结束发射操作
```kotlin
Observable.merge(Observable.range(1, 5), Observable.range(6, 5))
    .subscribe { print("$it  ") }
// [merge]: 1  2  3  4  5  6  7  8  9  10
```

### 6.3 startWith()

使用`startWith` 做组合操作 在发送事件之前追加事件

- `startWith`       追加一个事件
- `startWithArray`  追加多个事件

追加的事件会先发出
  
```kotlin
Observable.range(5, 3)
    .startWithArray(1, 2, 3, 4)
    .startWith(0).subscribe { print("$it  ") }
// [startWith]: 0  1  2  3  4  5  6  7
```
### 6.4 zip() 做组合操作

用来将多个数据项进行合并 根据各个被观察者发送事件的顺序一个个结合起来，最终发送的事件数量会与源 Observable 中最少事件的数量一样

为什么呢？因为数据源少的那个 Observable 发送完成后发送了 onComplete 方法，所以数据源多的那个就不会再发送事件了
 
```kotlin
Observable.zip(Observable.range(1, 6), Observable.range(6, 5), BiFunction<Int, Int, Int> { t1, t2 -> t1 * t2 })
    .subscribe { print("$it  ") }
// 1 2 3 4  5 6
// 6 7 8 9 10
// 看上面两行再看结果很明显了吧
// [zip]: 6  14  24  36  50
```
### 6.5 combineLast() 做组合操作

任意一个上游流产生事件时，就使用另外一个流最新的已知值。

```kotlin
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static rx.Observable.interval;
Observable.combineLatest(
    interval(17, MILLISECONDS).map(x -> "S" + x),
    interval(10, MILLISECONDS).map(x -> "F" + x),
    (s, f) -> f + ":" + s
).forEach(System.out::println);
```
打印日志
```shell
F0:S0
F1:S0
F2:S0
F2:S1
F3:S1
F4:S1
F4:S2
F5:S2
F5:S3
...             
F998:S586
F998:S587
F999:S587
F1000:S587
F1000:S588
F1001:S588
```
### 6.6 reduce() 做组合操作

与 scan() 操作符的作用一样也是将发送数据以一定逻辑聚合起来，

这两个的区别在于 scan() 每处理一次数据就会将事件发送给观察者，而 reduce() 会将所有数据聚合在一起才会发送事件给观察者

```kotlin
Observable.just(0, 1, 2, 3)
    .reduce { t1, t2 -> t1 + t2 }
    .subscribe { print("$it  ") }
// [reduce]: 6
```
### 6.7 collect() 做组合操作

将数据收集到数据结构当中

```kotlin
// `collect`接收两个参数 第一个是要收集到的数据解构 第二个是数据到数据结构中的操作
Observable.just(1, 2, 3, 4)
    .collect({ ArrayList<Int>() }, { t1, t2 -> t1.add(t2) })
    .subscribe(Consumer<ArrayList<Int>> {
            print("$it  ")
    })
// [collect]: [1, 2, 3, 4]
```
### 6.8 count() 做组合操作

返回被观察者发送事件的数量

```kotlin
Observable.just(1, 2, 3)
    .count()
    .subscribe(Consumer {print("$it  ")})
// [count]: 3
```
## 7. 功能操作符/辅助操作

### 7.1 delay() 用于在发射数据之前停顿指定的时间

```kotlin
Observable.range(1, 5).delay(1, TimeUnit.SECONDS).subscribe { print("$it  ") }
```
### 7.2 do 系列

```kotlin
// `doOnEach`  当每个`onNext`调用[前]触发 并可取出`onNext`发送的值  但是方法参数是一个`Notification<T>`的包装 可以通过`.value`取出`onNext`的值
// `doOnNext`  在每个`onNext`调用[前]触发 并可取出`onNext`发送的值  方法参数就是`onNext`的值
// `doAfterNext`   在每个`onNext`调用[后]触发 并可取出`onNext`发送的值  方法参数就是`onNext`的值
// `doOnComplete`  在`onComplete`调用[前]触发
// `doOnError`  在`onError`调用[前]触发
// `doOnSubscribe`  在`onSubscribe`调用[前]触发
// `doOnDispose`  在调用 Disposable 的 dispose() 之[后]回调该方法
// `doOnTerminate `  在 onError 或者 onComplete 发送之[前]回调
// `doAfterTerminate `   在onError 或者 onComplete 发送之[后]回调  取消订阅后就不会回调
// `doFinally`   在所有事件发送完毕之后回调该方法   即使取消订阅也会回调
// `onErrorReturn`   当接受到一个 onError() 事件之后回调，返回的值会回调 onNext() 方法，并正常结束该事件序列
// `onErrorResumeNext`   当接收到 onError() 事件时，返回一个新的 Observable，并正常结束事件序列
// `onExceptionResumeNext`   与 onErrorResumeNext() 作用基本一致，但是这个方法只能捕捉 Exception

Observable.create<String> { emitter ->
    emitter.onNext("K")
    emitter.onNext("Y")
    emitter.onNext("L")
    emitter.onNext("E")
    emitter.onComplete()
}.doOnTerminate {
    print("doOnNext: $  ")
}.subscribeBy(
    onNext = { print("accept: $it  ") },
    onComplete = { print("  onComplete  ") },
    onError = { print("  onError  ") }
)
```
### 7.3 retry()

另：`retryUntil` 出现错误事件之后，可以通过此方法判断是否继续发送事件 true 不重试 false 重试
 
如果出现错误事件，则会重新发送所有事件序列。times 是代表重新发的次数。

```kotlin
Observable.create<String> { emitter ->
    emitter.onNext("K")
    emitter.onError(Exception("404"))
}.retry(2)
    .subscribeBy(
        onNext = { print("accept: $it  ") },
        onComplete = { print("  onComplete  ") },
        onError = { print("  onError  ") }
    )
// [retry]: accept: K  accept: K  accept: K    onError
// 重试了2次
```
### 7.4 subscribeOn 和 observeOn 

简单地说，subscribeOn() 指定的就是发射事件的线程，observerOn 指定的就是订阅者接收事件的线程。

多次指定发射事件的线程只有第一次指定的有效，也就是说多次调用 subscribeOn() 只有第一次的有效，其余的会被忽略。

但多次指定订阅者接收线程是可以的，也就是说每调用一次 observerOn()，下游的线程就会切换一次

>subscribeOn 指定被观察者的线程，要注意的时，如果多次调用此方法，只有第一次有效。

observeOn 指定观察者的线程，每指定一次就会生效一次。

### 7.5 compose() 操作符

`compose`操作符和Transformer结合使用，一方面让代码看起来更加简洁化，另一方面能够提高代码的复用性。

RxJava提倡链式调用，`compose`能够防止链式被打破。

compose操作于整个数据流中，能够从数据流中得到原始的 Observable<T>/Flowable<T>...

当创建 Observable/Flowable... 时，compose操作符会立即执行，而不像其他的操作符需要在 onNext() 调用后才执行
	
```kotlin
Observable.just(1, 2)
    .compose(transformerInt2String())
    .compose(applySchedulers())
    .subscribe {
        print("$it  ")
        if (it == "1") print(" ${Thread.currentThread().name} ")
    }
// 用于`compose`举例子
// 将发射的Int转换为String
fun transformerInt2String() = ObservableTransformer<Int, String> { upstream -> upstream.map { int -> "$int" } }

// 切换线程
fun <T> applySchedulers() =
    ObservableTransformer<T, T> { upstream -> upstream.observeOn(Schedulers.io()).subscribeOn(Schedulers.io()) }
```

## 8. RxKotlin扩展库

RxKotlin扩展库的一个简单使用

更多查看：https://github.com/ReactiveX/RxKotlin/blob/2.x/README.md

```kotlin
val list = listOf("Alpha", "Beta", "Gamma", "Delta", "Epsilo
// 相当于是Observable.fromIterable(this) 和上面的fromArray()类似 一个数组 一个集合
list.toObservable()
    .filter { it.length > 5 }
    .subscribeBy(   // 对应上面`create`创建方式的最后调用的subscribe
        onNext = { print("$it  ") },
        onError = { it.printStackTrace() },
        onComplete = { print(" Done! ") })
	
// [rkExExample]: Epsilon   Done!
```


### 参考

给Android开发者的RxJava详解
https://gank.io/post/560e15be2dca930e00da1083
 
RxKotlin
https://github.com/ReactiveX/RxKotlin/blob/2.x/README.md
 
RxJava系列
https://www.jianshu.com/p/823252f110b0
 
RxJava2看这一篇文章就够了
https://juejin.im/post/5b17560e6fb9a01e2862246f
