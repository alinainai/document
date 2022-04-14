### 1. 什么是 LifeCycle

Lifecycle 是 Jetpack 的基础的组件之一，让开发者搭建依赖于生命周期变化的业务逻辑变的更简单，用一种统一的方式来监听 Activity、Fragment、Service、甚至是 Process 的生命周期变化，且大大减少了业务代码发生内存泄漏和 NPE 的风险

### 2. 使用方法

实现 DefaultLifecycleObserver 接口，直接使用对应的 onXXX 方法
```kotlin
class LifeCycleObserver1 : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.e("life", "TestLifeCycleObserver1 onStart")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.e("life", "TestLifeCycleObserver1 onDestroy")
    }
}
```
实现 LifecycleObserver 接口，通过注解方式实现方法的回调
```kotlin
class LifeCycleObserver2 : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        Log.e("life", "TestLifeCycleObserver1 start")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy() {
        Log.e("life", "TestLifeCycleObserver1 destroy")
    }
}
```
使用
```kotlin
LifecycleOwner.getLifecycle().addObserver(LifeCycleObserver1())
LifecycleOwner.getLifecycle().addObserver(LifeCycleObserver2())
```
### 3. 源码分析

CompatActivity 本身实现 LifecycleOwner 接口，LifecycleOwner 是单一方法接口，表示类持有 Lifecycle。

```java
public interface LifecycleOwner {
    @NonNull
    Lifecycle getLifecycle();
}
```

Lifecycle 是一个抽象类，用于存储有关组件（如 Activity 或 Fragment）的生命周期状态的信息，并允许其他对象观察此状态。

Lifecycle 使用两种枚举跟踪其关联组件的生命周期状态：事件 Event、状态 State

```java
public enum Event {
    ON_CREATE,
    ON_START,
    ON_RESUME,
    ON_PAUSE,
    ON_STOP,
    ON_DESTROY,   
    ON_ANY  //可以响应任意一个事件 
}
```
``` java
//生命周期状态. （Event是进入这种状态的事件）
public enum State {
    DESTROYED,
    INITIALIZED,
    CREATED,
    STARTED,
    RESUME
    
    public boolean isAtLeast(@NonNull State state) {
        return compareTo(state) >= 0;
    }
}
```

事件驱动状态改变

<img width="500" alt="构成 Android Activity 生命周期的状态和事件" src="https://user-images.githubusercontent.com/17560388/163300491-c5b34a2b-ac28-4c5f-b518-278fc5f0bd4c.png">

在 CompatActivity 中 getLifecycle() 返回一个 LifecycleRegistry 对象



### 参考

[使用生命周期感知型组件处理生命周期 ](https://developer.android.com/topic/libraries/architecture/lifecycle?hl=zh-cn#implementing-lco)

[Lifecycle源码解析](https://zhuanlan.zhihu.com/p/461750106)
