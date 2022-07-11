## 1. 什么是 LifeCycle

`Lifecycle` 是 `Jetpack` 的基础的组件之一，它让开发者搭建依赖于`生命周期变化`的业务逻辑变的更简单，用一种统一的方式来监听 `Activity`、`Fragment`、`Service`甚至是 `Process` 的生命周期变化，且大大减少了业务代码发生`内存泄漏`和 `NPE` 的风险。

## 2. 使用方法

1.通过实现`DefaultLifecycleObserver`接口，直接使用对应的 onXXX 方法

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
2.实现`LifecycleObserver`接口，通过注解方式实现方法的回调
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
## 3.Lifecycle 源码分析

`CompatActivity`类本身实现`LifecycleOwner`接口，`LifecycleOwner`是单一方法接口，表示类持有`Lifecycle`。

```java
public interface LifecycleOwner {
    @NonNull
    Lifecycle getLifecycle();
}
```

`Lifecycle` 是一个抽象类，用于存储有关组件（如 `Activity` 或 `Fragment`）的`生命周期状态`的信息，并允许其他对象观察此状态。

`Lifecycle` 使用两种枚举来表示其关联组件的生命周期状态：

- 事件 Event
- 状态 State

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

`Lifecycle`还有添加、移除` LifecycleObserver` ，以及记录 State 的功能

<img width="437" alt="image" src="https://user-images.githubusercontent.com/17560388/163302734-c3cf855c-1c7e-49b5-a650-8c4724d8b326.png">

构成 Activity 生命周期的状态和事件

<img width="500" alt="构成 Android Activity 生命周期的状态和事件" src="https://user-images.githubusercontent.com/17560388/163300843-a79fd13a-713d-4300-9077-e40fcffb8038.png">

## 4. LifecycleRegistry 对象

在 CompatActivity 中 getLifecycle() 返回一个 LifecycleRegistry 对象

```java
public class ComponentActivity extends androidx.core.app.ComponentActivity implements LifecycleOwner{

    //初始化 LifecycleRegistry 对象
    private final LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // 注册生命周期监听的 ReportFragment
        ReportFragment.injectIfNeededIn(this);
    }
    
    @Override
    public Lifecycle getLifecycle() { 
        // 返回 LifecycleRegistry 对象
        return mLifecycleRegistry;
    }
}
```

可以先看下 ReportFragment 类，Activity 生命周期的回调通过该类实现：

- 在 API >= 29 通过 Application.ActivityLifecycleCallbacks 的回调分发时间
- 在 API < 29 通过 Fragment 生命周期的回调分发事件

最终事件的分发通过 ReportFragment 中的静态方法 dispatch 实现，在 dispatch 方法中又调用 LifecycleRegistry#handleLifecycleEvent
```java
static void dispatch(@NonNull Activity activity, @NonNull Lifecycle.Event event) {
    if (activity instanceof LifecycleRegistryOwner) {
        ((LifecycleRegistryOwner) activity).getLifecycle().handleLifecycleEvent(event);
        return;
    }

    if (activity instanceof LifecycleOwner) {
        Lifecycle lifecycle = ((LifecycleOwner) activity).getLifecycle();
        if (lifecycle instanceof LifecycleRegistry) {
            ((LifecycleRegistry) lifecycle).handleLifecycleEvent(event);
        }
    }
}
```
LifecycleRegistry 是 Lifecycle 的实现类，它持有多个 LifecycleObserver 监听器。

