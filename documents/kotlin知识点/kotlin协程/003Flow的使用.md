## 1. 简单使用
和 RxJava 类似，flow 也是基于流(Stream)的数据处理，也是观察者模式。他也是遵循

### 1.1 通过 flow{} 创建流 

1. 通过 `flow{}` 创建流。
2. 使用 `emit 函数` 发射值。
3. 流使用 `collect 函数` 收集值。

```kotlin
flow {//创建流
    listOf<Int>(1,2,3).forEach {
        emit(it) //发射流
    }
}.collect{//接收流
    println(it)
}
```

Flow 是一种类似于序列的冷流，所以 `flow` 构建器中的代码直到流被收集的时候才运行
```kotlin
fun simple(): Flow<Int> = flow { 
    println("Flow started")
    for (i in 1..3) {
        delay(100)
        emit(i)
    }
}

fun main() = runBlocking<Unit> {
    println("Calling simple function...")
    val flow = simple()
    println("Calling collect...")
    flow.collect { value -> println(value) } 
    println("Calling collect again...")
    flow.collect { value -> println(value) } 
}
```

打印结果：

```shell
Calling simple function...
Calling collect...
Flow started
1
2
3
Calling collect again...
Flow started
1
2
3
```

### 1.2  通过 flowOn() 更改流发射的Context

`Flow` 通过 `flowOn()` 更改流发射的`Context`，`flowOn()` 的上游会在其更改的 `Context` 的`CoroutineDispatcher`中执行。
当上游流必须改变其上下文中的 `CoroutineDispatcher` 的时候，`flowOn` 操作符创建了另一个协程。

```kotlin
viewModelScope.launch {
    flow {emit(1)} // 运行在 dispatcher1
        .flowOn(dispatcher1)
        .map {"2"}// 运行在 dispatcher2
        .flowOn(dispatcher2)
        .collect { println(it)}// collect() 运行在调用该函数的协程体的 CoroutineDispatcher 中。这里是在 viewModelScope 的 main 中
}
```
### 1.3 过渡流操作符
- map
- filter
- transform(转换操作符): 使用 transform 操作符，我们可以 发射 任意值任意次
- take(限长操作符)


### 1.4 末端流操作符

末端操作符是在流上用于启动流收集的挂起函数。

- collect
- 转化为各种集合，例如 toList 与 toSet。
- 获取第一个（first）值与确保流发射单个（single）值的操作符。
- 使用 reduce 与 fold 将流规约到单个值

### 1.5 buffer 操作符
```kotlin
fun simple(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100) // 假装我们异步等待了 100 毫秒
        emit(i) // 发射下一个值
    }
}

fun main() = runBlocking<Unit> { 
    val time = measureTimeMillis {
        simple().collect { value -> 
            delay(300) // 假装我们花费 300 毫秒来处理它
            println(value) 
        } 
    }   
    println("Collected in $time ms")
} 
```
它会产生这样的结果，整个收集过程大约需要 1200 毫秒（3 个数字，每个花费 400 毫秒）：

```shell
1
2
3
Collected in 1220 ms  
```

使用 `buffer` 操作符来并发运行这个 `simple` 流中发射元素的代码以及收集的代码

```kotlin
fun main() = runBlocking<Unit> { 
    val time = measureTimeMillis {
        simple()
            .buffer() // 缓冲发射项，无需等待
            .collect { value -> 
                delay(300) // 假装我们花费 300 毫秒来处理它
                println(value) 
            } 
    }   
    println("Collected in $time ms")
}
```

它产生了相同的数字，只是更快了，由于我们高效地创建了处理流水线，仅仅需要等待第一个数字产生的 100 毫秒以及处理每个数字各需花费的 300 毫秒。
这种方式大约花费了 1000 毫秒来运行：

```shell
1
2
3
Collected in 1071 ms  
```

### 1.6 cancellable 操作符让繁忙的流可取消
```kotlin
(1..5).asFlow().cancellable().collect { value -> 
    if (value == 3) cancel()  
    println(value)
} 
```

###  1.7 conflate 操作符

当流代表部分操作结果或操作状态更新时，可能没有必要处理每个值，而是只处理最新的那个。在本示例中，当收集器处理它们太慢的时候， conflate 操作符可以用于跳过中间值。

```kotlin
fun simple(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100) // 假装我们异步等待了 100 毫秒
        emit(i) // 发射下一个值
    }
}

fun main() = runBlocking<Unit> { 
    val time = measureTimeMillis {
        simple()
            .conflate() // 合并发射项，不对每个值进行处理
            .collect { value -> 
                delay(300) // 假装我们花费 300 毫秒来处理它
                println(value) 
            } 
    }   
    println("Collected in $time ms")
}
```
虽然第一个数字仍在处理中，但第二个和第三个数字已经产生，因此第二个是 conflated ，只有最新的（第三个）被交付给收集器：
```shell
1
3
Collected in 758 ms
```
### 1.8 collectLatest 处理最新值

取消缓慢的收集器，并在每次发射新值的时候重新启动它

```kotlin
fun main() = runBlocking<Unit> { 
    val time = measureTimeMillis {
        simple()
            .collectLatest { value -> // 取消并重新发射最后一个值
                println("Collecting $value") 
                delay(300) // 假装我们花费 300 毫秒来处理它
                println("Done $value") 
            } 
    }   
    println("Collected in $time ms")
}
```
由于 `collectLatest` 的函数体需要花费 300 毫秒，但是新值每 100 秒发射一次，我们看到该代码块对每个值运行，但是只收集最后一个值：
```shell
Collecting 1
Collecting 2
Collecting 3
Done 3
Collected in 716 ms
```

