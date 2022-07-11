## 1. Jetpack的导航组件 

他是官方提供的一个路由组件，使用它可以实现单 `Activity` 多 `Fragment` UI架构

## 2. 基本使用

### 2.1 导包
```groove
ext.nav_version = "2.4.1"

// navigation
implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
implementation("androidx.navigation:navigation-ui-ktx:$nav_version")
// Feature module Support
implementation("androidx.navigation:navigation-dynamic-features-fragment:$nav_version")
// Jetpack Compose Integration
implementation("androidx.navigation:navigation-compose:$nav_version")
```

### 2.2 新建三个 Framgnet 用来相互跳转
三个Framgnet

```kotlin
//第一个Fragment
class HomeFragment : BaseVMFragment() {
    override fun layoutId() = R.layout.fragment_home

    override fun initData(root: View, savedInstanceState: Bundle?) {

        root.findViewById<View>(R.id.toMine).setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_home_to_mine)
        }
        root.findViewById<View>(R.id.toMore).setOnClickListener {
            // 使用Bundle传递参数
            val bundle = Bundle().apply {
                putString("key", "value")
            }
            // 界面切换动画，两种方式实现，还可以在 navigation 中实现
            val option = navOptions {
                anim {
                    enter = R.anim.right_in
                    exit = R.anim.scale_out
                    popExit = R.anim.right_out
                    popEnter = R.anim.scale_in
                }
            }
            Navigation.findNavController(it).navigate(R.id.action_home_to_more, bundle, option)
        }
    }
}
//第二个Framgnet
class MineFragment : BaseVMFragment() {
    override fun layoutId() = R.layout.fragment_mine

    override fun initData(root: View, savedInstanceState: Bundle?) {
        root.findViewById<View>(R.id.toMore).setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_mine_to_more)
        }
    }
}
//第三个Fragment
class MoreFragment : BaseVMFragment() {
    override fun layoutId() = R.layout.fragment_more

    override fun initData(root: View, savedInstanceState: Bundle?) {
        arguments?.let {
            LogExt.e(it.getString("key") ?: "null")
        }
    }
}
```

### 2.3 添加 navigation 的配置

在 `res`目录下中新建`navigation`文件包，新建`nav_account.xml`文件

```html
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/nav_account"
    app:startDestination="@id/fragment_home">

    <fragment
        android:id="@+id/fragment_home"
        android:name="com.mihua.ljxbao.ui.main.home.HomeFragment"
        android:label="fragment_home"
        tools:layout="@layout/fragment_home">
        <!-- 界面切换动画，两种方式实现，还可以在代码中实现 -->
        <action
            android:id="@+id/action_home_to_mine"
            app:destination="@id/fragment_mine"
            app:enterAnim="@anim/right_in"
            app:exitAnim="@anim/scale_out"
            app:popEnterAnim="@anim/scale_in"
            app:popExitAnim="@anim/right_out" />
        <action
            android:id="@+id/action_home_to_more"
            app:destination="@id/fragment_more" />
    </fragment>

    <fragment
        android:id="@+id/fragment_mine"
        android:name="com.mihua.ljxbao.ui.main.mine.MineFragment"
        android:label="fragment_page_2_label"
        tools:layout="@layout/fragment_mine">
        <action
            android:id="@+id/action_mine_to_more"
            app:destination="@id/fragment_more" />
        
    </fragment>

    <fragment
        android:id="@+id/fragment_more"
        android:name="com.mihua.ljxbao.ui.main.more.MoreFragment"
        android:label="fragment_page_1_label"
        tools:layout="@layout/fragment_more">
    </fragment>
</navigation>
```

<img width="480" alt="image" src="https://user-images.githubusercontent.com/17560388/162107080-244d6b14-74d0-4cbc-b263-2fe3ba673fe8.png">

### 2.4 用来承载的Activity
```kotlin
class NavigationActivity : BaseVMActivity() {
    override fun layoutId(savedInstanceState: Bundle?): Int {
        return R.layout.activity_navigation
    }
    
    override fun initData(savedInstanceState: Bundle?) {

    }
}
```
`Activity` 的 `layout`
```html
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".test.NavigationActivity">

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/nav_host_fragment"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:defaultNavHost="true" 
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:navGraph="@navigation/nav_account" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

- android:name 属性：包含 NavHost 实现的类名称。
- app:navGraph 属性将： NavHostFragment 与导航图相关联。导航图会在此 NavHostFragment 中指定用户可以导航到的所有目的地。
- app:defaultNavHost="true"： 属性确保您的 NavHostFragment 会拦截系统返回按钮。

## 3. 界面动画

scale_in.xml

```html
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <scale
        android:duration="@android:integer/config_shortAnimTime"
        android:fromXScale="0.98"
        android:fromYScale="0.98"
        android:interpolator="@android:anim/accelerate_interpolator"
        android:pivotX="50%"
        android:pivotY="50%"
        android:toXScale="1"
        android:toYScale="1" />
</set>
```

scale_out.xml

```html
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <scale
        android:duration="@android:integer/config_shortAnimTime"
        android:fromXScale="1"
        android:fromYScale="1"
        android:interpolator="@android:anim/accelerate_interpolator"
        android:pivotX="50%"
        android:pivotY="50%"
        android:toXScale="0.98"
        android:toYScale="0.98" />
</set>
```

right_in.xml

```html
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <translate
        android:duration="@integer/activity_anim_duration"
        android:fromXDelta="100%p"
        android:fromYDelta="0"
        android:toXDelta="0"
        android:toYDelta="0" />
</set>
```
right_out.xml

```html
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <translate
        android:duration="@integer/activity_anim_duration"
        android:fromXDelta="0"
        android:fromYDelta="0"
        android:toXDelta="100%p"
        android:toYDelta="0" />
</set>
```

### 3.1 动画添加方式

```html
<!-- 界面切换动画，两种方式实现，还可以在代码中实现 -->
<action
    android:id="@+id/action_home_to_mine"
    app:destination="@id/fragment_mine"
    app:enterAnim="@anim/right_in"
    app:exitAnim="@anim/scale_out"
    app:popEnterAnim="@anim/scale_in"
    app:popExitAnim="@anim/right_out" />
```

```kotlin
// 界面切换动画，两种方式实现，还可以在 navigation 中实现
val option = navOptions {
    anim {
        enter = R.anim.right_in
        exit = R.anim.scale_out
        popExit = R.anim.right_out
        popEnter = R.anim.scale_in
    }
}
```
## 4.传送数据

- 1.通过 `Bundle()` 对象,传递 `key:value`。上面代码有。

- 2.使用 `Safe Args` 传递安全的数据，这里就不写不参考代码了，感觉没有 `bundle` 方便


## 5.跳转到指定 fragment


## 参考

[Android 架构组件 Navigation 详解，构造一本Fragment的故事书！](https://juejin.cn/post/6844904068830199822#heading-13)

