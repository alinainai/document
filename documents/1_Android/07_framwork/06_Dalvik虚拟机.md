先看一下 `android` 系统的结构图

<img width="800" alt="interval弹珠图" src="https://user-images.githubusercontent.com/17560388/181872654-aaf75bb7-7bcb-4fe5-b4cb-481cc36af221.png">

## 1、DVM 和 JVM 字节码层面的差异

`Android` 应用程序是运行在 `DVM` 中，每一个应用程序对应有一个单独的`DVM`实例。
- `Dvm` 运行 `dex`文件 (dex 文件去除了 class 文件中的冗余信息（比如重复字符常量），并且结构更加紧凑，因此在 dex 解析阶段，可以减少 I/O 操作，提高了类的查找速度)
- `DVM` 使用的指令是基于`寄存器`的，而`JVM`使用的指令集是基于`堆栈`的。

基于堆栈指令的特点：很紧凑，例如，Java虚拟机使用的指令只占一个字节，因而称为字节码。

基于寄存器指令的特点：由于需要指定源地址和目标地址，因此需要占用更多的指令空间，例如，Dalvik虚拟机的某些指令需要占用两个字节。

## 2、DVM 和 JVM 内存管理与回收的差异

Dalvik 虚拟机中的堆被划分为了 2 部分：`Active Heap` 和 `Zygote Heap`。

<img width="600" alt="interval弹珠图" src="https://user-images.githubusercontent.com/17560388/181877102-490ea92b-da9b-446c-845a-e0d83a186066.png">

Card Table 以及两个 Heap Bitmap 主要是用来记录垃圾收集过程中对象的引用情况，以便实现 Concurrent GC

## 3、为什么要分 Zygote 和 Active 两部分？

Android 系统的第一个 Dalvik 虚拟机是由 Zygote 进程创建的，而应用程序进程是由 Zygote 进程 fork 出来的。

Zygote 进程是在系统启动时产生的，它会完成虚拟机的初始化，库的加载，预置类库的加载和初始化等操作，而在系统需要一个新的虚拟机实例时，Zygote 通过复制自身，最快速的提供一个进程；
另外，对于一些只读的系统库，所有虚拟机实例都和 Zygote 共享一块内存区域，大大节省了内存开销。

