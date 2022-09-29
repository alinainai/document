## 一、NDK简单介绍

NDK（Native Developer Kit）原生开发工具集，提供了一套 Java 和 c/c++ 相互调用的技术。

从一个demo开始：

我们新建一个 Native C++ 项目

<img width="240" alt="image" src="https://user-images.githubusercontent.com/17560388/192934644-766b2e2a-4b49-4476-918a-d886d78c26ff.png">

AS 会自动帮我们生成一个和 Activity 交互的 C++ Jni 文件，Activity 代码如下：我们用的是 kotlin 代码，在关键的地方已经添加注释

```kotlin
class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Example of a call to a native method
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



## 参考

- [NDK 使用入门](https://developer.android.google.cn/ndk/guides)
- [NDK开发（二）- JNI](https://www.jianshu.com/p/b0260cf9370f)
- [NDK 系列（5）：JNI 从入门到实践，爆肝万字详解！](https://www.jianshu.com/p/5f48a9190d9d)
