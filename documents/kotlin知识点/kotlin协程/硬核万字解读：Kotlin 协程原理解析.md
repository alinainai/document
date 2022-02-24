Kotlin 协程在 Kotlin1.1-1.2 版本中还是实验性质的，在 Kotlin 1.3 版本开始提供了一个稳定版本，越来越多的开发者开始使用 Kotlin 协程。那么 Kotlin 协程是什么？

Kotlin 协程是一套基于 Java Thread 的线程框架，相较于 Java Executor 及 RxJava 等线程框架可以更方便的实现异步调用，很容易实现线程切换。Kotlin 协程另外一个核心的功能是非阻塞式挂起，它帮助开发者消除了回调，可以使用同步的代码写出异步的操作，当然也就消除了一些业务场景的回调地狱。

本文通过源码角度分析，帮助读者了解协程的本质以及协程启动、挂起、恢复的原理，线程切换及 Kotlin 协程是如何消除了回调。

使用 Kotlin 协程的时候，需要通过 CoroutineScope 创建一个协程。
```kotlin
 GlobalScope.launch(Dispatchers.Default) {  
   //创建一个协程并启动它  
      //闭包内为协程体  
 } 
```
在上述代码中，通过 GlobalScope.launch 创建并启动了一个使用默认调度器 Dispatchers.Default 分配运行线程的协程，闭包内的内容是一个协程体。源码分析都基于这个示例。


### 准备

在正式开始源码分析之前，我们改造一下协程启动的代码，如下是完整代码，将`协程体`单独定义一个变量，并在协程体中调用`suspend挂起函数`。
```kotlin
 class MainActivity : AppCompatActivity() {  
     override fun onCreate(savedInstanceState: Bundle?) {  
         super.onCreate(savedInstanceState)  
         setContentView(R.layout.activity_main)  
         startCoroutine()  
     }  
   
     private fun startCoroutine() {  
         // funTest协程体  
         val funTest: suspend CoroutineScope.() -> Unit = {  
             println("funTest")  
             suspendFun1()  
             suspendFun2()  
         }  
         GlobalScope.launch(Dispatchers.Default, block = funTest)  
     }  
       
     // 挂起函数  
     suspend fun suspendFun1() {  
         println("suspendFun1")  
     }  
     // 挂起函数  
     suspend fun suspendFun2() {  
         println("suspendFun2")  
     }  
 } 
```
Kotlin协程中使用了状态机，编译器会将协程体编译成一个匿名内部类，每一个挂起函数的调用位置对应一个挂起点。


#### 01反编译

对上述代码进行反编译，反编译的代码如下：
```kotlin
 final class MainActivity$startCoroutine$funTest$1 extends SuspendLambda implements Function2<CoroutineScope, Continuation<? super Unit>, Object> {  
     // 当前状态机的状态，默认为0  
     int label;  
     ...  
     // 创建一个Continuation对象返回  
     public final Continuation<Unit> create(Object obj, Continuation<?> continuation) {  
         ...  
         MainActivity$startCoroutine$funTest$1 mainActivity$startCoroutine$funTest$1 = new MainActivity$startCoroutine$funTest$1(this.this$0, continuation);  
         ...  
         return mainActivity$startCoroutine$funTest$1;  
     }  
   
     public final Object invoke(Object obj, Object obj2) {  
         return ((MainActivity$startCoroutine$funTest$1) create(obj, (Continuation) obj2)).invokeSuspend(Unit.INSTANCE);  
     }  
     // 协程体操作被转成invokeSuspend方法的调用  
     public final Object invokeSuspend(Object $result) {  
         int i = this.label;  
         if (i == 0) {  
             ...  
             System.out.println("funTest");  
             // 将状态机状态置为1  
             this.label = 1;  
             // 挂起函数suspendFun1的调用  
             if (mainActivity.suspendFun1(this) == coroutine_suspended) {  
                 return coroutine_suspended;  
             }  
         } else if (i == 1) {  
             // 异常处理  
             ...  
             ResultKt.throwOnFailure($result);  
         } else if (i == 2) {  
             // 异常处理  
             ...  
             ResultKt.throwOnFailure($result);  
             return Unit.INSTANCE;  
         } else {  
             throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");  
         }  
         this.label = 2;  
         // 挂起函数suspendFun2的调用，参数this  
         if (mainActivity2.suspendFun2(this) == coroutine_suspended) {  
             return coroutine_suspended;  
         }  
         //结束标识   
         return Unit.INSTANCE;  
     }  
 }
```
相应的挂起函数被编译如下：
```kotlin
 // 挂起函数编译后多了一个 Continuation类型的参数  
 public final Object suspendFun1(Continuation<? super Unit> $completion) {  
         System.out.println("suspendFun1");  
         return Unit.INSTANCE;  
     }  
 // 挂起函数编译后多了一个 Continuation类型的参数  
 public final Object suspendFun2(Continuation<? super Unit> $completion) {  
         System.out.println("suspendFun2");  
         return Unit.INSTANCE;  
     }
```
在反编译的代码中，协程体 funTest 被编译成一个继承 SuspendLambda 的类，在类中实现 create(),invokeSuspend() 两个方法，其中 create() 逻辑处理是创建了一个协程体 funTest 类的实例。

挂起函数 suspendFun1()，suspendFun2() 本来是没有参数的，但被编译成带有一个 Continuation 参数的函数，这也是为什么在普通函数中无法调用挂起函数原因。

在编译前的代码中，协程体的操作就是调用 suspendFun1()、suspendFun2() 挂起函数，仔细一些查看编译后代码，其实可以发现，类的成员变量中有一个 label 字段，控制 invokeSuspend() 方法执行不同的条件分支，挂起函数的调用被分布在了不同的条件分支中，并且挂起函数传参为 this，也就是协程体自身。

由此协程体被编译成一个继承SuspendLambda的类，并将协程体中的操作分割成invokeSuspend()中不同条件分支的调用，在后面篇幅中就称这个类为协程体类。


#### 02概念小结

这里先总结出Kotlin协程在执行过程中会出现的一些概念，避免在后续源码分析中出现混淆：

- 协程体：协程中要执行的操作，它是一个被suspend修饰的lambda 表达式;
- 协程体类:编译器会将协程体编译成封装协程体操作的匿名内部类;
- 协程构建器:用于构建协程的函数，比如launch，async;
- 挂起函数:由suspend修饰的函数，挂起函数只能在挂起函数或者协程体中被调用,可以在挂起函数中调用其它挂起函数实现不阻塞当前执行线程的线程切换，比如withContext()，但挂起函数并不一定会挂起，如果没有执行挂起操作;
- 挂起点：一般对应挂起函数被调用的位置;
- 续体:续体的换概念可以理解为挂起后，协程体中剩余要执行代码，笔者在文章中，将其看作为协程体类，在协程体类中封装了协程的要执行的操作，由状态机的状态将操作分割了成不同的片段，每一个状态对应不同代码片段的执行，可以与续体的概念对应。

### 核心类

#### 01SuspendLambda


在上文中提到协程体编译成了一个继承SuspendLambda的类，接下来我们看下SuspendLambda是什么。

SuspendLambda的类图如下：

<img width="800" alt="SuspendLambda的类图" src="https://user-images.githubusercontent.com/17560388/155473086-2843f35f-c7aa-4c0b-8749-8523a8745dbe.png">

继承链：SuspendLambda -> ContinuationImpl -> BaseContinuationImpl -> Continuation

我们从继承链的最顶部 Continuation 类开始，依次分析各个类的作用：

```kotlin
 public interface Continuation<in T> {  
     /** 
      * 协程上下文 
      */  
     public val context: CoroutineContext  
     /** 
      * 用于协程启动及挂起的恢复，另外也可以作为协程运行完成的回调 
      */  
     public fun resumeWith(result: Result<T>)  
 }
```
Continuation 是一个接口，内部的的实现也很简单，一个协程上下文属性 context，一个方法声明 resumeWith()，用于协程启动或者挂起时恢复，也可用于协程运行完成时的回调使用;

BaseContinuationImpl 实现接口 Continuation，看下源码实现：
```kotlin
 internal abstract class BaseContinuationImpl(  
     public val completion: Continuation<Any?>?  
 ) : Continuation<Any?>, CoroutineStackFrame, Serializable {  
     public final override fun resumeWith(result: Result<Any?>) {  
          ...  
          invokeSuspend(param)  
          ...  
     }  
     protected abstract fun invokeSuspend(result: Result<Any?>): Any?  
 }
```
BaseContinuationImpl 定义了一个抽象方法 invokeSuspend()，并重写了 Continuation 的 resumeWith()，并在其中调用 invokeSuspend()，上文我们提到协程体的操作都被转换成了invokeSuspend() 的调用，那么协程体的执行其实就是 resumeWith() 被调用。在 BaseContinuationImpl 中 invokeSuspend() 只是一个抽象方法，它的具体实现是在协程体类中。

BaseContinuationImpl 的源码被简化一部分，这里先不用管过多的细节，先记住，触发 resumeWith() 就可以触发 invokeSuspend() ，就可以使我们的协程体中的操作被执行。

另外继承链中还有类 ContinuationImpl ，它继承 BaseContinuationImpl，它的作用是使用拦截器生成一个 DispatchedContinuation 对象，这也是一个 Continuation，这个对象内部封装线程调度器，以及代理了协程体对象，这里先了解它的作用，后面的章节中会分析它的实现。

到这里出现第一个 Continuation 对象，它是一个协程体类，内部的方法 invokeSuspend() 包含协程体的处理逻辑。

了解上面的这些概念，在接下来的分析过程中会轻松一些。


#### 02GlobalScope作用域

在示例中使用 GlobalScope 创建了一个协程，看一下 GlobalScope 的源码：
```kotlin
 public object GlobalScope : CoroutineScope {  
     // 重写coroutineContext，返回一个空的协程上下文  
     override val coroutineContext: CoroutineContext  
         get() = EmptyCoroutineContext  
 }  
 public interface CoroutineScope {  
     // 协程上下文  
     public val coroutineContext: CoroutineContext  
 }
```
GlobalScope 实现了 CoroutineScope 接口，而 CoroutineScope 只有一个属性 CoroutineContext 协程上下文，并且 GlobalScope 重写了这个上下文，返回了一个空的协程上下文。GlobalScope 由object 修饰，是一个单例对象，所以它的生命周期跟随整个应用。

