## 一、引用

在Java中，对象在堆/方法区上分配，由垃圾回收器扫描对象可达性进行回收。而在C/C++中，分配在堆上的对象需要我们手动调用 delete 去释放。JNI 层作为 Java 层和 C/C++ 层之间的桥接层，提供了一套规则来处理这种情况。

在JNI规范中定义了三种引用：局部引用（Local Reference）、全局引用（Global Reference）、弱全局引用（Weak Global Reference）

### 1、局部引用

大部分 JNI 函数会创建局部引用，局部引用只有在创建引用的本地方法返回前有效，也只在创建局部引用的线程中有效。在方法返回后，局部引用会自动释放，也可以通过 DeleteLocalRef 函数手动释放；

局部引用会阻止GC回收所引用的对象，不能在本地函数中跨函数使用，也不能跨线程使用。我们可以通过 NewLocalRef 和各种JNI接口创建（FindClass、NewObject、GetObjectClass和NewCharArray等）局部引用对象。

当使用大内存的Java对象，或者大量使用 Java 对象时，可以通过 DeleteLocalRef 函数手动释放局部对象





### 2、全局引用： 

全局引用可以跨方法、跨线程使用，直到它被手动释放才会失效。同局部引用一样，也会阻止它所引用的对象被GC回收。与局部引用创建方式不同的是，只能通过NewGlobalRef函数创建，不再使用对象时必须通过 DeleteGlobalRef 函数释放。

3、弱全局引用： 弱引用与全局引用类似，区别在于弱全局引用不会持有强引用，因此不会阻止垃圾回收器回收引用指向的对象。弱全局引用通过 NewGlobalWeakRef 函数创建，不再使用对象时必须通过 DeleteGlobalWeakRef 函数释放。
示例程序

// 局部引用
jclass localRefClz = env->FindClass("java/lang/String");
env->DeleteLocalRef(localRefClz);

// 全局引用
jclass globalRefClz = env->NewGlobalRef(localRefClz);
env->DeleteGlobalRef(globalRefClz);

// 弱全局引用
jclass weakRefClz = env->NewWeakGlobalRef(localRefClz);
env->DeleteGlobalWeakRef(weakRefClz);
5.3 JNI 引用的实现原理
在 JavaVM 和 JNIEnv 中，会分别建立多个表管理引用：

JavaVM 内有 globals 和 weak_globals 两个表管理全局引用和弱全局引用。由于 JavaVM 是进程共享的，因此全局引用可以跨方法和跨线程共享；
JavaEnv 内有 locals 表管理局部引用，由于 JavaEnv 是线程独占的，因此局部引用不能跨线程。另外虚拟机在进入和退出本地方法通过 Cookie 信息记录哪些局部引用是在哪些本地方法中创建的，因此局部引用是不能跨方法的。
5.4 比较引用是否指向相同对象
可以使用 JNI 函数 IsSameObject 判断两个引用是否指向相同对象（适用于三种引用类型），返回值为 JNI_TRUE 时表示相同，返回值为 JNI_FALSE 表示不同。例如：

示例程序

jclass localRef = ...
jclass globalRef = ...
bool isSampe = env->IsSamObject(localRef, globalRef）
另外，当引用与 NULL 比较时含义略有不同：

局部引用和全局引用与 NULL 比较： 用于判断引用是否指向 NULL 对象；
弱全局引用与 NULL 比较： 用于判断引用指向的对象是否被回收。


## 参考

- [NDK开发（二）- JNI](https://www.jianshu.com/p/b0260cf9370f)
- [NDK 系列（5）：JNI 从入门到实践，爆肝万字详解！](https://www.jianshu.com/p/5f48a9190d9d)
- [Android JNI学习(四)——JNI的常用方法的中文API](https://www.jianshu.com/p/67081d9b0a9c)
- [JNI/NDK开发指南（九）——JNI调用性能测试及优化](https://blog.csdn.net/xyang81/article/details/44279725)
- [NDK-JNI实战教程（二） JNI官方中文资料 ](https://www.cnblogs.com/jycboy/archive/2016/04/15/5396876.html#quanjujubuyinyong)