```java
public class LifecycleRegistry extends Lifecycle {

    // 持有 LifecycleObserver 的容器，ObserverWithState 类将 State 和 LifecycleObserver 包装到一起，并通过该类的 dispatchEvent 方法实现事件的分发
    private FastSafeIterableMap<LifecycleObserver, ObserverWithState> mObserverMap =  new FastSafeIterableMap<>();
           
    // Current state
    private State mState;

    // LifecycleOwner 弱引用
    private final WeakReference<LifecycleOwner> mLifecycleOwner;

    public void handleLifecycleEvent(@NonNull Lifecycle.Event event) {
        enforceMainThreadIfNeeded("handleLifecycleEvent");
        moveToState(event.getTargetState());
    }

    private void moveToState(State next) {
        if (mState == next) {
            return;
        }
        mState = next;
        if (mHandlingEvent || mAddingObserverCounter != 0) {
            mNewEventOccurred = true;
            // we will figure out what to do on upper level.
            return;
        }
        mHandlingEvent = true;
        sync(); //实现 mState 和 Observer 中的 mState 通过
        mHandlingEvent = false;
    }
    
   private void forwardPass(LifecycleOwner lifecycleOwner) {
        Iterator<Map.Entry<LifecycleObserver, ObserverWithState>> ascendingIterator =
                mObserverMap.iteratorWithAdditions();
        while (ascendingIterator.hasNext() && !mNewEventOccurred) {
            Map.Entry<LifecycleObserver, ObserverWithState> entry = ascendingIterator.next();
            ObserverWithState observer = entry.getValue();
            while ((observer.mState.compareTo(mState) < 0 && !mNewEventOccurred
                    && mObserverMap.contains(entry.getKey()))) {
                pushParentState(observer.mState);
                final Event event = Event.upFrom(observer.mState);
                if (event == null) {
                    throw new IllegalStateException("no event up from " + observer.mState);
                }
                observer.dispatchEvent(lifecycleOwner, event); //核心方法
                popParentState();
            }
        }
    }

    private void backwardPass(LifecycleOwner lifecycleOwner) {
        Iterator<Map.Entry<LifecycleObserver, ObserverWithState>> descendingIterator =
                mObserverMap.descendingIterator();
        while (descendingIterator.hasNext() && !mNewEventOccurred) {
            Map.Entry<LifecycleObserver, ObserverWithState> entry = descendingIterator.next();
            ObserverWithState observer = entry.getValue();
            while ((observer.mState.compareTo(mState) > 0 && !mNewEventOccurred
                    && mObserverMap.contains(entry.getKey()))) {
                Event event = Event.downFrom(observer.mState);
                if (event == null) {
                    throw new IllegalStateException("no event down from " + observer.mState);
                }
                pushParentState(event.getTargetState()); // 改变状态
                observer.dispatchEvent(lifecycleOwner, event); //事件分发核心方法
                popParentState();
            }
        }
    }

    // happens only on the top of stack (never in reentrance),
    // so it doesn't have to take in account parents
    private void sync() {
        LifecycleOwner lifecycleOwner = mLifecycleOwner.get();
        if (lifecycleOwner == null) {
            throw new IllegalStateException("LifecycleOwner of this LifecycleRegistry is already"
                    + "garbage collected. It is too late to change lifecycle state.");
        }
        while (!isSynced()) {
            mNewEventOccurred = false;
            // no need to check eldest for nullability, because isSynced does it for us.
            if (mState.compareTo(mObserverMap.eldest().getValue().mState) < 0) {
                backwardPass(lifecycleOwner); // 事件分发
            }
            Map.Entry<LifecycleObserver, ObserverWithState> newest = mObserverMap.newest();
            if (!mNewEventOccurred && newest != null
                    && mState.compareTo(newest.getValue().mState) > 0) {
                forwardPass(lifecycleOwner); // 事件分发
            }
        }
        mNewEventOccurred = false;
    }
```
LifecycleRegistry 事件分发的方法调用顺序 ：

handleLifecycleEvent -> moveToState -> sync() -> backwardPass/forwardPass -> ObserverWithState#dispatchEvent


## 参考

[使用生命周期感知型组件处理生命周期 ](https://developer.android.com/topic/libraries/architecture/lifecycle?hl=zh-cn#implementing-lco)

[Lifecycle源码解析](https://zhuanlan.zhihu.com/p/461750106)

[Lifecycle 源码分析](https://juejin.cn/post/7031787495985512461)