CoroutineScope 是一个作用范围，可以通过 CoroutineScope 的扩展函数去创建一个协程，当这个作用范围被取消的时候，它内部的协程也会被取消，比如 viewModelScope、lifecycleScope 具有这样的功能，但是 GlobalScope 除外，GlobalScope 是全局性的，无法通过自身取消内部协程。

示例中使用 launch() 函数创建了一个协程并启动它，看下launch()的实现:

```kotlin
 // launch是CoroutineScope的一个扩展函数  
 public fun CoroutineScope.launch(  
     context: CoroutineContext = EmptyCoroutineContext,  
     start: CoroutineStart = CoroutineStart.DEFAULT,  
     block: suspend CoroutineScope.()
 ): Job {  
     val newContext = newCoroutineContext(context)  
     val coroutine = if (start.isLazy)  
         LazyStandaloneCoroutine(newContext, block) else  
         StandaloneCoroutine(newContext, active = true)  
     coroutine.start(start, coroutine, block)  
     return coroutine  
 }
```
在一开始就看到了，launch 函数是 CoroutineScope 的一个扩展函数，CoroutineScope 只是一个接口，但是可以通过 CoroutineScope 的扩展方法进行协程的创建，除了 launch 函数还有 async 函数。

CoroutineScope 除了通过扩展函数创建协程还有其它两个作用，launch 函数返回一个Job对象，可以通过这个 Job 管理协程，另外 CoroutineScope 为协程提供一个上下文 CoroutineContext。

launch函数存在3个参数:

- CoroutineContext 协程的上下文
- CoroutineStart 协程的启动模式
- suspend CoroutineScope.() -> Unit 协程体

接下来分别看下这个三个参数的含义。


#### 03CoroutineContext上下文


CoroutineContext 协程的上下文，这是一个数据集合接口声明，协程中 Job、Dispatcher 调度器都可以是它的元素,CoroutineContext 有一个非常好的作用就是我们可以通过它拿到 Job、Dispatcher 调度器等数据。

源码解析：
```kotlin
 public interface CoroutineContext {  
     // 由operator修饰的操作符重载，对应“[]”操作符  
     // 通过key获取一个Element对象  
     public operator fun <E : Element> get(key: Key<E>): E?  
   
     // 遍历当前集合的每一个Element，并对每一个元素进行operation操作，将操作后的结果进行累加，以initial为起始开始累加，最终返回一个新的CoroutineContext上下文  
     public fun <R> fold(initial: R, operation: (R, Element) -> R): R  
     // 由operator修饰的操作符重载，对应“+”操作符；
     // 合并两个CoroutineContext对象中的Element元素，将合并后的上下文返回，如果存在相同key的Element对象，则对其进行覆盖；
     // EmptyCoroutineContext一个空实现的上下文；
     // CombinedContext是CoroutineContext接口的一个实现类，也是链表的具体实现的一个节点，节点存在两个元素：element 当前的节点的集合元素，left CoroutineContext类型，指向链表的下一个元素；
     // 另外plus函数在合并上下文的过程中将Key为ContinuationInterceptor的元素保持在链表的尾部，方便其快速的读取；
     // 先了解ContinuationInterceptor是一个拦截器，下文中会介绍它  

     public operator fun plus(context: CoroutineContext): CoroutineContext =  
         if (context === EmptyCoroutineContext) this else // 如果待合并的context是一个空上下文，返回当前的上下文  
             // fold遍历context集合  
             context.fold(this) { acc, element ->//acc为当前上下文的集合，element为context集合的元素  
                 val removed = acc.minusKey(element.key)//移除aac集合中的element元素，并返回移除后的一个集合  
                 if (removed === EmptyCoroutineContext)  
                      element // 如果移除后集合是一个空的上下文集合，那么当前element元素为合并后的上下文集合  
                 else {  
                     val interceptor = removed[ContinuationInterceptor]//获取拦截器  
                     if (interceptor == null) CombinedContext(removed, element) // 如果interceptor为空，生成CombinedContext节点，CombinedContext元素为element，指向的链表节点是removed  
                     else {  
                         // 将拦截器移至链表尾部方便读取  
                        val left = removed.minusKey(ContinuationInterceptor)  
                         if (left === EmptyCoroutineContext) CombinedContext(element, interceptor) else  
                             CombinedContext(CombinedContext(left, element), interceptor)  
                     }  
                 }  
             }  
   
     // 删除对应key的Element元素，返回删除后CoroutineContext  
     public fun minusKey(key: Key<*>): CoroutineContext     
     // 集合中每个元素的key  
     public interface Key<E : Element>  
   
     // 集合中的元素定义，也是一个接口  
     public interface Element : CoroutineContext {  
         // 元素的key  
         public val key: Key<*>  
   
         // 通过key获取该元素，对应操作符[]  
         public override operator fun <E : Element> get(key: Key<E>): E? =  
             @Suppress("UNCHECKED_CAST")  
             if (this.key == key) this as E else null  
         //// 提供遍历上下文中所有元素的能力。  
         public override fun <R> fold(initial: R, operation: (R, Element) -> R): R =  
             operation(initial, this)  
   
        // 删除对应key的Element元素  
         public override fun minusKey(key: Key<*>): CoroutineContext =  
             if (this.key == key) EmptyCoroutineContext else this  
     }  
 }
```
CoroutineContext 是一个接口，声明的方法展示了它的能力，是一个以 Key 为索引的数据集合，它的 Key 是一个 interface，每一个元素的类型是 Element，而 Element 又实现 CoroutineContext，所以它既可以是一个集合的元素，也可以是一个集合。

CombinedContext 是 CoroutineContext 接口的具体实现类，存在两个属性，其中 element 是一个 Element，代表集合的元素，left 是一个 CoroutineContext，代表链表的下一个节点。

通过 CoroutineContext#plus 可以看出，CoroutineContext 的数据存储方式是一个左向链表，链表的每一个节点是 CombinedContext，并且存在拦截器的情况下，拦截器永远是链表尾部的元素，这样设计目的是因为拦截器的使用频率很高，为了更快的读取拦截器;

看一下链表节点 CombinedContext 的实现，类图如下：

<img width="400" alt="CombinedContext的实现" src="https://user-images.githubusercontent.com/17560388/155476708-ea78c444-8c80-4235-879b-25640301fd45.png">

源码解析：
```kotlin
 // 左向链表实现  
 // element集合元素  
 // left 链表的下一个节点  
 internal class CombinedContext(  
     private val left: CoroutineContext,  
     private val element: Element  
 ) : CoroutineContext, Serializable {  
   
     // 在集合中获取一个以key为键的元素  
     override fun <E : Element> get(key: Key<E>): E? {  
         var cur = this  
         while (true) {  
             cur.element[key]?.let { return it }  
             val next = cur.left  
             if (next is CombinedContext) {  
                 cur = next  
             } else {  
                 return next[key]  
             }  
         }  
     }  
   
     // 遍历集合中所有的元素。  
     public override fun <R> fold(initial: R, operation: (R, Element) -> R): R =  
         operation(left.fold(initial, operation), element)  
   
     // 在集合中删除一个键值为key的元素  
     public override fun minusKey(key: Key<*>): CoroutineContext {  
         element[key]?.let { return left }  
         val newLeft = left.minusKey(key)  
         return when {  
             newLeft === left -> this  
             newLeft === EmptyCoroutineContext -> element  
             else -> CombinedContext(newLeft, element)  
         }  
     }  
   
     // 集合长度  
     private fun size(): Int {  
         var cur = this  
         var size = 2  
         while (true) {  
             cur = cur.left as? CombinedContext ?: return size  
             size++  
         }  
     }  
   
     // 集合中是否包含某个元素  
     private fun contains(element: Element): Boolean =  
         get(element.key) == element  
        ...  
 }
```
CoroutineContex 定义集合的能力，而 CombinedContext 是 CoroutineContext 集合能力的具体实现，这个实现是一个左向链表;


#### 04CoroutineStart 启动模式


CoroutineStart 是协程的启动模式，存在以下4种模式：

- DEFAULT 立即调度，可以在执行前被取消
- LAZY 需要时才启动，需要start、join等函数触发才可进行调度
- ATOMIC 立即调度，协程肯定会执行，执行前不可以被取消
- UNDISPATCHED 立即在当前线程执行，直到遇到第一个挂起点（可能切线程）

#### 05suspend CoroutineScope.() -> Unit

suspend CoroutineScope.() -> Unit 协程体，这是一个 Lambda 表达式，也就是协程中要执行的代码块，即上文中 launch 函数闭包中的代码，这是一个被 suspend 修饰符修饰的 "CoroutineScope扩展函数类型" 的参数，这样定义的好处就是可以在协程体中访问这个对象的属性，比如 CoroutineContext 上下文集合。

在反编译的章节中提到，这个 Lambda 表达式被编译成了协程体类。

从启动协程的示例代码中，launch 函数传入 Dispatchers.Default 默认调度器，这个 Dispatchers.Default 对应的 launch 函数的 CoroutineContext 参数.

#### 06Dispatchers调度器

Dispatchers 是协程中提供的线程调度器，用来切换线程，指定协程所运行的线程。

Dispatchers源码分析：
```kotlin 
 public actual object Dispatchers {  
   
     // 默认调度器  
     @JvmStatic  
     public actual val Default: CoroutineDispatcher = createDefaultDispatcher()  
     
     // UI调度器  
     @JvmStatic  
     public actual val Main: MainCoroutineDispatcher get() = MainDispatcherLoader.dispatcher  
     // 无限制调度器  
     @JvmStatic  
     public actual val Unconfined: CoroutineDispatcher = kotlinx.coroutines.Unconfined  
     // IO调度器  
     @JvmStatic  
     public val IO: CoroutineDispatcher = DefaultScheduler.IO  
 }
```
Dispatchers中提供了4种类型调度器：

