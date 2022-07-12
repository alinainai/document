## 1. 为什么需要泛型

泛型是在 Java 5 版本开始引入，有以下优点

- 类型检查，在编译阶段就能发现错误
- 更加语义化，看到 'List\<String\>' 我们就知道存储的数据类型是 String
- 自动类型转换，在取值时无需进行手动类型转换
- 能够将逻辑抽象出来，使得代码更加具有通用性

## 2. 类型擦除
  
泛型是在 Java 5 版本开始引入的，所以在 Java 4 中 ArrayList 还不属于泛型类，其内部通过 Object 向上转型和外部强制类型转换来实现数据存储和逻辑复用，此时开发者的项目中已经充斥了大量以下类型的代码：

```java
List stringList = new ArrayList();
stringList.add("业志陈");
stringList.add("https://juejin.cn/user/923245496518439");
String str = (String) stringList.get(0);
```

而在推出泛型的同时，Java 官方也必须保证二进制的向后兼容性，用 Java 4 编译出的 Class 文件也必须能够在 Java 5 上正常运行，即 Java 5 必须保证以下两种类型的代码能够在 Java 5 上共存且正常运行

```java
List stringList = new ArrayList();
List<String> stringList = new ArrayList();
```
  
为了实现这一目的，Java 就通过类型擦除这种比较别扭的方式来实现泛型
  
- 如果泛型没有设置上界约束，那么将泛型转化成 Object 类型
- 如果泛型设置了上界约束，那么将泛型转化成该上界约束

所以 Java 的泛型可以看做是一种特殊的语法糖，因此也被人称为伪泛型
  
## 3. 类型擦除的后遗症
  
Java 泛型对于类型的约束只在编译期存在，运行时仍然会按照 Java 5 之前的机制来运行，泛型的具体类型在运行时已经被删除了，所以 JVM 是识别不到我们在代码中指定的具体的泛型类型的

例如，虽然 List<String> 只能用于添加字符串，但我们只能泛化地识别到它属于 List<?> 类型，而无法具体判断出该 List 内部包含的具体类型。

```java
List<String> stringList = new ArrayList<>();
stringList instanceof ArrayList<?> //正常
stringList instanceof ArrayList<String> //报错
```
  
