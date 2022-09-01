## 1、简单使用

官方说明：LiveData 是一种可观察的数据存储器类。与常规的可观察类不同，LiveData 具有生命周期感知能力，意指它遵循其他应用组件（如 activity、fragment 或 service）的生命周期。这种感知能力可确保 LiveData 仅更新处于活跃生命周期状态的应用组件观察者。

`LiveData` 在 `MVVM` 中扮演着 `VM` 和 `View` 通信的角色。一般我们在 `ViewModel` 中针对数据会创建两个对应的 `LiveData`。
- 一个是 `LiveData` 类型，内部使用，
- 一个是 `MutableLiveData` 类型，暴露给 `Activity/Fragment`。

### 1.1 使用代码

```kotlin
class MV:ViewModle(){
    // 该类对外公开了 postValue 和 setValue 方法，在 ViewModle 内部负责对持有数据的修改
    private val _loginData = MutableLiveData<<LoginBean>>() 
    //LiveData 暴露给 View 层，这个类的 postValue 和 setValue 被 protect 修饰的，可以防止从 View 层修改数据
    val loginData: LiveData<Resource<LoginResponse>> 
        get() = _loginData 
    fun loadUserBean(){
        //从数据仓库获取数据后，在主线程中调用 setValue 赋值。一般setValue 要在主线程中调用
        _loginData.value = LoginBean()
    }
}
```
然后在 `Activity/Fragment` 使用，当数据发生变化的时候会自动回调 `observe(owner,observer)` 方法，2个具体参数后面分析的源码的时候在讲。
```kotlin
class MainActivity:AppCompatActivity(){
    private val mMv:MV by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMv.loginData.observe(this){data->...}
    }
}
```
注意注册的时机，一般在 onCreate 的中注册订阅事件

### 1.2 LiveData的特点

 1. 采用观察者模式，数据发生改变，可以自动回调。
 2. 不需要手动处理生命周期，不会因为 Activity 的销毁重建而丢失数据。
 3. 可以使用单例模式扩展 LiveData 对象以封装系统服务，以便在应用共享资源。

### 1.3 数据变化回调的时机
- LiveData 在数据发生更改时给`活跃的观察者`发送更新。
- 观察者从`非活跃状态更改为活跃状态时`也会收到更新。如果观察者`第二次从非活跃状态更改为活跃状态`，则`只有在自上次变为活跃状态以来值发生了更改时，才会收到更新`。

## 2、源码分析



### 2.1 LiveData 源码

```java
public abstract class LiveData<T> {
    private SafeIterableMap<Observer<? super T>, ObserverWrapper> mObservers = new SafeIterableMap<>(); //Observer 的容器 
    private volatile Object mData; // 存储value
    ...
    protected void postValue(T value) {
       ...
    }
    @MainThread
    protected void setValue(T value) {
       ...
    }
}
```
MultableLiveData 是 LiveData 子类，并对外公开了 postValue 和 setValue 的方法。

```java
public class MutableLiveData<T> extends LiveData<T> {
    //将 LiveData 中 protected 修饰符变为 public，对外暴露 postValue 方法
    @Override
    public void postValue(T value) { 
        super.postValue(value);
    }
    //将 LiveData 中 protected 修饰符变为 public，对外暴露 setValue 方法
    @Override
    public void setValue(T value) { 
        super.setValue(value);
    }
}
```

### 2.2 Observer 接口

我们在通过 `LiveData#observe(owner,observer)` 方法传入一个 `Observer` 接口。Observer 接口如下：

```java
public interface Observer<T> {
    void onChanged(@Nullable T t);
}
```

### 2.3 `LiveData#observe(...)`方法

LiveData 中的 `observe()` 和 `observeForever()` 都可以完成 Observer 的注册。

- observe(owner,observer) 方法只有在 owner 是 active 之后 ，数据发生改变才会触发 observer.onChanged()
- observeForever(observer) 方法没有传入 owner 对象，