- Default 默认调度器，适合CPU密集型任务调度器 比如逻辑计算；
- Main UI调度器；
- Unconfined 无限制调度器，对协程执行的线程不做限制，协程恢复时可以在任意线程；
- IO IO调度器，适合IO密集型任务调度器 比如读写文件，网络请求等。

以示例中的Dispatchers.Default为例分析，从上述Dispatchers的源码中可以看到，Default的类型是一个CoroutineDispatcher（所有的调度器都是CoroutineDispatcher的子类）。


#### 2.6.1 CoroutineDispatcher调度器

首先看一下它的类图：

<img width="800" alt="类图" src="https://user-images.githubusercontent.com/17560388/155477733-a78c6888-5ff5-431b-b78d-a5da5581f495.png">

CoroutineDispatcher 继承 AbstractCoroutineContextElement，AbstractCoroutineContextElement 是 Element接口的一个抽象实现类，而 Element 又实现 CoroutineContext 接口，所以调度器本身既是一个 CoroutineContext，也可以作为 CoroutineContext 集合的元素存放其中。

CoroutineDispatcher 还实现 ContinuationInterceptor 接口，ContinuationInterceptor 是一个拦截器的接口定义，也是 Kotlin 协程提供的拦截器的规范。
```kotlin
 ContinuationInterceptor
   public interface ContinuationInterceptor : CoroutineContext.Element {  
     // 实现CoroutineContext.Element接口，说明自身是CoroutineContext上下文集合的一个元素类型  
     // 定义伴生对象Key作为集合中的索引key，可直接通过类名访问该伴生对象  
     companion object Key : CoroutineContext.Key<ContinuationInterceptor>  
   
     // 传入一个Continuation对象，并返回一个新的Continuation对象  
     // 在协程中，这里的传参continuation就是协程体编译后Continuation对象  
     public fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T>  
   
     public fun releaseInterceptedContinuation(continuation: Continuation<*>) {  
         
     ...  
 }
```

首先 ContinuationInterceptor 实现 CoroutineContext.Element 接口，Element 是集合的元素类型，所以拦截器可以作为 CoroutineContext 集合的一个元素存放其中。

在 ContinuationInterceptor 中定义了一个伴生对象Key，它的类型是 CoroutineContext.Key<Element>，所以 Key 也是拦截器作为 Element 元素的索引，Key 是一个伴生对象，可以通过类名访问它，则 CoroutineContext[ContinuationInterceptor] 就可以在集合中获取到拦截器。这里使用伴生对象作为集合元素的索引，一是伴生对象成员全局唯一，另一个通过类名访问集合元素，更直观。

ContinuationInterceptor#interceptContinuation 的作用是对协程体类对象 continuation 做一次包装，并返回了一个新的 Continuation 对象，而这个新的 Continuation 对象本质上是代理了原有的协程体类对象 continuation。

上面介绍了拦截器的接口定义，接下来看看它的具体实现 CoroutineDispatcher 调度器，源码如下：
```kotlin
 //CoroutineDispatcher 
 public abstract class CoroutineDispatcher :  
     AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {  
     ...  
     // 是否需要线程调度  
     public open fun isDispatchNeeded(context: CoroutineContext): Boolean = true  
       // 线程调度，让一个runable对象在指定线程运行  
       public abstract fun dispatch(context: CoroutineContext, block: Runnable)  
            // 将协程体对象continuation封装为一个DispatchedContinuation对象  
     public final override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =  
         DispatchedContinuation(this, continuation)  
        @InternalCoroutinesApi  
     public override fun releaseInterceptedContinuation(continuation: Continuation<*>) {  
         (continuation as DispatchedContinuation<*>).reusableCancellableContinuation?.detachChild()  
     }  
            ...  
 }
``` 
源码中 CoroutineDispatcher 是一个抽象类，并实现了拦截器的接口，也就是说调度器本质上就是一个拦截器，所有的调度器都是继承这个类来实现自身的调度逻辑。

在 CoroutineDispatcher 中重写了 interceptContinuation()，将我们协程体类对象 Continuation 包装成一个 DispatchedContinuation 对象，这个 DispatchedContinuation 本质上是代理了协程体类对象 Continuation，并且它自身也是一个 Continuation。


#### 2.6.2 DispatchedContinuation 包装

DispatchedContinuation 是出现的第二个 Continuation 对象，代理协程体 Continuation 对象并持有线程调度器，它的作用就是使用线程调度器将协程体调度到指定的线程执行。熟悉一下DispatchedContinuation 的类图，然后看下它的源码实现：

<img width="800" alt="类图" src="https://user-images.githubusercontent.com/17560388/155478483-e952f44b-9c64-42e4-babb-c2134dc604e3.png">

我们看下源码：
```kotlin
 //DispatchedContinuation 
 internal class DispatchedContinuation<in T>(  
     // 调度器  
     @JvmField val dispatcher: CoroutineDispatcher,  
     // 协程体Continuation对象  
     @JvmField val continuation: Continuation<T>  
 ) : DispatchedTask<T>(MODE_ATOMIC_DEFAULT), CoroutineStackFrame, Continuation<T> by continuation {  
      
     // 使用delegate存储当前对象  
     override val delegate: Continuation<T>  
         get() = this  
     // ATOMIC启动模式  
     override fun resumeWith(result: Result<T>) {  
         val context = continuation.context  
         val state = result.toState()  
         // 是否需要线程调度  
         if (dispatcher.isDispatchNeeded(context)) {  
             _state = state  
             resumeMode = MODE_ATOMIC_DEFAULT  
             // dispatch 调度线程，第二个参数是一个Runnable类型，这里传参this也就是DispatchedContinuation自身  
             // DispatchedContinuation实际上也是一个Runnable对象，调用调度器的dispatch方法之后就可以使这个runnable在指定的线程运行了  
             dispatcher.dispatch(context, this)  
         } else {  
             executeUnconfined(state, MODE_ATOMIC_DEFAULT) {  
                 withCoroutineContext(this.context, countOrElement) {  
                     // 不需要调度，执行协程体的resumeWith  
                     continuation.resumeWith(result)  
                 }  
             }  
         }  
     }  
      // 默认启动模式  
      inline fun resumeCancellableWith(result: Result<T>) {  
         val state = result.toState()  
         if (dispatcher.isDispatchNeeded(context)) {  
             _state = state  
             resumeMode = MODE_CANCELLABLE  
             dispatcher.dispatch(context, this)  
         } else {  
             executeUnconfined(state, MODE_CANCELLABLE) {  
                 if (!resumeCancelled()) {  
                     resumeUndispatchedWith(result)  
                 }  
             }  
         }  
     }  
 }
```
    
DispatchedContinuation 存在两个参数，拦截器 dispatcher（在这里就是指的就是线程调度器 Dispatcher）另一个参数 continuation 指协程体类对象；

DispatchedContinuation 也实现了 Continuation 接口，并重写 resumeWith()，内部实现逻辑：

- 1.如果需要线程调度，则调用 dispatcher#dispatch 进行调度，而 dispatch() 的第二个参数是一个 runnable 对象（这里传参为this，即 DispatchedContinuation 对象本身，DispatchedContinuation 同时还实现了 Runnable 接口），不难猜出，这个 runnable 就会运行在调度的线程上；

- 2.不需要调度则直接调用协程体类 continuation 对象的 resumeWith()，前面的章节中提到,协程体的运行就是协程体类 Continuation 对象的 resumeWith() 被触发,所以这里就会让协程体在当前线程运行；

另外还有一个方法 resumeCancellableWith()，它和 resumeWith() 的实现很类似，在不同的启动模式下调度线程的方法调用不同。比如默认的启动模式调用 resumeCancellableWith()，ATOMIC启动模式则调用 resumeWith()。

在这里的 dispatcher 是抽象对象，具体的调度逻辑，在相应的调度器实现中封装，比如示例中的 Dispatchers.Default。

至此，经过拦截器的处理这时候协程体 Continuation 对象被包装成了带有调度逻辑的 DispatchedContinuation 对象。

DispatchedContinuation 还继承了 DispatchedTask 类，从类图中可以看到 DispatchedTask 最终实现了 Runable 接口，所以重点关注 DispatchedTask 的 run() 实现。
```koltin
 //DispatchedTask
 internal abstract class DispatchedTask<in T>(  
     @JvmField public var resumeMode: Int  
 ) : SchedulerTask() {  
 ’  
     // 在DispatchedContinuation中重写了该属性，delegate实际是指DispatchedContinuation对象  
     internal abstract val delegate: Continuation<T>  
   
     public final override fun run() {  
             ...  
             val delegate = delegate as DispatchedContinuation<T>  
             // 通过delegate拿到原始协程体Continuation对象  
             val continuation = delegate.continuation  
             ...  
             // 调用协程体的resumeWith  
             continuation.resume(getSuccessfulResult(state))  
             ...  
          
     }  
   
 }     

 // Continuation的扩展方法，触发Continuation内的方法resumeWith  
 public inline fun <T> Continuation<T>.resume(value: T): Unit =  
     resumeWith(Result.success(value)) 
```
在 DispatchedContinuation 中，重写了 delegate 属性并赋值为 this，所以在 DispatchedTask 中，delegate 就是 DispatchedContinuation。

在 run() 的逻辑中，通过 DispatchedContinuation 拿到了原始的协程体类 Continuation 对象，并通过 Continuation 的扩展方法 resume() 触发协程体的 resumeWith()，到这里就清楚了，只要让这个 runable 在指定的的线程运行就实现了线程的调度。而调度器的实现就是将这个 runable 对象在指定的线程运行，这也是 dispatcher#dispatch() 的作用。


2.6.3 Dispatchers.Default 默认调度器

dispatcher#dispatch() 的实现是在调度器的具体实现类中，比如示例中的 Dispatchers.Default，看一下 Dispatchers.Default 的整体类图:

<img width="800" alt="类图" src="https://user-images.githubusercontent.com/17560388/155480562-77b9d2d6-e583-40b5-9fd3-d3e1d82928d1.png">

现在继续分析 Dispatchers.Default 的实现。使用 createDefaultDispatcher() 创建一个默认的调度器：
    
