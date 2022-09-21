## 一、AIDL 简单介绍

上篇文章我们学习了 Binder 的相关知识，通常我们做 IPC 开发时，用的最多的就是 AIDL，AIDL 也是基于 Binder 实现的。

AIDL 的全称是 Android 接口自定义语言，和其他接口语言 (IDL) 类似。利用它定义客户端与服务均认可的编程接口，以便二者间进行进程间通信。

SDK Tools 会将 .aidl 文件编译为 .java 文件，我们将在下面的分析中结合例子讲解一下 .java 文件中的方法。

## 二、主要代码

我们新建一个 Android 项目作为我们的 demo，然后 new 一个 .aidl 文件。我们参考 《Android 开发艺术探索》，实现一个 IUserAidlInterface.aidl 接口如下：
```java
import com.egas.demo.bean.User; //注意: 这里是要引入 data 类，我们要在 aidl 中实现一个和 data 类对用的 aidl 文件

interface IUserAidlInterface {
     List<User> getUsers();
     boolean addUser(in User user);
}
```
我们用到的实体类和对应的aidl，如下
```java
package com.egas.demo.bean
// 我们引入了 id("kotlin-parcelize") 插件，通过注解直接实现 Parcelable 相关的代码，在该系列的第二篇文章中有讲解
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(val uId:Int,var name:String,var des:String) : Parcelable {
}
```

```java
//User.aidl: 这里要注意一下，要和 Java 中的 User 类包名要保持一致
package com.egas.demo.bean;

parcelable User;
```

本 demo 的 Aidl 是在一个项目中实现的，当然你也可以写一个 client 项目单独去实现客户端的代码，代码都是一样的，调用的时候注意要让 Service 所在的项目启动。

<img width="468" alt="demo 文件" src="https://user-images.githubusercontent.com/17560388/190108369-ec427526-e7a1-465b-8112-3fe0940cef8c.png">

我们 `rebuild` 一下，在 `app/build/generated/aidl_source_output_dir` 会生成 `IUserAidlInterface.java` 代码:

<img width="461" alt="image" src="https://user-images.githubusercontent.com/17560388/190111480-13f3a8c7-4968-426c-a6d0-1a760bb964c9.png">

具体代码，去掉了一些无用代码：

```java
package com.egas.demo;

public interface IUserAidlInterface extends android.os.IInterface {
 
    public static abstract class Stub extends android.os.Binder implements com.egas.demo.IUserAidlInterface {
        private static final java.lang.String DESCRIPTOR = "com.egas.demo.IUserAidlInterface";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        /**
         * Cast an IBinder object into an com.egas.demo.IUserAidlInterface interface,generating a proxy if needed.
         */
        public static com.egas.demo.IUserAidlInterface asInterface(android.os.IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin != null) && (iin instanceof com.egas.demo.IUserAidlInterface))) {
                return ((com.egas.demo.IUserAidlInterface) iin);
            }
            return new com.egas.demo.IUserAidlInterface.Stub.Proxy(obj);
        }

        @Override
        public android.os.IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
            java.lang.String descriptor = DESCRIPTOR;
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(descriptor);
                    return true;
                }
                case TRANSACTION_getUsers: {
                    data.enforceInterface(descriptor);
                    java.util.List<com.egas.demo.bean.User> _result = this.getUsers();
                    reply.writeNoException();
                    reply.writeTypedList(_result);
                    return true;
                }
                case TRANSACTION_addUser: {
                    data.enforceInterface(descriptor);
                    com.egas.demo.bean.User _arg0;
                    if ((0 != data.readInt())) {
                        _arg0 = com.egas.demo.bean.User.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    boolean _result = this.addUser(_arg0);
                    reply.writeNoException();
                    reply.writeInt(((_result) ? (1) : (0)));
                    return true;
                }
                default: {
                    return super.onTransact(code, data, reply, flags);
                }
            }
        }

        private static class Proxy implements com.egas.demo.IUserAidlInterface {
            private android.os.IBinder mRemote;

            Proxy(android.os.IBinder remote) {
                mRemote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return mRemote;
            }

            public java.lang.String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override
            public java.util.List<com.egas.demo.bean.User> getUsers() throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                java.util.List<com.egas.demo.bean.User> _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_getUsers, _data, _reply, 0);
                    if (!_status && getDefaultImpl() != null) {
                        return getDefaultImpl().getUsers();
                    }
                    _reply.readException();
                    _result = _reply.createTypedArrayList(com.egas.demo.bean.User.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public boolean addUser(com.egas.demo.bean.User user) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if ((user != null)) {
                        _data.writeInt(1);
                        user.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    boolean _status = mRemote.transact(Stub.TRANSACTION_addUser, _data, _reply, 0);
                    if (!_status && getDefaultImpl() != null) {
                        return getDefaultImpl().addUser(user);
                    }
                    _reply.readException();
                    _result = (0 != _reply.readInt());
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            public static com.egas.demo.IUserAidlInterface sDefaultImpl;
        }

        static final int TRANSACTION_getUsers = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
        static final int TRANSACTION_addUser = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    }

    public java.util.List<com.egas.demo.bean.User> getUsers() throws android.os.RemoteException;
    public boolean addUser(com.egas.demo.bean.User user) throws android.os.RemoteException;
}
```

