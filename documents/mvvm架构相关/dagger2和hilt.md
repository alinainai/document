### 1. 依赖注入的好处

依赖注入，解耦，保证代码的健壮性、灵活性和可维护性

### 2. dagger用到的注解

#### 2.1 @Inject （注入）
两个用处：
- 1.标记需要注入的对象。
- 2.标记注入对象使用的构造函数。
#### 2.2 @Module （模块）

@Module用于标注提供依赖的类，第三方的类中的构造方法没有添加 @Inject 注解可以通过 Module 添加，结合 @Provides 和 @Binder 使用。

#### 2.3 @Provides 和 @Binder 

在 Module 中注入数据的标注

#### 2.4 @Component 
生成 DaggerXXXComponent 的注解，标注接口

#### 2.5 @Qulifier 

区分同一种 type 生成的不同的实例

#### 2.6 @Scope 和 @Singleten 

@Scope 限定变量的使用范围，形成局部单例

@Singleten 是 Scope 一个特例，App的单例

### 3. Dagger使用的实例

#### 3.1 inject 修饰构造方法实现注入

@Inject 修饰数据的构造方法

```kotlin
data class Data constructor(var str:String){
   @Inject constructor():this("花花草草")
}
```

@Component 修饰 DataComponent 生成实现依赖注入的组件

```kotlin
@Component
interface DataComponent {
    fun inject(activity: DaggerActivity)
}
```

@Inject 修饰需要注入的实例

```kotlin
class DaggerActivity : AppCompatActivity() {

    @Inject
    lateinit var data: Data
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 由 DataComponent 生成的 DaggerDataComponent，实现注入
        DaggerDataComponent.builder().build().inject(this);
        setContentView(R.layout.activity_dagger2_ex1)
        Log.e("dagger2", "${data.str}") // E/dagger2: 花花草草
    }
}
```

最后打印

```shell
// E/dagger2: 花花草草
```
