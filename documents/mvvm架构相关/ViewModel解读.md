#### 1.简单使用

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
#### 2.源码解读

从 LoginActivity 入手








