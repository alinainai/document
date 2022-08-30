## 1、简单使用

MVVM 架构的中VM，负责`View层`和`Model层`的交互。以注重生命周期的方式存储和管理界面相关数据。

`ViewModel` 底层提供了和 `Activity/Fragment` 的生命周期相绑定的逻辑，在界面销毁时调用 `ViewModel#OnClear()` 方法，我们可以在该方法中处理一些解绑的操作。

### 1.1 特点

特点：在 Activity 横竖屏切换时也会保持同一个对象。
注意：内部不要持有 Activity/Fragment 对象。因为生命周期比 Activity/Fragment 长，可能会引起泄漏。

ViewModel 对象存在的时间范围是获取 ViewModel 时传递给 ViewModelProvider 的 Lifecycle。ViewModel 将一直留在内存中，直到限定其存在时间范围的 Lifecycle 永久消失：
- 对于 Activity，是在 Activity 完成时；
- 而对于 Fragment，是在 Fragment 分离时。

<img width="400" alt="ViewModel生命周期" src="https://user-images.githubusercontent.com/17560388/141035994-bc844b3e-b496-4872-8443-4b6a79f9b8ee.png">

### 1.2 基本使用

1.不带参数的 ViewModel 类
```kotlin
class MainViewModel() : ViewModel() 
```
在 Activity 中初始化
```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var mainViewModel: MainViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
    }
}
```
2.带参数的 ViewModel 类，通过工厂类创建带参数的 ViewModel

```kotlin
class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel() 
class LoginViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(loginRepository = LoginRepository(dataSource = LoginDataSource())) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```
在 Activity 中初始化
```kotlin
class LoginActivity : AppCompatActivity() {
    private lateinit var loginViewModel: LoginViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loginViewModel = ViewModelProvider(this, LoginViewModelFactory()).get(LoginViewModel::class.java)
    }
}
```

## 2、源码解读

我们以不带参数的 MainViewModel 的例子出发，可以将代码拆分成：`ViewModelProvider(this)` 和 `get(MainViewModel::class.java)`

### 2.1 `ViewModelProvider`的构造方法

ViewModelProvider 最终的构造方法有两个入参，我们先从 ViewModelProvider(owne) 开始调用逻辑

- ViewModelStore: ViewModelStore 类内部维护一个 HashMap 来存储 `Key:ViewModel` 键值对
- ViewModelProvider.Factory: 负责生成的 `ViewModel`，带参数的 ViewModel 需要自己定义 Factory

```java
public ViewModelProvider(@NonNull ViewModelStoreOwner owner) {
    this(owner.getViewModelStore(), owner instanceof HasDefaultViewModelProviderFactory
         ? ((HasDefaultViewModelProviderFactory) owner).getDefaultViewModelProviderFactory()
         : NewInstanceFactory.getInstance());
}
public ViewModelProvider(@NonNull ViewModelStoreOwner owner, @NonNull Factory factory) {
    this(owner.getViewModelStore(), factory);
}
public ViewModelProvider(@NonNull ViewModelStore store, @NonNull Factory factory) {
    mFactory = factory;
    mViewModelStore = store;
}
```
我们在 Activity 中使用的是 `ViewModelProvider(owne)` ，所以 Activity 实现了 ViewModelStoreOwner 接口。
ViewModelStoreOwner 接口中只有 `getViewModelStore():ViewModelStore`一个方法，我们追踪到 ComponentActivity 类，该类实现了 ViewModelStoreOwner 接口，并且内部维护了一个 mViewModelStore 对象。 

```java
// ComponentActivity.java
public ViewModelStore getViewModelStore() {
    if (getApplication() == null) {
        throw new IllegalStateException("Your activity is not yet attached to the Application instance. You can't request ViewModel before onCreate call.");
    }
    ensureViewModelStore();
    return mViewModelStore;
}

@SuppressWarnings("WeakerAccess") /* synthetic access */
void ensureViewModelStore() {
    if (mViewModelStore == null) {
        NonConfigurationInstances nc =  (NonConfigurationInstances) getLastNonConfigurationInstance();
        if (nc != null) {
            // Restore the ViewModelStore from NonConfigurationInstances
            mViewModelStore = nc.viewModelStore;
        }
        if (mViewModelStore == null) { // 如果缓存里面没有，直接创建新的
            mViewModelStore = new ViewModelStore();
        }
    }
}
```

NonConfigurationInstances 类很简单，简单的包装了一个 ViewModelStore 对象。

```java
// ComponentActivity$NonConfigurationInstances.java
static final class NonConfigurationInstances {
    Object custom;
    ViewModelStore viewModelStore;
}
```
### 2.2 通过 `get(MainViewModel::class.java)` 方法获取 ViewModel 对象

ViewModelProvider类
```java
private static final String DEFAULT_KEY ="androidx.lifecycle.ViewModelProvider.DefaultKey";

public <T extends ViewModel> T get(@NonNull Class<T> modelClass) {
    String canonicalName = modelClass.getCanonicalName();
    if (canonicalName == null) {
        throw new IllegalArgumentException("Local and anonymous classes can not be ViewModels");
    }
    return get(DEFAULT_KEY + ":" + canonicalName, modelClass);
}

public <T extends ViewModel> T get(@NonNull String key, @NonNull Class<T> modelClass) {
    ViewModel viewModel = mViewModelStore.get(key);

    if (modelClass.isInstance(viewModel)) {// 如果 mFactory 是 OnRequeryFactory 类型
        if (mFactory instanceof OnRequeryFactory) {
            ((OnRequeryFactory) mFactory).onRequery(viewModel);
        }
        return (T) viewModel;
    } else {
        //noinspection StatementWithEmptyBody
        if (viewModel != null) {
            // TODO: log a warning.
        }
    }
    if (mFactory instanceof KeyedFactory) {
        viewModel = ((KeyedFactory) mFactory).create(key, modelClass);
    } else {
        viewModel = mFactory.create(modelClass);
    }
    mViewModelStore.put(key, viewModel); // 将 viewModel 存入 mViewModelStore，缓存一下
    return (T) viewModel;
}
```

