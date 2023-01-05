## 一、Dart是单线程语言

**首先说明一点：单线程与异步操作并不冲突。**

### 1、计算机处理耗时操作的方式
1、共享内存的多线程，：比如 `Java`、 `C++`，我们普遍的做法是开启一个新的线程 `（Thread） `，在新的线程中完成这些异步的操作，再通过线程间通信的方式，将拿到的数据传递给主线程。 
2、单线程+事件循环：比如 `JavaScript`、`Dart`都是基于单线程加事件循环来完成耗时操作的处理。

### 2、单线程的异步操作

因为我们的一个应用程序大部分时间都是处于空闲的状态的，并不是无限制的在和用户进行交互。 
比如等待用户点击、网络请求数据的返回、文件读写的IO操作，这些等待的行为并不会阻塞我们的线程；这是因为类似于网络请求、文件读写的IO，我们都可以基于非阻塞调用；

### 3、阻塞式调用和非阻塞式调用

阻塞和非阻塞关注的是程序在等待调用结果（消息，返回值）时的状态。

- 阻塞式调用： 调用结果返回之前，当前线程会被挂起，调用线程只有在得到调用结果之后才会继续执行。
- 非阻塞式调用： 调用执行之后，当前线程不会停止执行，只需要过一段时间来检查一下有没有结果返回即可。

我们开发中的很多耗时操作，都可以基于这样的 `非阻塞式调用`：

- 比如网络请求本身使用了 `Socket `通信，而 `Socket `本身提供了 `select `模型，可以进行非阻塞方式的工作；
- 比如文件读写的 `IO `操作，我们可以使用操作系统提供的基于事件的回调机制；

这些操作都不会阻塞我们单线程的继续执行，我们的线程在等待的过程中可以继续去做别的事情。
单线程是如何来处理网络通信、IO操作它们返回的结果呢？答案就是事件循环（Event Loop）。

### 4、Dart的单线程

从这个角度看，Isolate 其实是消息驱动的并发编程，算是CSP模型的简化，跟多线程编程是完全不同的并发编程范式。

Isolate它在底层其实就是个线程，但是Dart VM 限制了Isolate的能力，使得Isolate之间不能直接共享内存且独立GC，只能通过Port机制收发消息。**Port发送数据时是Copy的，如果有大块内存真的要Copy多份，可能会有比较大的内存问题。**

在Dart VM中，有很多个这样的Isolate，其中有一个Root Isolate是在Engine启动时创建的，它负责UI渲染以及用户交互操作，需要及时响应，当存在耗时操作，则必须创建新的Isolate，否则UI渲染会被阻塞。

UI Task Runner被Flutter Engine用于执行Dart root Isolate代码，引擎启动的时候为其增加了必要的绑定，使其具备调度提交渲染帧的能力。对于每一帧，引擎要做的事情有：

1. Root isolate通知Flutter Engine有帧需要渲染；
2. Flutter Engine通知平台，需要在下一个vsync的时候得到通知；
3. 平台等待下一个vsync；
4. 对创建的对象和Widgets进行Layout并生成一个Layer Tree，这个Tree马上被提交给Flutter Engine。当前阶段没有进行任何光栅化，这个步骤仅是生成了对需要绘制内容的描述。
5. 创建或者更新Tree，这个Tree包含了用于屏幕上显示Widgets的语义信息。这个东西主要用于平台相关的辅助Accessibility元素的配置和渲染。

### 既然Flutter Engine有自己的Runner，那为何还要Dart的Isolate？
Dart isolate跟Flutter Runner是相互独立的，他们通过任务调度机制相互协作。

Dart 的 Isolate 是 Dart 虚拟机自己管理的，Flutter Engine 无法直接访问。Root Isolate 通过 Dart 的 C++ 调用能力把 UI 渲染相关的任务提交到 UI Runner 执行这样就可以跟Flutter Engine 相关模块进行交互。

