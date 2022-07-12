## Java 8

### 1.  Lambda表达式 和 函数式接口

`函数式接口` 就是有且仅有一个抽象方法，但是可以有多个非抽象方法的接口。可以隐式转化为 Lambda 表达式。

```java
  //使用java匿名内部类
  TreeSet<Integer> set = new TreeSet<>(new Comparator<Integer>() {
      @Override
      public int compare(Integer o1, Integer o2) {
          return Integer.compare(o1,o2);
      }
  });

  //使用JDK8 lambda表达式
  TreeSet<Integer> set2 = new TreeSet<>((x,y) -> Integer.compare(x,y)); 
```


### 2. 方法引用

通过方法引用，可以使用方法的名字来指向一个方法。使用一对冒号 `::` 来引用方法。

- 静态方法引用:Class::staticMethod
- 对象的实例方法引用: instance::methodd

### 3. 接口默认方法和静态方法

default 关键字

### 4. Optional 类

新增了 Optional 类用来解决空指针异常。

```java
Optional<String> str = Optional.of("str"); // 创建一个 String 类型的容器
boolean pre = str.isPresent(); // 值是否存在
str.ifPresent(System.out::println); // 值如果存在就调用 println 方法，这里传入的是 println 的方法引用
String res = str.get(); // 获取值
str = Optional.ofNullable(null); // 传入空值
res = str.orElse("aa"); // 如果值存在，返回值，否则返回传入的参数
str = Optional.of("str");
res = str.map(s -> "aa" + s).get(); // 如果有值，对其调用映射函数得到返回值，对返回值进行 Optional 包装并返回
res = str.flatMap(s -> Optional.of(s + "bb")).flatMap(s -> Optional.of(s + "cc")).get(); // 返回一个带有映射函数的 Optional 对象
``` 


## 参考

[史上最全jdk新特性总结，涵盖jdk8到jdk15！](https://blog.51cto.com/u_15360778/3943818)

[聊聊 Java8 以后各个版本的新特性](https://juejin.cn/post/6844903918586052616#heading-19)
