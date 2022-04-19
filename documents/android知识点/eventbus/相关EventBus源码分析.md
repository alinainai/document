### 相关介绍

[官方地址: https://github.com/greenrobot/EventBus](https://github.com/greenrobot/EventBus)

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/163953816-f7cfffd7-e2a2-4fc9-aa11-d88fe7102600.png">

添加依赖
```groovy
implementation("org.greenrobot:eventbus:3.3.1")
```
使用
```java
// 1. 定义事件 和 事件接收的方法
public static class MessageEvent { /* Additional fields if needed */ }

@Subscribe(threadMode = ThreadMode.MAIN)  
public void onMessageEvent(MessageEvent event) {
    // Do something
}

// 2. 在 Activity 或者 Fragment 中注册和反注册
@Override
public void onStart() {
    super.onStart();
    EventBus.getDefault().register(this);
}
@Override
public void onStop() {
    super.onStop();
    EventBus.getDefault().unregister(this);
}

// 3. 发送事件
EventBus.getDefault().post(new MessageEvent());
```
### 源码分析
