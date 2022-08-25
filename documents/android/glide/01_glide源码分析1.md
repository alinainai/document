本位基于 glide 4.12.0 代码分析

## 1、基本使用

git地址: [https://github.com/bumptech/glide](https://github.com/bumptech/glide)

添加依赖

```groovy
implementation ‘com.github.bumptech.glide:compiler:4.12.0’
```

## 2、从使用代码作为突破口

```java
Glide.with(fragment).load(imgUrl).into(imageView);
```

### 2.1  `Glide.with(...)` 方法

`Glide` 类实现了 `ComponentCallbacks2` 接口，`ComponentCallbacks2` 接口提供了 `lowMemory` 的监听方法。

```java
public class Glide implements ComponentCallbacks2 {
```
我们通过 Glide.with(...) 获取一个 RequestManager （看名字是请求的管理类）对象，在 Glide 类中还有很多 with(...) 静态重载方法。
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
public static RequestManager with(@NonNull FragmentActivity activity) {
    return getRetriever(activity).get(activity);
}
@NonNull
public static RequestManager with(@NonNull Fragment fragment) {
    return getRetriever(fragment.getContext()).get(fragment);
}
@NonNull
public static RequestManager with(@NonNull View view) {
    return getRetriever(view.getContext()).get(view);
}
```
每个重载方法中都会调用 `getRetriever(context)` 方法获取一个 `RequestManagerRetriever` 对象，RequestManagerRetriever 是负责管理 RequestManager 的类。
A collection of static methods for creating new RequestManagers or retrieving existing ones from activities and fragment. 有一系列的静态方法用来创建或者复用 activities 和 fragment 中存在的 RequestManagers。它是在 Glide 对象初始化的时候由 GlideBuilder 生成并传入 Glide 中。

```java
@NonNull
private static RequestManagerRetriever getRetriever(@Nullable Context context) {
    return Glide.get(context).getRequestManagerRetriever(); 
}
```
在 getRetriever(context) 方法中 通过调用 Glide#get(context) 获取一个 Glide 对象。









