### 1. 生命周期

Activity 在 onCreate 方法中使用事务替换 layout 中的 FragmentContainerView  中的 Fragment 

```shell
--------------------------------进入--------------------------------
Activity-OnCreate
Fragment-onAttach
Fragment-OnCreate
Fragment-onCreateView
Fragment-onViewCreated
Fragment-onStart
Activity-onStart
Activity-onResume
Fragment-onResume
--------------------------------退出--------------------------------
Fragment-onPause
Activity-onPause
Fragment-onStop
Activity-onStop
Fragment-onDestroyView
Fragment-onDestroy
Fragment-onDetach
Activity-onDestroy
--------------------------------home键--------------------------------
Fragment-onPause
Activity-onPause
Fragment-onStop
Activity-onStop
--------------------------------从home返回--------------------------------
Fragment-onStart
Activity-onStart
Activity-onResume
Fragment-onResume
```
### 2. 生命周期的本质

Fragment 生命周期的本质是状态的转移，Fragment 生命周期的七个状态如下

```shell
static final int INITIALIZING = -1;          // Not yet attached.
static final int ATTACHED = 0;               // Attached to the host.
static final int CREATED = 1;                // Created.
static final int VIEW_CREATED = 2;           // View Created.
static final int AWAITING_EXIT_EFFECTS = 3;  // Downward state, awaiting exit effects
static final int ACTIVITY_CREATED = 4;       // Fully created, not started.
static final int STARTED = 5;                // Created and started, not resumed.
static final int AWAITING_ENTER_EFFECTS = 6; // Upward state, awaiting enter effects
static final int RESUMED = 7;                // Created started and resumed.
```

<img width="700" alt="Activit 的 Fragment 架构" src="https://user-images.githubusercontent.com/17560388/161255665-3ef82534-1cde-470e-bbf7-4fa3b91eece7.png">

FragmentActivity 内部持有 FragmentController，FragmentController 持有一个 FragmentManager ，真正做事的就是这个 FragmentManager 的实现类 FragmentManagerImpl。

Activity 生命周期与 Fragment 生命周期的关系

|Activity 生命周期|FragmentManager|Fragment 状态转移|Fragment 生命周期回调|
|:----:|:----:|:----:|:----:|
|onCreate| dispatchCreate|INITIALIZING -> CREATE| onAttach、onCreate|
|onStart（首次）|dispatchActivityCreated<br>dispatchStart|CREATE -> ACTIVITY_CREATED -> STARTED |onCreateView<br>onViewCreated<br>onActivityCreated<br>onStart|
|onStart（非首次）|dispatchStart|ACTIVITY_CREATED -> STARTED| onStart|
|onResume|dispatchResume|STARTED -> RESUMED（Fragment 可交互）|onResume|
|onPause|dispatchPause|RESUMED -> STARTED| onPause|
|onStop|dispatchStop|STARTED -> ACTIVITY_CREATED| onStop|
|onDestroy|dispatchDestroy|ACTIVITY_CREATED -> CREATED -> INITIALIZING| onDestroyView<br>onDestroy<br> onDetach|





FragmentActivity
```java
@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    mFragments.attachHost(null /*parent*/);
    super.onCreate(savedInstanceState);
  	...
    mFragments.dispatchCreate();
}

@Override
protected void onDestroy() {
    super.onDestroy();

    if (mViewModelStore != null && !isChangingConfigurations()) {
        mViewModelStore.clear();
    }

    mFragments.dispatchDestroy();
}
```

dispatchCreate 方法

```java
//内部修改了两个状态
public void dispatchCreate() {
    mStateSaved = false;
    mStopped = false;
    dispatchStateChange(Fragment.CREATED);
}

private void dispatchStateChange(int nextState) {
    try {
        mExecutingActions = true;
        moveToState(nextState, false);// 转移到nextState
    } finally {
        mExecutingActions = false;
    }
    execPendingActions();
}

//一路下来会执行到
void moveToState(Fragment f, int newState, int transit, int transitionStyle,
                 boolean keepActive) {
    // Fragments that are not currently added will sit in the onCreate() state.
    if ((!f.mAdded || f.mDetached) && newState > Fragment.CREATED) {
        newState = Fragment.CREATED;
    }
    if (f.mRemoving && newState > f.mState) {
        if (f.mState == Fragment.INITIALIZING && f.isInBackStack()) {
            // Allow the fragment to be created so that it can be saved later.
            newState = Fragment.CREATED;
        } else {
            // While removing a fragment, we can't change it to a higher state.
            newState = f.mState;
        }
    }
	...
}
```

moveToState 进行状态转移

### 3. 事务替换 Fragment
- add & remove：Fragment 状态在 INITIALIZING 与 RESUMED 之间转移；
- detach & attach： Fragment 状态在 CREATE 与 RESUMED 之间转移；
- replace： 先移除所有 containerId 中的实例，再 add 一个 Fragment；
- show & hide： 只控制 Fragment 隐藏或显示，不会触发状态转移，也不会销毁 Fragment 视图或实例；
- hide & detach & remove 的区别： hide 不会销毁视图和实例、detach 只销毁视图不销毁实例、remove 会销毁实例（自然也销毁视图）。不过，如果 remove 的时候将事务添加到回退栈，那么 Fragment 实例就不会被销毁，只会销毁视图。