```kotlin 
 //useCoroutinesScheduler 读取key为 ”kotlinx.coroutines.scheduler“ 的系统属性，，默认值为on，所以useCoroutinesScheduler==true  
 internal actual fun createDefaultDispatcher(): CoroutineDispatcher =  
     if (useCoroutinesScheduler) DefaultScheduler else CommonPool  
 ...  
 默认情况下useCoroutinesScheduler为true，所以会构建一个DefaultScheduler  
   
 DefaultScheduler源码:   
 internal object DefaultScheduler : ExperimentalCoroutineDispatcher() {  
     // IO 调度器  
     val IO: CoroutineDispatcher = LimitingDispatcher(  
         this,  
         systemProp(IO_PARALLELISM_PROPERTY_NAME, 64.coerceAtLeast(AVAILABLE_PROCESSORS)),  
         "Dispatchers.IO",  
         TASK_PROBABLY_BLOCKING  
     )  
     ...  
 }
```
    
调度器的核心是重写 dispatch()，显然 DefaultScheduler 内并没有，从类图中可以看到 dispatch() 的实现是在它的父类中，看一下 DefaultScheduler 父类 ExperimentalCoroutineDispatcher源码实现：
```kotlin 
 public open class ExperimentalCoroutineDispatcher(  
     // 核心线程数  
     private val corePoolSize: Int,  
     // 最大线程数  
     private val maxPoolSize: Int,  
     // 线程保活时间  
     private val idleWorkerKeepAliveNs: Long,  
     // 线程池名称  
     private val schedulerName: String = "CoroutineScheduler"  
 ) : ExecutorCoroutineDispatcher() {  
     public constructor(  
         corePoolSize: Int = CORE_POOL_SIZE,  
         maxPoolSize: Int = MAX_POOL_SIZE,  
         schedulerName: String = DEFAULT_SCHEDULER_NAME  
     ) : this(corePoolSize, maxPoolSize, IDLE_WORKER_KEEP_ALIVE_NS, schedulerName)  
   
     @Deprecated(message = "Binary compatibility for Ktor 1.0-beta", level = DeprecationLevel.HIDDEN)  
     public constructor(  
         corePoolSize: Int = CORE_POOL_SIZE,  
         maxPoolSize: Int = MAX_POOL_SIZE  
     ) : this(corePoolSize, maxPoolSize, IDLE_WORKER_KEEP_ALIVE_NS)  
   
     override val executor: Executor  
         get() = coroutineScheduler  
   
     private var coroutineScheduler = createScheduler()  
   
     // block：DispatchedContinuation对象  
     override fun dispatch(context: CoroutineContext, block: Runnable): Unit =  
         try {  
             // 交付coroutineScheduler线程池分配线程  
             coroutineScheduler.dispatch(block)  
         } catch (e: RejectedExecutionException) {  
             DefaultExecutor.dispatch(context, block)  
         }  
     // 创建线程池  
     private fun createScheduler() = CoroutineScheduler(corePoolSize, maxPoolSize, idleWorkerKeepAliveNs, schedulerName)  
 }
```
很明显核心线程数、最大线程数，是线程池的概念，这些参数，在构建 CoroutineScheduler 对象的时候被使用，CoroutineScheduler 是一个 Kotlin 封装的线程池,协程运行的线程由coroutineScheduler 分配。

在 ExperimentalCoroutineDispatcher 中找到调度器 dispatch() 方法的实现，它的实现很简单，调用 coroutineScheduler.dispatch()。

调度器的 dispatch(CoroutineContext,Runnable) 方法声明有两个参数，其中第二个参数 Runnable，在分析 DispatchedContinuation 的章节中提到，传参为 DispatchedContinuation 自身，这个DispatchedContinuation 也作为 coroutineScheduler.dispatch() 方法的调用参数。

继续跟进看看 CoroutineScheduler 实现及它的 dispatch()。

再次提醒一下，coroutineScheduler.dispatch() 方法中，这个 Runnable 类型的参数 block 是指 DispatchedContinuation。


#### 2.6.4 Worker线程

CoroutineScheduler 是一个Kotlin实现的线程池，提供协程运行的线程。

分析 CoroutineScheduler 源码之前我们先看下 Worker，CoroutineScheduler 是一个线程池，它生成的就是线程，Worker 就是 Kotlin 协程的线程，Worker 的实现是继承了 Thread，本质上还是对java线程的一次封装，另下文中提及的 Tas k实际为一个 DispatchedContinuation 对象，DispatchedContinuation 继承Task ；

Worker存在5种状态：

- CPU_ACQUIRED 获取到cpu权限
- BLOCKING 正在执行IO阻塞任务
- PARKING 已处理完所有任务，线程挂起
- DORMANT 初始态
- TERMINATED 终止态

Worker源码解析：
```kotlin
 internal inner class Worker private constructor() : Thread() {  
   ...  
   // 私有任务队列，存储Task  
   @JvmField  
   val localQueue: WorkQueue = WorkQueue()  
   // 重写Thread的run方法，调用runWorker方法  
   override fun run() = runWorker()  
     
   private fun runWorker() {  
             var rescanned = false  
             while (!isTerminated && state != WorkerState.TERMINATED) {// 死循环，保证线程存活  
                  // 本地队列或者全局队列中获取一个task  
                  // mayHaveLocalTasks 本地任务队列中是否存在任务  
                  val task = findTask(mayHaveLocalTasks)  
   
                  if (task != null) {  
                      // 找到task并执行Task- 也就是DispatchedContinuation对象  
                         rescanned = false  
                         minDelayUntilStealableTaskNs = 0L  
                         executeTask(task)  
                         continue  
                   } else {  
                         // 表示线程私有队列中没有任务  
                         mayHaveLocalTasks = false  
                   }  
                   // 没有找到task执行以下流程  
                   // minDelayUntilStealableTaskNs != 0L 存在正在窃取的任务（从其它线程队列中获取任务）  
                   if (minDelayUntilStealableTaskNs != 0L) {  
                         if (!rescanned) {  
                             // 重新扫描标志，再来检查一次任务队列是否存在任务  
                             rescanned = true  
                         } else {  
                             // 再次扫描仍没有任务，更新线程状态为挂起  
                             rescanned = false  
                             // 更新线程状态为挂起  
                             tryReleaseCpu(WorkerState.PARKING)  
                             // 线程中断标识更新为中断  
                             interrupted()  
                                                         // 阻塞当前线程不超过minDelayUntilStealableTaskNs 纳秒，使其在尽可能在任务可窃取到后唤醒  
                            LockSupport.parkNanos(minDelayUntilStealableTaskNs)  
                             minDelayUntilStealableTaskNs = 0L  
                         }  
   
                         continue  
                     }  
                     tryPark()//无任务时，将线程挂起  
                 }  
                 tryReleaseCpu(WorkerState.TERMINATED)  
            }  
    }
```
Worker 继承 Thread 是一个线程，线程的启动会执行 run 方法，在 Worker 的 run() 中，调用 runWorker()，而 runWorker() 中首先启动了一个有条件的死循环，在线程的状态未被置为 TERMINATED终止时，线程一直存活，在循环体中遍历私有和全局任务队列，此时分为两个分支：

- 1. 如找到Task,则运行该Task

- 2. 如未找到判断是否存在可窃取的任务，这里的判断条件是根据 minDelayUntilStealableTaskNs 来进行的，它的定义就是经过本身值的时间之后，至少存在一个可窃取的任务：

minDelayUntilStealableTaskNs 非 0 时，重新扫描一遍队列，是否已有任务，如依然没有任务，进入下次循环，这次循环将线程阻塞 minDelayUntilStealableTaskNs 纳秒后唤醒，同时将minDelayUntilStealableTaskNs 置为 0；
minDelayUntilStealableTaskNs 为 0，没有可偷窃的任务，将线程进行挂起，等待唤醒;

下面给出 Worker#run() 的处理流程图：

<img width="800" alt="Worker#run()的处理流程图" src="https://user-images.githubusercontent.com/17560388/155482139-88fc210d-1a6e-4128-a4eb-98bdb7154736.png">

下面的篇幅中会对循环体中各个操作进行分析：

在循环体中首先进行了任务的查找 Worker#findTask()
```kotlin
 //Worker#findTask()
 fun findTask(scanLocalQueue: Boolean): Task? {  
         // scanLocalQueue 该参数在线程池任务分发,将任务提交到线程本地队列中时，被置为true  
         if (tryAcquireCpuPermit()) return findAnyTask(scanLocalQueue) // 可以占有CPU权限（还有剩余核心占用）  
               
             // 以下为不能占有CPU的处理  
             // 源码注释：If we can't acquire a CPU permit -- attempt to find blocking task  
             val task = if (scanLocalQueue) {  
             // 从Worker本地队列中获取任务，未获取到任务则去全局阻塞队列中获取任务，globalBlockingQueue定义在CoroutineScheduler中的IO阻塞队列  
                 localQueue.poll() ?: globalBlockingQueue.removeFirstOrNull()   
             } else {  
                 // 从全局阻塞队列中获取任务  
                 globalBlockingQueue.removeFirstOrNull()  
             }  
             // task为空，则去其它线程队列中获取一个阻塞型任务  
             return task ?: trySteal(blockingOnly = true)  
         }  
           
         // 是否占有CPU,占有则将线程状态置为WorkerState.CPU_ACQUIRED  
         private fun tryAcquireCpuPermit(): Boolean = when {  
             state == WorkerState.CPU_ACQUIRED -> true  
             this@CoroutineScheduler.tryAcquireCpuPermit() -> {  
                 state = WorkerState.CPU_ACQUIRED  
                 true  
             }  
             else -> false  
         }     
           
         // 获取到CPU权限的情况  
          private fun findAnyTask(scanLocalQueue: Boolean): Task? {  
               
             if (scanLocalQueue) {  
                 // scanLocalQueue==true时 Worder本地队列存在任务  
                 // globalFirst随机数，在==0的情况下从全局阻塞任务队列或者非阻塞cpu密集型任务队列中获取一个任务  
                 val globalFirst = nextInt(2 * corePoolSize) == 0       
                 if (globalFirst) pollGlobalQueues()?.let { return it }  
                 // 在Worder本地队列的获取task  
                 localQueue.poll()?.let { return it }  
                 // globalFirst非0的时从全局阻塞任务队列或者非阻塞cpu密集型任务队列中获取一个任务  
                 if (!globalFirst) pollGlobalQueues()?.let { return it }  
             } else {  
                 pollGlobalQueues()?.let { return it }  
             }   
             // 尝试从其它线程获取任务（cpu密集型或者io型）  
             return trySteal(blockingOnly = false)  
         }
```
查找任务时，首先检查CPU权限，这里存在两种情况：

