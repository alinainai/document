Kotlin 的泛型和 Java 相当接近:它们使用同样的方式声明泛型函数和泛型类。 和 Java一样，泛型类型的类型实参只在编译期存在。

不能把带类型实参的类型和 is 运算符一起使用，因为类型实参在运行时将被擦除。

#### 1. 上界 
```
<T : Number> //等同于 java 中的 <T extends Number>
```
#### 2. 如何确保 T 非空
```
List<T> `T` 默认父类为 `Any?`，如果想让 `T` 默认非空: List<T : Any>
```
#### 3.reified 实化泛型
reified 实化只对内联函数有效，reified 声明了类型参数不会在运行时被擦除
```kotlin
inline fun <reified T> Iterable<*>.filterIsInstance(): List<T> {
    val destination = mutableListOf<T>()
    for (element in this) {
        if (element is T) {
            destination.add(element)
        }
    }
    return destination
}
```
内联函数的类型参数可以标记成实化的，允许你在运行时对它们使用 `is` 检查，以及获得 `java.lang.Class` 实例。

#### 4. 协变:保留子类型化关系

如果 A 是 B 的子类型，那么 `Producer<A>` 就是 `Producer<B>` 的子类型
  
在 Kotiin 中，要声明类在某个类型参数上是可以协变的，在该`类型参数的名称`前加上 out 关键字即可:
```kotlin
//类被声明成在 T 上协变
interface Producer<out T>{
    fun produce() : T  
}
```

Kotlin 中的 只读接口 List 声明成了协变的 ，这意味着 List<String> 是 List<Any> 的子类型。


类型参数 `T` 上的关键宇 `out` 有两层含义
  
- 子类型化会被保留( Producer<Cat> 是 Producer<Anirnal> 的子类型)
- T只能用在out位置

`in` 位置和 `out` 位置

- 如果函数是把 `T` 当成返回类型，我们说它在 `out` 位置。该函数生产类型为 `T` 的值 。
- 如果 `T` 用 作函数参数的类型，它就在 `in` 位置。该函数消费类型为 `T` 的值。
  
<img width="214" alt="in位置和out位置" src="https://user-images.githubusercontent.com/17560388/154209747-bc62c7da-1041-4530-90a6-3b7c76053f19.png">

位置规则只覆盖了类外部可见的(public、 protected 和 internal) API。私有方法的参数既不在 `in` 位置也不在 `out` 位置。
   
#### 5. 逆变:反转子类型化关系
  
如果 B 是 A 的子类型，那么 Consumer<A> 就是 Consumer<B> 的 子类型。类型参数 A 和 B 交换了位置 ，所以我们说子类型化被反转了 。
  
<img width="531" alt="协变的、逆变的和不变型的类" src="https://user-images.githubusercontent.com/17560388/154214291-749c323f-0851-410e-8c65-f8d45f0bb99c.png">

#### 6. 逆变:反转子类型化关系
  
Kotlin的使用点变型直接对应Java的限界通配符。可以声明一个类在某个类型参数上是逆变的，如果该参数只是用在 in 位置。
  
- Kotiin中的 MutableList<out T> 和 Java 中的 MutableList<? extends T> 是一个意思。 
- in投影的 MutableList<in T>对应到 Java的 MutableList<? super T>。

函数接口声明成了在第一个类型参数上逆变而在第二个类型参数上协变，使(Animal)->Int 成为 (Cat)->Number 的子类型 。

#### 7. 星号投影

```kotlin
 List<*> //等同于 Java 的 List<?> 
```
当确切的类型实参是未知的或者不重要的时候 ，可以使用 `星号投影语法`。


  
  

