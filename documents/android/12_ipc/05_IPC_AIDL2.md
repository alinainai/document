在上一篇文档中我们简单的实现了一个 AIDL 的例子，在本篇中我们来进阶一下，看下 Aidl 的关键字和其他一些技巧

## 一、in/out/inout 关键字

在上篇文档 demo 的 addPerson() 方法中，user 参数有个 `in` 关键字，这个`in` 其实是定向 `tag ` ，用来**指出数据流通的方式**。还有2个是  `out ` 和  `inout `**。所有的非基本参数都需要一个定向 tag 来指出数据的流向，基本参数的定向 tag 默认并且只能是  `in ` **。

我们在 IUserAidlInterface.adil 添加两个新方法

```java
boolean addUserOut(out User user);
boolean addUserInOut(inout User user);
```

服务端实现:

```kotlin
fun addUser(user: User): Boolean {
    Log.e("服务端", "addUser: user = $user")
    user.name = "被addPersonIn修改"
    return users.add(user)
}

fun addUserOut(user: User): Boolean {
    Log.e("服务端", "addUserOut: user = $user")
    user.name = "被addPersonOut修改"
    return users.add(user)
}

fun addUserInout(user: User): Boolean {
    Log.e("服务端", "addUserInout: user = $user")
    user.name = "被addPersonInout修改"
    return users.add(user)
}
```

客户端实现:

```kotlin
private fun addPerson() {            
    val user = User("张花花")
    Log.e(TAG, "addPersonIn() 调用之前 user = $user")
    val addPersonResult = mRemoteServer?.addUser(user)
    Log.e(TAG, "addPersonIn() 调用之后 user = $user")
}
private fun addPersonOut() {
    val user = User("张草草")
    Log.e(TAG, "addPersonOut() 调用之前 user = $user")
    val addPersonResult = mRemoteServer?.addUserOut(user)
    Log.e(TAG, "addPersonOut() 调用之前 user = $user")
}
private fun addPersonInout() {
    val user = User("张草草")
    Log.e(TAG, "addPersonOut() 调用之前 user = $user")
    val addPersonResult = mRemoteServer?.addUserOut(user)
    Log.e(TAG, "addPersonOut() 调用之前 user = $user")
}
```

最后输出的日志如下:

```shell
//in 方式  服务端那边修改了,但是服务端这边不知道
客户端: addPersonIn() 调用之前 user = User(name=张花花)
服务端: addUser: user = User(name=张花花)
客户端: addPersonIn() 调用之后 user = User(name=张花花)

//out方式 客户端能感知服务端的修改,且客户端不能向服务端传数据
//可以看到服务端是没有拿到客户端的数据的!
客户端: addPersonOut() 调用之前 user = User(name=张草草)
服务端: addUserOut: user = User(name=)
客户端: addPersonOut() 调用之前 user = User(name=被addPersonOut修改)

//inout方式 客户端能感知服务端的修改
客户端: addPersonInout() 调用之前 user = User(name=张大树)
服务端: addUserInout: user = User(name=张大树)
客户端: addPersonInout() 调用之前 user = User(name=被addPersonInout修改)
```

由上面的 demo 可以更容易理解数据流向的含义。而且我们还发现了以下规律:

- in 方式是可以从客户端向服务端传数据的，out 则不行
- out 方式是可以从服务端向客户端传数据的，in 则不行
- 不管服务端是否有修改传过去的对象数据，客户端的对象引用是不会变的，变化的只是客户端的数据。合情合理,跨进程是序列化与反序列化的方式操作数据.

## 二、oneway 关键字

正常情况下 Client 调用 AIDL 接口方法时会阻塞，直到 Server 进程中该方法被执行完。oneway 可以修饰AIDL文件里的方法，oneway 修饰的方法在用户请求相应功能时不需要等待响应可直接调用返回，非阻塞效果，该关键字可以用来声明接口或者声明方法，如果接口声明中用到了oneway关键字，则该接口声明的所有方法都采用oneway方式。（注意,如果 client 和 Server 在同一进程中，oneway修饰的方法还是会阻塞）

验证: 

在 aidl 接口方法定义一个 oneway 修饰的方法，在服务端的方法实现中加一个 `Thread.sleep(2000)` 的阻塞，然后客户端调用这个方法。查看方法调用的前后时间

```kotlin
private fun addPersonOneway() {
    log(TAG, "oneway开始时间: ${System.currentTimeMillis()}")
    remoteServer?.addPersonOneway(Person("oneway"))
    log(TAG, "oneway结束时间: ${System.currentTimeMillis()}")
}
//oneway开始时间: 1608858291371
//oneway结束时间: 1608858291372
```

可以看到，客户端调用这个方法时确实是没有被阻塞的。

## 三、线程安全问题

**AIDL 的方法是在服务端的 Binder 线程池中执行的**，所以多个客户端同时进行连接且操作数据时可能存在多个线程同时访问的情形。这样的话，我们就需要在服务端 AIDL 方法中处理多线程同步问题。

AIDL方法运行线程:

```kotlin
override fun addPerson(user: User?): Boolean {
    log(TAG, "服务端 addPerson() 当前线程 : ${Thread.currentThread().name}")
    return mPersonList.add(person)
}
// 服务端 addPerson() 当前线程 : Binder:3961_3
```

## 四、添加回调



## 五、监听服务Death

Server 所以的进程可能随时会被杀掉。客户端需要能感知 binder 是否已经死亡，从而做一些收尾清理工作或者进程重新连接。

有如下4种方式能知道服务端是否已经挂掉：

1. 调用binder的pingBinder()检查,返回false则说明远程服务失效
2. 调用binder的linkToDeath()注册监听器,当远程服务失效时,就会收到回调
3. 绑定Service时用到的ServiceConnection有个onServiceDisconnected()回调在服务端断开时也能收到回调
4. 客户端调用远程方法时,抛出DeadObjectException(RemoteException)

写份代码验证一下,在客户端修改为如下:

```kotlin
private val mDeathRecipient = object : IBinder.DeathRecipient {
    override fun binderDied() {
        //监听 binder died
        log(TAG, "binder died")
        //移除死亡通知
        mService?.unlinkToDeath(this, 0)
        mService = null
        //重新连接
        connectService()
    }
}
private val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        this@AidlActivity.mService = service
        log(TAG, "onServiceConnected")
        service?.linkToDeath(mDeathRecipient, 0)
        mRemoteServer = IPersonManager.Stub.asInterface(service)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        log(TAG, "onServiceDisconnected")
    }
}
```

绑定服务之后,将服务端进程杀掉,输出日志如下:

```shell
//第一次连接
bindService true
onServiceConnected, thread = main
//杀掉服务端 
binder died, thread = Binder:29391_3
onServiceDisconnected, thread = main
//重连
bindService true
onServiceConnected, thread = main
```

确实是监听到了服务端断开连接的时刻.然后重新连接也是ok的. 这里需要注意的是`binderDied()`方法是运行在子线程的,`onServiceDisconnected()`是运行在主线程的,如果要在这里更新UI,得注意一下.

## 参考

- [AndroidDevelopers : RemoteCallbackList](https://developer.android.google.cn/reference/android/os/RemoteCallbackList.html)
- [AIDL中RemoteCallbackList的使用及权限验证方式](https://www.jianshu.com/p/69e5782dd3c3)
- [Android进程间通信 深入浅出AIDL](https://zhuanlan.zhihu.com/p/338093696)
- [RemoteCallbackList的例子](https://www.programcreek.com/java-api-examples/?api=android.os.RemoteCallbackList)
