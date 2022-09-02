本文源码基于 `Android SDK 32` 版本

## 1、简单使用

`Lifecycle` 是 `Jetpack` 的基础组件之一，它可以帮助开发者更好的处理和 `生命周期` 相依赖的业务逻辑。用一种统一的方式来监听 `Activity`、`Fragment`、`Service`甚至是 `Process` 的生命周期变化，且大大减少了业务代码发生`内存泄漏`和 `NPE` 的风险。

### 1.1 使用方法

1.我们可以通过实现`DefaultLifecycleObserver`接口，直接使用对应的生命周期回调方法，代码如下:

```kotlin
class LifeCycleObserver1 : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
    }
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
    }
}
```
2.实现`LifecycleObserver`接口，并通过注解来标记回调方法。在 SDK 32 中已标记为废弃，酌情使用。
```kotlin
class LifeCycleObserver2 : LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy() {
    }
}
```
3.实现 LifecycleEventObserver 接口，在 onStateChanged 方法中监听。
 ```kotlin
class LifeCycleObserver3 : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        
    }
}
```
在Activity中使用
```kotlin
class LifeCycleActivity:AppCompatActivity{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(LifeCycleObserver1())
        lifecycle.addObserver(LifeCycleObserver2())
        lifecycle.addObserver(LifeCycleObserver3())
    }
}
```
## 2、源码分析

<img width="800" alt="UML" src="https://user-images.githubusercontent.com/17560388/188046104-3fbd27c2-0d0c-485f-8b4b-63ecace70512.png">

### 2.1 LifecycleOwner 类

`LifecycleOwner` 是一个单方法接口，只有一个 `Lifecycle getLifecycle()` 方法。`ComponentActivity` 类实现了 `LifecycleOwner` 接口，Fragment 也实现了 LifecycleOwner 接口，`getLifecycle()`方法返回的都是 `Lifecycle` 的子类 `LifecycleRegistry` 实例。

```java
public interface LifecycleOwner {
    @NonNull
    Lifecycle getLifecycle(); // 对外提供一个 Lifecycle 对象
}
```

### 2.2 Lifecycle 类

`Lifecycle` 是一个抽象类，提供了处理 `LifecycleObserver` 的相关方法，截图如下

<img width="437" alt="image" src="https://user-images.githubusercontent.com/17560388/163302734-c3cf855c-1c7e-49b5-a650-8c4724d8b326.png">

另外，Lifecycle 中还有两个内部类：

- `事件 Event`: 当 Activity/Fragment 生命周期变化时会发送响应的事件
- `状态 State`: Lifecycle 根据事件修改持有 Lifecycle 的组件的当前状态。

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
构成 Activity 生命周期的状态和事件

<img width="500" alt="构成 Android Activity 生命周期的状态和事件" src="https://user-images.githubusercontent.com/17560388/163300843-a79fd13a-713d-4300-9077-e40fcffb8038.png">

我们主要看一下 ComponentActivity 类中 Lifecycle 的相关逻辑。

### 2.3 ComponentActivity类

在`ComponentActivity`中`getLifecycle()`返回一个`LifecycleRegistry`对象，LifecycleRegistry 是 Lifecycle 的子类。

在 ComponentActivity 的 onCreate 方法中会注入一个监听生命周期变化的 ReportFragment 类。

```java
public class ComponentActivity extends androidx.core.app.ComponentActivity implements LifecycleOwner{
    
    private final LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);// 初始化 LifecycleRegistry 对象
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ReportFragment.injectIfNeededIn(this);// 注入生命周期监听的 ReportFragment
    }
    
    @Override
    public Lifecycle getLifecycle() { 
        return mLifecycleRegistry; // 返回 LifecycleRegistry 对象
    }
}
```
### 2.4 ReportFragment类

ReportFragment 是一个透明的Fragment，我们看一下 ReportFragment 类的相关代码

