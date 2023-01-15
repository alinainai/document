
### 1. HotSpot JVM 的 对象结构

`JVM` 本身是用 `C++` 实现的，一个 `Java` 对象在是如何映射到C层的对象呢？

HotSpot JVM 设计了一个 `OOP-Klass` 的模型。

1. OOP: Ordinary Object Pointer (普通对象指针)，表示对象的实例信息
2. Klass则包含元数据和方法信息，用来描述Java类

之所以采用这个模型是因为 HotSopt JVM 的设计者不想让每个对象中都含有一个vtable（虚函数表），所以就把对象模型拆成 `klass` 和 `oop`，其中 `oop` 中不含有任何虚函数，而 `Klass` 就含有虚函数表，可以进行 `method dispatch`。

### 2. Klass 模型

Klass 包含元数据和方法信息，用来描述Java类。

`Klass` 是用来表示 `class` 的元数据，包括常量池、字段、方法、类名、父类等。`Klass` 对象中含有虚函数表 `vtbl` 以及父类虚函数表 `klass_vtbl`，因此可以根据 `java` 对象的实例类型方法的分发。

JVM 在加载class字节码文件时，会在方法区创建Klass对象，其中 instanceKlass 可以认为是 java.lang.Class 的 VM 级别的表示，但它们并不等价，其结构如下图所示，

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/150764425-8fa669bf-e3fe-489a-b20f-c32bd457f72a.png">

### 3. OOP 模型

OOP 指的是普通对象指针，用来表示对象的实例信息

所有的 OOP 类的共同基类为 oopDesc 类。它的结构如下：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/150771847-9a83fa06-7ce3-4323-888f-99273c9499c5.png">

当在Java中使用 new 关键字创建一个对象时，就会在 JVM 中创建一个 instanceOopDesc 实例对象。Foo 中 的localValue 就是保存在这个对象当中。

我们经常说 Java 对象在内存中的布局分为：对象头、实例数据、对齐填充。其实这3部分就是对应上面图中的 oopDesc 对象。

`_mark` 和 `_metadata` 一起组成了对象头部分：

- Mark Word：instanceOopDesc 中的 `_mark` 成员，允许压缩。它用于存储对象的运行时记录信息，如哈希值、GC 分代年龄(Age)、锁状态标志（偏向锁、轻量级锁、重量级锁）、线程持有的锁、偏向线程 ID、偏向时间戳等。
- 元数据指针：instanceOopDesc 中的 `_metadata` 成员，它是联合体，可以表示未压缩的 `Klass` 指针(`_klass`)和压缩的 `Klass` 指针。对应的 `klass` 指针指向一个存储类的元数据的 `Klass` 对象。

在对象头之后，JVM会继续填充Java对象中的具体实例数据，比如Foo中的localValue。

### 参考
[大话Java对象在虚拟机中是什么样子？](https://mp.weixin.qq.com/s/fyvoraVu9yjgqX-xhn6EHQ)
