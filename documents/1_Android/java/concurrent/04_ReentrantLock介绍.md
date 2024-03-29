## 1.synchronized 简介

1. 修饰实例方法；
2. 修饰静态类方法；
3. 修饰代码块。

`synchronized` 是独占式悲观锁，通过 `JVM` 隐式实现，只允许同一时刻只有一个线程操作资源。

每个对象都隐式包含一个 `monitor` 对象，加锁的过程其实就是竞争 `monitor` 的过程。

线程进入字节码 `monitorenter `指令之后，线程将持有 `monitor` 对象，执行 `monitorexit` 时释放 `monitor` 对象。

其他线程没有拿到 `monitor` 对象时，则需要阻塞等待获取该 `monitor` 对象。

### 1.1 synchronized 修饰方法和代码块的区别

synchronized 既可以作用于方法，也可以作用于某一代码块。但在实现上是有区别的。 比如如下代码，使用 synchronized 作用于代码块：

<img width="400" alt="实现细节" src="https://user-images.githubusercontent.com/17560388/120887375-fbc44b80-c624-11eb-90ec-98922b74c1d5.png">  

使用 javap -v 查看上述 `test1()` 方法的字节码，可以看到，编译而成的字节码中会包含 `monitorenter` 和 `monitorexit` 这两个字节码指令。

如下所示：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/120887381-0383f000-c625-11eb-90bf-1d5ba2ef33c3.png">  

为什么字节码中有 1 个 `monitorenter` 和 2 个 `monitorexit`：

因为虚拟机保证异常发生时也能释放锁。2 个 `monitorexit` 一个是代码正常执行结束后释放锁，一个是在代码执行异常时释放锁。

### 1.2 synchronized 修饰方法

再看下 `synchronized` 修饰方法有哪些区别：

<img width="400" alt="修饰方法" src="https://user-images.githubusercontent.com/17560388/120887392-1e566480-c625-11eb-888b-06473f82651f.png">  

`synchronized` 修饰的方法在被编译后，方法的 `flags` 属性中会被标记为 `ACC_SYNCHRONIZED`。

当虚拟机访问一个被标记为 `ACC_SYNCHRONIZED` 的方法时，会自动在方法的开始和结束（或异常）位置添加 `monitorenter` 和 `monitorexit` 指令。

### 1.3 monitorenter 和 monitorexit。

可以把 `monitorenter` 和 `monitorexit` 理解为一把具体的锁。

在这个锁中保存着两个比较重要的属性：计数器和指针。

- 计数器代表当前线程一共访问了几次这把锁；
- 指针指向持有这把锁的线程。

<img width="400" alt="计数器和指针" src="https://user-images.githubusercontent.com/17560388/120887398-2910f980-c625-11eb-8073-eb8b5b7f4050.png"> 

锁计数器默认为0，当执行`monitorenter`指令时，如锁计数器值为 `0` 说明这把锁并没有被其它线程持有。
这个线程会将计数器加1，并将锁中的`指针`指向自己。当执行`monitorexit`指令时，会将计数器减1。

## 2. ReentrantLock 简介

`ReentrantLock` 是 `Lock` 的默认实现方式之一，基于 `AQS`（Abstract Queued Synchronizer，队列同步器）实现的，默认是非公平锁。

在它的内部有一个 `state` 的状态字段用于表示锁是否被占用，如果是 0 则表示锁未被占用，此时线程就可以把 state 改为 1，并成功获得锁，而其他未获得锁的线程只能去排队等待获取锁资源。

`synchronized` 和 `ReentrantLock` 都提供了锁的功能，具备互斥性和不可见性。

在 `JDK 1.5` 中 `synchronized` 的性能远远低于 `ReentrantLock`，但在 `JDK 1.6` 之后 `synchronized` 的性能略低于 `ReentrantLock`。

### 2.1 synchronized和ReentrantLock的区别

它们的区别如下：

- `synchronized` 是 `JVM` 隐式实现的，而 `ReentrantLock` 是 `Java` 语言提供的 API；
- `ReentrantLock` 可设置为公平锁，而 `synchronized` 却不行；
- `ReentrantLock` 只能修饰代码块，而 `synchronized` 可以用于修饰方法、修饰代码块等；
- `ReentrantLock` 需要手动加锁和释放锁，如果忘记释放锁，则会造成资源被永久占用，而 `synchronized` 无需手动释放锁；
- `ReentrantLock` 可以知道是否成功获得了锁，而 `synchronized` 却不行。

### 2.2 公平锁和非公平锁