```java
public class ReportFragment extends android.app.Fragment {
 
    public static void injectIfNeededIn(Activity activity) {
        if (Build.VERSION.SDK_INT >= 29) {
            LifecycleCallbacks.registerIn(activity);
        }
        android.app.FragmentManager manager = activity.getFragmentManager();
        if (manager.findFragmentByTag(REPORT_FRAGMENT_TAG) == null) {
            manager.beginTransaction().add(new ReportFragment(), REPORT_FRAGMENT_TAG).commit();
            manager.executePendingTransactions();
        }
    }
    ...
    @Override
    public void onPause() {
        super.onPause();
        dispatch(Lifecycle.Event.ON_PAUSE);
    }
    
   private void dispatch(@NonNull Lifecycle.Event event) {
        if (Build.VERSION.SDK_INT < 29) {
            dispatch(getActivity(), event);
        }
    }
    ...
    @RequiresApi(29)
    static class LifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

        static void registerIn(Activity activity) {
            activity.registerActivityLifecycleCallbacks(new LifecycleCallbacks());
        }

        @Override
        public void onActivityPostCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            dispatch(activity, Lifecycle.Event.ON_CREATE);
        }
        ...
    }
}
```
在 ReportFragment 类中，会根据 SDK_INT 的版本采用不同的方案来监听 Activity 生命周期变化的回调

- 在 `SDK_INT >= 29` 通过 Application.ActivityLifecycleCallbacks 的回调分发事件
- 在 `SDK_INT < 29` 通过 Fragment 生命周期的回调分发事件

不管哪种方式最终都是通过 `ReportFragment` 中的静态方法 `ReportFragment#dispatch(activity,event)` 实现件的分发

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
`dispatch(activity,event)` 方法内部继续调用 `LifecycleRegistry#handleLifecycleEvent(event)`方法进行事件分发。

### 2.5 LifecycleRegistry类

`LifecycleRegistry` 是 `Lifecycle` 的子类，内部持有一个 `LifecycleObserver:ObserverWithState`的 Map 容器用来存储 `addObserver` 的 `LifecycleObserver`。

```java
public class LifecycleRegistry extends Lifecycle {

    // 持有 LifecycleObserver 的容器
    private FastSafeIterableMap<LifecycleObserver, ObserverWithState> mObserverMap =  new FastSafeIterableMap<>();
           
    private State mState; // LifecycleRegistry 的当前状态
    private final WeakReference<LifecycleOwner> mLifecycleOwner; // LifecycleOwner 的弱引用，防止泄漏

    private LifecycleRegistry(@NonNull LifecycleOwner provider, boolean enforceMainThread) {
        mLifecycleOwner = new WeakReference<>(provider);
        mState = INITIALIZED;
        mEnforceMainThread = enforceMainThread;
    }
    
    @Override
    public void addObserver(@NonNull LifecycleObserver observer) {
        State initialState = mState == DESTROYED ? DESTROYED : INITIALIZED;
        ObserverWithState statefulObserver = new ObserverWithState(observer, initialState); // 将 observer 和 state 包装为 ObserverWithState 对象
        ObserverWithState previous = mObserverMap.putIfAbsent(observer, statefulObserver); // 将 observer:ObserverWithState 存入 mObserverMap
        ...
    }
```
在 LifecycleRegistry#addObserver 方法中将 observer 和 state 包装成一个 ObserverWithState 对象，再把它 put 到 mObserverMap 中。创建一个 observer:ObserverWithState 的映射。

```java
static class ObserverWithState { // ObserverWithState 类
    State mState;
    LifecycleEventObserver mLifecycleObserver;
    
    ObserverWithState(LifecycleObserver observer, State initialState) {
        // 使用 Lifecycling 的 lifecycleEventObserver 方法，将传入的 LifecycleObserver 转换为 LifecycleEventObserver 类
        mLifecycleObserver = Lifecycling.lifecycleEventObserver(observer);
        mState = initialState;
    }
    
    void dispatchEvent(LifecycleOwner owner, Event event) {
        State newState = event.getTargetState();
        mState = min(mState, newState);
        mLifecycleObserver.onStateChanged(owner, event);
        mState = newState;
    }
}
```
`ObserverWithState` 构造中，使用 `Lifecycling#lifecycleEventObserver(LifecycleObserver)` 方法，将传入的 LifecycleObserver 转换为一个 LifecycleEventObserver 对象。 

LifecycleEventObserver 是一个接口，Owner 生命周期变化的的时候触发 onStateChanged 方法。

