
博客比较老，16年的 仅供参考 [ProGuard代码混淆技术详解](https://www.cnblogs.com/cr330326/p/5534915.html)

### 前言   
    受《APP研发录》启发，里面讲到一名Android程序员，在工作一段时间后，会感觉到迷茫，想进阶的话接下去是看Android系统源码呢，还是每天继续做应用，毕竟每天都是画UI和利用MobileAPI处理Json还是蛮无聊的，做着重复的事情，没有技术的上提升空间的。所以，根据里面提到的Android应用开发人员所需要精通的20个技术点，写篇文章进行总结，一方面是梳理下基础知识和巩固知识，另一方面也是弥补自我不足之处。
    那么，今天就来讲讲ProGuard代码混淆的相关技术知识点。
    
### 内容目录
- ProGuard简介
- ProGuard工作原理
- 如何编写一个ProGuard文件
- 其他注意事项
- 小结


### 1. ProGuard简介

因为Java代码是非常容易反编码的，况且Android开发的应用程序是用Java代码写的，为了很好的保护Java源代码，我们需要对编译好后的class文件进行混淆。

ProGuard是一个混淆代码的开源项目，它的主要作用是混淆代码，殊不知ProGuard还包括以下4个功能。

- 压缩(Shrink)：检测并移除代码中无用的类、字段、方法和特性（Attribute）。
- 优化(Optimize)：对字节码进行优化，移除无用的指令。
- 混淆(Obfuscate)：使用a，b，c，d这样简短而无意义的名称，对类、字段和方法进行重命名。
- 预检(Preveirfy)：在Java平台上对处理后的代码进行预检，确保加载的class文件是可执行的。

总而言之，根据官网的翻译：Proguard是一个Java类文件压缩器、优化器、混淆器、预校验器。压缩环节会检测以及移除没有用到的类、字段、方法以及属性。优化环节会分析以及优化方法的字节码。混淆环节会用无意义的短变量去重命名类、变量、方法。这些步骤让代码更精简，更高效，也更难被逆向（破解）。
 
### 2. ProGuard工作原理

ProGuar由shrink、optimize、obfuscate和preveirfy四个步骤组成，每个步骤都是可选的，我们可以通过配置脚本来决定执行其中的哪几个步骤。

![image](https://user-images.githubusercontent.com/17560388/174275957-c1cd15f6-b5f5-45c7-a13f-b126232ba876.png)

混淆就是移除没有用到的代码，然后对代码里面的类、变量、方法重命名为人可读性很差的简短名字。

那么有一个问题，ProGuard怎么知道这个代码没有被用到呢？

这里引入一个Entry Point（入口点）概念，Entry Point是在ProGuard过程中不会被处理的类或方法。在压缩的步骤中，ProGuard会从上述的Entry Point开始递归遍历，搜索哪些类和类的成员在使用，对于没有被使用的类和类的成员，就会在压缩段丢弃，在接下来的优化过程中，那些非Entry Point的类、方法都会被设置为private、static或final，不使用的参数会被移除，此外，有些方法会被标记为内联的，在混淆的步骤中，ProGuard会对非Entry Point的类和方法进行重命名。

那么这个入口点怎么来呢？就是从ProGuard的配置文件来，只要这个配置了，那么就不会被移除。
 
### 3.如何编写一个ProGuard文件

有个三步走的过程：
- 基本混淆
- 针对APP的量身定制
- 针对第三方jar包的解决方案

#### 3.1 基本混淆

混淆文件的基本配置信息，任何APP都要使用，可以作为模板使用，具体如下。

1.基本指令

```groove
# 代码混淆压缩比，在0和7之间，默认为5，一般不需要改
-optimizationpasses 5
 
# 混淆时不使用大小写混合，混淆后的类名为小写
-dontusemixedcaseclassnames
 
# 指定不去忽略非公共的库的类
-dontskipnonpubliclibraryclasses
 
# 指定不去忽略非公共的库的类的成员
-dontskipnonpubliclibraryclassmembers
 
# 不做预校验，preverify是proguard的4个步骤之一
# Android不需要preverify，去掉这一步可加快混淆速度
-dontpreverify
 
# 有了verbose这句话，混淆后就会生成映射文件
# 包含有类名->混淆后类名的映射关系
# 然后使用printmapping指定映射文件的名称
-verbose
-printmapping proguardMapping.txt
 
# 指定混淆时采用的算法，后面的参数是一个过滤器
# 这个过滤器是谷歌推荐的算法，一般不改变
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
 
# 保护代码中的Annotation不被混淆，这在JSON实体映射时非常重要，比如fastJson
-keepattributes *Annotation*
 
# 避免混淆泛型，这在JSON实体映射时非常重要，比如fastJson
-keepattributes Signature
 
//抛出异常时保留代码行号，在异常分析中可以方便定位
-keepattributes SourceFile,LineNumberTable

-dontskipnonpubliclibraryclasses用于告诉ProGuard，不要跳过对非公开类的处理。默认情况下是跳过的，因为程序中不会引用它们，有些情况下人们编写的代码与类库中的类在同一个包下，并且对包中内容加以引用，此时需要加入此条声明。

-dontusemixedcaseclassnames，这个是给Microsoft Windows用户的，因为ProGuard假定使用的操作系统是能区分两个只是大小写不同的文件名，但是Microsoft Windows不是这样的操作系统，所以必须为ProGuard指定-dontusemixedcaseclassnames选项
```

2.需要保留的东西

```groove
# 保留所有的本地native方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}
 
# 保留了继承自Activity、Application这些类的子类
# 因为这些子类，都有可能被外部调用
# 比如说，第一行就保证了所有Activity的子类不要被混淆
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep public class com.android.vending.licensing.ILicensingService
 
# 如果有引用android-support-v4.jar包，可以添加下面这行
-keep public class com.xxxx.app.ui.fragment.** {*;}
 
# 保留在Activity中的方法参数是view的方法，
# 从而我们在layout里面编写onClick就不会被影响
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}
 
# 枚举类不能被混淆
-keepclassmembers enum * {
public static **[] values();
public static ** valueOf(java.lang.String);
}
 
# 保留自定义控件（继承自View）不被混淆
-keep public class * extends android.view.View {
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
 
# 保留Parcelable序列化的类不被混淆
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
 
# 保留Serializable序列化的类不被混淆
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
 
# 对于R（资源）下的所有类及其方法，都不能被混淆
-keep class **.R$* {
    *;
}
 
# 对于带有回调函数onXXEvent的，不能被混淆
-keepclassmembers class * {
    void *(**On*Event);
}
```

### 3.2 针对APP的量身定制

1.保留实体类和成员被混淆

对于实体，保留它们的set和get方法，对于boolean型get方法，有人喜欢命名isXXX的方式，所以不要遗漏。如下：
```groove
# 保留实体类和成员不被混淆
-keep public class com.xxxx.entity.** {
    public void set*(***);
    public *** get*();
    public *** is*();
}
```
一种好的做法是把所有实体都放在一个包下进行管理，这样只写一次混淆就够了，避免以后在别的包中新增的实体而忘记保留，代码在混淆后因为找不到相应的实体类而崩溃。

2.内嵌类

内嵌类经常会被混淆，结果在调用的时候为空就崩溃了，最好的解决方法就是把这个内嵌类拿出来，单独成为一个类。如果一定要内置，那么这个类就必须在混淆的时候保留，比如如下：

```groove
# 保留内嵌类不被混淆
-keep class com.example.xxx.MainActivity$* { *; }
这个$符号就是用来分割内嵌类与其母体的标志。
```
 

3.对WebView的处理

```groove
# 对WebView的处理
-keepclassmembers class * extends android.webkit.webViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String)
}
-keepclassmembers class * extends android.webkit.webViewClient {
    public void *(android.webkit.webView, java.lang.String)
}
```
 

4.对JavaScript的处理

```groove
# 保留JS方法不被混淆
-keepclassmembers class com.example.xxx.MainActivity$JSInterface1 {
    <methods>;
}
```
其中JSInterface是MainActivity的子类


5.处理反射

在程序中使用SomeClass.class.method这样的静态方法，在ProGuard中是在压缩过程中被保留的，那么对于Class.forName("SomeClass")呢，SomeClass不会被压缩过程中移除，它会检查程序中使用的Class.forName方法，对参数SomeClass法外开恩，不会被移除。但是在混淆过程中，无论是Class.forName("SomeClass")，还是SomeClass.class，都不能蒙混过关，SomeClass这个类名称会被混淆，因此，我们要在ProGuard.cfg文件中保留这个类名称。

```groove
Class.forName("SomeClass")
SomeClass.class
SomeClass.class.getField("someField")
SomeClass.class.getDeclaredField("someField")
SomeClass.class.getMethod("someMethod", new Class[] {})
SomeClass.class.getMethod("someMethod", new Class[] { A.class })
SomeClass.class.getMethod("someMethod", new Class[] { A.class, B.class })
SomeClass.class.getDeclaredMethod("someMethod", new Class[] {})
SomeClass.class.getDeclaredMethod("someMethod", new Class[] { A.class })
SomeClass.class.getDeclaredMethod("someMethod", new Class[] { A.class, B.class })
AtomicIntegerFieldUpdater.newUpdater(SomeClass.class, "someField")
AtomicLongFieldUpdater.newUpdater(SomeClass.class, "someField")
AtomicReferenceFieldUpdater.newUpdater(SomeClass.class, SomeType.class, "someField")
```

在混淆的时候，要在项目中搜索一下上述方法，将相应的类或者方法的名称进行保留而不被混淆。

 

6.对于自定义View的解决方案

但凡在Layout目录下的XML布局文件配置的自定义View，都不能进行混淆。为此要遍历Layout下的所有的XML布局文件，找到那些自定义View，然后确认其是否在ProGuard文件中保留。有一种思路是，在我们使用自定义View时，前面都必须加上我们的包名，比如com.a.b.customeview，我们可以遍历所有Layout下的XML布局文件，查找所有匹配com.a.b的标签即可。
 
### 4.针对第三方jar包的解决方案

我们在Android项目中不可避免要使用很多第三方提供的SDK，一般而言，这些SDK是经过ProGuard混淆的，而我们所需要做的就是避免这些SDK的类和方法在我们APP被混淆。

1.针对android-support-v4.jar的解决方案

```groove
# 针对android-support-v4.jar的解决方案
-libraryjars libs/android-support-v4.jar
-dontwarn android.support.v4.**
-keep class android.support.v4.**  { *; }
-keep interface android.support.v4.app.** { *; }
-keep public class * extends android.support.v4.**
-keep public class * extends android.app.Fragment
```
2.其他的第三方jar包的解决方案

这个就取决于第三方包的混淆策略了，一般都有在各自的SDK中有关于混淆的说明文字，比如支付宝如下：


```groove
#对alipay的混淆处理
-libraryjars libs/alipaysdk.jar
-dontwarn com.alipay.android.app.**
-keep public class com.alipay.**  { *; }
```

值得注意的是，不是每个第三方SDK都需要-dontwarn 指令，这取决于混淆时第三方SDK是否出现警告，需要的时候再加上。

### 5 其他注意事项
当然在使用ProGuard过程中，还有一些注意的事项，如下。

1.如何确保混淆不会对项目产生影响

测试工作要基于混淆包进行，才能尽早发现问题
每天开发团队的冒烟测试，也要基于混淆包
发版前，重点的功能和模块要额外的测试，包括推送，分享，打赏

2.打包时忽略警告

当导出包的时候，发现很多could not reference class之类的warning信息，如果确认App在运行中和那些引用没有什么关系，可以添加-dontwarn 标签，就不会提示这些警告信息了
 
3.对于自定义类库的混淆处理

比如我们引用了一个叫做AndroidLib的类库，我们需要对Lib也进行混淆，然后在主项目的混淆文件中保留AndroidLib中的类和类的成员。
 
4.使用annotation避免混淆

另一种类或者属性被混淆的方式是，使用annotation，比如这样：
```java
@keep
@keepPublicGetterSetters
public class Bean{
    public  boolean booleanProperty;
    public  int intProperty;
    public  String stringProperty;
}
```
5.在项目中指定混淆文件

到最后，发现没有介绍如何在项目中指定混淆文件。在项目中有一个project.properties文件，在其中写这么一句话，就可以确保每次手动打包生成的apk是混淆过的。
proguard.config=proguard.cfg
其中，proguard.cfg是混淆文件的名称。

### 小结
总之ProGuard是一个比较枯燥的过程，但Android项目没有了ProGuard就真不行了，这样可以保证我们开发出的APK可以更健壮，毕竟很多核心代码质量也算是一个APK的核心竞争力吧。