- `公平锁`的含义是线程需要按照请求的顺序来获得锁；
- `非公平锁`则允许线程在发送请求的同时该锁的状态恰好变成了可用，那么此线程就可以跳过队列中所有排队的线程直接拥有该锁。

而`公平锁`由于有`挂起`和`恢复`所以存在一定的开销，因此性能不如`非公平锁`，所以 `ReentrantLock` 和 `synchronized` 默认都是`非公平锁`的实现方式。


## 3. ReentrantLock 源码分析


### 3.1 基本使用

`ReentrantLock` 是通过 `lock()` 来获取锁，并通过 `unlock()` 释放锁，使用代码如下：

```java
Lock lock = new ReentrantLock();
lock.lock(); // 加锁
try {
    //......业务处理
} finally {
    lock.unlock(); // 释放锁
}
```

### 3.2 ReentrantLock的构造方法

```java
public ReentrantLock() {
    sync = new NonfairSync(); // 非公平锁
}
public ReentrantLock(boolean fair) {
    sync = fair ? new FairSync() : new NonfairSync();
}
```

### 3.3 lock()方法的实现类

`ReentrantLock` 中的 `lock()` 是通过 `sync.lock()` 实现的，但 `Sync` 类中的 `lock()` 是一个抽象方法，需要子类 `NonfairSync` 或 `FairSync` 去实现。

`NonfairSync` 中的 `lock()`

```java
final void lock() {
    if (compareAndSetState(0, 1))
        // 将当前线程设置为此锁的持有者
        setExclusiveOwnerThread(Thread.currentThread());
    else
        acquire(1);
}
```

FairSync 中的 lock() 源码如下：

```java
final void lock() {
    acquire(1);
}
```

可以看出非公平锁比公平锁只是多了一行 `compareAndSetState` 方法，该方法是尝试将 `state` 值由 0 置换为 1，如果设置成功的话，则说明当前没有其他线程持有该锁，不用再去排队了，可直接占用该锁。否则，则需要通过 `acquire` 方法去排队。

### 3.4 父类`AQS`中的`acquire`源码：

```java
public final void acquire(int arg) {
    if (!tryAcquire(arg) && 
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```

tryAcquire 方法尝试获取锁，如果获取锁失败，则把它加入到阻塞队列中。

tryAcquire 的源码：

```java
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        // 公平锁比非公平锁多了一行代码 !hasQueuedPredecessors() 
        if (!hasQueuedPredecessors() && 
            compareAndSetState(0, acquires)) { //尝试获取锁
            setExclusiveOwnerThread(current); // 获取成功，标记被抢占
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) {
        //重入锁，获取锁的线程再次去拿这把锁，直接获取成功，并将 state 的值 +1 后重新设置，供后面释放锁的时候进行多次释放使用。
        int nextc = c + acquires;
        if (nextc < 0)
            throw new Error("Maximum lock count exceeded");
        setState(nextc); // set state=state+1
        return true;
    }
    return false;
}
```

对于此方法来说，公平锁比非公平锁只多一行代码 !hasQueuedPredecessors()，它用来查看队列中是否有比它等待时间更久的线程，如果没有，就尝试一下是否能获取到锁，如果获取成功，则标记为已经被占用。

如果获取锁失败，则调用 addWaiter 方法把线程包装成 Node 对象，同时放入到队列中，但 addWaiter 方法并不会尝试获取锁，acquireQueued 方法才会尝试获取锁，如果获取失败，则此节点会被挂起.

### hasQueuedPredecessors 的源码

```java
public final boolean hasQueuedPredecessors() {
    Node t = tail; 
    Node h = head;
    Node s;
    return h != t &&
        ((s = h.next) == null || s.thread != Thread.currentThread());
}
```

### addWaiter 用来添加新的节点到队列的尾部。
 
```java
private Node addWaiter(Node mode) {
    //根据传进来的参数mode=Node.EXCLUSIVE，表示将要构造一个独占锁。
    Node node = new Node(Thread.currentThread(), mode);
    Node pred = tail;
    //tail为空的情况下直接调用enq方法去进行head和tail的初始化。
    if (pred != null) {//tail不为空的情况下，将新构造节点的前驱设置为原尾部节点。       
        node.prev = pred;
        //使用CAS进行交换，如果成功，则将原尾部节点的后继节点设置为新节点，做双向列表关联；（这里要注意一点，交换成功的同时有其他线程读取该列表，有可能读取不到新节点。例如A线程
        //执行完下方步骤1后，还未执行步骤2，遍历的时候将会获取不到新节点，这也是 hasQueuedPredecessors方法 中的第一种情况）
        //如果不成功，则代表有竞争，有其他线程修改了尾部，则去调用下方enq方法
        if (compareAndSetTail(pred, node)) {   //1
            pred.next = node;   //2
            return node;
        }
    }
    enq(node);
    return node;
}

private Node enq(final Node node) {
   for (;;) {
       Node t = tail;
       if (t == null) { // Must initialize
           //初始化head和tail，初始化完成后，会继续执行外面的死循环，进行compareAndSetTail将新节点设置到尾部，和上述执行流程一样，这里就不详述了。
           if (compareAndSetHead(new Node()))
               tail = head;
       } else {
           node.prev = t;
           if (compareAndSetTail(t, node)) {
               t.next = node;
               return t;
           }
       }
   }
}
```

