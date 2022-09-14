## 一、AIDL 简单介绍

上篇文章我们学习了 Binder 的相关知识，通常我们做 IPC 开发时，用的最多的就是 AIDL。当然，AIDL 也是基于 Binder 实现的。

AIDL 的全称是 Android 接口自定义语言，和其他接口语言 (IDL) 类似。利用它定义客户端与服务均认可的编程接口，以便二者使用进程间通信 (IPC) 进行相互通信。简单点说 AIDL 就是 Android 提供的一种方便定义 IPC 的技术。

SDK Tools 会将 .aidl 文件编译为 .java 文件，我们将在下面的分析中结合例子讲解一下 .java 文件中的方法。

## 二、主要代码

我们新建一个 Android项目，然后 new 一个 .aidl 文件。我们参考 《Android 开发艺术探索》的例子，在我们的 demo 中也实现一套。

先看一下demo的主要文件，已经做了相关的标注。我们 aidl 是在一个项目中实现的，当然你也可以写一个 client 项目单独去实现客户端的代码，代码都是一样的，调用的时候注意要让 Service 所在的项目启动。

<img width="468" alt="demo 文件" src="https://user-images.githubusercontent.com/17560388/190108369-ec427526-e7a1-465b-8112-3fe0940cef8c.png">

我们看一下 Aidl 包文件的代码
```java
//IUserAidlInterface.aidl
package com.egas.demo;

import com.egas.demo.bean.User; //注意这里要引入 data 类

interface IUserAidlInterface {
     List<User> getUsers();
     boolean addUser(in User user);
}

//User.aidl: 这里要注意一下，要和 Java 中的 User 类包名要保持一致
package com.egas.demo.bean;

parcelable User;
```
在简单看下 Java 包的相关代码

```java
// 我们引入了 id("kotlin-parcelize") 插件，通过注解直接实现 Parcelable 相关的代码，在第二篇文章中有讲解
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(val uId:Int,var name:String,var des:String) : Parcelable {
}
```

我们 `rebuild` 一下，在 `app/build/generated/aidl_source_output_dir` 会生成 `IUserAidlInterface.java` 代码

我们先简单看下 `IUserAidlInterface.java` 的结构

<img width="461" alt="image" src="https://user-images.githubusercontent.com/17560388/190111480-13f3a8c7-4968-426c-a6d0-1a760bb964c9.png">

具体代码如下：

```java
package com.egas.demo;
//引入data类
public interface IUserAidlInterface extends android.os.IInterface
{
  /** Default implementation for IUserAidlInterface. */
  public static class Default implements com.egas.demo.IUserAidlInterface ...// 默认实现，返回了 null

  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.egas.demo.IUserAidlInterface
  {
    private static final java.lang.String DESCRIPTOR = "com.egas.demo.IUserAidlInterface";
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.egas.demo.IUserAidlInterface interface,generating a proxy if needed.
     */
    public static com.egas.demo.IUserAidlInterface asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.egas.demo.IUserAidlInterface))) {
        return ((com.egas.demo.IUserAidlInterface)iin);
      }
      return new com.egas.demo.IUserAidlInterface.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
        case TRANSACTION_getUsers:
        {
          data.enforceInterface(descriptor);
          java.util.List<com.egas.demo.bean.User> _result = this.getUsers();
          reply.writeNoException();
          reply.writeTypedList(_result);
          return true;
        }
        case TRANSACTION_addUser:
        {
          data.enforceInterface(descriptor);
          com.egas.demo.bean.User _arg0;
          if ((0!=data.readInt())) {
            _arg0 = com.egas.demo.bean.User.CREATOR.createFromParcel(data);
          }
          else {
            _arg0 = null;
          }
          boolean _result = this.addUser(_arg0);
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          return true;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
    }
    private static class Proxy implements com.egas.demo.IUserAidlInterface
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public java.util.List<com.egas.demo.bean.User> getUsers() throws android.os.RemoteException
      {
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
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean addUser(com.egas.demo.bean.User user) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          if ((user!=null)) {
            _data.writeInt(1);
            user.writeToParcel(_data, 0);
          }
          else {
            _data.writeInt(0);
          }
          boolean _status = mRemote.transact(Stub.TRANSACTION_addUser, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            return getDefaultImpl().addUser(user);
          }
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      public static com.egas.demo.IUserAidlInterface sDefaultImpl;
    }
    static final int TRANSACTION_getUsers = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_addUser = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    public static boolean setDefaultImpl(com.egas.demo.IUserAidlInterface impl) {
      // Only one user of this interface can use this function
      // at a time. This is a heuristic to detect if two different
      // users in the same process use this function.
      if (Stub.Proxy.sDefaultImpl != null) {
        throw new IllegalStateException("setDefaultImpl() called twice");
      }
      if (impl != null) {
        Stub.Proxy.sDefaultImpl = impl;
        return true;
      }
      return false;
    }
    public static com.egas.demo.IUserAidlInterface getDefaultImpl() {
      return Stub.Proxy.sDefaultImpl;
    }
  }
  public java.util.List<com.egas.demo.bean.User> getUsers() throws android.os.RemoteException;
  public boolean addUser(com.egas.demo.bean.User user) throws android.os.RemoteException;
}
```

