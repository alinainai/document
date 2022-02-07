### 1. Volatile 关键字

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
### 4. 当子类需要实现双接口默认方法时，需要在类中重写方法否则编译不过

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
### 5. Sealed 类的作用

先看一段代码不适用 Sealed 类的代码

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
使用 Sealed 优化

```kotlin
/**
 * 1.在 when 表达式中处理所有 sealed 类的子类 ，不再需要提供默认分支。
 * 2. sealed 默认隐含 open 修饰符。
 * 3.在 when 中使用 sealed 类并且添加一个新的子类的时候，有返回值的 when 表达式会导致编译失败，它会告诉你哪里的代码必须要修改。
 */
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

内联最适用于参数为函数类型的函数
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

不加 inline 反编译，会导致多生成一个内部类，这个是 lambda 函数多出来的类

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

假设一个 inline 函数上面有多个 lambda 参数，某个 lambda 参数不需要内联可以用 noinline 修饰

```kotlin
private inline fun showToast(function1: () -> Unit, noinline function2: () -> Unit, message: String) {
    function1.invoke()
    function2.invoke()
    ToastUtils.show(message)
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
