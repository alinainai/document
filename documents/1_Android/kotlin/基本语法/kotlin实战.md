## 第二章、基础语法

1、增强的类型推断
```kotlin
val string = "kotlin"
val int = 1214
val long = 1314L
val float = 13.14f
val double = 13.34
val double2 = 10.1e6
```
2、函数的默认返回值
```kotlin
// 默认的隐式返回类型为 Unit，它类似于 Java 中的 void，但是 Unit 是一个单例类，void 是一个关键字
fun set(x:Int){
}
```
3、声明变量的规则
```kotlin
val a = 6 // val的含义：引用不可变，可以当成 value(值) 看待，如同 Java 的 final 修饰的变量，推荐使用 val 修饰变量
var b = 5 // varible（变量）
```
4、函数也是一种类型
```kotlin
(Int,String) -> Unit
((Int,String) -> Unit)?
```

volatile 关键字
```kotlin
@Volatile
var instance: ThreadPoolManager
```

### 2. Synchronized 关键字

```kotlin
@Synchronized
fun getInstance(): ThreadPoolManager? {}

fun getInstance(): ThreadPoolManager? {
    synchronized(ThreadPoolManager::class.java, {
    })
}
```

### 3. 枚举

Java 的写法

```java
enum Sex {
    MAN(1), WOMAN(2);
    Sex(int type) {}
}
```

Kotlin 的写法

```kotlin
enum class Sex (var type: Int) {
    MAN(1), WOMAN(2)
}
```
### 4. 双接口问题

当子类需要实现双接口默认方法时，需要在类中重写方法否则编译不过

```kotlin
interface Focus {
    fun showOff() = print("i am focus showOff")
}

interface Click {
    fun showOff() = print("i am click showOff")
}

class Button: Click, Focus {
    override fun showOff() {
        super<Click>.showOff()
    }
}
```
### 5. sealed 关键字的作用

处理 `when` 关键字时，如果父类不加 `sealed` 修饰，需要加一个 `else->` 去处理额外分支的代码（虽然事实上没有额外分支，但是编译器并不知道）

```kotlin
interface Expr
class Num(val value:Int):Expr
class Sum(val left:Expr,val right:Expr):Expr

//必须带else分支
fun eval(e:Expr):Int{
    return  when(e){
        is Num ->e.value
        is Sum -> eval(e.left) + eval(e.right)
        else ->throw IllegalArgumentException("Error Type")
    }
}
```
使用 `sealed` 优化后，可以放心的去掉 `else->` 分支的代码。

`sealed`的特点:
- 1.在 `when` 表达式中处理所有 `sealed` 类的子类 ，不再需要提供默认分支。
- 2.`sealed` 默认隐含 `open` 修饰符。
- 3.在 `when` 中使用 `sealed` 类并且添加一个新的子类的时候，有返回值的 `when` 表达式会导致编译失败，它会告诉你哪里的代码必须要修改。

```kotlin
sealed class Expr{
    class Num(val value:Int):Expr()
    class Sum(val left:Num,val right:Num):Expr()
}
fun eval(e:Expr):Int{
  return  when(e){
        is Expr.Num ->e.value
        is Expr.Sum -> eval(e.left) + eval(e.right)
    }
}
```
### 6. 内联函数

内联最适用于`参数为函数类型`的函数

```kotlin
class Demo {
    fun test() {
        showToast({
            println("测试输出了")
        }, "toast")
    }

    private inline fun showToast(function: () -> Unit, message: String) {
        function.invoke()
        ToastUtils.show(message)
    }
}
```
编译后
```kotlin
/* compiled from: Demo.kt */
public final class Demo {
    public final void test() {
        System.out.println("\u6d4b\u8bd5\u8f93\u51fa\u4e86");
        ToastUtils.show("toast");
    }
}
```

不加 `inline`关键字，反编译后会导致多生成一个内部类，这个是 `lambda` 函数多出来的类

```kotlin
/* compiled from: Demo.kt */
public final class Demo {

    public final void test() {
        showToast(1.INSTANCE, "7777777");
    }

    private final void showToast(Function0<Unit> function, String message) {
        function.invoke();
        ToastUtils.show(message);
    }
}
```
```kotlin
/* compiled from: Demo.kt */
final class Demo$test$1 extends Lambda implements Function0<Unit> {
    public static final Demo$test$1 INSTANCE = new Demo$test$1();

    Demo$test$1() {
        super(0);
    }

    public final void invoke() {
        System.out.println("\u6d4b\u8bd5\u8f93\u51fa\u4e86");
    }
}
```

假设一个 `inline` 函数上面有多个 `lambda` 参数，某个 `lambda` 参数不需要内联可以用 `noinline` 修饰

```kotlin
private inline fun showToast(function1: () -> Unit, noinline function2: () -> Unit, message: String) {
    function1.invoke()
    function2.invoke()
    ToastUtils.show(message)
}
```
`crossinline` 内联加强

如果内联函数内部还使用 `lambda` 表达式，需要在 内联函数的参数加上 `crossinline`，否则编译不通过。但是传入 `foo` 的 `lambda` 表达式不能添加 `return` 语句。

```kotlin
inline fun foo(crossinline f: () -> Unit) {
    bar { f() }
}

fun bar(f: () -> Unit) {
    f()
}
```

### 7. 类委托语法
```kotlin
// 实现一个默认的日志策略类
class LogStrategyImpl : ILogStrategy {
    override fun log(message: String) {
        Log.i("测试输出", message)
    }
}

interface ILogStrategy {
    fun log(message: String)
}

// 创建一个日志代理类
class LogStrategyProxy(strategy: ILogStrategy) : ILogStrategy by strategy

// 使用
val logStrategyImpl = LogStrategyImpl()
LogStrategyProxy(logStrategyImpl).log("666666")
```