tip: 有一组与 xxx 操作符执行相同基本逻辑的 xxxLatest 操作符，但是在新值产生的时候取消执行其块中的代码

## 2.组合多个流

### 2.1 zip:和 RxJava 的 zip 相同

组合两个流中的相关值，尽管两个流不同步

```kotlin
val nums = (1..3).asFlow().onEach { delay(300) } // 发射数字 1..3，间隔 300 毫秒
val strs = flowOf("one", "two", "three").onEach { delay(400) } // 每 400 毫秒发射一次字符串
val startTime = System.currentTimeMillis() // 记录开始的时间
nums.zip(strs) { a, b -> "$a -> $b" } // 使用“zip”组合单个字符串
    .collect { value -> // 收集并打印
        println("$value at ${System.currentTimeMillis() - startTime} ms from start") 
    } 
```

打印数据

```shell
1 -> one at 453 ms from start
2 -> two at 853 ms from start
3 -> three at 1255 ms from start
```

### 2.2 combine:和 RxJava combineLast 相同

每当上游流产生新值的时候都会调用 collect 方法

```kotlin
val nums = (1..3).asFlow().onEach { delay(300) } // 发射数字 1..3，间隔 300 毫秒
val strs = flowOf("one", "two", "three").onEach { delay(400) } // 每 400 毫秒发射一次字符串
val startTime = System.currentTimeMillis() // 记录开始的时间
nums.combine(strs) { a, b -> "$a -> $b" } // 使用“combine”组合单个字符串
    .collect { value -> // 收集并打印
        println("$value at ${System.currentTimeMillis() - startTime} ms from start") 
    }
```

打印数据，nums 或 strs 流中的每次发射都会打印一行：

```shell    
1 -> one at 452 ms from start
2 -> one at 651 ms from start
2 -> two at 854 ms from start
3 -> two at 952 ms from start
3 -> three at 1256 ms from start
```

### 2.3 flatMapConcat 和 flatMapMerge

`flatMapConcat`: 将 `flow` 里面的值铺平分别再迭代发射，发射是有序的，和生产数据的顺序一致
```kotlin
fun requestFlow(i: Int): Flow<String> = flow {
    emit("$i: First") 
    delay(500) // 等待 500 毫秒
    emit("$i: Second")    
}

fun main() = runBlocking<Unit> { 
    val startTime = System.currentTimeMillis() // 记录开始时间
    (1..3).asFlow().onEach { delay(100) } // 每 100 毫秒发射一个数字 
        .flatMapConcat { requestFlow(it) }                                                                           
        .collect { value -> // 收集并打印
            println("$value at ${System.currentTimeMillis() - startTime} ms from start") 
        } 
}
``` 
打印数据:

```shell 
1: First at 121 ms from start
1: Second at 622 ms from start
2: First at 727 ms from start
2: Second at 1227 ms from start
3: First at 1328 ms from start
3: Second at 1829 ms from start
```
`flatMapMerge`: 和 `flatMapConcat` 类似，发射是有序的，不同点是它的发射是并发的。
```kotlin
fun main() = runBlocking<Unit> { 
    val startTime = System.currentTimeMillis() // 记录开始时间
    (1..3).asFlow().onEach { delay(100) } // 每 100 毫秒发射一个数字 
        .flatMapMerge { requestFlow(it) }                                                                           
        .collect { value -> // 收集并打印
            println("$value at ${System.currentTimeMillis() - startTime} ms from start") 
        } 
}
```

```shell 
1: First at 121 ms from start
1: Second at 622 ms from start
2: First at 727 ms from start
2: Second at 1227 ms from start
3: First at 1328 ms from start
3: Second at 1829 ms from start
```
### 2.4 flatMapLatest

在发出新流后立即取消先前流的收集。
```kotlin
fun main() = runBlocking<Unit> { 
    val startTime = System.currentTimeMillis() // 记录开始时间
    (1..3).asFlow().onEach { delay(100) } // 每 100 毫秒发射一个数字 
        .flatMapLatest { requestFlow(it) }                                                                           
        .collect { value -> // 收集并打印
            println("$value at ${System.currentTimeMillis() - startTime} ms from start") 
        } 
}
```

```shell
1: First at 142 ms from start
2: First at 322 ms from start
3: First at 425 ms from start
3: Second at 931 ms from start
```

## 3.捕获异常

### 3.1 通过 try catch 捕获，不过不能做到异常透明性。
### 3.2 使用 catch 操作符来保留此异常的透明性并允许封装它的异常处理。

catch 操作符的代码块可以分析异常并根据捕获到的异常以不同的方式对其做出反应：

- 可以使用 throw 重新抛出异常。
- 可以使用 catch 代码块中的 emit 将异常转换为值发射出去。
- 可以将异常忽略，或用日志打印，或使用一些其他代码处理它。

catch 过渡操作符遵循异常透明性，仅捕获上游异常（catch 操作符上游的异常，但是它下面的不是）。 如果 collect { ... } 块（位于 catch 之下）抛出一个异常，那么异常会逃逸

fun simple(): Flow<Int> = flow {
    for (i in 1..3) {
        println("Emitting $i")
        emit(i)
    }
}

fun main() = runBlocking<Unit> {
    simple()
        .catch { e -> println("Caught $e") } // 不会捕获下游异常
        .collect { value ->
            check(value <= 1) { "Collected $value" }                 
            println(value) 
        }
} 

## 参考

[Reactive Streams and Kotlin Flows](https://elizarov.medium.com/reactive-streams-and-kotlin-flows-bfd12772cda4)

[kotlin->协程->异步流](https://www.kotlincn.net/docs/reference/coroutines/flow.html)

[对比 RxJava 入门 Kotlin-flow](https://juejin.cn/post/6978028971237572644#heading-2)
