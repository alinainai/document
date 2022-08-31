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


```kotlin
// 1.不带参数的 ViewModel 类
class MainViewModel() : ViewModel()

// 2.带参数的 ViewModel 类，通过工厂类创建带参数的 ViewModel
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
class MainActivity : AppCompatActivity() {
    private lateinit var mainViewModel: MainViewModel
    private lateinit var loginViewModel: LoginViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        loginViewModel = ViewModelProvider(this, LoginViewModelFactory()).get(LoginViewModel::class.java)
    }
}
```
## 2、源码解读

我们以不带参数的 MainViewModel 的例子出发，可以将代码拆分成：

- `ViewModelProvider(this)` 
- `get(MainViewModel::class.java)`

### 2.1 `ViewModelProvider`的构造方法

ViewModelProvider 最终的构造方法有两个入参:
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
在例子中`ViewModelProvider(owne)`传入的 ower 是 `Activity`的 this 对象，所以 Activity 实现了 ViewModelStoreOwner 接口。ViewModelStoreOwner 接口中只有 `getViewModelStore():ViewModelStore`一个方法。

我们追踪到 ComponentActivity 类，该类实现了 ViewModelStoreOwner 接口，并且内部维护了一个 ViewModelStore 对象。 

```java
// ComponentActivity.java
public ViewModelStore getViewModelStore() {
    if (getApplication() == null) {
        throw new IllegalStateException("Your activity is not yet attached to the Application instance. You can't request ViewModel before onCreate call.");
    }
    ensureViewModelStore(); // 保证 ViewModelStore 不为空
    return mViewModelStore;
}

@SuppressWarnings("WeakerAccess") /* synthetic access */
void ensureViewModelStore() {
    if (mViewModelStore == null) {
        NonConfigurationInstances nc =  (NonConfigurationInstances) getLastNonConfigurationInstance();
        if (nc != null) {
            // Restore the ViewModelStore from NonConfigurationInstances
            mViewModelStore = nc.viewModelStore; // 先看 mtLastNonConfigurationInstance 有没有存储 mViewModelStore 对象
        }
        if (mViewModelStore == null) { // 如果缓存里面没有，直接创建新的
            mViewModelStore = new ViewModelStore();
        }
    }
}
```
ViewModelStoreOwner 是一个单一方法接口
```java
public interface ViewModelStoreOwner {
    @NonNull
    ViewModelStore getViewModelStore(); // 对外提供一个 ViewModelStore 对象
}
```
ViewModelStore 类是 ViewModels 的存储类，代码很简单。持有一个 HashMap 用来存储 Key:ViewModel 的映射。
```java
public class ViewModelStore {

    private final HashMap<String, ViewModel> mMap = new HashMap<>();

    final void put(String key, ViewModel viewModel) {
        ViewModel oldViewModel = mMap.put(key, viewModel);
        if (oldViewModel != null) {
            oldViewModel.onCleared();
        }
    }

    final ViewModel get(String key) {
        return mMap.get(key);
    }

    Set<String> keys() {
        return new HashSet<>(mMap.keySet());
    }

    /**
     *  Clears internal storage and notifies ViewModels that they are no longer used.
     */
    public final void clear() {
        for (ViewModel vm : mMap.values()) {
            vm.clear();
        }
        mMap.clear();
    }
}
```
### 2.2 通过 `get(class)` 方法获取 ViewModel 对象

