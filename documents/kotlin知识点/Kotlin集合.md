### 介绍

所有和集合相关的操作都在 kotlin.collections 包中。

--List 有序可重复
--Set 无序不可重复
--Map 键值对

和Java一样。

在Java的基础上又新增 只读 和 可写 两种类型。

### 可写 List

可以用 val 修饰，因为添加值不会影响 numbers 的应用，val 修饰不可重新赋值。

```Kotlin
val numbers = mutableListOf("one", "two", "three", "four")
numbers.add("five") 
```

### 只读 List

List 默认实现是ArrayList。

只读集合类型是型变的。 这意味着，如果类 Rectangle 继承自 Shape，则可以在需要 List <Shape> 的任何地方使用 List <Rectangle>。 
  
换句话说，集合类型与元素类型具有相同的子类型关系。 
  
map 在值（value）类型上是型变的，但在键（key）类型上不是。






