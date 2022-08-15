### 1. LiveData的简单使用

ViewModel 使用：

```kotlin
// 内部更新数据使用 MutableLiveData，因为该类对外公开了 postValue 和 setValue 方法
private val _loginLiveData = MutableLiveData<<LoginBean>>() 

//对外公开的 LiveData，不提供 postValue 和 setValue。
val loginLiveData: LiveData<Resource<LoginResponse>> 
    get() = _loginLiveData 

// LiveData 通过 setValue 赋值
_loginLiveData.value = LoginBean()

```

Activity/Fragment 使用

```kotlin
loginViewModel.loginLiveData.observe(this){bean->
}
```

**LiveData的特点**：

 1. 采用观察者模式，数据发生改变，可以自动回调。
 2. 不需要手动处理生命周期，不会因为Activity的销毁重建而丢失数据。

**LiveData 和 MultableLiveData 的区别**:

MultableLiveData 是 LiveData 子类。并对外公开了 postValue 和 setValue 的方法。

### 2. 源码分析

`LiveData（abstract类）` 的子类 `MutableLiveData`的源码

```java
public class MutableLiveData<T> extends LiveData<T> {
    //将 protected 修饰符变为 public，这个方法一般在子线程中使用，来设置数据
    @Override
    public void postValue(T value) { 
        super.postValue(value);
    }

    //将 protected 修饰符变为 public
    @Override
    public void setValue(T value) { 
        super.setValue(value);
    }
}
```

通过调用 `LiveData` 的 observe/observeForever 方法设置一个 `Observer` 回调

Observer 接口如下：

```java
public interface Observer<T> {
    void onChanged(@Nullable T t);
}
```

我们看下 LiveData 的 `observe()` 和 `observeForever()` 的区别

```java
public abstract class LiveData<T> {
    //...
    // 只有 onStart 后，对数据的修改才会触发 observer.onChanged()
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer) {}

    // 无论何时，只要数据发生改变，就会触发 observer.onChanged()
    public void observeForever(@NonNull Observer<T> observer) {}
    //... 
}
```
`observeForever` 的实现跟 `observe` 是类似的，这里我们重点看一下 `observe()` 的实现过程

####  LiveData#observe 方法

2个参数：
- LifecycleOwner: 声明周期持有类
- Observer: 监听器

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
### 3. 两种方式回调 Observer#onChanged 方式

1. 当界面生命周期发生变化时 `LiveData` 方法的调用情况
2. 当数据发生变化时 `LiveData` 方法的调用情况

#### 3.1 界面生命周期发生变化时，LiveData 方法的调用情况

当生命周期发生变化的时候，会调用 onStateChanged 方法，然后

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
    
    //当生命周期发生变化时，会调用该方法
    @Override
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) { 
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

#### 3.2 数据发生发生变化时，LiveData的方法调用情况

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

1. mDispatchingValue 这个变量用来控制是否进入while 循环，以及 while 循环是否已经结束
2. mDispatchInvalidated 这个变量用来控制 for 循环是否要重新开始


看下 considerNotify 的函数，调用了之前livedata设置的observer的onChanged函数

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

### 4. 数据倒灌问题

当 LifeCircleOwner 的状态发生变化的时候，会调用 LiveData.ObserverWrapper 的 activeStateChanged 函数，如果这个时候 ObserverWrapper 的状态是 active，就会调用 LiveData 的dispatchingValue。

<img width="600" alt="类图" src="https://user-images.githubusercontent.com/17560388/154472828-30b4818b-e6f1-4f60-829c-719e17e7f37d.png">

在LiveData的dispatchingValue中，又会调用LiveData的considerNotify方法。

<img width="600" alt="类图" src="https://user-images.githubusercontent.com/17560388/154472868-a17402f6-5115-4eb1-874a-18492fc43f66.png">


在 LiveData的considerNotify 方法中，红框中的逻辑是关键，如果 ObserverWrapper 的m LastVersion 小于 LiveData的mVersion，就会去回调 mObserver 的 onChanged 方法。而每个新的订阅者，其 version 都是-1，LiveData 一旦设置过其 version 是大于-1的（每次 LiveData 设置值都会使其 version 加1），这样就会导致 LiveDataBus 每注册一个新的订阅者，这个订阅者立刻会收到一个回调，即使这个设置的动作发生在订阅之前。

<img width="600" alt="类图" src="https://user-images.githubusercontent.com/17560388/154472905-34981214-8fc3-42ee-ac8a-d3e7418d42bb.png">

问题原因总结:

对于 LiveData，其初始的 version 是-1，当我们调用了其 setValue 或者 postValue，其 vesion 会+1；对于每一个观察者的封装 ObserverWrapper ，其初始 version 也为-1，也就是说，每一个新注册的观察者，其 version 为-1；当 LiveData 设置这个 ObserverWrapper 的时候，如果 LiveData的version 大于 ObserverWrapper 的 version，LiveData 就会强制把当前 value 推送给 Observer。

解决：

[SingleLiveEvent.java ](https://github.com/android/architecture-samples/blob/dev-todo-mvvm-live/todoapp/app/src/main/java/com/example/android/architecture/blueprints/todoapp/SingleLiveEvent.java)

```java
/*
 *  Copyright 2017 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A lifecycle-aware observable that sends only new updates after subscription, used for events like
 * navigation and Snackbar messages.
 * <p>
 * This avoids a common problem with events: on configuration change (like rotation) an update
 * can be emitted if the observer is active. This LiveData only calls the observable if there's an
 * explicit call to setValue() or call().
 * <p>
 * Note that only one observer is going to be notified of changes.
 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {

    private static final String TAG = "SingleLiveEvent";

    private final AtomicBoolean mPending = new AtomicBoolean(false);

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

### 参考


[SingleLiveEvent.java ](https://github.com/android/architecture-samples/blob/dev-todo-mvvm-live/todoapp/app/src/main/java/com/example/android/architecture/blueprints/todoapp/SingleLiveEvent.java)

[Activity销毁重建导致LiveData数据倒灌](https://juejin.cn/post/6986936609522319391)

[Android消息总线的演进之路：用LiveDataBus替代RxBus、EventBus](https://tech.meituan.com/2018/07/26/android-livedatabus.html)