1.可以占用 cpu 权限，这里有一个反饥饿随机数的机制，随机从线程私有队列及全局队列中获取任务，如果获取不到，则通过 trySteal(blockingOnly = false) 方法，尝试从其它线程获取 `cpu密集型任务`或者 `IO任务`；

globalFirst 是一种反饥饿机制，作用就是概率性的从本地队列及全局队列中获取 Task，确保内部和外部任务的进度;

2.不能占用 cpu权限，这里源码中有一段注释：If we can't acquire a CPU permit -- attempt to find blocking task，在获取不到cpu许可时，尝试找到一个阻塞任务。这里的处理是优先取本地队列任务，未获取到则取全局IO队列，都未获取到，则通过trySteal(blockingOnly = true)方法，尝试从其它线程获取IO任务；

Worker#findTask()处理流程图:

<img width="800" alt="类图" src="https://user-images.githubusercontent.com/17560388/155483855-3a4e0841-03ea-42e1-b4f7-571d8a3dc3b0.png">

获取到任务后，这个任务可能是IO密集型的也可能是cpu密集型的，接下来就是执行这个任务，即executeTask()执行任务的相关逻辑处理 ：
```kotlin
 //Worker#executeTask ()
 private fun executeTask(task: Task) {  
           // taskMode存在两种类型  
           // TASK_NON_BLOCKING 非阻塞任务  
           // TASK_PROBABLY_BLOCKING 阻塞任务  
           val taskMode = task.mode  
           idleReset(taskMode)  
           beforeTask(taskMode)  
           runSafely(task)  
           afterTask(taskMode)  
       }
```
Worker#idleReset(): 如当前线程是状态为 WorkerState.PARKING，而且要执行的任务为阻塞型的任务时，将线程状态置为 WorkerState.BLOCKING：
```kotlin
 //Worker#idleReset()
 private fun idleReset(mode: Int) {  
             terminationDeadline = 0L   
             if (state == WorkerState.PARKING) {// 线程状态为挂起  
                 assert { mode == TASK_PROBABLY_BLOCKING }  
                 // 只有任务为IO任务时，进行线程状态的转换  
                 state = WorkerState.BLOCKING  
             }  
         } 
```
    
Worker#beforeTask()：如当前任务为IO型任务，则释放cpu权限，进行线程唤醒，如唤醒失败，尝试新建一个新的线程.
    
```kotlin 
 //Worker#beforeTask()
 private fun beforeTask(taskMode: Int) {  
             // 当前任务为cpu密集型任务，不用处理  
             if (taskMode == TASK_NON_BLOCKING) return  
             if (tryReleaseCpu(WorkerState.BLOCKING)) 
             {                   // 当前任务为IO任务，且当前线程占有CPU权限  
                 signalCpuWork()  
             }  
         }  
  // 将线程状态置为newState，如果线程占有CPU进行释放  
  internal fun tryReleaseCpu(newState: WorkerState): Boolean {  
             val previousState = state  
             val hadCpu = previousState == WorkerState.CPU_ACQUIRED  
             if (hadCpu) releaseCpuPermit()// 释放cpu权限  
             if (previousState != newState) state = newState  
             return hadCpu  
         }  
     // 唤醒一个线程或者启动一个新的线程，该方法定义在CoroutineScheduler中  
     internal fun signalCpuWork() {  
         // parkedWorkersStack堆栈中获取一个挂起线程的index,  
         // 通过index获取一个挂起线程，如并行唤醒  
         if (tryUnpark()) return  
         // 唤醒失败，创建一个新的线程  
         if (tryCreateWorker()) return  
         tryUnpark()  
     }  
     
   private fun tryUnpark(): Boolean {  
         while (true) {  
             val worker = parkedWorkersStackPop() ?: return false //获取一个挂起线程  
             if (worker.workerCtl.compareAndSet(PARKED, CLAIMED)) {  
                 LockSupport.unpark(worker)//唤醒  
                 return true  
             }  
         }  
     }  
```
如果唤醒挂起线程失败，则尝试进行一个新线程的创建：
```kotlin  
 //CoroutineScheduler # tryCreateWorker ()
 private fun tryCreateWorker(state: Long = controlState.value): Boolean {  
        val created = createdWorkers(state)// 创建的的线程总数  
        val blocking = blockingTasks(state)// 处理阻塞任务的线程数量  
        val cpuWorkers = (created - blocking).coerceAtLeast(0)//得到非阻塞任务的线程数量  
        if (cpuWorkers < corePoolSize) {// 小于核心线程数量，进行线程的创建  
            val newCpuWorkers = createNewWorker()  
            if (newCpuWorkers == 1 && corePoolSize > 1) createNewWorker()// 当前非阻塞型线程数量为1，同时核心线程数量大于1时，再进行一个线程的创建，  
            if (newCpuWorkers > 0) return true  
        }  
        return false  
    }  
      
 // 创建线程  
 private fun createNewWorker(): Int {  
        synchronized(workers) {  
            ...  
            val created = createdWorkers(state)// 创建的的线程总数  
            val blocking = blockingTasks(state)// 阻塞的线程数量  
            val cpuWorkers = (created - blocking).coerceAtLeast(0) // 得到非阻塞线程数量  
            if (cpuWorkers >= corePoolSize) return 0//超过最大核心线程数，不能进行新线程创建  
           if (created >= maxPoolSize) return 0// 超过最大线程数限制，不能进行新线程创建  
            ...  
            val worker = Worker(newIndex)  
            workers[newIndex] = worker  
            require(newIndex == incrementCreatedWorkers())  
            worker.start()// 线程启动  
            return cpuWorkers + 1  
        }  
    }    
```
新创建线程存在两个限制条件：

- 1.非阻塞线程数小于核心线程数量；
- 2.已创建的线程数量小于最大线程数量；

当创建好一个线程之后，如果满足非阻塞线程数量为1，同时核心数量总数大于1时，再次创建一个新的线程，用来“偷窃”其它线程的任务，这样做的目的是为了提高效率;

在 beforeTask() 的处理中，如果当前任务为 IO任务，且当前线程占有 CPU权限，会对权限进行释放，紧接着会唤醒一个线程，如没有待唤醒的线程，会尝试新建一个线程并启动，IO任务占用的CPU很少，这样做可以让新唤醒或者新建的线程占用cpu的时间片执行其他task;

Worker#runSafely()就是真正运行runable了，代码很简单就是调用了task.run()。
```kotlin
 //Worker#runSafely()
 fun runSafely(task: Task) {  
       ...  
       task.run()  
       ...  
     }
```
执行runSafely()方法之后，task就真正运行起来了，task任务结束后会调用Worker#afterTask()进行线程状态的重置。
```kotlin
 //Worker#afterTask()
 private fun afterTask(taskMode: Int) {  
           if (taskMode == TASK_NON_BLOCKING) return  
           decrementBlockingTasks()  
           val currentState = state  
           // Shutdown sequence of blocking dispatcher  
           if (currentState !== WorkerState.TERMINATED) {  
               assert { currentState == WorkerState.BLOCKING }   
               // 如果当前线程执行的是阻塞任务，任务执行完后，将线程新状态置为初始态  
               state = WorkerState.DORMANT  
           }  
       }
```
上文分析的executeTask()方法执行一个任务，在执行任务前，及任务结束后，都对阻塞型任务做了一些处理，这是因为阻塞的任务开始后不需要或者占用很少cpu的权限，所以当前线程如果占有cpu权限，为了提高资源的利用率，可以释放cpu权限，而且可以通过唤醒或者新建一个线程去占用这个cpu时间片去执行其它的任务，当任务结束后，也将线程的状态重置为初始态;

再看下找不到任务，线程挂起时的逻辑：
```kotlin
  Worker#tryPark()     
     private fun tryPark() {  
             if (!inStack()) {  
                 // 如worker没有在挂起线程的stack中将其push  
                 parkedWorkersStackPush(this)  
                 return  
             }  
             assert { localQueue.size == 0 }  
             workerCtl.value = PARKED // Update value once  
             while (inStack()) {   
                 // 挂起处理  
                 if (isTerminated || state == WorkerState.TERMINATED) break  
                 tryReleaseCpu(WorkerState.PARKING)  
                 interrupted() // Cleanup interruptions  
                 park()  
             }  
         }
```
在tryPark()的处理中首先判断了worker是否在挂起线程stack中，如没有则push,其后做了return处理，这样当线程尝试挂起时，因为return了，又进入一次循环查找任务。如已在stack中则对worker进行挂起处理。

与tryPark()对应的是tryUnpark()，负责worker的唤醒。
```kotlin
 //CoroutineScheduler#tryUnpark()
 private fun tryUnpark(): Boolean {  
       while (true) {  
           // 从挂起线程stack中pop出一个woker  
           val worker = parkedWorkersStackPop() ?: return false  
           if (worker.workerCtl.compareAndSet(PARKED, CLAIMED)) {  
               // 唤醒worker  
               LockSupport.unpark(worker)  
               return true  
           }  
       }  
   }
```  
tryUnpark() 并不是 Worker 中的方法，而是在 CoroutineScheduler 线程池中，tryUnpark() 实现逻辑并不复杂，从 stack 中 pop 出一个挂起线程，并对其进行唤醒。

可以再看下Worker中个对任务处理流程，加深印象：

<img width="800" alt="任务处理流程" src="https://user-images.githubusercontent.com/17560388/155484757-2950eee2-70ee-49bc-9bff-76d8c2e626ff.png">

#### 2.6.5 CoroutineScheduler线程池

