### 1. Fragment中调用startActivityForResult的几种情况

1. 用 getActivity 方法发起调用，只有父 Activity 的 onActivityResult 会调用，Fragment 中的 onActivityResult 不会被调用
2. 直接发起 startActivityForResult 调用，当前的 Fragment 的 onActivityResult，和父 Activity 的 onActivityResult 都会调用
3. 用 getParentFragment 发起调用，则只有父 Activity 和父 Fragment 的 onActivityResult 会被调用，当前的 Fragment 的 onActivityResult 不会被调用。


第2条和第3条父 Activity 的 onActivityResult 中必须添加 super.onActivityResult()


### 2. Fragment中直接调用 startActivityForResult

这种情况会直接调用到 Fragment 的 startActivityForResult 方法

```java
//Fragment.class
public void startActivityForResult(Intent intent, int requestCode, @Nullable Bundle options) {
    if (mHost == null) {
        throw new IllegalStateException("Fragment " + this + " not attached to Activity");
    }
    mHost.onStartActivityFromFragment(this /*fragment*/, intent, requestCode, options);
}
```
上面的 mHost 对应 Fragment 的父 FragmentActivity，所以会调用父 FragmentActivity 的 startActivityFromFragment 方法
```java
//FragmentActivity.class
public void startActivityFromFragment(Fragment fragment, Intent intent,
        int requestCode, @Nullable Bundle options) {
    mStartedActivityFromFragment = true;
    try {
    	  //一般 requestCode 都不会为-1，所以不会走if里面
        if (requestCode == -1) {
            ActivityCompat.startActivityForResult(this, intent, -1, options);
            return;
        }
        // 检查 requestCode 是否越界，不能超过 2^16
        checkForValidRequestCode(requestCode);
        //根据 requestIndex 可以获取到对 应Fragment 的唯一标识 mWho
        int requestIndex = allocateRequestIndex(fragment);
        //发起 startActivityForResult调用，这里requestIndex和requestCode关联起来
        ActivityCompat.startActivityForResult(
                this, intent, ((requestIndex + 1) << 16) + (requestCode & 0xffff), options);
    } finally {
        mStartedActivityFromFragment = false;
    }
}
```
每一个 Fragment 在内部都有一个唯一的标识字段 who，
在 FragmentActivity 中把所有调用 startActivityFromFragment 方法的 fragment 的 requestCode 和 who 通过 key-value的方式保存在 mPendingFragmentActivityResult s变量中

```java
private int allocateRequestIndex(Fragment fragment) {
    //...
    int requestIndex = mNextCandidateRequestIndex;    
    //将requestIndex和fragment的mWho保存起来
    mPendingFragmentActivityResults.put(requestIndex, fragment.mWho);
    mNextCandidateRequestIndex =
            (mNextCandidateRequestIndex + 1) % MAX_NUM_PENDING_FRAGMENT_ACTIVITY_RESULTS;
    return requestIndex;
}
```
这里 allocateRequestIndex 方法就把 requestIndex 和 Fragment的mWho 变量关联起来

在上面的 startActivityFromFragment 方法中调用 ActivityCompat 的 startActivityForResult 方法发起启动 Activity 的时候又把 requestIndex 和 requestCode 关联了起来
这样后面回调 onActivityResult 方法时就可以根据 requestCode 获取对应的Fragment，以便调用 Fragment 的o nActivityResult 方法

然后看一下 ActivityCompat 的 startActivityForResult 方法
```java
public static void startActivityForResult(@NonNull Activity activity, @NonNull Intent intent,
        int requestCode, @Nullable Bundle options) {
    if (Build.VERSION.SDK_INT >= 16) {
        activity.startActivityForResult(intent, requestCode, options);
    } else {
        activity.startActivityForResult(intent, requestCode);
    }
}
```
### 3. onActivityResult方法回调

最先被回调的是父 Activity 的 onActivityResult ，也就是我们的 FragmentActivity 的 onActivityResult

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    mFragments.noteStateNotSaved();
    int requestIndex = requestCode>>16;
    //requestIndex = 0 就表示没有Fragment发起过 startActivityForResult 调用
    if (requestIndex != 0) {
        requestIndex--;
		    //根据requestIndex获取Fragment的who变量
        String who = mPendingFragmentActivityResults.get(requestIndex);
        mPendingFragmentActivityResults.remove(requestIndex);
        if (who == null) {
            Log.w(TAG, "Activity result delivered for unknown Fragment.");
            return;
        }
        // 根据 who 变量获取目标Fragment
        Fragment targetFragment = mFragments.findFragmentByWho(who);
        if (targetFragment == null) {
            Log.w(TAG, "Activity result no fragment exists for who: " + who);
        } else {
        	  //最后调用 Fragment 的 onActivityResult
            targetFragment.onActivityResult(requestCode & 0xffff, resultCode, data);
        }
        return;
    }
    //...
    super.onActivityResult(requestCode, resultCode, data);
}
```
从上面的方法中可以看出 FragmentActivity 中的 onActivityResult 方法中对于 Fragment 的 startActivityForResult 调用已经做了处理。

这里就有一个问题需要注意，Activity重写 onActivityResult 方法时，我们必须在onActivityResult方法加上super.onActivityResult()，否则Fragment中的onActivityResult方法就没有办法回调到了。

### 4. getParentFragment发起调用

这种情况一般发生在嵌套多层Fragment的时候

getParentFragment 发起调用的过程和上面的类似，只不过发起调用的是当前 Fragment 的父 Fragment，所以最后回调的也是父 Activity 的onActivityResult 方法和父 Fragment 的 onActivityResult 方法。
所以如果想在子 Fragment 中监听到 onActivityResult 方法的回调，就不要用这种方式。

### 5. getActivity方法发起调用

直接调用的是父Activity的onActivityResult方法

```java
//FragmentActivity.class
@Override
    public void startActivityForResult(Intent intent, int requestCode) {
        // If this was started from a Fragment we've already checked the upper 16 bits were not in
        // use, and then repurposed them for the Fragment's index.
        if (!mStartedActivityFromFragment) {
            if (requestCode != -1) {
                checkForValidRequestCode(requestCode);
            }
        }
        super.startActivityForResult(intent, requestCode);
    }
```
从源码可以看出，这种方式最后不会回调 Fragment 的 onActivityResult 方法

