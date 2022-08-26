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
// Glide#initializeGlide(context,builder,annotationGeneratedModule): Glide 的初始化方法
private static void initializeGlide(@NonNull Context context, @NonNull GlideBuilder builder, @Nullable GeneratedAppGlideModule annotationGeneratedModule) {
    ...
    // 创建一个生成 RequestManager 的工厂类
    RequestManagerRetriever.RequestManagerFactory factory = annotationGeneratedModule != null ? annotationGeneratedModule.getRequestManagerFactory(): null;
    builder.setRequestManagerFactory(factory);
    Glide glide = builder.build(applicationContext);
    ...
}
// GlideBuilder#build(context) 方法: 返回一个 Glide 对象
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





## 参考
- [Glide v4 源码解析（二）](https://blog.yorek.xyz/android/3rd-library/glide2/#2-glide) 
- [深入分析Glide源码](https://blog.csdn.net/xx326664162/article/details/107663872)
- [Glide 源码分析解读-基于最新版Glide 4.9.0](https://www.jianshu.com/p/9bb50924d42a)