分析完Woker线程之后，我们再来看下线程池的实现，在调度器章节的最后分析到，线程最终由 CoroutineScheduler#dispatch() 来分配运行的线程，我们看一下它的实现：
```kotlin
 //CoroutineScheduler
 internal class CoroutineScheduler(  
     // 核心线程数  
     @JvmField val corePoolSize: Int,  
      // 最大线程数  
     @JvmField val maxPoolSize: Int,  
      // 线程保活时间  
     @JvmField val idleWorkerKeepAliveNs: Long = IDLE_WORKER_KEEP_ALIVE_NS,  
      // 线程池名称  
     @JvmField val schedulerName: String = DEFAULT_SCHEDULER_NAME  
 ) : Executor, Closeable {  
     // 非阻塞任务队列  
     @JvmField  
     val globalCpuQueue = GlobalQueue()  
     // IO阻塞型任务队列  
     @JvmField  
     val globalBlockingQueue = GlobalQueue()  
     // block - DispatchedContinuation对象  
     fun dispatch(block: Runnable, taskContext: TaskContext = NonBlockingContext, tailDispatch: Boolean = false) {  
         trackTask()   
         // 创建一个Task对象  
         val task = createTask(block, taskContext)  
         // 当前线程是否为一个Woker线程，如是则返回，否则返回null  
         val currentWorker = currentWorker()  
         // 将task加入到工作线程的队列中  
         val notAdded = currentWorker.submitToLocalQueue(task, tailDispatch)  
         if (notAdded != null) {  
             // 添加本地队列失败，将task添加至全局队列  
             if (!addToGlobalQueue(notAdded)) {  
                 throw RejectedExecutionException("$schedulerName was terminated")  
             }  
         }  
         val skipUnpark = tailDispatch && currentWorker != null  
     // 唤醒或者新建一个线程去执行task  
         if (task.mode == TASK_NON_BLOCKING) {// task为非阻塞任务  
             if (skipUnpark) return  
             signalCpuWork()  
         } else {// task为阻塞任务  
             signalBlockingWork(skipUnpark = skipUnpark)  
         }  
     }  
 }  
    // 将runbale封装成一个task  
   internal fun createTask(block: Runnable, taskContext: TaskContext): Task {  
         //  从上文调用流程中可知，这个Runnable对象是DispatchedContinuation，而在它的继承链中，继承了Task,所有此处返回它自己即block  
         val nanoTime = schedulerTimeSource.nanoTime()  
         if (block is Task) {  
             block.submissionTime = nanoTime  
             block.taskContext = taskContext  
             return block  
                      }  
         return TaskImpl(block, nanoTime, taskContext)  
     }  
```
在 CoroutineScheduler#dispatch() 中，会将 Runbale 对象封装成一个 Task ,如当前线程是一个 Worker，优先将 task 添加至当前线程的任务队列，否则会将任务添加到 Global 队列中，最后进行线程唤起或者创建新线程执行该任务；

至此，对 Kotlin 协程中一些核心类进行了分析，对其作用做个总结如下：

- 协程体类：封装协程体的操作逻辑。
- Dispatchers ：提供4种线程调度器。
- CoroutineDispatcher ：调度器的父类，CoroutineDispatcher#interceptContinuation()将协程体类对象包装成DispatchedContinuation。
- DispatchedContinuation：代理协程体类对象，并持有线程调度器。
- CoroutineScheduler:线程池，提供协程运行的线程。
- Worker：Worker的实现是继承了Thread，本质上还是对java线程的一次封装。

#### 2.6.6  IO调度器

回过头去再看一下Dispatchers.Default调度器的类图，IO调度器是Dispatchers.Default内的一个变量，并且它和Default调度器共享CoroutineScheduler线程池。

上面的大部分篇幅中分析了协程的一些核心类的作用，下面从示例中配置的各项条件开始，进行一个整体流程的分析，这里将会串联起这些类。


launch的实现

再看一下launch函数的实现代码

```kotlin 
 public fun CoroutineScope.launch(
        // 调度器，在示例代码中是指Dispatchers.Default
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        // CoroutineContext创建一个新的Context
        val newContext = newCoroutineContext(context)
        // 启动模式的判断，示例中是默认的启动模式，执行else分支
        val coroutine = if (start.isLazy)
            LazyStandaloneCoroutine(newContext, block) else
            StandaloneCoroutine(newContext, active = true)
        coroutine.start(start, coroutine, block)
        return coroutine
    }
```
#### 01Job

launch函数的返回值是一个Job,通过launch或者async创建的协程都会返回一个Job实例，它的作用是管理协程的生命周期，也作为协程的唯一标志。

Job的状态：

- New: 新建
- Active: 活跃
- Cancelling: 正在取消
- Cancelled: 已取消
- Completing: 完成中
- Completed: 已完成

Job
```kotlin    
  public interface Job : CoroutineContext.Element {  
       // 在CoroutineContext集合中的Key：Job  
     public companion object Key : CoroutineContext.Key<Job>  
        
     // true:协程处于活跃态  
     public val isActive: Boolean  
   
     // true：协程处于完成态  
     public val isCompleted: Boolean  
   
     // true:协程处于取消状态  
     // 注意：通过手动job.cancle或者协程异常会使协程进入Cancelling状态，这时候isCancelled也为true,当所有子协程运行完成的时候才会进入Cancelled已取消的状态  
     public val isCancelled: Boolean  
   
     // true:协程已开始  
     public fun start(): Boolean  
     // 取消协程  
     public fun cancel(cause: CancellationException? = null)  
   
     // 绑定子job  
     public fun attachChild(child: ChildJob): ChildHandle  
     // 等待协程执行完成  
     public suspend fun join()  
   
     // 协程状态为完成时的监听  
     public fun invokeOnCompletion(handler: CompletionHandler): DisposableHandle  
   
     // 协程取消或者完成的监听  
     // onCancelling为true时，协程状态为Cancelling时回调  
     // onCancelling为false时，协程状态为Completed回调  
     // invokeImmediately是否可以回调，onCancelling为true才进行回调  
     // handler 回调  
     public fun invokeOnCompletion(  
         onCancelling: Boolean = false,  
         invokeImmediately: Boolean = true,  
         handler: CompletionHandler): DisposableHandle  
   
     ...  
 }
```
Job 实现了 CoroutineContext.Element，它是 CoroutineContext 集合的元素类型，并且 Key 为Job。Job 内提供了isActive、isCompleted、isCancelled 属性用以判断协程的状态，以及取消协程、等待协程完成、监听协程状态的操作。


#### 02launch函数体


接下来看下launch函数体的实现：

newCoroutineContext()是CoroutineScope的一个扩展方法，它的作用就是将传参context与CoroutineScope中的CoroutineContext集合合并，并返回一个新的CoroutineContext，在示例中，就是将Dispatchers.Default与CoroutineScope中的CoroutineContext合并；

CoroutineScope. newCoroutineContext()
```kotlin    
 // CoroutineScope.newCoroutineContext 说明newCoroutineContext是一个扩展函数  
 public actual fun CoroutineScope.newCoroutineContext(context: CoroutineContext): CoroutineContext {  
     // 符号“+”对应CoroutineContext的plus方法  
     val combined = coroutineContext + context  
          // 看下else非debug的情况，得到合并后的combined复制给变量debug  
     val debug = if (DEBUG) combined + CoroutineId(COROUTINE_ID.incrementAndGet()) else combined  
     // 实例中调度器使用的Dispatchers.Default，所以这里执行else分支，直接返回coroutineContext + context相加后的结果  
     return if (combined !== Dispatchers.Default && combined[ContinuationInterceptor] == null)  
         debug + Dispatchers.Default else debug  
 }
```    
经过newCoroutineContext方法的调用，得到了一个存储调度器的CoroutineContext集合。

示例中的没有设置启动模式，则启动为默认的模式，构建一个 StandaloneCoroutine ，并调用它的 start 方法。StandaloneCoroutine 又作为 launch 函数的返回值返回，所以它还是一个job对象。

StandaloneCoroutine具体来说是一个协程对象，实现比较简单，继承AbstractCoroutine，并重写了handleJobException()异常处理方法，所有的协程对象都继承AbstractCoroutine。继续看它的父类AbstractCoroutine，类图如下:

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/155485769-e3cf3fc8-34a4-4c51-ada4-0abeb6dae4c4.png">

AbstractCoroutine 继承或者实现了 JobSupport、Job、Continuation、CoroutineScope。

JobSupport是Job的具体实现，AbstractCoroutine可以作为一个Job控制协程的生命周期，同时实现Continuation接口，也可以作为一个Continuation，重写的resmueWith()方法的一个重要作用是外部协程挂起的恢复处理。这里出现了第三个Continuation对象AbstractCoroutine。
```kotlin
 //AbstractCoroutine#resmueWith
 public final override fun resumeWith(result: Result<T>) {  
         val state = makeCompletingOnce(result.toState())  
         // 子协程未完成，父协程需要等待子协程完成之后才可以完成  
         if (state === COMPLETING_WAITING_CHILDREN) return  
         // 子协程全部执行完成或者没有子协程的情况下不需要等待  
         afterResume(state)  
     }  
       
  protected open fun afterResume(state: Any?): Unit = afterCompletion(state)  
    
  // JobSupport#afterCompletion  
  protected open fun afterCompletion(state: Any?) {}  
```
在 AbstractCoroutine#resmueWith 中首先根据 JobSupport#makeCompletingOnce 返回状态判断，协程是否处于等待子协程完成的状态：

- state == COMPLETING_WAITING_CHILDREN 等待子协程完成，自身才可完成。子协程完成后触发afterCompletion()
- state != COMPLETING_WAITING_CHILDREN 没有子协程或者所有子协程已经完成，自身可以完成，直接触发afterCompletion()
    
协程对象可以通过重写 afterCompletion() 处理协程完成之后的操作，下文中的协程恢复章节中，withContext() 中 DispatchedCoroutine 协程对象，通过afterCompletion()恢复了外层的协程的运行。