```java
public abstract class LiveData<T> {
    ...
    // 只有在 owner 是 active 之后 ，数据发生改变才会触发 observer.onChanged()
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer) {}

    // 无论何时，只要数据发生改变，就会触发 observer.onChanged()，我们暂时先不关心这个方法
    public void observeForever(@NonNull Observer<T> observer) {}
    ... 
}
```
我们重点看一下 `LiveData#observe(...)` 的实现过程，`observe(...)` 方法的2个参数：
- LifecycleOwner: `Lifecycle`的持有类，一般是 `Activity/Fragment`
- Observer: 监听器，通过 onChanged 方法监听数据变化

```kotlin
@MainThread 
public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
    assertMainThread("observe"); // observe 方法要在主线程中调用
    if (owner.getLifecycle().getCurrentState() == DESTROYED) { // 如果 ower 销毁了，直接返回
        return;
    }
    LifecycleBoundObserver wrapper = new LifecycleBoundObserver(owner, observer); 
    ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);
    if (existing != null && !existing.isAttachedTo(owner)) {
        throw new IllegalArgumentException("Cannot add the same observer with different lifecycles");
    }
    if (existing != null) {
        return;
    }
    //LifecycleBoundObserver 实现了 LifecycleEventObserver 接口，将 wrapper(LifecycleBoundObserver) 和 owner 建立生命周期的订阅关系。
    owner.getLifecycle().addObserver(wrapper);
}
```
我们通过 `LiveData#observe(...)` 方法实现了 Observer 和 LiveData 的订阅。在该方法内部将 owner 和 observer 包装为一个 `LifecycleBoundObserver` 对象，并将wrapper(LifecycleBoundObserver) 和 owner 建立生命周期的订阅关系。

## 3、LifecycleBoundObserver 类中的逻辑

LifecycleBoundObserver 继承自 `ObserverWrapper` 并实现了 `LifecycleEventObserver` 接口。当 owner 的生命周期变化时会触发 `LifecycleBoundObserver#onStateChanged(source,event)` 的回调。我们看下 LifecycleBoundObserver 类和该类中的方法。

### 3.1 LifecycleBoundObserver 类
```java
class LifecycleBoundObserver extends ObserverWrapper implements LifecycleEventObserver {
    @NonNull
    final LifecycleOwner mOwner;
 
    @Override
    boolean shouldBeActive() {//判断 mOwner 是否是激活状态，即 mOwner 的 Lifecycle.state 至少是 STARTED(即 STARTED、RESUMED)
        return mOwner.getLifecycle().getCurrentState().isAtLeast(STARTED);
    }
    
    @Override 
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) { // 生命周期变化时触发该方法
        Lifecycle.State currentState = mOwner.getLifecycle().getCurrentState();
        if (currentState == DESTROYED) { 
            removeObserver(mObserver); // Removes the given observer from the observers list.
            return;
        }
        Lifecycle.State prevState = null;
        while (prevState != currentState) {// 状态变化后
            prevState = currentState;
            activeStateChanged(shouldBeActive()); // 继续回调 activeStateChanged 方法
            currentState = mOwner.getLifecycle().getCurrentState();
        }
    }

    @Override
    void detachObserver() {
        mOwner.getLifecycle().removeObserver(this);
    }
}
```
在 `onStateChanged(...)` 方法中会继续调用`父类(ObserverWrapper)`的 `ObserverWrapper#activeStateChanged(newActive)` 方法:

