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