### acquireQueued的源码如下：

```java
//队列中的线程尝试获取锁，失败则会被挂起
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true; // 获取锁是否成功的状态标识
    try {
        boolean interrupted = false; // 线程是否被中断
        for (;;) {
            // 获取前一个节点（前驱节点）
            final Node p = node.predecessor();
            // 当前节点为头节点的下一个节点时，有权尝试获取锁
            if (p == head && tryAcquire(arg)) {
                setHead(node); // 获取成功，将当前节点设置为 head 节点
                p.next = null; // 原 head 节点出队，等待被 GC
                failed = false; // 获取成功
                return interrupted;
            }
            // 判断获取锁失败后是否可以挂起
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                // 线程若被中断，返回 true
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```

该方法会使用 for(;;) 无限循环的方式来尝试获取锁，若获取失败，则调用 shouldParkAfterFailedAcquire 方法，尝试挂起当前线程，源码如下：

```java
//判断线程是否可以被挂起
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    // 获得前驱节点的状态
    int ws = pred.waitStatus;
    // 前驱节点的状态为 SIGNAL，当前线程可以被挂起（阻塞）
    if (ws == Node.SIGNAL)
        return true;
    if (ws > 0) { 
        do {
        // 若前驱节点状态为 CANCELLED，那就一直往前找，直到找到一个正常等待的状态为止
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);
        // 并将当前节点排在它后边
        pred.next = node;
    } else {
        // 把前驱节点的状态修改为 SIGNAL
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
    }
    return false;
}
```
线程入列被挂起的前提条件是，前驱节点的状态为 SIGNAL，SIGNAL 状态的含义是后继节点处于等待状态，当前节点释放锁后将会唤醒后继节点。
所以在上面这段代码中，会先判断前驱节点的状态，如果为 SIGNAL，则当前线程可以被挂起并返回 true；
如果前驱节点的状态 >0，则表示前驱节点取消了，这时候需要一直往前找，直到找到最近一个正常等待的前驱节点，然后把它作为自己的前驱节点；
如果前驱节点正常（未取消），则修改前驱节点状态为 SIGNAL。

到这里整个加锁的流程就已经走完了，最后的情况是，没有拿到锁的线程会在队列中被挂起，直到拥有锁的线程释放锁之后，才会去唤醒其他的线程去获取锁资源，整个运行流程如下图所示：

unlock 相比于 lock 来说就简单很多了，源码如下：

```java
public void unlock() {
    sync.release(1);
}
public final boolean release(int arg) {
    // 尝试释放锁
    if (tryRelease(arg)) {
        // 释放成功
        Node h = head;
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);
        return true;
    }
    return false;
}
```
锁的释放流程为，先调用 tryRelease 方法尝试释放锁，如果释放成功，则查看头结点的状态是否为 SIGNAL，如果是，则唤醒头结点的下个节点关联的线程；如果释放锁失败，则返回 false。

tryRelease 源码如下：

```java
//尝试释放当前线程占有的锁
protected final boolean tryRelease(int releases) {
    int c = getState() - releases; // 释放锁后的状态，0 表示释放锁成功
    // 如果拥有锁的线程不是当前线程的话抛出异常
    if (Thread.currentThread() != getExclusiveOwnerThread())
        throw new IllegalMonitorStateException();
    boolean free = false;
    if (c == 0) { // 锁被成功释放
        free = true;
        setExclusiveOwnerThread(null); // 清空独占线程
    }
    setState(c); // 更新 state 值，0 表示为释放锁成功
    return free;
}
```
在 tryRelease 方法中，会先判断当前的线程是不是占用锁的线程，如果不是的话，则会抛出异常；
如果是的话，则先计算锁的状态值 getState() - releases 是否为 0，如果为 0，则表示可以正常的释放锁，然后清空独占的线程，最后会更新锁的状态并返回执行结果。

https://www.codenong.com/cs106477645/



