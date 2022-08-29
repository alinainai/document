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

我们通过 `Glide.with(...)` 获取一个 `RequestManager（请求管理类）`对象，在 `Glide` 类中有很多 `with(...)`的静态重载方法。主要还是下面三个：

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
每个重载方法中都会调用 `getRetriever(context)` 方法获取一个 `RequestManagerRetriever` 对象。
`RequestManagerRetriever` 是负责管理 `RequestManager` 的类。该类有一系列的静态方法用来创建或者复用 activities/fragment 中存在的 RequestManagers。

```java
@NonNull
private static RequestManagerRetriever getRetriever(@Nullable Context context) {
    return Glide.get(context).getRequestManagerRetriever(); 
}
```
在 `getRetriever(context)` 方法中 通过调用 `Glide#get(context)` 获取 `Glide` 单例对象，通过 `Glide#getRequestManagerRetriever()` 获取 `RequestManagerRetriever` 对象。

### 2.2 GlideBuilder 

这里我们不详细分析 `Glide` 生成过程的源码。只看一下 `GlideBuilder` 创建 `Glide` 相关的代码，由 `GlideBuilder#build方法`会为 Glide 设置一个 RequestManagerRetriever。

Glide 初始化相关的方法，我们简化了一下只看下基本过程
```java
public static Glide get(@NonNull Context context) {
    if (glide == null) {
        checkAndInitializeGlide(context, annotationGeneratedModule);
    }
    return glide;
}
private static void checkAndInitializeGlide(@NonNull Context context, @Nullable GeneratedAppGlideModule generatedAppGlideModule) {
    if (isInitializing) {
      throw new IllegalStateException...
    }
    isInitializing = true;
    initializeGlide(context, generatedAppGlideModule);
    isInitializing = false;
}
private static void initializeGlide(@NonNull Context context, @NonNull GlideBuilder builder, @Nullable GeneratedAppGlideModule annotationGeneratedModule) {
    // 创建一个生成 RequestManager 的 RequestManagerFactory类的对象
    RequestManagerRetriever.RequestManagerFactory factory = annotationGeneratedModule != null ? annotationGeneratedModule.getRequestManagerFactory(): null;
    builder.setRequestManagerFactory(factory);
    Glide glide = builder.build(applicationContext);
}
```
然后通过`GlideBuilder#build(context)` 方法，配置一些必要的参数并返回一个 Glide 对象
```java
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
      engine = new Engine(memoryCache, diskCacheFactory, diskCacheExecutor, sourceExecutor,
                   GlideExecutor.newUnlimitedSourceExecutor(),animationExecutor,isActiveResourceRetentionAllowed);
    }
    
    if (defaultRequestListeners == null) {
        defaultRequestListeners = Collections.emptyList();
    } else {
        defaultRequestListeners = Collections.unmodifiableList(defaultRequestListeners);
    }

    GlideExperiments experiments = glideExperimentsBuilder.build();
    // new 一个 RequestManagerRetriever 对象
    RequestManagerRetriever requestManagerRetriever = new RequestManagerRetriever(requestManagerFactory, experiments);

    return new Glide(context, engine, memoryCache,
        bitmapPool, arrayPool,
        requestManagerRetriever,connectivityMonitorFactory,
        logLevel,defaultRequestOptionsFactory, defaultTransitionOptions,defaultRequestListeners,
        experiments);
}
```
`Glide` 内部持有了一个 `RequestManagerRetriever` 的对象，并通过静态方法 getRetriever 获取该回到 RequestManagerRetriever 对象。
回到 Glide.with 方法中，我们将继续调用 RequestManagerRetriever.get(...) 方法处理我们传入的参数。

### 2.3 RequestManagerRetriever#get(...) 方法

RequestManagerRetriever#get() 虽然也有很多重载方法，最终会根据条件调用下面的伪代码（我们去掉了一下多余的非空判断）:

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
- 如果是从 `background` 线程调用或者传入的 context 是 Application 类型，返回一个 ApplicationManager 
- 如果传入的参数是 `Activity/Fragment`，使用 supportFragmentGet 方法获取一个和 FM 关联的 SupportRequestManagerFragment 对象并设置 RequestManager。

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

总结一下：
在`Glide.with()`方法中，我们先通过`Glide.get`获取一个`Glide`单例。
`Glide`单例内部持有一个 `RequestManagerRetriever` 对象，通过调用 `RequestManagerRetriever#get()` 创建并返回了一个和加载目标所在的宿主的 `FM` 绑定的 `RequestManager`。

下面我们来分析一下 `RequestManager#load(...)` 方法

## 3、RequestManager.load(...) 方法分析

我们例子中加载的是图片的网络url，我们先看一下 `RequestManager.load(String)` 方法。在 `RequestManager` 方法中:
```java
public RequestBuilder<Drawable> load(@Nullable String string) {
    return asDrawable().load(string);
}
public RequestBuilder<Drawable> asDrawable() {
    return as(Drawable.class);
}
public <ResourceType> RequestBuilder<ResourceType> as(@NonNull Class<ResourceType> resourceClass) {
    return new RequestBuilder<>(glide, this, resourceClass, context);
}
```
`load(string)` 方法最终会返回一个 `RequestBuilder<Drawable>` 对象。

之前我们说过稍后分析 Glide 的构造方法，主要是因为 Glide 的构造方法中有很多和加载相关的配置项。我们先看一下 Glide 的构造，看下都包含了什么配置：
```java
Glide(
      @NonNull Context context,
      @NonNull Engine engine,
      @NonNull MemoryCache memoryCache,
      @NonNull BitmapPool bitmapPool,
      @NonNull ArrayPool arrayPool,
      @NonNull RequestManagerRetriever requestManagerRetriever,
      @NonNull ConnectivityMonitorFactory connectivityMonitorFactory,
      int logLevel,
      @NonNull RequestOptionsFactory defaultRequestOptionsFactory,
      @NonNull Map<Class<?>, TransitionOptions<?, ?>> defaultTransitionOptions,
      @NonNull List<RequestListener<Object>> defaultRequestListeners,
      GlideExperiments experiments) {
    ...
    registry = new Registry();
    ...
    registry
        .append(ByteBuffer.class, new ByteBufferEncoder())
        .append(InputStream.class, new StreamEncoder(arrayPool))
        .append(Registry.BUCKET_BITMAP, ByteBuffer.class, Bitmap.class, byteBufferBitmapDecoder)
        .append(Registry.BUCKET_BITMAP, InputStream.class, Bitmap.class, streamBitmapDecoder);
    ...
    ImageViewTargetFactory imageViewTargetFactory = new ImageViewTargetFactory();
    glideContext = new GlideContext(context, arrayPool, registry, imageViewTargetFactory, defaultRequestOptionsFactory,
            defaultTransitionOptions, defaultRequestListeners, engine, experiments, logLevel);
}
```
好多配置项已经在 `GlideBuilder` 中初始化了，`Glide` 构造的核心代码是创建一个 `registry`，并给 `registry` 添加 `loading, decoding, and encoding` 的逻辑。






## 参考
- [Glide v4 源码解析（二）](https://blog.yorek.xyz/android/3rd-library/glide2/#2-glide) 
- [深入分析Glide源码](https://blog.csdn.net/xx326664162/article/details/107663872)
- [Glide 源码分析解读-基于最新版Glide 4.9.0](https://www.jianshu.com/p/9bb50924d42a)






