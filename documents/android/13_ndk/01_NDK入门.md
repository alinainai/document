## 一、NDK简单介绍

NDK（Native Developer Kit）提供了一套 Java 和 C/C++ 相互调用的工具集。

JNI（Java Native Interface）是 JAVA 平台中的一个强大功能。使用 JNI 编程的程序能够调用 C/C++编写的本地代码，同时也可以调用 JAVA 编写的代码。

从一个demo开始：我们新建一个 Native C++ 项目

<img width="240" alt="image" src="https://user-images.githubusercontent.com/17560388/192934644-766b2e2a-4b49-4476-918a-d886d78c26ff.png">

AS 会自动生成一个 Activity 以及和他交互的 Jni 文件，Activity 代码如下（我们用的是 kotlin 代码，在关键的地方已经添加注释）：

```kotlin
class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.sampleText.text = stringFromJNI() // 3、使用 JNI 返回的 String
    }

    private external fun stringFromJNI(): String // 2、在 kotlin 中注意关键字是 external, 在 Java 中是 native 关键字

    companion object {
        init {
            System.loadLibrary("demo") // 1、在静态方法中加载 demo 类
        }
    }
}
```
demo.cpp 的代码：

```c++
#include <jni.h> // 引入头文件，相当于 java 的导包
#include <string> // c++ 中的 string

extern "C" JNIEXPORT jstring JNICALL
Java_com_egas_demo_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
```
我们简单的分析一下该文件相关的关键字：

#### 1、`extern "C"`
表示按照类C的编译和连接规约来编译和连接，因为 C 和 C++ 语法有区别，C++ 形式编译的代码 C 可能识别不了，比如 C 中没有重载方法

#### 2、JNIEXPORT
表示一个函数需要暴露给共享库外部使用时

```c++
// Windows 平台 :
#define JNIEXPORT __declspec(dllexport)
#define JNIIMPORT __declspec(dllimport)
// Linux 平台：
#define JNIIMPORT
#define JNIEXPORT  __attribute__ ((visibility ("default"))) // 让该方法对于外界可见
```

#### 3、JNICALL
表示一个函数是 JNI 函数，
```c++
// Windows 平台 :
#define JNICALL __stdcall // __stdcall 是一种函数调用参数的约定 ,表示函数的调用参数是从右往左。
// Linux 平台：
#define JNICALL // Linux 没有进行定义
```

#### 4、jobject

JNI 层对于 Java 层应用类型对象的表示。

每一个从 Java 调用的 native 方法，在 JNI 函数中都会传递一个当前对象的引用。区分 2 种情况：

- 1、静态 native 方法： 第二个参数为 jclass 类型，指向 native 方法所在类的 Class 对象；
- 2、实例 native 方法： 第二个参数为 jobject 类型，指向调用 native 方法的对象。

#### 5、 JavaVM 和 JNIEnv 的作用

JavaVM 和 JNIEnv 是定义在 jni.h 头文件中最关键的两个数据结构：

- JavaVM： 代表 Java 虚拟机，每个 Java 进程有且仅有一个全局的 JavaVM 对象，JavaVM 可以跨线程共享；
- JNIEnv： 代表 Java 运行环境，每个 Java 线程都有各自独立的 JNIEnv 对象，JNIEnv 不可以跨线程共享。

JavaVM 和 JNIEnv 的类型定义在 C 和 C++ 中略有不同，但本质上是相同的，内部由一系列指向虚拟机内部的函数指针组成。 类似于 Java 中的 Interface 概念，不同的虚拟机实现会从它们派生出不同的实现类，而向 JNI 层屏蔽了虚拟机内部实现（例如在 Android ART 虚拟机中，它们的实现分别是 JavaVMExt 和 JNIEnvExt）。

```c++
struct _JNIEnv;
struct _JavaVM;

#if defined(__cplusplus) // C++
typedef _JNIEnv JNIEnv;
typedef _JavaVM JavaVM;
#else // C 
typedef const struct JNINativeInterface* JNIEnv;
typedef const struct JNIInvokeInterface* JavaVM;
#endif

struct _JavaVM {
    // 相当于 C 版本中的 JNIEnv
    const struct JNIInvokeInterface* functions;

    // 转发给 functions 代理
    jint DestroyJavaVM()
    { return functions->DestroyJavaVM(this); }
    ...
};

struct _JNIEnv {
    // 相当于 C 版本的 JavaVM
    const struct JNINativeInterface* functions;

    // 转发给 functions 代理
    jint GetVersion()
    { return functions->GetVersion(this); }
    ...
};

struct JNIInvokeInterface {
    jint        (*DestroyJavaVM)(JavaVM*);
    jint        (*AttachCurrentThread)(JavaVM*, JNIEnv**, void*);
    jint        (*DetachCurrentThread)(JavaVM*);
    jint        (*GetEnv)(JavaVM*, void**, jint);
    jint        (*AttachCurrentThreadAsDaemon)(JavaVM*, JNIEnv**, void*);
};

struct JNINativeInterface {
    jint        (*GetVersion)(JNIEnv *);
    jclass      (*DefineClass)(JNIEnv*, const char*, jobject, const jbyte*, jsize);
    jclass      (*FindClass)(JNIEnv*, const char*);
    ...
};
```
不管是在 C 语言中还是在 C++ 中，JNIInvokeInterface* 和 JNINativeInterface* 这两个结构体指针才是 JavaVM 和 JNIEnv 的实体。不过 C++ 中加了一层包装，在语法上更简洁，例如：
```c++
// 在 C 语言中，要使用 (*env)->
(*env)->FindClass(env, "java/lang/String");
// 在 C++ 中，要使用 env->
env->FindClass("java/lang/String");
```
## 二、方法名


## 参考

- [NDK 使用入门](https://developer.android.google.cn/ndk/guides)
- [NDK开发（二）- JNI](https://www.jianshu.com/p/b0260cf9370f)
- [NDK 系列（5）：JNI 从入门到实践，爆肝万字详解！](https://www.jianshu.com/p/5f48a9190d9d)
- [Android：JNI 与 NDK到底是什么？（含实例教学）](https://blog.csdn.net/carson_ho/article/details/73250163)
- [(译文) JNI编程指南与规范1~4章](https://juejin.cn/post/6930972583848312846)