在 `IUserAidlInterface.java` 设计到了很多和 Binder 相关的类，如，IBinder、IInterface、Binder、Stub等，再下一节中我们先看一下这些类的作用和相关的方法。


## 三、Java类职责描述

`IUserAidlInterface.java` 涉及到相关类如下：

- IBinder : IBinder 是一个接口，代表了一种跨进程通信的能力。只要实现了这个借口，这个对象就能跨进程传输。
- IInterface :  IInterface 代表的就是 Server 进程对象具备什么样的能力（能提供哪些方法，其实对应的就是 AIDL 文件中定义的接口）
- Binder : Java 层的 Binder 类，代表的其实就是 Binder 本地对象。BinderProxy 类是 Binder 类的一个内部类，它代表远程进程的 Binder 对象的本地代理；这两个类都继承自 IBinder, 因而都具有跨进程传输的能力；实际上，在跨越进程的时候，Binder 驱动会自动完成这两个对象的转换。
- Stub : AIDL 的时候，编译工具会给我们生成一个名为 Stub 的静态内部类；这个类继承了 Binder, 说明它是一个 Binder 本地对象，它实现了 IInterface 接口，表明它具有 Server 承诺给 Client 的能力；Stub 是一个抽象类，具体的 IInterface 的相关实现需要开发者自己实现。

### 5.2 实现过程讲解
一次跨进程通信必然会涉及到两个进程，在这个例子中 RemoteService 作为服务端进程，提供服务；ClientActivity 作为客户端进程，使用 RemoteService 提供的服务。如下图：

<img width="400" alt="AIDL" src="https://user-images.githubusercontent.com/17560388/182274407-8d1816fd-96e9-410e-bc93-09b9bbf9288b.png">

那么服务端进程具备什么样的能力？能为客户端提供什么样的服务呢？还记得我们前面介绍过的 IInterface 吗，它代表的就是服务端进程具体什么样的能力。因此我们需要定义一个 BookManager 接口，BookManager 继承自 IIterface，表明服务端具备什么样的能力。
```java
/**
 * 这个类用来定义服务端 RemoteService 具备什么样的能力
 */
public interface BookManager extends IInterface {

    void addBook(Book book) throws RemoteException;
}
```
只定义服务端具备什么要的能力是不够的，既然是跨进程调用，那么接下来我们得实现一个跨进程调用对象 Stub。Stub 继承 Binder, 说明它是一个 Binder 本地对象；实现 IInterface 接口，表明具有 Server 承诺给 Client 的能力；Stub 是一个抽象类，具体的 IInterface 的相关实现需要调用方自己实现。
```java
public abstract class Stub extends Binder implements BookManager {

    ...
    public static BookManager asInterface(IBinder binder) {
        if (binder == null)
            return null;
        IInterface iin = binder.queryLocalInterface(DESCRIPTOR);
        if (iin != null && iin instanceof BookManager)
            return (BookManager) iin;
        return new Proxy(binder);
    }
    ...
    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        switch (code) {

            case INTERFACE_TRANSACTION:
                reply.writeString(DESCRIPTOR);
                return true;

            case TRANSAVTION_addBook:
                data.enforceInterface(DESCRIPTOR);
                Book arg0 = null;
                if (data.readInt() != 0) {
                    arg0 = Book.CREATOR.createFromParcel(data);
                }
                this.addBook(arg0);
                reply.writeNoException();
                return true;

        }
        return super.onTransact(code, data, reply, flags);
    }

    ...
}
```
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


