## 1. App Startup 是干什么的

`App Startup` 提供了一个 `ContentProvider` 来运行所有依赖项的初始化，避免每个第三方库单独使用 `ContentProvider` 进行初始化，从而提高了应用的程序的启动速度。

<img width="200" alt="类图" src="https://user-images.githubusercontent.com/17560388/162897677-0a5d544f-62bd-45fb-a73b-cc35cd44783d.png">

## 2. 自动初始化

### 2.1 导包

引入 Startup 库

```groove
implementation("androidx.startup:startup-runtime:1.1.1")
```
### 2.2 实现 Initializer<T> 接口

- create() 方法：主要初始化方法，并返回一个 T 的实例。
- dependencies() 方法：返回一个该 initializer 依赖的其他 Initializer<T> 的列表，用这个方法来控制 startup 的顺序。

看一下 `LitePal` 的例子

```kotlin
class LitePalInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        LitePal.initialize(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList() // LitePalInitializer 没有依赖于其他的 Initializer，这里返回 emptyList
    }
}
```  
  
### 2.3 将自定义 Initializer 配置到 AndroidManifest.xml 当中
  
```html
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="com.example.LitePalInitializer"
        android:value="androidx.startup" />
</provider>
```
  
## 3.手动初始化（延迟初始化)
    
如果我们不想在App启动的时候自动初始化 LitePalInitializer，可以采用手动初始化的方式去启动
   
在 `LitePalInitializer` 的 `meta-data` 当中加入了一个 `tools:node="remove"` 的标记

```html 
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="com.example.LitePalInitializer"
        tools:node="remove" />
</provider>
```
  
手动初始化 LitePal

```kotlin
AppInitializer.getInstance(this).initializeComponent(LitePalInitializer::class.java)
```
  
## 4. Disable automatic initialization for all components
  
To disable all automatic initialization, remove the entire entry for InitializationProvider from the manifest:

```html 
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    tools:node="remove" />  
```  
## 5.官方带依赖的例子

[App Startup 使用](https://developer.android.com/topic/libraries/app-startup)

## 6.自动初始化的源码分析
    
App Startup 在 ContentProvider 中调用了AppInitializer#discoverAndInitialize()执行自动初始化。
    
先看下 AppInitializer#discoverAndInitialize 方法

```java
final Set<Class<? extends Initializer<?>>> mDiscovered;

void discoverAndInitialize() {
   
    ComponentName provider = new ComponentName(mContext.getPackageName(), InitializationProvider.class.getName());
    ProviderInfo providerInfo = mContext.getPackageManager().getProviderInfo(provider, GET_META_DATA);
    String startup = mContext.getString(R.string.androidx_startup);
    Bundle metadata = providerInfo.metaData;
    
    // 核心代码：遍历 meta-data 数据
    if (metadata != null) {
        Set<Class<?>> initializing = new HashSet<>();
        Set<String> keys = metadata.keySet();
        for (String key : keys) {
            String value = metadata.getString(key, null);
            if (startup.equals(value)) {
                Class<?> clazz = Class.forName(key);
                if (Initializer.class.isAssignableFrom(clazz)) {
                    Class<? extends Initializer<?>> component = (Class<? extends Initializer<?>>) clazz;
                    mDiscovered.add(component);
                    // 初始化此组件
                    doInitialize(component, initializing);
                }
            }
        }
    }
}

// mDiscovered 用于判断组件是否已经自动启动
public boolean isEagerlyInitialized(@NonNull Class<? extends Initializer<?>> component) {
    return mDiscovered.contains(component);
}
```
AppInitializer#doInitialize 方法
```java
private static final Object sLock = new Object();
    
final Map<Class<?>, Object> mInitialized;

<T> T doInitialize(Class<? extends Initializer<?>> component, Set<Class<?>> initializing) {
    Object result;
    
    // 判断 initializing 中存在当前组件，说明存在循环依赖
    if (initializing.contains(component)) {
        String message = String.format("Cannot initialize %s. Cycle detected.", component.getName());
        throw new IllegalStateException(message);
    }
    // 检查当前组件是否已初始化
    if (!mInitialized.containsKey(component)) {
        initializing.add(component);
        // 反射实例化 Initializer 接口
        Object instance = component.getDeclaredConstructor().newInstance();
        Initializer<?> initializer = (Initializer<?>) instance;
        // 遍历所依赖的组件
        List<Class<? extends Initializer<?>>> dependencies = initializer.dependencies();
        if (!dependencies.isEmpty()) {
            for (Class<? extends Initializer<?>> clazz : dependencies) {        
                // 如果所依赖的组件未初始化，递归执行初始化
                if (!mInitialized.containsKey(clazz)) {
                    doInitialize(clazz, initializing); 注意：这里将 initializing 作为参数传入
                }
            }
        }
        result = initializer.create(mContext);
        initializing.remove(component);
        mInitialized.put(component, result);
    } else {
        result = mInitialized.get(component);
    }
     return (T) result;
}
```

手动初始化（懒加载）的源码分析
```java
public <T> T initializeComponent(@NonNull Class<? extends Initializer<T>> component) {
    return return doInitialize(component); // 此方法加锁
}
@NonNull
@SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
<T> T doInitialize(@NonNull Class<? extends Initializer<?>> component) {
    Object result;
    synchronized (sLock) {
        result = mInitialized.get(component);
        if (result == null) {
            result = doInitialize(component, new HashSet<Class<?>>());
        }
    }
    return (T) result;
}
```
## 参考

[Android 开发者>Jetpack>Startup](https://developer.android.com/jetpack/androidx/releases/startup)
  
[App Startup 使用](https://developer.android.com/topic/libraries/app-startup)
  
[Jetpack新成员，App Startup一篇就懂](https://mp.weixin.qq.com/s?__biz=MzA5MzI3NjE2MA==&mid=2650251523&idx=1&sn=3409d80cc6c4252cbd4fb0e327eb3dcc&chksm=8863506cbf14d97aa6b640b6794395a158137e97b9db5804e2718b204affa3bb5c2aba8f6676&mpshare=1&scene=24&srcid=08259PAiFnKfqf8selFIZ3qD&sharer_sharetime=1598317827172&sharer_shareid=653d606fda642b58c9d033eeb6c60861#rd)
