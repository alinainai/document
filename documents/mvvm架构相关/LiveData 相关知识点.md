## 1. LiveData的简单使用

ViewModel 使用：

```kotlin
private val _loginLiveData = MutableLiveData<<LoginBean>>() //内部数据更新 MutableLiveData 
val loginLiveData: LiveData<Resource<LoginResponse>> get() = _loginLiveData //对外暴露 LiveData

//LiveData setValue
_loginLiveData.value = LoginBean()
```

Activity/Fragment 使用

```kotlin
loginViewModel.loginLiveData.observe(this){bean->
}
```
### 1.1 LiveData 和 MultableLiveData 的区别

MultableLiveData 是 LiveData 子类。并对外公开了 postValue 和 setValue 的方法。

### 1.2 特点

 1. 采用观察者模式，数据发生改变，可以自动回调。
 2. 不需要手动处理生命周期，不会因为Activity的销毁重建而丢失数据。

## 2. 源码分析

LiveData（abstract类） 的 实现类MutableLiveData：

```kotlin
public class MutableLiveData<T> extends LiveData<T> {
    @Override
    public void postValue(T value) { //LiveData.postValue() 是一个 protected 方法
        super.postValue(value);
    }

    @Override
    public void setValue(T value) { // LiveData.setValue() 是一个 protected 方法
        super.setValue(value);
    }
}
```
创建了Livedata 后，需要通过observe方法或者observeForever 方法设置一个回调，这个回调接口就是Observer

### 2.1 回调接口

```kotlin
public interface Observer<T> {
    /**
     * Called when the data is changed.
     * @param t  The new data
     */
    void onChanged(@Nullable T t);
}
```
```kotlin
public abstract class LiveData<T> {
    ... ...
    // 只有 onStart 后，对数据的修改才会触发 observer.onChanged()
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer) {}

    // 无论何时，只要数据发生改变，就会触发 observer.onChanged()
    public void observeForever(@NonNull Observer<T> observer) {}
    ... ...
}
```
observeForever 的实现跟 observe 是类似的，这里我们重点看一下 observe()的实现过程

### 2.2 observe 方法

```kotlin
@MainThread
public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
    assertMainThread("observe");
    if (owner.getLifecycle().getCurrentState() == DESTROYED) {
        return;
    }  
    LifecycleBoundObserver wrapper = new LifecycleBoundObserver(owner, observer);
    ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);
    if (existing != null && !existing.isAttachedTo(owner)) {
        throw new IllegalArgumentException("Cannot add the same observer"
                + " with different lifecycles");
    }
    if (existing != null) {
        return;
    }
    //把 wrapper 与A ctivity/Fragment 的生命周期建立关系，当UI的生命周期发生变化的时候，就会去回调 wrapper 中的 onStateChanged
    owner.getLifecycle().addObserver(wrapper);
}
```
### 2.3 界面生命周期发生变化时LiveData方法的调用情况

当生命周期发生变化的时候，会调用 LifecycleBoundObserver # onStateChanged 方法。

```kotlin
class LifecycleBoundObserver extends ObserverWrapper implements LifecycleEventObserver {
    @NonNull
    final LifecycleOwner mOwner
    
    LifecycleBoundObserver(@NonNull LifecycleOwner owner, Observer<? super T> observer) {
        super(observer);
        mOwner = owner
    }
    
    @Override
    boolean shouldBeActive() {
        return mOwner.getLifecycle().getCurrentState().isAtLeast(STARTED);
    }
    
    @Override
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) { //当生命周期发生变化时，会调用该方法
        if (mOwner.getLifecycle().getCurrentState() == DESTROYED) { 
            removeObserver(mObserver); //当UI的生命周期为DESTROYED，取消对数据变化的监听，自动移除回调
            return;
        }
        //改变数据，传递的参数是 shouldBeActive()，shouldBeActive 方法会计算看当前的状态是否是 STARTED，也就是 onStart-onPause 期间生命周期
        activeStateChanged(shouldBeActive());
    }
    
    @Override
    boolean isAttachedTo(LifecycleOwner owner) {
        return mOwner == owner;
    }
    
    @Override
    void detachObserver() {
        mOwner.getLifecycle().removeObserver(this);
    }
}
```

我们继续看下 activeStateChanged 方法是如何对数据进行处理的，它是 ObserverWrapper 类中的一个方法。

