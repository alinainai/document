### 1. App Startup 是干什么的

App Startup 提供了一个 ContentProvider 来运行所有依赖项的初始化，避免每个第三方库单独使用 ContentProvider 进行初始化，从而提高了应用的程序的启动速度。

<img width="300" alt="类图" src="https://user-images.githubusercontent.com/17560388/162897677-0a5d544f-62bd-45fb-a73b-cc35cd44783d.png">

### 2. App Startup 自动初始化

1.引入 Startup 库
```groove
implementation("androidx.startup:startup-runtime:1.1.1")
```
2.实现 Initializer<T> 接口

- create() 方法：主要初始化方法，并返回一个 T 的实例。
- dependencies() 方法：返回一个该 initializer 依赖的其他 Initializer<T> 的列表，用这个方法来控制 startup 的顺序。

看一下郭霖老师 LitePal 的例子

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
  
3.将自定义Initializer配置到AndroidManifest.xml当中
  
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
  
### 3. App Startup 手动初始化（延迟初始化)
  
LitePalInitializer的meta-data当中加入了一个 tools:node="remove" 的标记

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
  
手动去初始化LitePal的代码也极其简单

```kotlin
AppInitializer.getInstance(this).initializeComponent(LitePalInitializer::class.java)
```
  
### 4. Disable automatic initialization for all components
  
To disable all automatic initialization, remove the entire entry for InitializationProvider from the manifest:

```html 
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    tools:node="remove" />  
```  
### 5. 官方带依赖的例子

[App Startup 使用](https://developer.android.com/topic/libraries/app-startup)


### 参考

[Android 开发者>Jetpack>Startup](https://developer.android.com/jetpack/androidx/releases/startup)
  
[App Startup 使用](https://developer.android.com/topic/libraries/app-startup)
  
[Jetpack新成员，App Startup一篇就懂](https://mp.weixin.qq.com/s?__biz=MzA5MzI3NjE2MA==&mid=2650251523&idx=1&sn=3409d80cc6c4252cbd4fb0e327eb3dcc&chksm=8863506cbf14d97aa6b640b6794395a158137e97b9db5804e2718b204affa3bb5c2aba8f6676&mpshare=1&scene=24&srcid=08259PAiFnKfqf8selFIZ3qD&sharer_sharetime=1598317827172&sharer_shareid=653d606fda642b58c9d033eeb6c60861#rd)
