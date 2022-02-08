### 1. 更适合 android 的 di

hilt 是 google 对 dagger2 进一步的封装，更适合 Android。

添加依赖

project 的 build.gradle

```groovy
buildscript {
    dependencies {
        classpath 'com.google.dagger:hilt-android-gradle-plugin:2.28-alpha'
    }
}
```
app 的 build.gradle

```groovy
apply plugin: 'kotlin-kapt'
apply plugin: 'dagger.hilt.android.plugin'

android {
  compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
  }
}

dependencies {
    implementation "com.google.dagger:hilt-android:2.28-alpha"
    kapt "com.google.dagger:hilt-android-compiler:2.28-alpha"
}
```

在 Application 中添加 hilt 支持

```kotlin
@HiltAndroidApp
class App : Application() { ... }
```

### 2. 简单使用

Hilt 目前支持注入以下 Android 类：

- Application（通过使用 @HiltAndroidApp）
- Activity
- Fragment
- View
- Service
- BroadcastReceiver

在 Activity 中注入 `@Inject` 的实例对象，`@AndroidEntryPoint` 会为项目中的每个 `Android` 类生成一个单独的 `Hilt` 组件。这些组件可以从它们各自的父类接收依赖项，如组件层次结构中所述。

```kotlin
class AnalyticsAdapter @Inject constructor(
  private val service: AnalyticsService
) { ... }

@AndroidEntryPoint
class ExampleActivity : AppCompatActivity() {
  @Inject lateinit var analytics: AnalyticsAdapter
  ...  
}
```

### 3. 使用 module 实现注入

module 是一个带有 @Module 注释的类。与 Dagger 模块一样，它会告知 Hilt 如何提供某些类型的实例。
与 Dagger 模块不同的是，您必须使用 @InstallIn 为 Hilt Module 添加注释，以告知 Hilt 每个 Module 将用在或安装在哪个 Android 类中。

```kotlin
@Module
@InstallIn(ActivityComponent::class)
abstract class AnalyticsModule {

  @Binds
  abstract fun bindAnalyticsService(
    analyticsServiceImpl: AnalyticsServiceImpl
  ): AnalyticsService
  
}
```

AnalyticsModule 被 @InstallIn(ActivityComponent::class) 注释，AnalyticsModule 中的所有依赖项都可以在应用的所有 Activity 中使用。

**Tips:**

和 Dagger2 相同，注入类可以通过 @Binds 和 @Provides 提供。当然也支持通过限定注解来指定同类型的不同实例对象的注入，如下代码:

```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthInterceptorOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OtherInterceptorOkHttpClient
```

**Hilt 中的预定义限定符**

通过 @ApplicationContext 和 @ActivityContext 限定符来提供 Application 或 Activity 的 Context 类。

```kotlin
class AnalyticsAdapter @Inject constructor(
    @ActivityContext private val context: Context,
    private val service: AnalyticsService
) { ... }
```
### 4. @InstallIn 注释中引用的Component 和 Android 类的对应关系

<img width="600" alt="引用的组件和Android类的对应关系" src="https://user-images.githubusercontent.com/17560388/151653701-f497f8e2-6cd5-4ab3-9d23-4663a199567d.png">

## 5. Component 的生命周期

Hilt 会按照相应 Android 类的生命周期自动创建和销毁生成的组件类的实例。

<img width="600" alt="组件的生命周期" src="https://user-images.githubusercontent.com/17560388/151653682-b8f52006-ad42-48a1-9932-467e6af3f694.png">

### 6. Component 的作用域

和 Dagger2 相同，如果不指定作用域，每绑定一次都会生成一个新的实例。
我们可以通过指定 Component 的作用域，在同一个作用域下共用一个 Component 实例。


### 参考

[Android Hilt使用教程（包含实例)](https://www.jianshu.com/p/f32beb3614e5)

[使用 Hilt 实现依赖项注入](https://developer.android.google.cn/training/dependency-injection/hilt-android?hl=zh-cn)
