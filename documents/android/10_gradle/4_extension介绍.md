## 1、Extension 介绍

使用 Extension 可以为插件提供配置项，比如 `android{}` 就是 `Android Gradle Plugin` 的扩展。我们可以在 `android{}`中配置项目所依赖的SDK版本和一些其他的配置

```groovy
android {
    compileSdk 32
    defaultConfig {
        applicationId "com.egas.demo"
        minSdk 23
        ...
    }
    ...
}
```

### 1.1 创建一个 Extension

我们可以借助 ExtensionContainer 来创建我们自定义的 Extension。ExtensionContainer 是管理 Extension 的一个容器，我们可以通过 ExtensionContainer 去对 Extension 进行相应的操作。

我们可以通过下面4种方式获取 Project 的 ExtensionContainer 对象。

```groovy
//当前在 app 的 build.gradle 文件中
extensions 
project.extensions
getExtensions()
project.getExtensions()
```
ExtensionContainer 可以通过 add/create 方法添加 Extension

我们先看一下两种API说明和使用，先看一下 create 的重载方法
```groovy
/**
 * publicType: 对外公布的 Extension 类型
 * name: Extension 的 name，不能重复，重复会抛出异常
 * instanceType: Extension 使用的类
 * constructionArguments: 构造 instanceType 默认使用的参数，可省略
 */
 <T> T create(Class<T> publicType, String name, Class<? extends T> instanceType, Object... constructionArguments);
 <T> T create(TypeOf<T> publicType, String name, Class<? extends T> instanceType, Object... constructionArguments);
 <T> T create(String name, Class<T> type, Object... constructionArguments);
```
在看一下 add 相关方法
```groovy
/**
 * publicType: 对外公布的 Extension 类型
 * name: Extension 的 name，不能重复，重复会抛出异常
 * extension: Extension 使用的对象，这里要传入对象
 */
 <T> void add(Class<T> publicType, String name, T extension);
 <T> void add(TypeOf<T> publicType, String name, T extension);
 void add(String name, Object extension);
```
使用：我们在 app 的 build.gradle 方法中创建我们使用的例子
```groovy
class Tag {
    String name
    Tag() {}
    Tag(String name) {
        this.name = name
    }
    String toString() {
        return "This Tag is $name"
    }
}
class IDCard extends Tag {
    int des = 5
    IDCard() {
    }
    IDCard(int des) {
        this.des = des
    }
    String toString() {
        return super.toString() + " Its des is $des."
    }
}
//create
project.extensions.create('tag1', Tag)
Tag a1 = project.extensions.create(Tag, 'tag2', IDCard, 10) // create 创建的 Extension 会返回创建的 instance
project.extensions.create(TypeOf.typeOf(Tag), 'tag3', IDCard, 15)
//add
project.extensions.add('tag4',IDCard)
project.extensions.add(Tag,'tag5',new IDCard(10))
project.extensions.add(TypeOf.typeOf(Tag),'tag6',new IDCard(15))

tag1 {
    name 'tag1'
}
tag2 {
    name 'tag2'
}
tag3 {
    name 'tag3'
}
tag4 {
    name 'tag4'
}
tag5 {
    name = 'tag5' // 注意这里的等号
}
tag6 {
    name = 'tag6' // 注意这里的等号
}

project.task('printExtension') {
    doLast {
        println project.tag1
        println project.tag2
        println project.tag3
        println project.tag4
        println project.tag5
        println project.tag6
    }
}
```
日志：
```shell
> Task :app:printExtension
This Tag is tag1
This Tag is tag2 Its des is 10.
This Tag is tag3 Its des is 15.
This Tag is tag4 Its des is 5.
This Tag is tag5 Its des is 10.
This Tag is tag6 Its des is 15.
```

add 和 create 的区别 

1. create 创建的 Extension 对象都默认实现了 ExtensionAware 接口，可以强转为 ExtensionAware。
2. create 创建的 Extension 会返回创建的 instance，add 系列方法没有返回值
3. add 使用创建对象构造方式时，要使用 "=" 添加映射关系，如 tag5、tag6.

如果不想使用 "="，可以在实体类中创建属性的对应方法:

```groovy
class Tag {
    ...
    void name(String name){
        this.name = name
    }
    ...
}
class IDCard extends Tag {
    ...
    void des(String des){
        this.des = des
    }
    ...
}
```
### 1.2 查找 Extension