```java
public interface LifecycleEventObserver extends LifecycleObserver {
    void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event);
}
```
我们再看一下 Lifecycling#lifecycleEventObserver 方法，我们简化一下，只看 object 是 FullLifecycleObserver 类型的情况：
```java
public class Lifecycling {
    @NonNull
    static LifecycleEventObserver lifecycleEventObserver(Object object) {
        boolean isLifecycleEventObserver = object instanceof LifecycleEventObserver;
        boolean isFullLifecycleObserver = object instanceof FullLifecycleObserver;
        ...
        if (isFullLifecycleObserver) { // 采用适配器模式将传入的 FullLifecycleObserver 转换为一个 LifecycleEventObserver对象
            return new FullLifecycleObserverAdapter((FullLifecycleObserver) object, null); 
        }
    }
}
```
FullLifecycleObserverAdapter 代码，典型的适配器模式
```java
class FullLifecycleObserverAdapter implements LifecycleEventObserver {
    private final FullLifecycleObserver mFullLifecycleObserver;
    private final LifecycleEventObserver mLifecycleEventObserver;
    
    FullLifecycleObserverAdapter(FullLifecycleObserver fullLifecycleObserver, LifecycleEventObserver lifecycleEventObserver) {
        mFullLifecycleObserver = fullLifecycleObserver;
        mLifecycleEventObserver = lifecycleEventObserver;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                mFullLifecycleObserver.onCreate(source);
                break;
            case ON_START:
                mFullLifecycleObserver.onStart(source);
                break;
            case ON_RESUME:
                mFullLifecycleObserver.onResume(source);
                break;
            case ON_PAUSE:
                mFullLifecycleObserver.onPause(source);
                break;
            case ON_STOP:
                mFullLifecycleObserver.onStop(source);
                break;
            case ON_DESTROY:
                mFullLifecycleObserver.onDestroy(source);
                break;
            case ON_ANY:
                throw new IllegalArgumentException("ON_ANY must not been send by anybody");
        }
        if (mLifecycleEventObserver != null) {
            mLifecycleEventObserver.onStateChanged(source, event);
        }
    }
```


##  3、事件分发

在 Re

```java
public void handleLifecycleEvent(@NonNull Lifecycle.Event event) {
    enforceMainThreadIfNeeded("handleLifecycleEvent");
    moveToState(event.getTargetState());
}

private void moveToState(State next) {
    if (mState == next) { //如果当前状态和 next 相同直接 return
        return;
    }
    mState = next;
    if (mHandlingEvent || mAddingObserverCounter != 0) {
        mNewEventOccurred = true;
        // we will figure out what to do on upper level.
        return;
    }
    mHandlingEvent = true;
    sync(); //将 mState 同步给 Observer 中的 state
    mHandlingEvent = false;
}
  
private void sync() {
    LifecycleOwner lifecycleOwner = mLifecycleOwner.get();
    while (!isSynced()) {
        mNewEventOccurred = false;
        // no need to check eldest for nullability, because isSynced does it for us.
        if (mState.compareTo(mObserverMap.eldest().getValue().mState) < 0) {
            backwardPass(lifecycleOwner); // 事件分发
        }
        Map.Entry<LifecycleObserver, ObserverWithState> newest = mObserverMap.newest();
        if (!mNewEventOccurred && newest != null && mState.compareTo(newest.getValue().mState) > 0) {
            forwardPass(lifecycleOwner); // 事件分发
        }
    }
    mNewEventOccurred = false;
}
```
在 sync 方法中继续调用 forwardPass 和 backwardPass 方法实现事件的同步
```java
private void forwardPass(LifecycleOwner lifecycleOwner) {
    Iterator<Map.Entry<LifecycleObserver, ObserverWithState>> ascendingIterator = mObserverMap.iteratorWithAdditions();
    while (ascendingIterator.hasNext() && !mNewEventOccurred) {
        Map.Entry<LifecycleObserver, ObserverWithState> entry = ascendingIterator.next();
        ObserverWithState observer = entry.getValue();
        while ((observer.mState.compareTo(mState) < 0 && !mNewEventOccurred && mObserverMap.contains(entry.getKey()))) {
            pushParentState(observer.mState);
            final Event event = Event.upFrom(observer.mState);
            if (event == null) { throw new IllegalStateException("no event up from " + observer.mState); }
            observer.dispatchEvent(lifecycleOwner, event); //核心方法
            popParentState();
        }
    }
}

private void backwardPass(LifecycleOwner lifecycleOwner) {
    Iterator<Map.Entry<LifecycleObserver, ObserverWithState>> descendingIterator = mObserverMap.descendingIterator();
    while (descendingIterator.hasNext() && !mNewEventOccurred) {
        Map.Entry<LifecycleObserver, ObserverWithState> entry = descendingIterator.next();
        ObserverWithState observer = entry.getValue();
        while ((observer.mState.compareTo(mState) > 0 && !mNewEventOccurred && mObserverMap.contains(entry.getKey()))) {
            Event event = Event.downFrom(observer.mState);
            if (event == null) { throw new IllegalStateException("no event down from " + observer.mState); }
            pushParentState(event.getTargetState()); // 改变状态
            observer.dispatchEvent(lifecycleOwner, event); //事件分发核心方法
            popParentState();
        }
    }
}
```
`backwardPass` 和 `forwardPass` 会调用 `Evnet`类中处理 `State` 和 `Event` 关系的方法:
```java
public enum Event {
    @Nullable
    public static Event downFrom(@NonNull State state) {
        switch (state) {
            case CREATED:
                return ON_DESTROY;
            case STARTED:
                return ON_STOP;
            case RESUMED:
                return ON_PAUSE;
            default:
                return null;
        }
    }

    @Nullable
    public static Event downTo(@NonNull State state) {
        switch (state) {
            case DESTROYED:
                return ON_DESTROY;
            case CREATED:
                return ON_STOP;
            case STARTED:
                return ON_PAUSE;
            default:
                return null;
        }
    }

    @Nullable
    public static Event upFrom(@NonNull State state) {
        switch (state) {
            case INITIALIZED:
                return ON_CREATE;
            case CREATED:
                return ON_START;
            case STARTED:
                return ON_RESUME;
            default:
                return null;
        }
    }

    @Nullable
    public static Event upTo(@NonNull State state) {
        switch (state) {
            case CREATED:
                return ON_CREATE;
            case STARTED:
                return ON_START;
            case RESUMED:
                return ON_RESUME;
            default:
                return null;
        }
    }

    @NonNull
    public State getTargetState() {
        switch (this) {
            case ON_CREATE:
            case ON_STOP:
                return State.CREATED;
            case ON_START:
            case ON_PAUSE:
                return State.STARTED;
            case ON_RESUME:
                return State.RESUMED;
            case ON_DESTROY:
                return State.DESTROYED;
            case ON_ANY:
                break;
        }
        throw new IllegalArgumentException(this + " has no target state");
    }
}
```
LifecycleRegistry 事件分发的方法调用顺序：