上面的代码中出现了很多和 Binder 相关的类，如，IBinder、IInterface、Binder、Stub等。下一节中我们先看一下这些类的作用和相关的方法。

## 三、类职责描述

`IUserAidlInterface.java` 涉及到相关类如下：

### 3.1 IBinder 接口

它代表了一种跨进程传输的能力；只要实现了这个接口，就能将这个对象进行跨进程传递；这是驱动底层支持的；在跨进程数据流经驱动的时候，驱动会识别IBinder类型的数据，从而自动完成不同进程Binder本地对象以及Binder代理对象的转换。

### 3.2 IInterface 接口

```java
// Base class for Binder interfaces. When defining a new interface, you must derive it from IInterface.
public interface IInterface{
    /**
     * Retrieve the Binder object associated with this interface.
     * You must use this instead of a plain cast, so that proxy objects can return the correct result.
     */
    public IBinder asBinder();
}
```
### 3.3 Java层的Binder类

Binder类代表的其实就是 Binder 本地对象。BinderProxy 类是 Binder 类的一个内部类，它代表远程进程的 Binder 对象的本地代理；
这两个类都继承自IBinder, 因而都具有跨进程传输的能力；实际上，在跨越进程的时候，Binder驱动会自动完成这两个对象的转换。

### 3.4 Stub 类
在使用AIDL的时候，编译工具会给我们生成一个Stub的静态内部类；
这个类继承了 Binder, 说明它是一个 Binder 本地对象，它实现了 IInterface 接口，表明它具有远程 Server 承诺给 Client 的能力；
Stub是一个抽象类，具体的 IInterface 的相关实现需要我们手动完成，这里使用了策略模式。

### 3.5 过程讲解

一次跨进程通信必然会涉及到两个进程，在这个例子中 RemoteService 作为服务端进程，提供服务；ClientActivity 作为客户端进程，使用 RemoteService 提供的服务。如下图：

系统帮我们生成  之后，我们只需要继承 ICompute.Stub 这个抽象类，实现它的方法，然后在 Service 的 onBind方法里面返回就实现了AIDL。这个Stub类非常重要，具体看看它做了什么。

Stub类继承自Binder，意味着这个Stub其实自己是一个Binder本地对象，然后实现了ICompute接口，ICompute本身是一个IInterface，因此他携带某种客户端需要的能力（这里是方法add)。此类有一个内部类Proxy，也就是Binder代理对象；

然后看看asInterface方法，我们在bind一个Service之后，在onServiceConnecttion的回调里面，就是通过这个方法拿到一个远程的service的，这个方法做了什么呢？


/**
 * Cast an IBinder object into an com.example.test.app.ICompute interface,
 * generating a proxy if needed.
 */
public static com.example.test.app.ICompute asInterface(android.os.IBinder obj) {
    if ((obj == null)) {
        return null;
    }
    android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
    if (((iin != null) && (iin instanceof com.example.test.app.ICompute))) {
        return ((com.example.test.app.ICompute) iin);
    }
    return new com.example.test.app.ICompute.Stub.Proxy(obj);
}
首先看函数的参数IBinder类型的obj，这个对象是驱动给我们的，如果是Binder本地对象，那么它就是Binder类型，如果是Binder代理对象，那就是BinderProxy类型；然后，正如上面自动生成的文档所说，它会试着查找Binder本地对象，如果找到，说明Client和Server都在同一个进程，这个参数直接就是本地对象，直接强制类型转换然后返回，如果找不到，说明是远程对象（处于另外一个进程）那么就需要创建一个Binde代理对象，让这个Binder代理实现对于远程对象的访问。一般来说，如果是与一个远程Service对象进行通信，那么这里返回的一定是一个Binder代理对象，这个IBinder参数的实际上是BinderProxy;

