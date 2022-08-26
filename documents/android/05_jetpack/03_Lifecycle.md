## 1、基本使用

### 1.1 什么是 LifeCycle

`Lifecycle` 是 `Jetpack` 的基础的组件之一，它提供了开发者用来搭建依赖于 `生命周期变化` 的业务逻辑变更的能力。用一种统一的方式来监听 `Activity`、`Fragment`、`Service`甚至是 `Process` 的生命周期变化，且大大减少了业务代码发生`内存泄漏`和 `NPE` 的风险。

### 1.2 使用方法

1.我们可以通过实现`DefaultLifecycleObserver`接口，直接使用对应的 onXXX 方法，代码如下:

```kotlin
class LifeCycleObserver1 : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.e("life", "LifeCycleObserver1 onStart")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.e("life", "LifeCycleObserver1 onDestroy")
    }
}
```
2.实现`LifecycleObserver`接口，并通过注解来标记回调方法
```kotlin
class LifeCycleObserver2 : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        Log.e("life", "LifeCycleObserver2 start")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy() {
        Log.e("life", "LifeCycleObserver2 destroy")
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
    }
}
```
## 2、源码分析

### 2.1 LifecycleOwner 类

`LifecycleOwner` 是一个单方法接口，只有一个 `Lifecycle getLifecycle()` 方法。

```java
public interface LifecycleOwner {
    @NonNull
    Lifecycle getLifecycle();
}
```
`ComponentActivity` 类实现了 `LifecycleOwner` 接口。表示 `ComponentActivity` 类中具有 `Lifecycle` 对象。

### 2.2 Lifecycle 类

`Lifecycle` 是一个抽象类，用于存储有关组件（如 `Activity` 或 `Fragment`）的`生命周期的状态信息`，并允许其他对象观察此状态。

<img width="437" alt="image" src="https://user-images.githubusercontent.com/17560388/163302734-c3cf855c-1c7e-49b5-a650-8c4724d8b326.png">

`Lifecycle`中定义了添加、移除`LifecycleObserver`的方法，并使用两种枚举来表示其关联的组件的生命周期状态：

- `事件 Event`: 从框架和 Lifecycle 类分派的生命周期事件。这些事件映射到 activity 和 fragment 中的回调事件。
- `状态 State`: 由 Lifecycle 对象跟踪的组件的当前状态。

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

### 2.3 ComponentActivity类

在`ComponentActivity`中 `getLifecycle()` 返回一个 `LifecycleRegistry` 对象

```java
public class ComponentActivity extends androidx.core.app.ComponentActivity implements LifecycleOwner{

    //初始化 LifecycleRegistry 对象
    private final LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // 注入生命周期监听的 ReportFragment
        ReportFragment.injectIfNeededIn(this);
    }
    
    @Override
    public Lifecycle getLifecycle() { 
        return mLifecycleRegistry; // 返回 LifecycleRegistry 对象
    }
}
```
### 2.4 ReportFragment类

在 `ComponentActivity#onCreate` 方法中注入了一个 ReportFragment 对象

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
- 在 `SDK_INT >= 29` 通过 Application.ActivityLifecycleCallbacks 的回调分发事件
- 在 `SDK_INT < 29` 通过 Fragment 生命周期的回调分发事件

不管哪种方式最终都是通过 `ReportFragment` 中的静态方法 `dispatch(activity,event)` 实现件的分发

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
`dispatch(activity,event)` 方法内部继续调用 `LifecycleRegistry#handleLifecycleEvent`方法进行事件分发。


### 2.5 LifecycleRegistry类

`LifecycleRegistry` 是 `Lifecycle` 的子类，内部持有一个 `LifecycleObserver:ObserverWithState`的 Map 容器。

```java
public class LifecycleRegistry extends Lifecycle {

    // 持有 LifecycleObserver 的容器，ObserverWithState 类将 State 和 LifecycleObserver 包装到一起，并通过该类的 dispatchEvent 方法实现事件的分发
    private FastSafeIterableMap<LifecycleObserver, ObserverWithState> mObserverMap =  new FastSafeIterableMap<>();
           
    private State mState; // Current state
     
    private final WeakReference<LifecycleOwner> mLifecycleOwner; // LifecycleOwner 弱引用

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
        sync(); //实现 mState 和 Observer 中的 mState 同步
        mHandlingEvent = false;
    }
    
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

    private void sync() {
        LifecycleOwner lifecycleOwner = mLifecycleOwner.get();
        while (!isSynced()) {
            mNewEventOccurred = false;
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
LifecycleRegistry 事件分发的方法调用顺序 ：

>`handleLifecycleEvent` -> `moveToState` -> `sync()` -> `backwardPass/forwardPass` -> `ObserverWithState#dispatchEvent`

## 参考
- [使用生命周期感知型组件处理生命周期 ](https://developer.android.com/topic/libraries/architecture/lifecycle?hl=zh-cn#implementing-lco)
- [Lifecycle源码解析](https://zhuanlan.zhihu.com/p/461750106)
- [Lifecycle 源码分析](https://juejin.cn/post/7031787495985512461)
