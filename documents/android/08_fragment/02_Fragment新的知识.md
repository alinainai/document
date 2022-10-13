## 引用新版本
```groovy
implementation "androidx.appcompat:appcompat:1.3.1"
implementation "androidx.fragment:fragment:1.3.6"
implementation "androidx.fragment:fragment-ktx:1.3.6"
```
## 一、新的初始化方式

```java
public class Fragment implements ComponentCallbacks, View.OnCreateContextMenuListener, LifecycleOwner,
        ViewModelStoreOwner, HasDefaultViewModelProviderFactory, SavedStateRegistryOwner,
        ActivityResultCaller {

    @LayoutRes
    private int mContentLayoutId;

    @ContentView
    public Fragment(@LayoutRes int contentLayoutId) {
        this();
        mContentLayoutId = contentLayoutId;
    }

    @MainThread
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (mContentLayoutId != 0) {
            return inflater.inflate(mContentLayoutId, container, false);
        }
        return null;
    }
}
```

```kotlin
class PlaceholderFragment : Fragment(R.layout.fragment_placeholder)
```

## 二、新的 FragmentContainerView 

使用新控件 `FragmentContainerView` 添加 `Fragment`

## 三、使用 FragmentFactory 实例化 Fragment

为了解决无法自由定义有参构造函数的问题，Fragment 提供了 FragmentFactory 来参与实例化 Fragment 的过程

```kotlin
class MyFragmentFactory(private val bgColor: Int) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        val clazz = loadFragmentClass(classLoader, className)
        if (clazz == FragmentFactoryFragment::class.java) {
            return FragmentFactoryFragment(bgColor)
        }
        return super.instantiate(classLoader, className)
    }
}
```

之后我们在代码中仅需要向 supportFragmentManager 声明需要注入的相关的 xxxFragment.class 即可，无需显式实例化，实例化过程交由 MyFragmentFactory 来完成

```kotlin
class FragmentFactoryActivity : BaseActivity() {
    override val bind by getBind<ActivityFragmentFactoryBinding>()
    override fun onCreate(savedInstanceState: Bundle?) {
        //需要在 super.onCreate 之前调用，因为 Activity 需要依靠 FragmentFactory 来完成 Fragment 的恢复重建
        supportFragmentManager.fragmentFactory = MyFragmentFactory(Color.parseColor("#00BCD4"))
        super.onCreate(savedInstanceState)
        supportFragmentManager.commit {
            add(R.id.fragmentContainerView, FragmentFactoryFragment::class.java, null)
            setReorderingAllowed(true)
            disallowAddToBackStack()
        }
    }
}
```

使用 FragmentFactory 的好处有：

- 将本应该直接传递给 Fragment 的构造参数转交给了 FragmentFactory，这样系统在恢复重建时就能统一通过 instantiate 方法来重新实例化 Fragment，而无需关心 Fragment 的构造函数。

- 只要 FragmentFactory 包含了所有 Fragment 均需要的构造参数，那么同个 FragmentFactory 就可以用于实例化多种不同的 Fragment，从而解决了需要为每个 Fragment 均声明静态工厂方法的问题，Fragment 也省去了向 Bundle 赋值取值的操作，减少了开发者的工作量

FragmentFactory 也存在着局限性：

由于需要考虑 Fragment 恢复重建的场景，因此我们在 super.onCreate 之前就需要先初始化 supportFragmentManager.fragmentFactory，这样 Activity 在恢复重建的时候才能根据已有参数来重新实例化 Fragment，这就要求我们必须在一开始的时候就确定 FragmentFactory 的构造参数，也即 Fragment 的构造参数，而这在日常开发中并非总是能够做到的，因为 Fragment 的构造参数可能是需要动态生成的

## 四、人性化的通讯方式 Fragment Result API

使用 FragmentResult 进行数据通信不需要持有任何 Fragment 或者 Activity 的引用，仅需要使用 FragmentManager 就可以实现

声明一个 Activity 和两个 Fragment，分别向对方正在监听的 requestKey 下发数据，数据通过 Bundle 来进行传递，Activity 和 Fragment 只需要面向特定的 requestKey 来发送数据或者监听数据即可，无需关心数据的来源和去向