### 参考
- [Dart 异步编程](https://zhuanlan.zhihu.com/p/102671345)
- [Flutter(五)之彻底搞懂Dart异步](https://zhuanlan.zhihu.com/p/83781258)
- [flutter入门](https://juejin.cn/post/6844904007874379783#heading-3)

## 二、Dart 异步代码

Dart 类库有非常多的返回 `Future` 或者 `Stream` 对象的函数。 这些函数被称为异步函数：它们只会在设置好一些耗时操作之后返回，比如像 IO 操作。而不是等到这个操作完成。  
`async`和`await`关键词支持了异步编程，允许您写出和同步代码很像的异步代码。

### Future
`Future`表示一个异步操作的最终完成（或失败）及其结果值的表示。简单来说，它就是用于处理异步操作的，异步处理成功了就执行成功的操作，异步处理失败了就捕获错误或者停止后续操作。一个`Future`只会对应一个结果，要么成功，要么失败。

由于本身功能较多，这里我们只介绍其常用的API及特性。还有，请记住，`Future`的所有API的返回值仍然是一个`Future`对象，所以可以很方便的进行链式调用。

### Future.then
为了方便示例，在本例中我们使用`Future.delayed`创建了一个延时任务（实际场景会是一个真正的耗时任务，比如一次网络请求），即2秒后返回结果字符串"hi world!"，然后我们在then中接收异步结果并打印结果，代码如下：

```dart
Future.delayed(new Duration(seconds: 2),(){
   return "hi world!";
}).then((data){
   print(data);
});
```
### Future.catchError
如果异步任务发生错误，我们可以在`catchError`中捕获错误，我们将上面示例改为：
```dart
Future.delayed(new Duration(seconds: 2),(){
   //return "hi world!";
   throw AssertionError("Error");  
}).then((data){
   //执行成功会走到这里  
   print("success");
}).catchError((e){
   //执行失败会走到这里  
   print(e);
});
```
在本示例中，我们在异步任务中抛出了一个异常，`then`的回调函数将不会被执行，取而代之的是`catchError`回调函数将被调用；但是，并不是只有`catchError`回调才能捕获错误，`then`方法还有一个可选参数`onError`，我们也可以它来捕获异常：
```dart
Future.delayed(new Duration(seconds: 2), () {
	//return "hi world!";
	throw AssertionError("Error");
}).then((data) {
	print("success");
}, onError: (e) {
	print(e);
});
```
### Future.whenComplete
有些时候，我们会遇到无论异步任务执行成功或失败都需要做一些事的场景，比如在网络请求前弹出加载对话框，在请求结束后关闭对话框。这种场景，有两种方法，第一种是分别在`then`或`catch`中关闭一下对话框，第二种就是使用`Future`的`whenComplete`回调，我们将上面示例改一下：
```dart
Future.delayed(new Duration(seconds: 2),(){
   //return "hi world!";
   throw AssertionError("Error");
}).then((data){
   //执行成功会走到这里 
   print(data);
}).catchError((e){
   //执行失败会走到这里   
   print(e);
}).whenComplete((){
   //无论成功或失败都会走到这里
});
```
### Future.wait
有些时候，我们需要等待多个异步任务都执行结束后才进行一些操作，比如我们有一个界面，需要先分别从两个网络接口获取数据，获取成功后，我们需要将两个接口数据进行特定的处理后再显示到UI界面上，应该怎么做？答案是`Future.wait`，它接受一个`Future`数组参数，只有数组中所有`Future`都执行成功后，才会触发`then`的成功回调，只要有一个`Future`执行失败，就会触发错误回调。下面，我们通过模拟`Future.delayed`来模拟两个数据获取的异步任务，等两个异步任务都执行成功时，将两个异步任务的结果拼接打印出来，代码如下：
```dart
Future.wait([
  // 2秒后返回结果  
  Future.delayed(new Duration(seconds: 2), () {
    return "hello";
  }),
  // 4秒后返回结果  
  Future.delayed(new Duration(seconds: 4), () {
    return " world";
  })
]).then((results){
  print(results[0]+results[1]);
}).catchError((e){
  print(e);
});
```
执行上面代码，4秒后你会在控制台中看到“hello world”。

### async/await
Dart中的`async/await`和 JavaScript中 的`async/await`功能和用法是一模一样的，如果你已经了解 JavaScript 中的`async/await`的用法，可以直接跳过本节。

### 回调地狱(Callback Hell)
如果代码中有大量异步逻辑，并且出现大量异步任务依赖其它异步任务的结果时，必然会出现`Future.then`回调中套回调情况。举个例子，比如现在有个需求场景是用户先登录，登录成功后会获得用户 ID，然后通过用户 ID，再去请求用户个人信息，获取到用户个人信息后，为了使用方便，我们需要将其缓存在本地文件系统，代码如下：
```dart
//先分别定义各个异步任务
Future<String> login(String userName, String pwd){
	...
    //用户登录
};
Future<String> getUserInfo(String id){
	...
    //获取用户信息 
};
Future saveUserInfo(String userInfo){
	...
	// 保存用户信息 
}; 
```
接下来，执行整个任务流：
```dart
login("alice","******").then((id){
 //登录成功后通过，id获取用户信息    
 getUserInfo(id).then((userInfo){
    //获取用户信息后保存 
    saveUserInfo(userInfo).then((){
       //保存用户信息，接下来执行其它操作
        ...
    });
  });
})
```
可以感受一下，如果业务逻辑中有大量异步依赖的情况，将会出现上面这种在回调里面套回调的情况，过多的嵌套会导致的代码可读性下降以及出错率提高，并且非常难维护，这个问题被形象的称为回调地狱（Callback Hell）。回调地狱问题在之前JavaScript中非常突出，也是JavaScript被吐槽最多的点，但随着ECMAScript6和ECMAScript7标准发布后，这个问题得到了非常好的解决，而解决回调地狱的两大神器正是 ECMAScript6 引入了 Promise，以及ECMAScript7中引入的async/await。 而在Dart中几乎是完全平移了JavaScript中的这两者：Future相当于Promise，而async/await连名字都没改。接下来我们看看通过Future和async/await如何消除上面示例中的嵌套问题。
```dart
#使用Future消除Callback Hell
login("alice","******").then((id){
  	return getUserInfo(id);
}).then((userInfo){
    return saveUserInfo(userInfo);
}).then((e){
   //执行接下来的操作 
}).catchError((e){
  //错误处理  
  print(e);
});
```
正如上文所述， “`Future`的所有 API 的返回值仍然是一个`Future`对象，所以可以很方便的进行链式调用” ，如果在`then`中返回的是一个`Future`的话，该`future`会执行，执行结束后会触发后面的`then`回调，这样依次向下，就避免了层层嵌套。

### 使用async/await消除callback hell
通过`Future`回调中再返回`Future`的方式虽然能避免层层嵌套，但是还是有一层回调，有没有一种方式能够让我们可以像写同步代码那样来执行异步任务而不使用回调的方式？答案是肯定的，这就要使用`async/await`了，下面我们先直接看代码，然后再解释，代码如下：
```dart
task() async {
   try{
    String id = await login("alice","******");
    String userInfo = await getUserInfo(id);
    await saveUserInfo(userInfo);
    //执行接下来的操作   
   } catch(e){
    //错误处理   
    print(e);   
   }  
}
```
`async`用来表示函数是异步的，定义的函数会返回一个`Future`对象，可以使用`then`方法添加回调函数。
`await`后面是一个`Future`，表示等待该异步任务完成，异步完成后才会往下走；`await`必须出现在`async`函数内部。
可以看到，我们通过async/await将一个异步流用同步的代码表示出来了。

其实，无论是在JavaScript还是Dart中，`async/await`都只是一个语法糖，编译器或解释器最终都会将其转化为一个`Promise（Future）`的调用链。

### Stream
`Stream` 也是用于接收异步事件数据，和`Future`不同的是，它可以接收多个异步操作的结果（成功或失败）。 也就是说，在执行异步任务时，可以通过多次触发成功或失败事件来传递结果数据或错误异常。`Stream`常用于会多次读取数据的异步任务场景，如网络内容下载、文件读写等。举个例子：
```dart
Stream.fromFutures([
  // 1秒后返回结果
  Future.delayed(new Duration(seconds: 1), () {
    return "hello 1";
  }),
  // 抛出一个异常
  Future.delayed(new Duration(seconds: 2),(){
    throw AssertionError("Error");
  }),
  // 3秒后返回结果
  Future.delayed(new Duration(seconds: 3), () {
    return "hello 3";
  })
]).listen((data){
   print(data);
}, onError: (e){
   print(e.message);
},onDone: (){

});
```
上面的代码依次会输出：
```dart
I/flutter (17666): hello 1
I/flutter (17666): Error
I/flutter (17666): hello 3
```
代码很简单，就不赘述了。