继续launch函数体的分析：
```kotlin
coroutine.start(start, coroutine, block)  
```
这里有一个启动模式的判断，不管哪一个模式，coroutine.start都是调用AbstractCoroutine.start()
```kotlin
 //AbstractCoroutine#start()
 public fun <R> start(start: CoroutineStart, receiver: R, block: suspend R.() -> T) {  
       ...  
       //  block :协程体  
       //  receiver:协程对象  
       //  this：AbstractCoroutine  
       start(block, receiver, this)  
   } 
```
AbstractCoroutine.start 方法的实现用又调用了 start() 方法，这里并不是递归调用，这个 start 指的是 CoroutineStart 的一个变量，CoroutineStart 是一个枚举类型，内部重写了invoke()方法，这里又涉及到操作符的重载，start() 实际是触发 CoroutineStart.invoke() ,所以通过 start 的调用最终执行 invoke() 方法。

继续看CoroutineStart的invoke()，因为是start()的参数为3个，跟进3参的invoke()

注意三个参数中的AbstractCoroutine最后赋值给了谁！！！
```kotlin
 CoroutineStart#invoke
 public operator fun <R, T> invoke(block: suspend R.() -> T, receiver: R, completion: Continuation<T>): Unit =  
    when (this) {  
        // completion：start传过来的AbstractCoroutine  
        DEFAULT -> block.startCoroutineCancellable(receiver, completion)  
        ATOMIC -> block.startCoroutine(receiver, completion)  
        UNDISPATCHED -> block.startCoroutineUndispatched(receiver, completion)  
        LAZY -> Unit // will start lazily  
    }
```
这里有几条分支，没有设置启动模式则为默认，执行DEFAULT分支
```kotlin
block.startCoroutineCancellable(receiver, completion)。
```
还记得这个block是谁吗？就是协程体类。继续跟进 block.startCoroutineCancellable()
```kotlin
// 返回一个Continuation  
 // 参数completion是AbstractCoroutine  
 internal fun <R, T> (suspend (R) -> T).startCoroutineCancellable(receiver: R, completion: Continuation<T>) =  
     runSafely(completion) {  
         createCoroutineUnintercepted(receiver, completion).intercepted().resumeCancellableWith(Result.success(Unit))  
     } 
```
startCoroutineCancellable的返回值是一个Continuation对象，它的实现是一个链式调用，一步一步看，先看createCoroutineUnintercepted(receiver, completion)。

```kotlin
 //createCoroutineUnintercepted() 
 public actual fun <T> (suspend () -> T).createCoroutineUnintercepted(  
     completion: Continuation<T>  
 ): Continuation<Unit> {  
     // probeCompletion ：AbstractCoroutine  
     val probeCompletion = probeCoroutineCreated(completion)  
     return if (this is BaseContinuationImpl)  
         create(probeCompletion)  
     else  
         createCoroutineFromSuspendFunction(probeCompletion) {  
             (this as Function1<Continuation<T>, Any?>).invoke(it)  
         }  
 }
```
createCoroutineUnintercepted()是一个扩展函数，通过协程体调用，所以源码中this is BaseContinuationImpl的判断中this指协程体类，编译章节中协程体被编译成SuspendLambda的子类，再看一下SuspendLambda的继承链：SuspendLambda->ContinuationImpl->BaseContinuationImpl->Continuation

SuspendLambda是BaseContinuationImpl的一个子类，所以这里的判断if (this is BaseContinuationImpl)为true,执行create()方法。这个create()又是谁的?

在一个类的扩展方法中我们是可以访问类中属性及方法的，所以调用的是协程体类的create()。再看一眼协程体类中的create()实现：

```kotlin
// continuation：AbstractCoroutine  
     public final Continuation<Unit> create(Object obj, Continuation<?> continuation) {  
         ...  
         MainActivity$startCoroutine$funTest$1 mainActivity$startCoroutine$funTest$1 = new MainActivity$startCoroutine$funTest$1(this.this$0, continuation);  
         return mainActivity$startCoroutine$funTest$1;  
     }
```
create()方法创建了一个协程体类的实例，到这里真正拿到了一个协程体类的实例。

注意看下构造函数的参数continuation，continuation就是AbstractCoroutine，在协程体类的继承链中，这个continuation一直传递到了BaseContinuationImpl父类中，后续分析挂起恢复时，会看到它的使用。继续分析intercepted()
```kotlin
public actual fun <T> Continuation<T>.intercepted(): Continuation<T> =  
     (this as? ContinuationImpl)?.intercepted() ?: this  
```
代码很简单，首先将 this 强转成了 ContinuationImpl 类型，this 是协程体类的实例，继承 ContinuationImpl，可以进行强转，接着看 ContinuationImpl.intercepted()。

```kotlin
public fun intercepted(): Continuation<Any?> =  
        intercepted  
            ?: (context[ContinuationInterceptor]?.interceptContinuation(this) ?: this)  
                .also { intercepted = it }
```
context[ContinuationInterceptor] 从集合中取到调度器，并调用调度器的interceptContinuation()，而调度器的方法interceptContinuation()的作用是将协程体Continuation包装成一个DispatchedContinuation，之后的源码中会调用DispatchedContinuation的resumeCancellableWith()，而在resumeCancellableWith()中将DispatchedContinuation分发给调度器进行了线程的调度，之后协程就在执行的线程启动了。


#### 03启动流程小结


以示例代码为前置条件，调度器为Dispatchers.Default,启动模式为CoroutineStart.DEFAULT:

- 1. CoroutineScope#launch()创建一个协程，在其内部实现中根据启动模式为CoroutineStart.DEFAULT，创建一个StandaloneCoroutine协程对象，并触发StandaloneCoroutine#start(start, coroutine, block);

- 2. StandaloneCoroutine的父类是AbstractCoroutine，StandaloneCoroutine#start()的实现在其父类中，即AbstractCoroutine#start();

- 3. 在AbstractCoroutine#start()中，触发CoroutineStart#invoke();

- 4. CoroutineStart#invoke()的处理逻辑中，根据调度器为Dispatchers.Default，调用协程体的startCoroutineCancellable()方法;

- 5. startCoroutineCancellable()的内部处理是一个链式调用：
```kotlin
createCoroutineUnintercepted(..).intercepted().resumeCancellableWith(Result.success(Unit))
```
createCoroutineUnintercepted()创建一个协程体类对象;
    
intercepted()使用拦截器（调度器）将协程体类对象包装成DispatchedContinuation（DispatchedContinuation代理了协程体类Continuation对象,并持有调度器）;

调用DispatchedContinuation#resumeCancellableWith()。
    
- 6. 在DispatchedContinuation#resumeCancellableWith()中，使用线程调度器触发dispatcher#dispatch(context, this)进行调度，该调度器为Dispatchers.Default;

- 7. Dispatchers.Default#dispatch()调度处理中，将DispatchedContinuation分发到CoroutineScheduler线程池中，由CoroutineScheduler分配一个线程Worker,最终在Woreder的run()方法中触发了DispatchedContinuation的run(),其内部实现是使协程体Continuation对象的resumeWithI()得以执行，前文中分析到协程体的执行其实就是resumeWith()方法被调用,这样协程体就可以在执行的线程中执行了;


下面给出时序图会更清晰一些：

<img width="800" alt="类图" src="https://user-images.githubusercontent.com/17560388/155488663-90262adf-b5ae-4bca-961c-aa2866c0eebb.png">

在上文的分析中出现了三个Continuation类型的对象：

- 1. SuspendLambda 协程体类对象，封装协程体的操作；
- 2. DispatchedContinuation 持有线程调度器，代理协程体类对象；
- 3. AbstractCoroutine 恢复外部协程挂起；

理解这三个Continuation对象的作用及实现，基本可以理解协程原理。

现在大概知道了协程启动一个的流程，除此之外Kotlin协程还有两个核心概念，挂起及恢复，接下来看一下协程是如何实现挂起的。


## 挂起

挂起有一个特点就是，挂起而不阻塞线程，这里要清楚一点，挂起的本质是切线程，并且在相应的逻辑处理完成之后，再重新切回线程。挂起使协程体的操作被return而停止，等待恢复，它阻塞的是协程体的操作，并未阻塞线程。


### 01 BaseContinuationImpl


再瞅一眼BaseContinuationImpl的源码实现,BaseContinuationImpl负责协程体逻辑的处理:
```kotlin
internal abstract class BaseContinuationImpl(  
     // completion：实参是一个AbstractCoroutine  
     public val completion: Continuation<Any?>?  
 ) : Continuation<Any?>, CoroutineStackFrame, Serializable {  
     public final override fun resumeWith(result: Result<Any?>) {  
         var current = this  
         var param = result  
         while (true) {  
             probeCoroutineResumed(current)  
             with(current) {  
                 val completion = completion!!   
                 val outcome: Result<Any?> =  
                     try {  
                         // 调用invokeSuspend方法，协程体真正开始执行  
                         val outcome = invokeSuspend(param)  
                         // invokeSuspend方法返回值为COROUTINE_SUSPENDED，resumeWith方法被return，结束执行，说明执行了挂起操作  
                         if (outcome === COROUTINE_SUSPENDED) return  
                         // 协程体执行成功的结果  
                         Result.success(outcome)  
                     } catch (exception: Throwable) {  
                         // 协程体出现异常的结果  
                         Result.failure(exception)  
                     }  
                 releaseIntercepted() // this state machine instance is terminating  
                  
                 if (completion is BaseContinuationImpl) {               
                     current = completion  
                     param = outcome  
                 } else {  
                     // 在示例代码中，completion是一个AbstractCoroutine，是指launch函数创建的StandaloneCoroutine  
                     completion.resumeWith(outcome)  
                     return  
                 }  
             }  
         }  
     }  
     ...  
 }  
```
invokeSuspend()的执行就是协程体的执行，当invokeSuspend()返回值为COROUTINE_SUSPENDED时，会执行return操作，resumeWith()的执行被结束掉，协程体的操作也被结束掉了，而COROUTINE_SUSPENDED代表协程发生挂起。

