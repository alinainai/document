### 1. Fragment 的 start 与 Activity 的 start 的调用顺序

```shell
Activity: onCreate-start
Fragment: onAttach
Fragment: onCreate

Activity: onCreate-end
Fragment: onCreateView
Fragment: onViewCreated
Fragment: onActivityCreated
Fragment: onViewStateRestored
Fragment: onCreateAnimation
Fragment: onCreateAnimator
Fragment: onStart

Activity: onStart:
Activity: onResume:
Fragment: onResume:
```
### 2. Activit 的 Fragment 架构

<img width="400" alt="Activit 的 Fragment 架构" src="https://user-images.githubusercontent.com/17560388/161255665-3ef82534-1cde-470e-bbf7-4fa3b91eece7.png">

FragmentActivity 内部持有 FragmentController，FragmentController 持有一个 FragmentManager ，真正做事的就是这个 FragmentManager 的实现类 FragmentManagerImpl。

Fragment有七个状态

```shell
static final int INVALID_STATE = -1;   // 为空时无效
static final int INITIALIZING = 0;     // 未创建
static final int CREATED = 1;          // 已创建，位于后台
static final int ACTIVITY_CREATED = 2; // Activity已经创建，Fragment位于后台
static final int STOPPED = 3;          // 创建完成，没有开始
static final int STARTED = 4;          // 开始运行，但是位于后台
static final int RESUMED = 5;          // 显示到前台
```

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






