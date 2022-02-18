### 1.简单使用

ViewModel 是管理 View 使用数据的并可以监听生命周期容器类。

为什么使用官方的 ViewModel 而不是使用自定义（就想 MVP 中的 Presenter）的类去管理数据呢？

因为 ViewModel 可以很好的和 Activity/Fragment 的生命周期相绑定，在 ViewModel # OnClear() 方法中处理一些解绑的操作。

ViewModel 在 Activity 横竖屏切换时也会保持同一个对象。

官方的生命周期图：

<img width="400" alt="ViewModel生命周期" src="https://user-images.githubusercontent.com/17560388/141035994-bc844b3e-b496-4872-8443-4b6a79f9b8ee.png">

简单的介绍一下带参数的 ViewModel 类基本使用：

```kotlin
//带参数的 ViewModel 类
class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel() {
    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm
    //... ...
}
```

通过工厂类实现带参数的 ViewModel

```kotlin
class LoginViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(
                loginRepository = LoginRepository(
                    dataSource = LoginDataSource()
                )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

在 Activity 中使用

```kotlin
class LoginActivity : AppCompatActivity() {
    private lateinit var loginViewModel: LoginViewModel
    //... ...
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)
    }
    //... ...
}
```
### 2.源码解读

获取ViewModel实例

```kotlin
// MainActivity.kt
val viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
```

将代码拆分成：ViewModelProvider(this) 和 get(MainViewModel::class.java)

#### 2.1 **ViewModelProvider(this)** 的构造方法

构造方法的两个入参

- ViewModelStore : ViewModelStore 类内部维护一个 HashMap 来存储 `Key:ViewModel` 键值对
- ViewModelProvider.Factory : 负责生成的 `ViewModel`

```java
//ViewModelProvider.java
public ViewModelProvider(@NonNull ViewModelStoreOwner owner) {
    this(owner.getViewModelStore(), owner instanceof HasDefaultViewModelProviderFactory
         ? ((HasDefaultViewModelProviderFactory) owner).getDefaultViewModelProviderFactory()
         : NewInstanceFactory.getInstance());
}
```

通过 ComponentActivity # getViewModelStore() 获取 Activity 中的 ViewModelStore 对象。

```java
// ComponentActivity.java
public ViewModelStore getViewModelStore() {
    if (getApplication() == null) {
        throw new IllegalStateException("Your activity is not yet attached to the "
                                        + "Application instance. You can't request ViewModel before onCreate call.");
    }
    ensureViewModelStore();
    return mViewModelStore;
}

@SuppressWarnings("WeakerAccess") /* synthetic access */
void ensureViewModelStore() {
    if (mViewModelStore == null) {
        NonConfigurationInstances nc =  (NonConfigurationInstances) getLastNonConfigurationInstance();
        if (nc != null) { //先判断是否能从 NonConfigurationInstances 获取到 ViewModelStore 对象
            // Restore the ViewModelStore from NonConfigurationInstances
            // 从缓存中恢复
            mViewModelStore = nc.viewModelStore;
        }
        // 如果缓存里面没有，直接创建新的
        if (mViewModelStore == null) {
            mViewModelStore = new ViewModelStore();
        }
    }
}
```

NonConfigurationInstances 类很简单，简单的包装 ViewModelStore 对象。

```java
// ComponentActivity$NonConfigurationInstances.java
static final class NonConfigurationInstances {
    Object custom;
    ViewModelStore viewModelStore;
}
```
#### 2.2 通过 get(MainViewModel::class.java) 方法获取 ViewModel 对象

```java
// ViewModelProvider.java

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

    if (modelClass.isInstance(viewModel)) {
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
    mViewModelStore.put(key, viewModel);
    return (T) viewModel;
}
```

### 3. 通过 ComponentActivity#onRetainNonConfigurationInstance() 方法，在屏幕旋转前存储 ViewModelStore 对象

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
ActivityClientRecord performDestroyActivity(IBinder token, boolean finishing,
                                            int configChanges, boolean getNonConfigInstance, String reason) {
    ActivityClientRecord r = mActivities.get(token);
    ···
        if (getNonConfigInstance) {
            try {
                r.lastNonConfigurationInstances
                    = r.activity.retainNonConfigurationInstances();
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