```kotlin
private abstract class ObserverWrapper {

    final Observer<? super T> mObserver;
    boolean mActive;
    int mLastVersion = START_VERSION; // -1

    ObserverWrapper(Observer<? super T> observer) {
        mObserver = observer;
    }

    void activeStateChanged(boolean newActive) {
        if (newActive == mActive) { // 1、激活状态没有发生变化，直接返回。
            return;
        }
        // immediately set active state, so we'd never dispatch anything to inactive owner。
        mActive = newActive; // 立即更新 active 状态，防止分发逻辑被拦截
        changeActiveCounter(mActive ? 1 : -1); // 更新 mActiveCount，并根据条件判断是否回调 LiveData#onActive() 和 LiveData#onInactive()
        if (mActive) { //如果 ObserverWrapper 是 active 状态，调用 LiveData#dispatchingValue(ObserverWrapper) 进行分发，并将 ObserverWrapper#this 作为参数传入
            dispatchingValue(this);
        }
    }
}
// LiveData#changeActiveCounter
@MainThread
void changeActiveCounter(int change) {
    int previousActiveCount = mActiveCount;
    mActiveCount += change;
    if (mChangingActiveState) {
        return;
    }
    mChangingActiveState = true;
    try {
        while (previousActiveCount != mActiveCount) {
            boolean needToCallActive = previousActiveCount == 0 && mActiveCount > 0; //根据 mActiveCount 是否为 0 判断当前 LiveData 是否是激活状态
            boolean needToCallInactive = previousActiveCount > 0 && mActiveCount == 0;
            previousActiveCount = mActiveCount;
            if (needToCallActive) {
                onActive(); // 从 Inactive 变为 Active 时回调该方法，LiveData中是空函数，可根据需要进行重写 
            } else if (needToCallInactive) {
                onInactive(); // // 从 Active 变为 Inactive 时回调该方法，LiveData中是空函数，可根据需要进行重写 
            }
        }
    } finally {
        mChangingActiveState = false;
    }
}
```
当 Ower 的状态至少是 STARTED 时（ObserverWrapper 的 mActive 为 true）会继续调用 `LiveData#dispatchingValue(ObserverWrapper)` 方法，并将 `this` 作为参数传入。我们继续往下看 `dispatchingValue(this)` 的逻辑。

### 3.2 LiveData#dispatchingValue 方法

进入到 `LiveData#dispatchingValue(ObserverWrapper)` 方法

```kotlin
void dispatchingValue(@Nullable ObserverWrapper initiator) { // 1、从 ObserverWrapper#activeStateChanged 调用时会传入 ObserverWrapper#this 对象
    if (mDispatchingValue) { //如果已经开始分发，设置一下无效的tag，return
        mDispatchInvalidated = true;
        return;
    }  
    mDispatchingValue = true; //进入while 循环前，设置 mDispatchingValue 为 true 。
    do {      
        mDispatchInvalidated = false; // 重置无效tag
        if (initiator != null) { //2、如果 initiator 不为空，调用 considerNotify(initiator) 方法。
            considerNotify(initiator);
            initiator = null;
        } else { //initiator为空，循环通知 LiveData#mObservers 中所有的回调函数
            for (Iterator<Map.Entry<Observer<? super T>, ObserverWrapper>> iterator =  mObservers.iteratorWithAdditions(); iterator.hasNext();) {
                considerNotify(iterator.next().getValue());
                if (mDispatchInvalidated) {// 注意这里，当正在分发的时候 dispatchingValue 方法又被调用，mDispatchInvalidated 会被置为 true。
                    break;
                }
            }
        }
    } while (mDispatchInvalidated);
    mDispatchingValue = false;  // 退出 while 后，将 mDispatchingValue 设置为 false，逻辑很严谨
}
```
`ObserverWrapper#activeStateChanged` 继续调用 `LiveData#dispatchingValue` 方法，

### 3.4 继续跟踪 considerNotify

看下 `LiveData#considerNotify(observer)` 方法:

```kotlin
private void considerNotify(ObserverWrapper observer) {
    if (!observer.mActive) { // 继续拦截不是 active 的 observer
        return;
    }
    if (!observer.shouldBeActive()) { // 如果生命周期变为非活跃，回调 observer 的 activeStateChanged 方法，入参为 false
        observer.activeStateChanged(false);
        return;
    }
    if (observer.mLastVersion >= mVersion) { // 根据 observer 中的数据版本判断是否需要更新，如果 observer.mLastVersion 小于 LiveData.mVersion
        return;
    }
    observer.mLastVersion = mVersion; // 更新一下 observer.mLastVersion
    //noinspection unchecked
    observer.mObserver.onChanged((T) mData); // 调用 Observer 的 onChanged(T)，数据源变化
}
```
在该方法中经过一些列的判断后，最终实现了 `Observer#onChanged(T)` 的回调。