再看一下invokeSuspend()的实现，什么时候会返回COROUTINE_SUSPENDED：
```kotlin
 // 协程体操作被转成invokeSuspend方法的调用  
     public final Object invokeSuspend(Object $result) {  
         int i = this.label;//默认为0  
         if (i == 0) {  
             ResultKt.throwOnFailure($result);  
             System.out.println("funTest");    
             ...  
             this.label = 1;  
             // 挂起点：挂起函数suspendFun1的调用  
             if (mainActivity.suspendFun1(this) == coroutine_suspended) {  
                 return coroutine_suspended;  
             }  
         } else if (i == 1) {  
             // 异常处理  
             ResultKt.throwOnFailure($result);  
         } else if (i == 2) {  
             // 异常处理  
             ResultKt.throwOnFailure($result);  
             return Unit.INSTANCE;  
         } else {  
             throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");  
         }  
         this.label = 2;  
         // 挂起点：挂起函数suspendFun2的调用  
         if (mainActivity2.suspendFun2(this) == coroutine_suspended) {  
             return coroutine_suspended;  
         }  
         ...  
         return Unit.INSTANCE;  
     }
```
结合源码看一下，默认情况下label==0，i==0,执行label = 1赋值操作，及调用挂起函数suspendFun1，此处判断suspendFun1方法返回值为coroutine_suspended时，就会返回 coroutine_suspended，也就是当suspendFun1()内存在挂起操作的时候它的返回值就是coroutine_suspended。

假设suspendFun1挂起函数内执行了挂起操作，suspendFun1()方法结束并返回coroutine_suspended，resumeWith()方法在收到返回值coroutine_suspended也进行了return操作，resumeWith()和invokeSuspend()方法执行都结束了，但是suspendFun2()方法还没有调用，这里有没有发生阻塞？并没有，协程挂起并不是阻塞了当前的线程，而是执行了return操作，结束了协程体的调用。

现在知道了一个结论，挂起函数内执行挂起操作的时候会返回coroutine_suspended标志，结束协程体的运行，使协程挂起，接下来看下协程提供的挂起函数中是如何操作的。


### 02withContext()挂起函数


withContext()是kotlin协程提供的挂起函数。

```kotlin
public suspend fun <T> withContext(  
     context: CoroutineContext,  
     block: suspend CoroutineScope.() -> T  
 ): T {  
     contract {  
         callsInPlace(block, InvocationKind.EXACTLY_ONCE)  
     }  
     // 返回启动withContext的协程体  
     return suspendCoroutineUninterceptedOrReturn sc@ { uCont ->  
         // 构建一个新的newContext，合并当前协程体以及withContext协程体的CoroutineContext  
         val oldContext = uCont.context  
         val newContext = oldContext + context  
         // 检查协程是否活跃，如果线程处于非活跃的状态，抛出cancle异常  
         newContext.checkCompletion()  
         ...  
         // DispatchedCoroutine也是一个AbstractCoroutine对象，负责协程完成的回调，  
         // 注意这里的Continuation的传参为uCont，及发起withContext的协程对象  
         val coroutine = DispatchedCoroutine(newContext, uCont)  
         coroutine.initParentJob()  
                  // 和协程启动的流程一样，启动withContext的协程  
         // 注意这里的传参coroutine为DispatchedCoroutine，它持有需要恢复的协程  
         block.startCoroutineCancellable(coroutine, coroutine)  
         // 返回结果为挂起还是完成  
         coroutine.getResult()  
     }  
 }
```
在withContext()的源码可以看到，withContext()的协程体的启动和原有协程的启动流程是一样的，DispatchedCoroutin是AbstractCoroutine的一个子类，并且在创建DispatchedCoroutin时的传参是外层协程体对象，这是因为当withContext()的协程体完成的时候需要通过外层协程体对象恢复当前协程的运行，这个一会分析，先看下协程的挂起coroutine.getResult()的实现。
```kotlin
 // DispatchedCoroutine#getResult  
  fun getResult(): Any? {  
        // 返回COROUTINE_SUSPENDED，挂起  
        if (trySuspend()) return COROUTINE_SUSPENDED  
        val state = this.state.unboxState()  
        // 出现异常  
        if (state is CompletedExceptionally) throw state.cause  
        @Suppress("UNCHECKED_CAST")  
        // 未出现异常结果返回  
        return state as T  
    }  
    
  // DispatchedCoroutine#trySuspend  
  private val _decision = atomic(UNDECIDED)  
  private fun trySuspend(): Boolean {  
        _decision.loop { decision ->  
            when (decision) {  
                // compareAndSet原子操作，当前值与预期值一致时返回true,以原子方式更新自身的值  
                UNDECIDED -> if (this._decision.compareAndSet(UNDECIDED, SUSPENDED)) return true  
                RESUMED -> return false  
                else -> error("Already suspended")  
            }  
        }  
    }  
```
是否挂起，结束协程运行，关键在是否返回COROUTINE_SUSPENDED标志，在getResult()方法中的处理逻辑，就是看trySuspend()是否返回true。

trySuspend()方法中，_decision默认为UNDECIDED，预期的参数值传参也为UNDECIDED，所以，trySuspend返回true，最终getResult方法返回了COROUTINE_SUSPENDED，协程被挂起了。

可以看一下流程图:

<img width="800" alt="类图" src="https://user-images.githubusercontent.com/17560388/155489517-dc77bc24-3c80-4b4a-826b-0c0843a09e7b.png">

与挂起对应的就是恢复了，接下来分析，协程挂起后是如何恢复的。


### 挂起恢复

以在挂起章节中，withContext()为例，withContex()的协程的启动调用了startCoroutineCancellable()方法。
```kotlin
block.startCoroutineCancellable(coroutine, coroutine)  
```
startCoroutineCancellable方法的第二个参数为协程完成的回调，在withContext中的传参为coroutine，它是DispatchedCoroutine，而DispatchedCoroutine里面持有待恢复的协程，看一下它的类图：

<img width="800" alt="类图" src="https://user-images.githubusercontent.com/17560388/155489428-b83991d4-babe-418a-ae01-1eb1db87f710.png">


从类图中可以看出DispatchedCoroutine是AbstractCoroutine的一个子类，再回顾一下AbstractCoroutine的作用，AbstractCoroutine是协程完成时的回调，当协程完成时会调用它的内部方法resumeWith()，内部的处理逻辑最后会触发JubSpuuort#afterCompletion()，而在DispatchedCoroutine中重写了afterCompletion()，看下DispatchedCoroutine的实现。
```kotlin
 private class DispatchedCoroutine<in T>(  
     context: CoroutineContext,  
     // 外部需要恢复的协程  
     uCont: Continuation<T>  
 ) : ScopeCoroutine<T>(context, uCont) {  
   
     private val _decision = atomic(UNDECIDED)  
   
     // 尝试挂起协程  
     private fun trySuspend(): Boolean {  
         _decision.loop { decision ->  
             when (decision) {  
                 UNDECIDED -> if (this._decision.compareAndSet(UNDECIDED, SUSPENDED)) return true  
                 RESUMED -> return false  
                 else -> error("Already suspended")  
             }  
         }  
     }  
     private fun tryResume(): Boolean {  
         _decision.loop { decision ->  
             when (decision) {  
                 // 未发生挂起  
                 UNDECIDED -> if (this._decision.compareAndSet(UNDECIDED, RESUMED)) return true  
                 // 发生挂起  
                 SUSPENDED -> return false  
                 else -> error("Already resumed")  
             }  
         }  
     }  
   
     override fun afterCompletion(state: Any?) {  
         afterResume(state)  
     }  
   
     override fun afterResume(state: Any?) {  
         // 在getResult()之前，协程已运行结束，未发生挂起，不需要恢复外层协程  
         if (tryResume()) return   
         // 恢复外层协程  
         uCont.intercepted().resumeCancellableWith(recoverResult(state, uCont))  
     }  
   
     // 获取协程运行结果，挂起章节中有介绍过  
     fun getResult(): Any? {  
         if (trySuspend()) return COROUTINE_SUSPENDED  
         val state = this.state.unboxState()  
         if (state is CompletedExceptionally) throw state.cause  
         return state as T  
     }  
 }  
```
在DispatchedCoroutine中，重写了afterCompletion()及afterResume()，并且afterCompletion()调用afterResume()，而afterResume()中首先判断了协程是否被挂起，如已挂起则恢复外部的协程。恢复外部协程时，同样是通过线程调度，将协程在指定的线程运行，这样也就可以在挂起恢复时，重新切回线程,再次触发invokeSuspend()，根据label状态值，执行下一个代码片。

附上流程图：

<img width="800" alt="类图" src="https://user-images.githubusercontent.com/17560388/155489307-c307b9bd-47ed-4f03-ae65-e2951a393b66.png">

## 性能

<img width="800" alt="类图" src="https://user-images.githubusercontent.com/17560388/155489240-c200ad9e-8e9b-4371-a43c-98c34fb5f544.png">


官方文档中提供了一个示例用于Kotlin协程与Java Thread的性能对比，如上图，文档注释的大概意思是说启动10W个协程，一秒后输出print(）。如果使用Thread会发生什么？大多数情况下你的代码将发生OOM。

在文章中其实介绍过Kotlin协程是基于Java Thread的，但更准确的说法，Dispatchers.Default、Dispatchers.IO线程调度器，它们是基于Thread线程池的实现，以一个Thread线程池与一个Thread进行性能对比，稍微有些瑕疵。笔者认为Kotlin协程对于开发者的帮助其实就是方便。


## 总结

以上，从源码角度对Kotlin协程的启动、挂起、恢复的过程进行了分析，Kotlin协程原理并不局限于文章中分析的这些，更多细节比如父子协程的绑定，协程的异常处理，Job的核心实现，多层挂起函数嵌套挂起等等，读者可以把文章做为Kotlin协程原理的敲门砖，再对其感兴趣的方向进行研读。

至此，Kotlin协程的源码分析就结束了，文章中大家有什么疑问，或者文中存在不准确的地方，欢迎大家一起交流。


    
### 原文链接
    
[硬核万字解读——Kotlin协程原理解析](https://mp.weixin.qq.com/s/N9BiufCWTRuoh6J-QERlWQ)
    
### 参考：

[Kotlin协程源码分析-2 调用挂起函数](https://fanmingyi.blog.csdn.net/article/details/105027646)
    
[Kotlin Coroutine 源码解析（1） —— 协程是如何运行的](https://blog.csdn.net/xx326664162/article/details/113106875)

[Kotlin/kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines)
