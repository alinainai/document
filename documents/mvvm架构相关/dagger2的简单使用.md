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

#### 3.1 @inject 修饰构造方法实现注入

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
#### 3.2 Module 中使用 @Provides 提供数据

一般我们使用的第三方库不会提供 @Inject 修饰的构造参数，如 Retrofit 和 Okhttp 等库。这种情况下我们可以使用 @Module 实现注入数据的提供方。

首先，数据类中去掉 @Inject 

```kotlin
data class Data constructor(var str:String)
```

```kotlin
@Module
object DataModule {
    @Provides
    fun provideData(): Data {
      return Data("草草花花")
    }
}
```

将 moudule 添加到 Component 中

```kotlin
@Component(modules = [DataModule2::class])
interface DataComponent {
    fun inject(activity: DaggerActivity)
}
```

同 3.1 中的 DaggerActivity 一样，@Inject 修饰需要注入的实例

```kotlin
class DaggerActivity : AppCompatActivity() {

    @Inject
    lateinit var data: Data
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 由 DataComponent 生成的 DaggerDataComponent，实现注入
        //Tips : https://dagger.dev/unused-modules 可以不添加 module
        DaggerDataComponent.builder().build().inject(this);
        setContentView(R.layout.activity_dagger2_ex1)
        Log.e("dagger2", "${data.str}") // E/dagger2: 草草花花
    }
}
```

打印数据

```shell
// E/dagger2: 草草花花
```
#### 3.3 Module 中使用 @Binds 提供数据

开发中使用到的接口和实现可以通过 @Binds 来提供数据，比如 dataSource 的实现 remoteDataSource 和 localDataSource。

一个简答的例子：

数据类

``` kotlin
interface ICall {
    fun call()
}
class CallImpl @Inject constructor() :ICall{
    override fun call() {
        Log.e("dagger2","call")
    }
}
```

注意 Module 类是 abstract 的

``` kotlin
@Module
abstract class DataModule1 {
    @TestScope
    @Binds abstract fun bindCall(call:CallImpl):ICall
}
``` 

Component

``` kotlin
@Component(modules = [DataModule1::class])
interface DataComponent {
    fun inject(activity: DaggerActivity)
}
``` 

``` kotlin
class DaggerActivity : AppCompatActivity() {
    @Inject
    lateinit var call: ICall
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerDataComponent.builder().build().inject(this);
        setContentView(R.layout.activity_dagger2_ex1)
        call.call()
    }
}
``` 
### 4.通过添加 @Scope 限制注入实例的使用范围 

首先自定义一个 @Scope
``` kotlin
@Scope
@Retention(value = AnnotationRetention.RUNTIME)
annotation class TestScope {
}
``` 
在 3.2中 我们通过 @Provides 提供数据，在 Activity 中我们注入一个 Data 实例，我们在注入一个 Data 实例。

这样 Dagger 会注入两个实例，如果想让两个实例是同一个实例，需要给 Component 和 @Provider 的方法添加 @TestScope 注解。

``` kotlin
class DaggerActivity : AppCompatActivity() {
    @Inject
    lateinit var data: Data
    @Inject
    lateinit var data1: Data
    ... ...
}
``` 

``` kotlin
@TestScope
@Component(modules = [DataModule1::class])
interface DataComponent {
    fun inject(activity: DaggerActivity)
}
@Module
object DataModule {
    @TestScope
    @Provides
    fun provideData(): Data {
      return Data("草草花花")
    }
}
```
这样 data 和 data1 是同一个实例

比如说在 DaggerActivity 中注入一个 Presenter，Presenter 中也有个 Data 对象，

我们要求这个 Data 对象在当前 Activit 相关的类中使用单例，就可以使用上面的方式。

一般我们在使用自定义 Scope 时候，都会自定义 ActivityScope 和 FragmentScope 方便使用。

### 5.通过 @Qulifier 来区分同一中类型的不同实例

还是用 3.2 来举例子。我们在 module 中提供了一个 "草草花花" 的 Data 实例。由于业务需求，我们还想提供一个 "花花草草" Data 实例。

``` kotlin
@Module
object DataModule2 {  
    @Provides
    fun provideRepo(): Data {
        return Data("草草花花")
    }
    //这样做不对
    @Provides
    fun provideRepoEx(): Data {
        return Data("花花草草")
    }
}
``` 
但是 Dagger 不知道我们要注入 Activit 是哪种实例，所以会报一个编译错误。

这种情况下我们我可以使用 @Qulifier 自定义注解来帮助 Dagger 区分我们要注入的哪种实例对象。

首先自定义两个 Qualifier 注解

``` kotlin
@Qualifier
@Retention(value = AnnotationRetention.RUNTIME)
annotation class QualifierA

@Qualifier
@Retention(value = AnnotationRetention.RUNTIME)
annotation class QualifierB
``` 
然后再 module 和 Activity 使用
``` kotlin
@Module
object DataModule2 {
    @QualifierA
    @Provides
    fun provideRepo(): Data {
        return Data("草草花花")
    }
    @QualifierB
    @Provides
    fun provideRepoEx(): Data {
        return Data("花花草草")
    }
}

class DaggerActivity : AppCompatActivity() {
    @QualifierA
    @Inject
    lateinit var data1: Data //草草花花
    @QualifierB
    @Inject
    lateinit var data2: Data //花花草草
    ... ...
}
``` 