## 3、为什么ViewModel的生命周期比Activity长

通过 ComponentActivity#onRetainNonConfigurationInstance() 方法，在屏幕旋转前存储 ViewModelStore 对象

```java
// ComponentActivity.java
public final Object onRetainNonConfigurationInstance() {
    // Maintain backward compatibility.
    Object custom = onRetainCustomNonConfigurationInstance();

    ViewModelStore viewModelStore = mViewModelStore;
    if (viewModelStore == null) {
        // No one called getViewModelStore(), so see if there was an existing
        // ViewModelStore from our last NonConfigurationInstance
        NonConfigurationInstances nc =
            (NonConfigurationInstances) getLastNonConfigurationInstance();
        if (nc != null) {
            viewModelStore = nc.viewModelStore;
        }
    }

    if (viewModelStore == null && custom == null) {
        return null;
    }
    // 存储的
    NonConfigurationInstances nci = new NonConfigurationInstances();
    nci.custom = custom;
    nci.viewModelStore = viewModelStore;
    return nci;
}
```

Activity 重建后，通过 Activity#getLastNonConfigurationInstance() 恢复。

```java
// Activity.java
public Object getLastNonConfigurationInstance() {
    return mLastNonConfigurationInstances != null
        ? mLastNonConfigurationInstances.activity : null;
}
```
mLastNonConfigurationInstances 赋值的地方：

```java
// Activity.java
final void attach(Context context, ActivityThread aThread,
            Instrumentation instr, IBinder token, int ident,
            Application application, Intent intent, ActivityInfo info,
            CharSequence title, Activity parent, String id,
            NonConfigurationInstances lastNonConfigurationInstances,
            Configuration config, String referrer, IVoiceInteractor voiceInteractor,
            Window window, ActivityConfigCallback activityConfigCallback, IBinder assistToken) {
        attachBaseContext(context);

        //···
        mLastNonConfigurationInstances = lastNonConfigurationInstances;
        //···
    }
```

从 Activity 的启动流程可知，Activity#attach() 方法是在 ActivityThread 调用的：

```java
// ActivityThread.java

Activity.NonConfigurationInstances lastNonConfigurationInstances;

/**  Core implementation of activity launch. */
private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
    activity.attach(appContext, this, getInstrumentation(), r.token,
                    r.ident, app, r.intent, r.activityInfo, title, r.parent,
                    r.embeddedID, r.lastNonConfigurationInstances, config,
                    r.referrer, r.voiceInteractor, window, r.configCallback,
                    r.assistToken);    
}
```
通过上面代码分析我们知道，数据存储在 ActivityClientRecord 中，在 Activity 启动时将 ActivityClientRecord 中 的lastNonConfigurationInstances 通过 attach() 方法赋值到对应的Activity 中，然后通过 getLastNonConfigurationInstance() 恢复数据。

### 4.屏幕旋转前数据的存储

屏幕旋转前，数据在 onRetainNonConfigurationInstance()  保存。在Activity的retainNonConfigurationInstances()方法中被调用。
那 retainNonConfigurationInstances() 方法又是在哪调用的呢？肯定也跟 ActivityThread 有关，在ActivityThread搜索下，代码如下：
```java
// ActivityThread.java    
ActivityClientRecord performDestroyActivity(IBinder token, boolean finishing, int configChanges, boolean getNonConfigInstance, String reason) {
    ActivityClientRecord r = mActivities.get(token);
    ···
        if (getNonConfigInstance) {
            try {
                r.lastNonConfigurationInstances = r.activity.retainNonConfigurationInstances();
            } catch (Exception e) {
                ···
            }
        }
    ···
    return r;
}
```

performDestroyActivity() 调用了 retainNonConfigurationInstances() 方法并把数据保存到了 ActivityClientRecord 的 lastNonConfigurationInstances 中。

### 5. 为什么屏幕旋转后 ViewModel 不会重建

首先看下屏幕旋转的生命周期

<img width="583" alt="image" src="https://user-images.githubusercontent.com/17560388/154606788-7435824e-8844-4bd1-8739-61d2a250b024.png">

屏幕旋转前，Activity销毁时：

ComponentActivity 调用 onRetainNonConfigurationInstance() 方法，将要销毁的 Activity 的 mViewModelStore 转化为 NonConfigurationInstances 对象，继续调用 Activity 的retainNonConfigurationInstances() 方法，最终在 ActivityThread 的 performDestroyActivity() 中将数据保存在 ActivityClientRecord 中。

Activity重建后：

在 Activity 启动时，ActivityThread 调用 performLaunchActivity() 方法，将存储在 ActivityClientRecord 中的 lastNonConfigurationInstances 通过 Activity 的 attach() 方法传递到对应的 Activity 中，然后通过 getLastNonConfigurationInstance() 恢复 mViewModelStore 实例对象，最后根据对应的 key 拿到销毁前对应的 ViewModel 实例。

此外，当系统内存不足，系统将后台应用回收后，ViewModel中的数据不会恢复。