- setFragmentResult: 方法表示的是向 requestKey 下发数据
- setFragmentResultListener: 表示的是对 requestKey 进行监听

 ```kotlin
const val requestKeyToActivity = "requestKeyToActivity"
private const val requestKeyFirst = "requestKeyFirst"
private const val requestKeySecond = "requestKeySecond"

class FragmentResultApiAFragment : Fragment(R.layout.fragment_fragment_result_api_a) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnSend.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("fromFragmentResultApiAFragment", Random.nextInt().toString())
            //向对 requestKeyFirst 进行监听的 Fragment 传递数据
            parentFragmentManager.setFragmentResult(requestKeyFirst, bundle)
        }
        btnSendMsgToActivity.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("fromFragmentResultApiAFragment", Random.nextInt().toString())
            //向对 requestKeyToActivity 进行监听的 Activity 传递数据
            parentFragmentManager.setFragmentResult(requestKeyToActivity, bundle)
        }
        //对 requestKeySecond 进行监听
        parentFragmentManager.setFragmentResultListener(
            requestKeySecond,
            this,{ requestKey, result ->
                tvMessage.text = "requestKey: $requestKey \n result: $result"
            })
    }
}

class FragmentResultApiBFragment : Fragment(R.layout.fragment_fragment_result_api_b) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnSend.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("fromFragmentResultApiBFragment", Random.nextInt().toString())
            //向对 requestKeySecond 进行监听的 Fragment 传递数据
            parentFragmentManager.setFragmentResult(requestKeySecond, bundle)
        }
        //对 requestKeyFirst 进行监听
        parentFragmentManager.setFragmentResultListener(
            requestKeyFirst,
            this,
            { requestKey, result ->
                tvMessage.text = "requestKey: $requestKey \n result: $result"
            })
    }
}

class FragmentResultApiActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.setFragmentResultListener(
            requestKeyToActivity,
            this,{ requestKey, result ->
                bind.tvMessage.text = "requestKey: $requestKey \n result: $result"
            })
    }
}
```

使用 Fragment Result API 的好处有：

Fragment Result API 并不局限于 Fragment 之间，只要 Activity 也持有和 Fragment 相同的 FragmentManager 实例，也可以用以上方式在 Activity 和 Fragment 之间实现数据通信。但需要注意，一个 requestKey 所属的数据只能被一个消费者得到，后注册的 FragmentResultListener 会把先注册的给覆盖掉

Fragment Result API 也可以用于在父 Fragment 和子 Fragment 之间传递数据，但前提是两者持有的是相同的 FragmentManager 实例。例如，如果要将数据从子 Fragment 传递给父 Fragment，父 Fragment 应通过getChildFragmentManager() 来调用setFragmentResultListener()，而不是getParentFragmentManager()

数据的发送者只负责下发数据，不关心也不知道有多少个接收者，也不知道数据是否有被消费。数据的接收者只负责接收数据，不关心也不知道有多少个数据源。避免了 Activity 和 Fragment 之间存在引用关系，使得每个个体之间更加独立

只有当 Activity 和 Fragment 至少处于 STARTED 状态，即只有处于活跃状态时才会接收到数据回调，非活跃状态下连续传值也只会保留最新值，当切换到 DESTROYED 状态时也会自动移除监听，从而保证了生命周期的安全性

每一个载体（Activity 或者 Fragment）都包含一个和自身同等级的 FragmentManager 用于管理子 Fragment，对应 Activity 的 supportFragmentManager 和 Fragment 的 childFragmentManager；每一个 子 Fragment 也都包含一个来自于载体的 FragmentManager，对应 Fragment 的 parentFragmentManager

## 5. 通过 OnBackPressedDispatcher 拦截 Activity 的 onBackPressed()

我们可以在 Fragment 中向 Activity 添加一个 OnBackPressedCallback 回调，传递的值 true 即代表该 Fragment 会拦截用户的每一次返回操作并进行回调，我们需要根据业务逻辑在合适的时候将其置为 false，从而放开对onBackPressed的控制权。此外，addCallback 方法的第一个参数是 LifecycleOwner 类型，也即当前的 Fragment 对象，该参数确保了仅在 Fragment 的生命周期至少处于 ON_START 状态时才进行回调，并在 ON_DESTROY 时自动移除监听，从而保证了生命周期的安全性

 ```java
public class FormEntryFragment extends Fragment {
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        OnBackPressedCallback callback = new OnBackPressedCallback(
            true // default to enabled
        ) {
            @Override
            public void handleOnBackPressed() {
                showAreYouSureDialog();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(
            this, // LifecycleOwner
            callback);
    }
}
 ```

## 参考

[一文读懂 Fragment 的方方面面](https://juejin.cn/post/7006970844542926855#heading-10)