```kotlin
private abstract class ObserverWrapper {

    final Observer<? super T> mObserver;
    boolean mActive;
    int mLastVersion = START_VERSION; // -1

    ObserverWrapper(Observer<? super T> observer) {
        mObserver = observer;
    }

    abstract boolean shouldBeActive();

    boolean isAttachedTo(LifecycleOwner owner) {
        return false;
    }

    void detachObserver() {
    }

    void activeStateChanged(boolean newActive) {
        // 当激活状态没有发生变化，直接返回。onStart-onPause 为 true  在这之外的生命周期为false
        if (newActive == mActive) {
            return;
        }
        // immediately set active state, so we'd never dispatch anything to inactive owner
        mActive = newActive;
        boolean wasInactive = LiveData.this.mActiveCount == 0;
        LiveData.this.mActiveCount += mActive ? 1 : -1;
        if (wasInactive && mActive) {
            onActive();//空函数，可根据需要进行重写
        }
        if (LiveData.this.mActiveCount == 0 && !mActive) {
            onInactive(); // 空函数，可根据需要进行重写    
        }
        if (mActive) { //结合上面的状态判断，我们知道了，生命周期状态从 Inactive 到 Active，就会调用回调函数
            dispatchingValue(this);
        }
    }
}
```

为什么在生命周期的活跃状态 从Inactive 到 Active，要去调用 livedata 设置的回调函数呢？

当界面从 Inactive 变为 Active（onStart-onPause 周期内），如果不调用回调函数，UI的界面还是显示上一次的数据。

### 2.4 LiveData数据发生发生变化时，LiveData的方法调用情况

当数据发生变化，需要调用 LiveData 的 setValue/postValue 方法

从setValue入手，看一下流程

```kotlin
protected void setValue(T value) {
    assertMainThread("setValue");
    mVersion++; // 更新 LiveData 数据版本号mVersion
    mData = value; // 保存这次变化的数据
    dispatchingValue(null); //调用回调函数
}
```

进入到 LiveData # dispatchingValue 方法

```kotlin
void dispatchingValue(@Nullable ObserverWrapper initiator) {
    if (mDispatchingValue) { //如果此时正在进行数据分发
        mDispatchInvalidated = true; //标记一下tag，这次数据变化没有进入分发操作
        return;
    }  
    mDispatchingValue = true; //进入while 循环前，设置为true，如果此时又来了一个数据变化，需要在上面 if 判断中进行拦截。
    do {      
        mDispatchInvalidated = false; //开始for循环前，设置为false，for循环完，也会退出while循环
        if (initiator != null) { //如果 initiator 不为空，通知指定的回调函数
            considerNotify(initiator);
            initiator = null;
        } else { //initiator为空，循环通知 LiveData 中所有的回调函数
            for (Iterator<Map.Entry<Observer<? super T>, ObserverWrapper>> iterator =  mObservers.iteratorWithAdditions(); iterator.hasNext();) {
                considerNotify(iterator.next().getValue());
                //这里 mDispatchInvalidated 为 true，表示在 while 循环未结束的时候，有其他数据发生变化，并调用了该函数
                //在上面的if判断中设置了 mDispatchInvalidated = true，
                // 结束本次for循环，但是没有退出 while 循环，开始下一次for循环
                if (mDispatchInvalidated) {
                    break;
                }
            }
        }
    } while (mDispatchInvalidated);
    mDispatchingValue = false;  //退出while 后，设置为false，正常处理数据变化
}
```

注意上面的两个变量

1. mDispatchingValue 这个变量用来控制，是否进入while 循环，以及while 循环 是否已经结束
2. mDispatchInvalidated 这个变量用来控制for 循环是否要重新开始


看下considerNotify 的函数，调用了之前livedata设置的observer的onChanged函数

```kotlin
private void considerNotify(ObserverWrapper observer) {
    if (!observer.mActive) {
        return;
    }
    if (!observer.shouldBeActive()) { // 如果当前的生命周期是非活跃，就不回调 onChanged 函数，并在 LifecycleBoundObserver 中记录状态
        observer.activeStateChanged(false);
        return;
    }
    if (observer.mLastVersion >= mVersion) { //如果 observer 中的数据版本已经是最新了
        return;
    }
    observer.mLastVersion = mVersion;
    //noinspection unchecked
    observer.mObserver.onChanged((T) mData);
}
```

看完了 setValue，postValue 就很简单了：

```kotlin
protected void postValue(T value) {
    boolean postTask; //是否需要 post task
    synchronized (mDataLock) {
        postTask = mPendingData == NOT_SET; //当 mPendingData ! = NOT_SET，也就是 mPostValueRunnable 还没有执行，将 postTask 置为 false
        mPendingData = value;
    }
    if (!postTask) { 
        //上一个 post 的 mPostValueRunnable 还没执行，就不需要再 post 了。但是注意，上面的 mPendingData 已经更新为新数据了。
        //用官方的话，就是  If you called this method multiple times before a main thread executed a posted task, only the last value would be dispatched.
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
        //这里调用了setValue，和上面分析的流程一样
        setValue((T) newValue);
    }
};
```