>`handleLifecycleEvent` -> `moveToState` -> `sync()` -> `backwardPass/forwardPass` -> `ObserverWithState#dispatchEvent`

## 总结

LifeCycle 让我们更好的处理和 `Activity/Fragment` 生命周期相关的代码逻辑，不用再去 `onCreate/onResume...` 中做回调。使用方式很简单，在 onCreate 方法中注册 lifecycle.addObserver(LifeCycleObserver())，然后将代码逻辑放到注册的 LifeCycleObserver 中，实现和 Activity 的解耦。

源码也很简单，`Activity/Fragment` 内部维护一个 `LifecycleRegistry` 类，将注册的 `LifeCycleObserver` 添加到 `LifecycleRegistry` 的内部容器中。
在 `Activity/Fragment` 中插入一个 `ReportFragment` 对象，当 `Activity/Fragment` 生命周期变化时调用 `ReportFragment#dispatch(activity,event)` 实现事件分发。

SDK>=29时:在 Activity 中注册 `Application.ActivityLifecycleCallbacks`，然后在对应的方法中发送 Event。
SDK<29时: 在 ReportFragment 的生命周期发送相应的 Event。

`ReportFragment#dispatch(activity,event)` 内部又调用 `activity.getLifecycle().handleLifecycleEvent(event)` 将操作又回调给 `LifecycleRegistry` 类。

`LifecycleRegistry` 类中的内部容器持有 `LifeCycleObserver:ObserverWithState` 的 `key-value` 映射，`LifecycleRegistry` 通过调用 `ObserverWithState#dispatchEvent(owner,event)` 方法来实现 `LifeCycleObserver` 中的方法回调。


## 参考
- [使用生命周期感知型组件处理生命周期](https://developer.android.com/topic/libraries/architecture/lifecycle?hl=zh-cn#implementing-lco)
- [Lifecycle源码解析](https://zhuanlan.zhihu.com/p/461750106)
- [Lifecycle 源码分析](https://juejin.cn/post/7031787495985512461)
