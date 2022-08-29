基于 `glide 4.12.0` 代码分析

## 1、基本使用

>git地址: [https://github.com/bumptech/glide](https://github.com/bumptech/glide)

添加依赖

```groovy
implementation ‘com.github.bumptech.glide:compiler:4.12.0’
```
简单使用
```java
Glide.with(fragment).load(imgUrl).into(imageView);
```
## 2、Glide.with(...)源码分析

`Glide` 类实现了 `ComponentCallbacks2` 接口，`ComponentCallbacks2` 接口提供了 `lowMemory` 的监听方法。

```java
public class Glide implements ComponentCallbacks2 {
```

### 2.1  `Glide.with(...)` 方法

我们通过 `Glide.with(...)` 获取一个 `RequestManager（请求管理类）`对象，在 `Glide` 类中有很多 `with(...)`的静态重载方法。先看下其中3个:

```java
@NonNull
public static RequestManager with(@NonNull Context context) {
    return getRetriever(context).get(context);
}
@NonNull
public static RequestManager with(@NonNull Activity activity) {
    return getRetriever(activity).get(activity);
}
@NonNull
public static RequestManager with(@NonNull Fragment fragment) {
    return getRetriever(fragment.getContext()).get(fragment);
}
```
每个重载方法中都会调用 `getRetriever(context)` 方法获取一个 `RequestManagerRetriever` 对象，`RequestManagerRetriever` 是负责管理 `RequestManager` 的类。
`RequestManagerRetriever`类中有一系列的静态方法用来创建或者复用 activities/fragment 中存在的 RequestManagers。
由 `GlideBuilder` 设置到 `Glide`中。

```java
@NonNull
private static RequestManagerRetriever getRetriever(@Nullable Context context) {
    return Glide.get(context).getRequestManagerRetriever(); 
}
```
在 `getRetriever(context)` 方法中 通过调用 `Glide#get(context)` 获取 `Glide` 单例对象，通过 `Glide#getRequestManagerRetriever()` 获取 `RequestManagerRetriever` 对象。

### 2.2 GlideBuilder 

这里我们不详细分析 `Glide` 生成过程的源码。只看一下 `GlideBuilder` 创建 `Glide` 相关的代码，方便咱们了解 Glide 在加载图片时用到的类。

```java
// Glide # initializeGlide(context,builder,annotationGeneratedModule): Glide 的初始化方法
private static void initializeGlide(@NonNull Context context, @NonNull GlideBuilder builder, @Nullable GeneratedAppGlideModule annotationGeneratedModule) {
    ...
    // 创建一个生成 RequestManager 的 RequestManagerFactory类的对象
    RequestManagerRetriever.RequestManagerFactory factory = annotationGeneratedModule != null ? annotationGeneratedModule.getRequestManagerFactory(): null;
    builder.setRequestManagerFactory(factory);
    Glide glide = builder.build(applicationContext);
    ...
}
// GlideBuilder # build(context) 方法: 返回一个 Glide 对象
@NonNull
Glide build(@NonNull Context context) {

    if (sourceExecutor == null) {
      sourceExecutor = GlideExecutor.newSourceExecutor();
    }
    if (diskCacheExecutor == null) {
      diskCacheExecutor = GlideExecutor.newDiskCacheExecutor();
    }
    if (animationExecutor == null) {
      animationExecutor = GlideExecutor.newAnimationExecutor();
    }
    
    if (memorySizeCalculator == null) {
      memorySizeCalculator = new MemorySizeCalculator.Builder(context).build();
    }
    if (connectivityMonitorFactory == null) {
      connectivityMonitorFactory = new DefaultConnectivityMonitorFactory();
    }
    if (bitmapPool == null) {
      int size = memorySizeCalculator.getBitmapPoolSize();
      if (size > 0) {
        bitmapPool = new LruBitmapPool(size);
      } else {
        bitmapPool = new BitmapPoolAdapter();
      }
    }
    if (arrayPool == null) {
      arrayPool = new LruArrayPool(memorySizeCalculator.getArrayPoolSizeInBytes());
    }
    
    if (memoryCache == null) {
      memoryCache = new LruResourceCache(memorySizeCalculator.getMemoryCacheSize());
    }
    if (diskCacheFactory == null) {
      diskCacheFactory = new InternalCacheDiskCacheFactory(context);
    }

    if (engine == null) {
      engine = new Engine(
                   memoryCache,
                   diskCacheFactory,
                   diskCacheExecutor,
                   sourceExecutor,
                   GlideExecutor.newUnlimitedSourceExecutor(),
                   animationExecutor,
                   isActiveResourceRetentionAllowed);
    }
    
    if (defaultRequestListeners == null) {
        defaultRequestListeners = Collections.emptyList();
    } else {
        defaultRequestListeners = Collections.unmodifiableList(defaultRequestListeners);
    }

    GlideExperiments experiments = glideExperimentsBuilder.build();
    // new 一个 RequestManagerRetriever 类
    RequestManagerRetriever requestManagerRetriever = new RequestManagerRetriever(requestManagerFactory, experiments);

    return new Glide(
        context,
        engine,
        memoryCache,
        bitmapPool,
        arrayPool,
        requestManagerRetriever,
        connectivityMonitorFactory,
        logLevel,
        defaultRequestOptionsFactory,
        defaultTransitionOptions,
        defaultRequestListeners,
        experiments);
}
```
glide 的构造方法后面在分析。从上面代码中我们知道 `Glide` 内部持有了一个 `RequestManagerRetriever` 的对象。我们来看一下 `RequestManagerRetriever # get()` 方法

### 2.3 RequestManagerRetriever # get() 方法

RequestManagerRetriever # get() 虽然也有很多重载方法，单最终会根据条件调用下面的伪代码，我们去掉了一下多余的非空判断

```java
// get(context) 相关
public RequestManager get(@NonNull Context context) {
  if (Util.isOnMainThread() && !(context instanceof Application)) { // 如果是主线程并且 context 不是 Application 类型
    FragmentManager fm = activity.getSupportFragmentManager(); // 这里忽略了 app.fragment.Fragment，只看 androidx.fragment.app.Fragment 的相关代码。
    return supportFragmentGet(activity, fm, /*parentHint=*/ null, isActivityVisible(activity));
  }
  return getApplicationManager(context);
}
// get(fragment)
public RequestManager get(@NonNull Fragment fragment) {
    FragmentManager fm = fragment.getChildFragmentManager();
    return supportFragmentGet(fragment.getContext(), fm, fragment, fragment.isVisible());
  }
}
```
在 get(...) 方法中:
- 如果是从 `BackGround` 线程调用或者传入的 context 是 Application 类型，返回一个 ApplicationManager 
- 如果传入的参数是 `Activity/Fragment`，使用 supportFragmentGet 方法创建一个和 FM 关联的 SupportRequestManagerFragment 对象并设置 RequestManager。

```java
private RequestManager getApplicationManager(@NonNull Context context) {
  // Either an application context or we're on a background thread.
  if (applicationManager == null) {
    synchronized (this) {
      if (applicationManager == null) {
        Glide glide = Glide.get(context.getApplicationContext());
        applicationManager = factory.build(glide, new ApplicationLifecycle(), new EmptyRequestManagerTreeNode(), context.getApplicationContext());
      }
    }
  }
  return applicationManager;
}

private RequestManager supportFragmentGet(@NonNull Context context, @NonNull FragmentManager fm, @Nullable Fragment parentHint, boolean isParentVisible) {
    SupportRequestManagerFragment current = getSupportRequestManagerFragment(fm, parentHint); // 根据 fm 获取 SupportRequestManagerFragment 
    RequestManager requestManager = current.getRequestManager();
    if (requestManager == null) {
        Glide glide = Glide.get(context);
        requestManager = factory.build(glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode(), context);
        if (isParentVisible) {
          requestManager.onStart();
        }
        current.setRequestManager(requestManager);
    }
    return requestManager;
}
// 获取 SupportRequestManagerFragment 的方法，有的返回，无则添加。
private SupportRequestManagerFragment getSupportRequestManagerFragment(@NonNull final FragmentManager fm, @Nullable Fragment parentHint) {
    SupportRequestManagerFragment current = (SupportRequestManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
    if (current == null) {
        current = pendingSupportRequestManagerFragments.get(fm);
        if (current == null) {
            current = new SupportRequestManagerFragment();
            current.setParentFragmentHint(parentHint);
            pendingSupportRequestManagerFragments.put(fm, current);
            fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();
            handler.obtainMessage(ID_REMOVE_SUPPORT_FRAGMENT_MANAGER, fm).sendToTarget();
        }
    }
    return current;
}
```
`get(Fragment)`和`get(FragmentActivity)`方法都会调用`supportFragmentGet`方法，只是传入参数不同

`Glide` 使用一个加载目标所在的宿主 `Activity/Fragment` 的子`Fragment(SupportRequestManagerFragment)`来安全保存一个 `RequestManager`，`RequestManager`被`Glide`用来开始、停止、管理`Glide`请求。

通过 `Glide.with()`方法，我们创建了 `Glide` 单例，并成功创建且返回了一个 `RequestManager`。

下面我们来分析一下 `RequestManager # load(...)‘ 方法

## 3、RequestManager.load(...) 方法分析




## 参考
- [Glide v4 源码解析（二）](https://blog.yorek.xyz/android/3rd-library/glide2/#2-glide) 
- [深入分析Glide源码](https://blog.csdn.net/xx326664162/article/details/107663872)
- [Glide 源码分析解读-基于最新版Glide 4.9.0](https://www.jianshu.com/p/9bb50924d42a)






