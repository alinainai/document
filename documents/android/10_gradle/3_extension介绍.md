## 1、Extension 介绍

拓展项：使用 Extension 可以为插件提供配置项，比如 `android{}` 就是 `Android Gradle Plugin` 的扩展。我们可以在 `android{}`中配置项目所依赖的SDK版本和一些其他的配置

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

我们可以通过下面代码获取 Project 中的 ExtensionContainer 对象。

```groovy
//当前在 app 的 build.gradle 文件中
extensions //方式1
project.extensions //方式2
getExtensions() //方式3
project.getExtensions() //方式4
```
ExtensionContainer 可以通过 add/create 方法添加 Extension

我们先看一下两种API说明和使用，先看一下 create 的重载方法
```groovy
/**
 * publicType: 对外公开的 Extension 类型
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
 * publicType: 对外公开的 Extension 类型
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

和 create 的区别 

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

在开头 android 的例子中，android 内部还有嵌套的 defaultConfig 配置，这种就是嵌套的的 Extension。

我们可以通过下面的代码创建一个嵌套的的 Extension






<img width="773" alt="image" src="https://user-images.githubusercontent.com/17560388/190983485-7ced6512-4f37-4866-8f1b-e45bc2a9fc80.png">

## 参考

- [Android Gradle学习(五)：Extension详解](https://www.jianshu.com/p/58d86b4c0ee5)
- [Gradle 系列（2）手把手带你自定义 Gradle 插件](https://juejin.cn/post/7098383560746696718#heading-13)
- [Gradle 系列 （四）、Gradle 插件实战应用](https://juejin.cn/post/6989877607126794247#heading-3)
