### 1.简单使用

ViewModel 是管理 View 使用数据的并可以监听生命周期容器类。

为什么使用官方的 ViewModel 而不是使用自定义（就想 MVP 中的 Presenter）的类去管理数据呢？

因为 ViewModel 可以很好的和 Activity/Fragment 的生命周期相绑定，在 ViewModel # OnClear() 方法中处理一些解绑的操作。

ViewModel 在 Activity 横竖屏切换时也会保持同一个对象。

官方的生命周期图：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/141035994-bc844b3e-b496-4872-8443-4b6a79f9b8ee.png">

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

**ViewModelProvider(this)**:

```kotlin
// ViewModelProvider.java
public ViewModelProvider(@NonNull ViewModelStoreOwner owner) {
    this(owner.getViewModelStore(), owner instanceof HasDefaultViewModelProviderFactory
         ? ((HasDefaultViewModelProviderFactory) owner).getDefaultViewModelProviderFactory()
         : NewInstanceFactory.getInstance());
}

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
        // 先从 NonConfigurationInstances 获取
        NonConfigurationInstances nc =
            (NonConfigurationInstances) getLastNonConfigurationInstance();
        if (nc != null) {
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






屏幕旋转前，Activity销毁时：
ComponentActivity调用onRetainNonConfigurationInstance()方法，将要销毁的Activity的mViewModelStore转化为NonConfigurationInstances对象，继续调用Activity的retainNonConfigurationInstances()方法，最终在ActivityThread的performDestroyActivity()中将数据保存在ActivityClientRecord中。
Activity重建后：
在Activity启动时，ActivityThread调用performLaunchActivity()方法，将存储在ActivityClientRecord中的lastNonConfigurationInstances通过Activity的attach()方法传递到对应的Activity中，然后通过getLastNonConfigurationInstance()恢复mViewModelStore实例对象，最后根据对应的key拿到销毁前对应的ViewModel实例。
此外，当系统内存不足，系统将后台应用回收后，ViewModel中的数据不会恢复。










