### 1. 什么是 LifeCycle

Lifecycle 是 Jetpack 的基础的组件之一，让开发者搭建依赖于生命周期变化的业务逻辑变的更简单，用一种统一的方式来监听 Activity、Fragment、Service、甚至是 Process 的生命周期变化，且大大减少了业务代码发生内存泄漏和 NPE 的风险

### 2. 源码分析

CompatActivity 本身实现 LifecycleOwner 接口，LifecycleOwner 是单一方法接口，表示类具有 Lifecycle。

```java
public interface LifecycleOwner {
    @NonNull
    Lifecycle getLifecycle();
}
```

Lifecycle 是一个抽象类，用于存储有关组件（如 Activity 或 Fragment）的生命周期状态的信息，并允许其他对象观察此状态。

Lifecycle 使用两种枚举跟踪其关联组件的生命周期状态

事件 Event

```java
public enum Event {

    ON_CREATE,
    ON_START,
    ON_RESUME,
    ON_PAUSE,
    ON_STOP,
    ON_DESTROY,
    //可以响应任意一个事件
    ON_ANY
    
}
```

状态 State

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




在 CompatActivity 中 getLifecycle() 返回一个 LifecycleRegistry 对象

### 参考

[使用生命周期感知型组件处理生命周期 ](https://developer.android.com/topic/libraries/architecture/lifecycle?hl=zh-cn#implementing-lco)

[Lifecycle源码解析](https://zhuanlan.zhihu.com/p/461750106)