再看看我们对于aidl的add 方法的实现；在Stub类里面，add是一个抽象方法，我们需要继承这个类并实现它；如果Client和Server在同一个进程，那么直接就是调用这个方法；那么，如果是远程调用，这中间发生了什么呢？Client是如何调用到Server的方法的？

我们知道，对于远程方法的调用，是通过Binder代理完成的，在这个例子里面就是Proxy类；Proxy对于add方法的实现如下：

1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
16
17
18
Override
public int add(int a, int b) throws android.os.RemoteException {
    android.os.Parcel _data = android.os.Parcel.obtain();
    android.os.Parcel _reply = android.os.Parcel.obtain();
    int _result;
    try {
        _data.writeInterfaceToken(DESCRIPTOR);
        _data.writeInt(a);
        _data.writeInt(b);
        mRemote.transact(Stub.TRANSACTION_add, _data, _reply, 0);
        _reply.readException();
        _result = _reply.readInt();
    } finally {
        _reply.recycle();
        _data.recycle();
    }
    return _result;
}
它首先用Parcel把数据序列化了，然后调用了transact方法；这个transact到底做了什么呢？这个Proxy类在asInterface方法里面被创建，前面提到过，如果是Binder代理那么说明驱动返回的IBinder实际是BinderProxy, 因此我们的Proxy类里面的mRemote实际类型应该是BinderProxy；我们看看BinderProxy的transact方法：(Binder.java的内部类)

1
2
public native boolean transact(int code, Parcel data, Parcel reply,
            int flags) throws RemoteException;
这是一个本地方法；它的实现在native层，具体来说在frameworks/base/core/jni/android_util_Binder.cpp文件，里面进行了一系列的函数调用，调用链实在太长这里就不给出了；要知道的是它最终调用到了talkWithDriver函数；看这个函数的名字就知道，通信过程要交给驱动完成了；这个函数最后通过ioctl系统调用，Client进程陷入内核态，Client调用add方法的线程挂起等待返回；驱动完成一系列的操作之后唤醒Server进程，调用了Server进程本地对象的onTransact函数（实际上由Server端线程池完成）。我们再看Binder本地对象的onTransact方法（这里就是Stub类里面的此方法）：

1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
16
17
18
19
20
21
@Override
public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
    switch (code) {
        case INTERFACE_TRANSACTION: {
            reply.writeString(DESCRIPTOR);
            return true;
        }
        case TRANSACTION_add: {
            data.enforceInterface(DESCRIPTOR);
            int _arg0;
            _arg0 = data.readInt();
            int _arg1;
            _arg1 = data.readInt();
            int _result = this.add(_arg0, _arg1);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }
    }
    return super.onTransact(code, data, reply, flags);
}
在Server进程里面，onTransact根据调用号（每个AIDL函数都有一个编号，在跨进程的时候，不会传递函数，而是传递编号指明调用哪个函数）调用相关函数；在这个例子里面，调用了Binder本地对象的add方法；这个方法将结果返回给驱动，驱动唤醒挂起的Client进程里面的线程并将结果返回。于是一次跨进程调用就完成了。

至此，你应该对AIDL这种通信方式里面的各个类以及各个角色有了一定的了解；它总是那么一种固定的模式：一个需要跨进程传递的对象一定继承自IBinder，如果是Binder本地对象，那么一定继承Binder实现IInterface，如果是代理对象，那么就实现了IInterface并持有了IBinder引用；

Proxy与Stub不一样，虽然他们都既是Binder又是IInterface，不同的是Stub采用的是继承（is 关系），Proxy采用的是组合（has 关系）。他们均实现了所有的IInterface函数，不同的是，Stub又使用策略模式调用的是虚函数（待子类实现），而Proxy则使用组合模式。为什么Stub采用继承而Proxy采用组合？事实上，Stub本身is一个IBinder（Binder），它本身就是一个能跨越进程边界传输的对象，所以它得继承IBinder实现transact这个函数从而得到跨越进程的能力（这个能力由驱动赋予）。Proxy类使用组合，是因为他不关心自己是什么，它也不需要跨越进程传输，它只需要拥有这个能力即可，要拥有这个能力，只需要保留一个对IBinder的引用。如果把这个过程做一个类比，在封建社会，Stub好比皇帝，可以号令天下，他生而具有这个权利（不要说宣扬封建迷信。。）如果一个人也想号令天下，可以，“挟天子以令诸侯”。为什么不自己去当皇帝，其一，一般情况没必要，当了皇帝其实限制也蛮多的是不是？我现在既能掌管天下，又能不受约束（Java单继承）；其二，名不正言不顺啊，我本来特么就不是（Binder），你非要我是说不过去，搞不好还会造反。最后呢，如果想当皇帝也可以，那就是asBinder了。在Stub类里面，asBinder返回this，在Proxy里面返回的是持有的组合类IBinder的引用。