```groovy
Object findByName(String name)
<T> T findByType(Class<T> type)
Object getByName(String name)       // 找不到会抛异常
<T> T getByType(Class<T> type)  // 找不到会抛异常
```
创建一个查找 Extension 的任务

```groovy
project.task('findExtension') {
    doLast {
        println project.extensions.getByName("tag1")
        println project.extensions.findByName("tag1")
        println project.extensions.findByType(TypeOf.typeOf(IDCard)) // 注意这里，返回是第一个 IDCard 类型的 Extension
        println project.extensions.getByType(TypeOf.typeOf(IDCard)) // 同上
    }
}
```
日志如下：
```java
> Task :app:findExtension
This Tag is tag1
This Tag is tag1
This Tag is tag4 Its des is 5.
This Tag is tag4 Its des is 5.
```
## 2、嵌套的的 Extension

在开头 android 配置的例子中，内部还有嵌套的 defaultConfig 配置，这种就是嵌套的的 Extension。

我们可以通过下面方式创建一个嵌套的的 Extension。

```groovy
class Outer {
    String msg
    Inner inner = new Inner()
    
    void inner(Action<Inner> action) { //创建内部Extension，名称为方法名 inner
        action.execute(inner)
    }
    String toString() {
        return "OuterExt[ msg = ${msg}] " + inner
    }
}
class Inner {
    String msg
    String toString() {
        return "InnerExt[ msg = ${msg}]"
    }
}

def outExt = project.extensions.create("outer", Outer)

outer {
    msg "this is a outer message."
    inner {
        msg = "This is a inner message."
    }
}

project.task('nestedExtension') {
    doLast {
        println outExt
    }
}
```
日志如下
```shell
> Task :app:nestedExtension
OuterExt[ msg = this is a outer message.] InnerExt[ msg = This is a inner message.]
```
我们可在 `Inner` 类中定义一个 `msg(string):void` 方法，然后在 `inner` 配置中去掉等号。

## 3、可命名的配置项

在 app 的 build.gradle 中可以通过定义 buildTypes 配置来生成不同 build 类型的包，如下：

```groovy
android {
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        debug {
            ...
        }
        preview {// 支持任意命名
            ...
        }
    }
}
```
buildTypes 就是一个 NamedDomainObjectContainer<T> 对象。

<img width="773" alt="image" src="https://user-images.githubusercontent.com/17560388/190983485-7ced6512-4f37-4866-8f1b-e45bc2a9fc80.png">

NamedDomainObjectContainer 直译是命名领域对象容器，是一个支持配置不固定数量配置的容器。主要功能分为 3 点：

- Set 容器： 支持添加多个 T 类型对象，并且不允许命名重复；
- 命名 DSL： 支持以 DSL 的方式配置 T 类型对象，T 类型必须带有 String name 属性，且必须有以 name 为参数的 public 构造函数；
- SortSet 容器： 容器将保证元素以 name 自然顺序排序。

我们继续通过一个简单的例子实现一个 NamedDomainObjectContainer
```groovy
class FlavorConfig{
    String name //注意：必须要有 name 属性进行标识
    boolean isDebug
    FlavorConfig(String name) {
        this.name = name
    }
    //配置与属性同名的方法
    void isDebug(boolean isDebug) {
        this.isDebug = isDebug
    }
}

NamedDomainObjectContainer<FlavorConfig> container = project.container(FlavorConfig)
project.extensions.add('flavorConfig',container)

flavorConfig {
    google {
        isDebug false
    }
    wechat {
        isDebug true
    }
}
//创建 task
project.tasks.create("namedDomainTask"){
    doLast {
        project.flavorConfig.each{
            println "$it.name: $it.isDebug "
        }
    }
}
```
打印日志如下
```shell
> Task :app:namedDomainTask
google: false 
wechat: true 
```
    
## 4、变体
    
变体属于 AGP（Android Gradle Plugin）中的知识点，AGP 给 android 对象提供了三种类型变体（Variants）：

1、applicationVariants：只适用于 app plugin
2、libraryVariants：只适用于 library plugin
3、testVariants：在 app plugin 与 libarary plugin 中都适用，这个一般很少用

其中我们最常用的便是 applicationVariants
    
### 4.1 打印 applicationVariants 
 