总结：

1. 我们通过 LiveData.observe(owner,observe) 设置一个 Observe 对象，并在 observe(owner,observe) 方法中将 owner 和 observe 包装为一个 LifecycleBoundObserver 对象，并和 Lifecycle 建立订阅关系（LifecycleBoundObserver 是 ObserverWrapper 的子类并实现了 LifecycleEventObserver 接口）。
2. owner 生命周期发生变化时，调用 LifecycleBoundObserver#onStateChanged 方法。
3. LifecycleBoundObserver#onStateChanged 继续调用父类 ObserverWrapper#activeStateChanged 方法。
4. owner 可见后，ObserverWrapper.mActive 变为 true，并调用 LiveData#dispatchingValue(ObserverWrapper);
5. LiveData#dispatchingValue(ObserverWrapper) 调用 LiveData#considerNotify(initiator)，LiveData#considerNotify(initiator) 再调用 Observer.onChanged(T) 方法。

这就是为什么观察者从`非活跃状态更改为活跃状态时`也会收到更新。

## 4、数据发生变化的源码逻辑

我们接着看当数据发生变化，即调用 `LiveData` 的 `setValue/postValue` 方法时，代码的调用逻辑

### 4.1 先看一下 setValue(T)

```kotlin
protected void setValue(T value) {
    assertMainThread("setValue");
    mVersion++; // 更新 LiveData 数据版本号mVersion
    mData = value; // 保存这次变化的数据
    dispatchingValue(null); //调用回调函数
}
```
当调用 `LiveData#setValue(T)` 方法后，先设置 mVersion 自加，更新 LiveData 的 mData 数据。
然后直接调用 dispatchingValue 方法，并且入参为 `null`。
我们在 LiveData#dispatchingValue(ObserverWrapper) 方法的时候分析过：如果入参为空，mObservers 中所有的 Observer 都会被遍历回调。

### 4.2 postValue(T)方法
postValue(T) 可以在子线程更新数据
```kotlin
protected void postValue(T value) {
    boolean postTask;
    synchronized (mDataLock) {
        postTask = mPendingData == NOT_SET;
        mPendingData = value;
    }
    if (!postTask) { 
        return;
    }
    // postValue 可以从后台线程调用，因为它会在主线程中执行任务
    ArchTaskExecutor.getInstance().postToMainThread(mPostValueRunnable);
}

private final Runnable mPostValueRunnable = new Runnable() {
    @Override
    public void run() {
        Object newValue;
        synchronized (mDataLock) { 
            newValue = mPendingData;
            mPendingData = NOT_SET;
        }
        //noinspection unchecked
        setValue((T) newValue); // 这里调用了setValue，和上面分析的流程一样
    }
};
```
## 5、remove相关
在 LifecycleBoundObserver 类 onStateChanged 方法中，当 Owner 的状态变为 DESTROYED 时，会调用 removeObserver(mObserver) 方法。看代码吧
```java
class LifecycleBoundObserver extends ObserverWrapper implements LifecycleEventObserver {
    @Override 
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) { // 生命周期变化时触发该方法
        Lifecycle.State currentState = mOwner.getLifecycle().getCurrentState();
        if (currentState == DESTROYED) { 
            removeObserver(mObserver); // Removes the given observer from the observers list.
            return;
        }
        ...
    }
    
    @Override
    void detachObserver() {
        mOwner.getLifecycle().removeObserver(this);
    }
}    
// LiveData#removeObserver 方法
public void removeObserver(@NonNull final Observer<? super T> observer) {
    assertMainThread("removeObserver");
    ObserverWrapper removed = mObservers.remove(observer);
    if (removed == null) {
        return;
    }
    removed.detachObserver();
    removed.activeStateChanged(false);
}
```