再去翻阅系统的ActivityManagerServer的源码，就知道哪一个类是什么角色了：IActivityManager是一个IInterface，它代表远程Service具有什么能力，ActivityManagerNative指的是Binder本地对象（类似AIDL工具生成的Stub类），这个类是抽象类，它的实现是ActivityManagerService；因此对于AMS的最终操作都会进入ActivityManagerService这个真正实现；同时如果仔细观察，ActivityManagerNative.java里面有一个非公开类ActivityManagerProxy, 它代表的就是Binder代理对象；是不是跟AIDL模型一模一样呢？那么ActivityManager是什么？他不过是一个管理类而已，可以看到真正的操作都是转发给ActivityManagerNative进而交给他的实现ActivityManagerService 完成的。
Stub 类中我们重点介绍下 asInterface 和 onTransact。

先说说 asInterface，当 Client 端在创建和服务端的连接，调用 bindService 时需要创建一个 ServiceConnection 对象作为入参。在 ServiceConnection 的回调方法 onServiceConnected 中 会通过这个 asInterface(IBinder binder) 拿到 BookManager 对象，这个 IBinder 类型的入参 binder 是驱动传给我们的，正如你在代码中看到的一样，方法中会去调用 binder.queryLocalInterface() 去查找 Binder 本地对象，如果找到了就说明 Client 和 Server 在同一进程，那么这个 binder 本身就是 Binder 本地对象，可以直接使用。否则说明是 binder 是个远程对象，也就是 BinderProxy。因此需要我们创建一个代理对象 Proxy，通过这个代理对象来是实现远程访问。

接下来我们就要实现这个代理类 Proxy 了，既然是代理类自然需要实现 BookManager 接口。
```java
public class Proxy implements BookManager {

    ...

    public Proxy(IBinder remote) {
        this.remote = remote;
    }

    @Override
    public void addBook(Book book) throws RemoteException {

        Parcel data = Parcel.obtain();
        Parcel replay = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            if (book != null) {
                data.writeInt(1);
                book.writeToParcel(data, 0);
            } else {
                data.writeInt(0);
            }
            remote.transact(Stub.TRANSAVTION_addBook, data, replay, 0);
            replay.readException();
        } finally {
            replay.recycle();
            data.recycle();
        }
    }

    ...
}
```
我们看看 addBook() 的实现；在 Stub 类中，addBook(Book book) 是一个抽象方法，Client 端需要继承并实现它。

- 如果 Client 和 Server 在同一个进程，那么直接就是调用这个方法。

- 如果是远程调用，Client 想要调用 Server 的方法就需要通过 Binder 代理来完成，也就是上面的 Proxy。

在 Proxy 中的 addBook() 方法中首先通过 Parcel 将数据序列化，然后调用 remote.transact()。正如前文所述 Proxy 是在 Stub 的 asInterface 中创建，能走到创建 Proxy 这一步就说明 Proxy 构造函数的入参是 BinderProxy，即这里的 remote 是个 BinderProxy 对象。最终通过一系列的函数调用，Client 进程通过系统调用陷入内核态，Client 进程中执行 addBook() 的线程挂起等待返回；驱动完成一系列的操作之后唤醒 Server 进程，调用 Server 进程本地对象的 onTransact()。最终又走到了 Stub 中的 onTransact() 中，onTransact() 根据函数编号调用相关函数（在 Stub 类中为 BookManager 接口中的每个函数中定义了一个编号，只不过上面的源码中我们简化掉了；在跨进程调用的时候，不会传递函数而是传递编号来指明要调用哪个函数）；我们这个例子里面，调用了 Binder 本地对象的 addBook() 并将结果返回给驱动，驱动唤醒 Client 进程里刚刚挂起的线程并将结果返回。


## 参考

- [你真的理解AIDL中的in，out，inout么？](https://www.jianshu.com/p/ddbb40c7a251)
- [Android进程间通信 深入浅出AIDL](https://zhuanlan.zhihu.com/p/338093696)
- [AIDL实现的音乐播放器](https://github.com/naman14/Timber)