ViewModelProvider 类中的`get(class)`方法的相关代码
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
    if (modelClass.isInstance(viewModel)) {
        if (mFactory instanceof OnRequeryFactory) { // 如果 mFactory 是 OnRequeryFactory 类型
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

## 3、为什么屏幕旋转后 ViewModel 不会重建

官方文档中有说明: ViewModel 生命周期要比 Activity 要长，并且旋转屏幕导致的 Activity 重建不会触发 ViewModel 的重建。

下面我们跟随源看下 ViewModel 在 Activity 销毁的时候做了怎样的处理才能达到这种效果。

### 3.1 Destroy中的处理

Activity 的生命周期回调是通过 ActivityThread 来触发的。我们知道当屏幕旋转之后会执行 Activity 的 onDestroy 方法，所以我们直接来看一下触发 onDestroy 的`ActivityThread#performDestroyActivity`方法，该方法中和 ViewModel 相关的代码逻辑如下:

```java
ActivityClientRecord performDestroyActivity(IBinder token, boolean finishing, int configChanges, boolean getNonConfigInstance, String reason) {
    ActivityClientRecord r = mActivities.get(token);
    ...
    if (getNonConfigInstance) {
        try {
            r.lastNonConfigurationInstances = r.activity.retainNonConfigurationInstances();
        } ...
    }
    return r;
}
```
`activity#retainNonConfigurationInstances()` 内部继续调用 `onRetainNonConfigurationInstance` 方法
```java
NonConfigurationInstances retainNonConfigurationInstances() {
    Object activity = onRetainNonConfigurationInstance(); // 调用 ComponentActivity#onRetainNonConfigurationInstance() 方法
    HashMap<String, Object> children = onRetainNonConfigurationChildInstances();
    NonConfigurationInstances nci = new NonConfigurationInstances();
    nci.activity = activity;
    nci.children = children;
    return nci;
}
```
Activity 类中的 onRetainNonConfigurationInstance 没有方法体,直接返回了 null。所以我们直接看下 `ComponentActivity#onRetainNonConfigurationInstance()` 方法，注意这里是 ComponentActivity 中的方法
```java
public final Object onRetainNonConfigurationInstance() {
    // Maintain backward compatibility.
    Object custom = onRetainCustomNonConfigurationInstance();

    ViewModelStore viewModelStore = mViewModelStore;
    if (viewModelStore == null) {// 如果 mViewModelStore 为空，表示没调用过 getViewModelStore() 方法
        // No one called getViewModelStore(), so see if there was an existing ViewModelStore from our last NonConfigurationInstance
        NonConfigurationInstances nc = (NonConfigurationInstances) getLastNonConfigurationInstance();
        if (nc != null) {
            viewModelStore = nc.viewModelStore;
        }
    }

    if (viewModelStore == null && custom == null) {
        return null;
    }
    // 将 ViewModelStore 包装为一个 ComponetActivity$NonConfigurationInstances 对象并返回
    NonConfigurationInstances nci = new NonConfigurationInstances();     
    nci.custom = custom;
    nci.viewModelStore = viewModelStore;
    return nci;
}
```
当 mViewModelStore 为空的时候获取 getLastNonConfigurationInstance() 中存储的 NonConfigurationInstances 对象。如果 mViewModelStore 和 getLastNonConfigurationInstance 中的 ViewModelStore 都为空直接返回 null。

NonConfigurationInstances 类很简单，简单的包装了一个 ViewModelStore 对象。
```java
// ComponentActivity$NonConfigurationInstances.java
static final class NonConfigurationInstances {
    Object custom;
    ViewModelStore viewModelStore;
}
```

ComponentActivity#onRetainNonConfigurationInstance() 方法最终会返回一个包装了 ViewModelStore 的 ComponetActivity$NonConfigurationInstances 对象。

继续看一下 Activity#getLastNonConfigurationInstance() 方法

```java
public Object getLastNonConfigurationInstance() {// 获取上次存贮在 mLastNonConfigurationInstances 的 NonConfigurationInstances 对象
    return mLastNonConfigurationInstances != null? mLastNonConfigurationInstances.activity : null;
}
```
通过上面代码分析我们知道，调用 ActivityThread#performDestroyActivity的时候，会将包含 ViewModelStore 的数据存储在 ActivityClientRecord.lastNonConfigurationInstances 中。

### 3.2 在启动时  

在 Activity启动流程中，的我们知道Activity的启动是 ActivityThread#performLaunchActivity 触发的，在该方法内部会调用 `Activity#attach()` 方法

```java
private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
    activity.attach(appContext, this, getInstrumentation(), r.token,
                    r.ident, app, r.intent, r.activityInfo, title, r.parent,
                    r.embeddedID, r.lastNonConfigurationInstances, config,
                    r.referrer, r.voiceInteractor, window, r.configCallback,
                    r.assistToken);    
}
```

在 Activity#attach 方法中将 ActivityClientRecord 中的 lastNonConfigurationInstances 赋值给 Activity.mLastNonConfigurationInstances 对象。
```java
final void attach(Context context, ActivityThread aThread,
            Instrumentation instr, IBinder token, int ident,
            Application application, Intent intent, ActivityInfo info,
            CharSequence title, Activity parent, String id,
            NonConfigurationInstances lastNonConfigurationInstances,
            Configuration config, String referrer, IVoiceInteractor voiceInteractor,
            Window window, ActivityConfigCallback activityConfigCallback, IBinder assistToken) {
        attachBaseContext(context);
        ···
        mLastNonConfigurationInstances = lastNonConfigurationInstances;
    }
```
上文中我们提到过 ComponentActivity#getViewModelStore() 方法中会调用 ensureViewModelStore()方法来保证获取的 mViewModelStore 不为空。ensureViewModelStore() 方法
中会优先通过 getLastNonConfigurationInstance() 方法看看 mLastNonConfigurationInstances 中有没有缓存 NonConfigurationInstances 对象。这样就形成了一个闭环。
```java
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
## 4、ViewModel的onCleared()

分析过了 ViewModel 的创建过程，再来看看 ViewModel 的销毁过程。

在 ComponentActivity 构造方法中，注册一个匿名的 LifecycleEventObserver 对象。当页面不是因为 ChangingConfiguration 引起的销毁，调用 ViewModelStore 中的 clear() 方法。
```java
public ComponentActivity() {
    ...
    getLifecycle().addObserver(new LifecycleEventObserver() {
        @Override
        public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                if (!isChangingConfigurations()) { // 不是因为 ChangingConfiguration 引起的销毁
                    getViewModelStore().clear();
                }
            }
        }
    });
}
```
在 ViewModelStore 中，调用 Map 中每个 ViewModel 实例的 clear()方法，最后将 map 清空，至此，完成了 ViewModel 的销毁工作。
```java
public final void clear() {
    for (ViewModel vm : mMap.values()) {
        vm.clear();
    }
    mMap.clear();
}
```