## 6、数据倒灌问题

当 LifeCircleOwner 的状态发生变化的时候，会调用 LiveData.ObserverWrapper 的 activeStateChanged 函数，如果这个时候 ObserverWrapper 的状态是 active，就会调用 LiveData 的 dispatchingValue。

<img width="600" alt="类图" src="https://user-images.githubusercontent.com/17560388/154472828-30b4818b-e6f1-4f60-829c-719e17e7f37d.png">

在 LiveData 的 dispatchingValue 中，又会调用 LiveData 的 considerNotify 方法。

<img width="600" alt="类图" src="https://user-images.githubusercontent.com/17560388/154472868-a17402f6-5115-4eb1-874a-18492fc43f66.png">


在 LiveData 的 considerNotify 方法中，红框中的逻辑是关键，如果 ObserverWrapper 的 mLastVersion 小于 LiveData 的 mVersion，就会去回调 mObserver 的 onChanged 方法。而每个新的订阅者，其 version 都是 -1，LiveData 一旦设置过其 version 是大于 -1 的（每次 LiveData 设置值都会使其 version 加1），这样就会导致 LiveDataBus 每注册一个新的订阅者，这个订阅者立刻会收到一个回调，即使这个设置的动作发生在订阅之前。

<img width="600" alt="类图" src="https://user-images.githubusercontent.com/17560388/154472905-34981214-8fc3-42ee-ac8a-d3e7418d42bb.png">

问题原因总结:

对于 LiveData，其初始的 version 是 -1，当我们调用了其 setValue 或者 postValue，其 vesion 会 +1；对于每一个观察者的封装 ObserverWrapper ，其初始 version 也为-1，也就是说，每一个新注册的观察者，其 version 为-1；当 LiveData 设置这个 ObserverWrapper 的时候，如果 LiveData的version 大于 ObserverWrapper 的 version，LiveData 就会强制把当前 value 推送给 Observer。

解决：看一下 android 官方的解决方案

[SingleLiveEvent.java ](https://github.com/android/architecture-samples/blob/dev-todo-mvvm-live/todoapp/app/src/main/java/com/example/android/architecture/blueprints/todoapp/SingleLiveEvent.java)

```java
public class SingleLiveEvent<T> extends MutableLiveData<T> {

    private static final String TAG = "SingleLiveEvent";
    private final AtomicBoolean mPending = new AtomicBoolean(false); // 通过一个原子类记录当前 value 是否被处理

    @MainThread
    public void observe(LifecycleOwner owner, final Observer<T> observer) {

        if (hasActiveObservers()) {
            Log.w(TAG, "Multiple observers registered but only one will be notified of changes.");
        }
        // Observe the internal MutableLiveData
        super.observe(owner, new Observer<T>() {
            @Override
            public void onChanged(@Nullable T t) {
                if (mPending.compareAndSet(true, false)) {
                    observer.onChanged(t);
                }
            }
        });
    }

    @MainThread
    public void setValue(@Nullable T t) {
        mPending.set(true);
        super.setValue(t);
    }

    /**
     * Used for cases where T is Void, to make calls cleaner.
     */
    @MainThread
    public void call() {
        setValue(null);
    }
}
```

## 参考

- [LiveData 概览](https://developer.android.com/topic/libraries/architecture/livedata?hl=zh-cn)
- [SingleLiveEvent.java ](https://github.com/android/architecture-samples/blob/dev-todo-mvvm-live/todoapp/app/src/main/java/com/example/android/architecture/blueprints/todoapp/SingleLiveEvent.java)
- [Activity销毁重建导致LiveData数据倒灌](https://juejin.cn/post/6986936609522319391)
- [Android消息总线的演进之路：用LiveDataBus替代RxBus、EventBus](https://tech.meituan.com/2018/07/26/android-livedatabus.html)
