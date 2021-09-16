### 四种引用

#### 强引用
```java
String a = new String("强引用")
```
#### 软引用

内存不足时被回收，一般用作缓存。

```java
SoftReference<byte<>> m = =new SoftReference<>(new byte[1024*1024*10])
```

#### 弱引用

遇到GC就会被回收，一般用来处理内存泄漏问题。

```java
WeakReference<M> m = new WeakReference<>(new M) 
```

#### 虚引用

主要是用来监测堆外内存对象的回收。get 方法不能获取到对象，对象回收后向queue中发送一个消息。

<img src="https://user-images.githubusercontent.com/17560388/132688606-0a9779d0-0f12-43e9-ad4e-e53a7c89f2c8.png" alt="图片替换文本" width="500"  align="bottom" />

```java
private static final ReferenceQueue<M> QUEUE = new ReferenceQueue<>();

PhantomReference<M> m = new PhantomReference<>(new M，QUEUE);
new Thread(()->{
    while(true){
        Reference<? extends M> poll = QUEUE.poll();
        if(poll!=null){
            System.out.println("虚引用被回收：" + poll);
        }
    }
})

```
### ThreadLocal 介绍

#### 1.ThreadLocal 是什么？
ThreadLocal 是一个本地线程副本变量工具类。主要用于将私有线程和该线程存放的副本对象做一个映射，各个线程之间的变量互不干扰，在高并发场景下，可以实现无状态的调用，适用于各个线程不共享变量值的操作。

#### 2.ThreadLocal 工作原理是什么？
ThreadLocal 原理：每个线程的内部都维护了一个 ThreadLocalMap，它是一个 Map（key,value）数据格式，key 是一个弱引用，也就是 ThreadLocal 本身，而 value 存的是线程变量的值。

也就是说 ThreadLocal 本身并不存储线程的变量值，它只是一个工具，用来维护线程内部的 Map，帮助存和取变量。

数据结构，如下图所示：

<img src="https://user-images.githubusercontent.com/17560388/132778631-32002d5e-2139-471a-a12b-d3764e3c7900.png" alt="图片替换文本" width="600"  align="bottom" />

#### 3.ThreadLocal 如何解决 Hash 冲突？
与 HashMap 不同，ThreadLocalMap 结构非常简单，没有 next 引用，也就是说 ThreadLocalMap 中解决 Hash 冲突的方式并非链表的方式，而是采用线性探测的方式。所谓线性探测，就是根据初始 key 的 hashcode 值确定元素在 table 数组中的位置，如果发现这个位置上已经被其他的 key 值占用，则利用固定的算法寻找一定步长的下个位置，依次判断，直至找到能够存放的位置。

源代码实现如下：

```java
/
 * Increment i modulo len.
 */
private static int nextIndex(int i, int len) {
    return ((i + 1 < len) ? i + 1 : 0);
}

/
 * Decrement i modulo len.
 */
private static int prevIndex(int i, int len) {
    return ((i - 1 >= 0) ? i - 1 : len - 1);
}
```
#### 4.ThreadLocal 的内存泄露是怎么回事？
ThreadLocal 在 ThreadLocalMap 中是以一个弱引用身份被 Entry 中的 Key 引用的，因此如果 ThreadLocal 没有外部强引用来引用它，那么 ThreadLocal 会在下次 JVM 垃圾收集时被回收。这个时候 Entry 中的 key 已经被回收，但是 value 又是一强引用不会被垃圾收集器回收，这样 ThreadLocal 的线程如果一直持续运行，value 就一直得不到回收，这样就会发生内存泄露。

#### 5.为什么 ThreadLocalMap 的 key 是弱引用？
我们知道 ThreadLocalMap 中的 key 是弱引用，而 value 是强引用才会导致内存泄露的问题，至于为什么要这样设计，这样分为两种情况来讨论：

1. key 使用强引用：这样会导致一个问题，引用的 ThreadLocal 的对象被回收了，但是 ThreadLocalMap 还持有 ThreadLocal 的强引用，如果没有手动删除，ThreadLocal 不会被回收，则会导致内存泄漏。
2. key 使用弱引用：这样的话，引用的 ThreadLocal 的对象被回收了，由于 ThreadLocalMap 持有 ThreadLocal 的弱引用，即使没有手动删除，ThreadLocal 也会被回收。value 在下一次 ThreadLocalMap 调用 set、get、remove 的时候会被清除。

比较以上两种情况，我们可以发现：由于 ThreadLocalMap 的生命周期跟 Thread 一样长，如果都没有手动删除对应 key，都会导致内存泄漏，但是使用弱引用可以多一层保障，弱引用 ThreadLocal 不会内存泄漏，对应的 value 在下一次 ThreadLocalMap 调用 set、get、remove 的时候被清除，算是最优的解决方案。

#### 6.ThreadLocal 的应用场景有哪些？
ThreadLocal 适用于独立变量副本的情况，比如 Hibernate 的 session 获取场景。

示例代码：

```java
private static final ThreadLocal<Session> threadLocal = new ThreadLocal<Session>();

public static Session getCurrentSession(){
    Session session =  threadLocal.get();
    try {
        if(session ==null&&!session.isOpen()){
            //...
        }
        threadLocal.set(session);
    } catch (Exception e) {
        // TODO: handle exception
    }
    return session;
}
```