我们可以通过 Project 对象获取 android 属性，然后通过 android 获取变体：
```groovy   
//当前在 app 的 build.gradle 文件中，3 种方式获取的都是同一个变体
android.applicationVariants
project.android.applicationVariants
project.property('android').applicationVariants
```
为了更好的演示，我们在 app 的 build.gradle 增加如下内容：
```groovy
android {
    ...
    buildTypes {
        debug{
        }
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    productFlavors{
        flavorDimensions 'isFree'
        google{
            dimension 'isFree'
        }
        winxin{
            dimension 'isFree'
        }
    }
}
```
上述配置会产生 4 个变体，通过 buildTypes 和 productFlavors 的排列组合所产生，我们遍历打印一下每个变体的 name 和 baseName
    
注意：
1. 从 AGP 3.0 开始，必须至少明确指定一个 flavor dimension
2. 通过 android 对象获取的 applicationVariants 或 libraryVariants 是所有的变体，我们可以通过遍历取出每一个变体

```groovy
//当前在 app 的 build.gradle 文件中
afterEvaluate {
    project.android.applicationVariants.all{ variant ->
        println "$variant.name $variant.baseName"
    }
}
```
打印结果
```shell
> Configure project :app
googleDebug google-debug
winxinDebug winxin-debug
googleRelease google-release
winxinRelease winxin-release
```

### 4.2 对 applicationVariants 中的 Task 进行 Hook

通常我们会使用变体来对构建过程中的 Task 进行 hook，如下：

```groovy
//当前在 app 的 build.gradle 文件中
afterEvaluate {
    project.android.applicationVariants.all{ variant ->
        def task = variant.mergeResources
        println "$task.name"
    }
}
```
打印结果
```shell   
> Configure project :app
packageGoogleDebugResources
packageWinxinDebugResources
packageGoogleReleaseResources
packageWinxinReleaseResources
```   

上述操作我们拿到了所有变体对应的 mergeResources Task 并打印了它的名称
    
### 4.3 使用 applicationVariants 对 APK 进行重命名
    
applicationVariants 中每一个变体对应的输出文件便是一个 APK，因此我们可以通过 applicationVariants 对 APK 进行重命名，如下：
```groovy
//当前在 app 的 build.gradle 文件中
project.android.applicationVariants.all{ variant ->
    variant.outputs.all{
        outputFileName = "${variant.baseName}" + ".apk"
        println outputFileName
    }
}
```
打印结果
```shell    
> Configure project :app
google-debug.apk
winxin-debug.apk
google-release.apk
winxin-release.apk
``` 
## 5、插件中使用Extension
在本系列的第三篇文档中，我们简单的实现了一个 gradle 插件。在上面的章节中我们学习了 Extension 的使用，在下面的例子中我们在自定义的插件中实现一个嵌套的 Extension。
    
直接贴下我们实现的代码，首先在 CustomPlugin.kt 中注册我们的 extensions
```kotlin
class CustomPlugin : Plugin<Project> {
    companion object {
        const val UPLOAD_EXTENSION_NAME = "outer"
    }
    override fun apply(project: Project) {
        println("Hello CustomPlugin")
        project.extensions.create(UPLOAD_EXTENSION_NAME, Outer::class.java) //使用 create 注册一个 Extension
        project.afterEvaluate {
         val outer = project.extensions.findByName(UPLOAD_EXTENSION_NAME) as Outer?
            outer?.let {
                println("outer.name =${it.name?:"null"} outer.inner.name =${it.inner?.name?:"null"}")
            }
        }
    }
}
// Outer 类代码如下
open class Outer {
    var name: String? = ""
    var inner = Inner()
    fun inner(innerAct:Action<Inner>){
        innerAct.execute(inner)
    }
}
open class Inner {
    var name: String? = ""
} 
```
然后在我们 app 的 build.gradle 中添加相关配置
```groovy
outer{
    name = "outer_name"
    inner{
        name = "inner_name"
    }
}
```    
重新发布插件，然后 Rebuild 项目，会有如下日志
```shell   
> Configure project :app
Hello CustomPlugin
outer.name =outer_name outer.inner.name =inner_name
```   
ok... 
    
## 参考

- [Android Gradle学习(五)：Extension详解](https://www.jianshu.com/p/58d86b4c0ee5)
- [Gradle 系列（2）手把手带你自定义 Gradle 插件](https://juejin.cn/post/7098383560746696718#heading-13)
- [Gradle 系列 （四）、Gradle 插件实战应用](https://juejin.cn/post/6989877607126794247#heading-3)
