## 一、NDK简单介绍

### 1、JNI和NDk

`JNI（Java Native Interface）`是 JAVA 平台中的一个强大功能。作为 JAVA 虚拟机的部分实现，JNI 是一个双向接口，允许 JAVA 应用程序调用本地代码，反之亦然。

`NDK（Native Developer Kit）`是 Android 系统提供的一套 Java 和 C/C++ 相互调用的工具集。

### 2、android demo

从一个 android demo 开始：我们新建一个 `Native C++` 项目，Java Staduard 选择 `C++ 11` 版本。

AS 会自动生成一个 Activity 以及和他交互的 Jni 文件，Activity 主要代码如下（我们用的是 kotlin 代码，和Java有些区别，在关键的地方已经添加了相关注释）：

```kotlin
binding.sampleText.text = stringFromJNI() // 3、使用 JNI 返回的 String
private external fun stringFromJNI(): String // 2、在 kotlin 中注意关键字是 external, 在 Java 中是 native 关键字
companion object {
    init {
        System.loadLibrary("demo") // 1、在静态方法中配置 demo 包
    }
}
```
demo.cpp 的代码：

```c++
#include <jni.h> // 引入头文件，相当于 java 的导包，jni 相关的方法声明和宏都在其中。
#include <string> // C++ 中的 string

extern "C" JNIEXPORT jstring JNICALL
Java_com_egas_demo_MainActivity_stringFromJNI(JNIEnv* env, jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
```

### 3、关键字分析
我们简单的分析一下 `demo.cpp` 方法相关的关键字：

- `extern "C"`
表示按照类C的编译和连接规约来编译和连接，因为 C 和 C++ 语法有区别，C++ 形式编译的代码 C 可能识别不了，比如 C 中没有重载方法

- JNIEXPORT
表示一个函数需要暴露给共享库外部使用时

```c++
// Windows 平台定义如下 :
#define JNIEXPORT __declspec(dllexport)
#define JNIIMPORT __declspec(dllimport)
// Linux 平台定义如下：
#define JNIIMPORT
#define JNIEXPORT  __attribute__ ((visibility ("default"))) // 让该方法对于外界可见
```

- JNICALL
表示一个函数是 JNI 函数，
```c++
// Windows 平台 :
#define JNICALL __stdcall // __stdcall 是一种函数调用参数的约定 ,表示函数的调用参数是从右往左。
// Linux 平台：
#define JNICALL // Linux 没有进行定义
```

- jobject
jobject 是 JNI 层对于 Java 层应用类型对象的映射。

从 Java 调用 native 方法，在 JNI 函数中会传递一个当前对象的引用。

区分 2 种情况：

1. 对于静态的 native 方法： 第二个参数为 jclass 类型，指向 native 方法所在类的 Class 对象；
2. 对月实例 native 方法： 第二个参数为 jobject 类型，指向调用 native 方法的对象。

- JavaVM 和 JNIEnv 

JavaVM 和 JNIEnv 是定义在 jni.h 头文件中最关键的两个数据结构：

- JavaVM： 代表 Java 虚拟机，每个 Java 进程有且仅有一个全局的 JavaVM 对象，线程共享；
- JNIEnv： 代表 Java 运行环境，每个 Java 线程都有各自独立的 JNIEnv 对象，线程私有。

JavaVM 和 JNIEnv 的类型定义在 C 和 C++ 中略有不同，但本质上是相同的，内部由一系列指向虚拟机内部的函数指针组成。 

类似于 Java 中的 Interface 概念，不同的虚拟机实现会从它们派生出不同的实现类，而向 JNI 层屏蔽了虚拟机内部实现（例如在 Android ART 虚拟机中，它们的实现分别是 JavaVMExt 和 JNIEnvExt）。

```c++
struct _JNIEnv;
struct _JavaVM;

#if defined(__cplusplus) // C++ 环境
typedef _JNIEnv JNIEnv;
typedef _JavaVM JavaVM;
#else // C 环境
typedef const struct JNINativeInterface* JNIEnv;
typedef const struct JNIInvokeInterface* JavaVM;
#endif

struct _JavaVM {
    const struct JNIInvokeInterface* functions;
    
    jint DestroyJavaVM()
    { return functions->DestroyJavaVM(this); } // 转发给 functions 代理
    ...
};

struct _JNIEnv {
    const struct JNINativeInterface* functions;
    
    jint GetVersion()
    { return functions->GetVersion(this); } // 转发给 functions 代理
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
不管是在 C 语言中还是在 C++ 中，`JNIInvokeInterface*` 和 `JNINativeInterface*` 这两个结构体指针才是 `JavaVM` 和 `JNIEnv` 的实体。C++ 加了一层包装，在语法上使用更简洁。
```c++
// 在 C 语言中，要使用 (*env)->
(*env)->FindClass(env, "java/lang/String");
// 在 C++ 中，要使用 env->
env->FindClass("java/lang/String");
```

本章节demo参见：[https://github.com/alinainai/AndroidDemo/tree/feature/feature_ndk_v1](https://github.com/alinainai/AndroidDemo/tree/feature/feature_ndk_v1)

## 二、Java调用Jni

在例子中， Java 中的 native 方法最终会与 Jni 中 `Java_类名(下划线分割包名)_方法名` 相对应，并且默认带有两个参数，JNIEnv* 和 jobject(静态方法 是 jclass 类型):

```c++
Java_com_egas_demo_MainActivity_stringFromJNI(JNIEnv* env, jobject /* this */) {
```
在 MainActivity 中通过 `stringFromJNI()` 可以直接获取返回结果。我们先看一下 java 和 jni 类型的映射关系

### 1、基本类型

对于基本数据类型，会直接转换为 C/C++ 的基础数据类型，例如 int 类型映射为 jint 类型。由于 jint 是 C/C++ 类型，所以可以直接当作普通 C/C++ 变量使用，而不需要依赖 JNIEnv 环境对象；

注意：基础数据类型在映射时是直接映射，而不会发生数据格式转换。例如，Java char 类型在映射为 jchar 后旧是保持 Java 层的样子，数据长度依旧是 2 个字节，而字符编码依旧是 UNIT-16 编码。

具体映射关系都定义在 [jni.h](https://github.com/openjdk/jdk/blob/master/src/java.base/share/native/include/jni.h) 头文件中，映射的表格如下：

|Java Language Type|Natice Type |Description|
| :-----:|:-----:|:-----:|
|boolean	|jboolean|	unsigned 8 bits|
|byte	|jbyte	|signed 8 bits|
|char	|jchar	|unsigned 16 bits|
|short	|jshort	|signed 16 bits|
|int	|jint|signed 32 bits|
|long	|jlong	|signed 64 bits|
|float	|jfloat	|32 bits|
|double	|jdouble|64 bits|

### 2、引用类型

对于引用数据类型，对象只会转换为一个 C/C++ 指针，例如 Object 类型映射为 jobject 类型。

由于指针指向 Java 虚拟机内部的数据结构，所以不可能直接在 C/C++ 代码中操作对象，而是需要依赖 JNIEnv 环境对象。另外，为了避免对象在使用时突然被回收，在本地方法返回前，虚拟机会固定（pin）对象，阻止其 GC。

映射关系：

<img width="600" alt="类图" src="https://user-images.githubusercontent.com/17560388/194711830-4e603314-3c4a-4cff-84a5-056acc82b77c.png">

我们先看一下 jni 对 String 类型的处理

### 3、String 类型

jni 专门定义了一个 jstring 来接收 Java 传递过来的 String 类型。但是由于字符编码的类型不同，我们不能在C/C++的环境中直接使用 jstring 字符串。

JNIEnv 中提供了几个方法专门处理 jstring 类型：

```c++
// 将 jstring 指向的内容转换为一个 UTF-8 的 C/C++ 字符串
// isCopy，是否复制，有下面两个参数：
// JNI_TRUE： 使用拷贝模式，JVM 将拷贝一份原始数据来生成 UTF-8 字符串；
// JNI_FALSE： 使用复用模式，JVM 将复用同一份原始数据来生成 UTF-8 字符串。
const char* GetStringUTFChars(jstring str, jboolean *isCopy)
```
```c++
// 将 jstring 生成的 C/C++ 字符串释放掉
void ReleaseStringUTFChars(jstring str, const char* chars)
```
```c++
// 生成一个 jstring 对象，使用的时候要记得判空，由于空间不足可能会生成失败
jstring NewStringUTF(const char *utf)
```
示例代码
```c++
std::string hello = "Hello from C++ ";
const char *javaStr = env->GetStringUTFChars(owner, JNI_FALSE);
hello.append(javaStr);
env->ReleaseStringUTFChars(owner, javaStr);
jstring str = env->NewStringUTF(hello.c_str());
if(str){...}
```
### 4、数组

数组类都派生自 jarray 类，数组又分为基本类型数组和引用类型数组。

可以通过下面方法获取数组的 size:
```c++
jsize GetArrayLength(jarray array)
```
#### 4.1 基本类型数组

如 jintArray、jbooleanArray，和 jstring 的处理方式差不多，JNIEnv 提供了几个处理基本类型数组的方法，我们以 jintArray 为例：

```c++
// 生成一个 jintArray 
 jintArray NewIntArray(jsize length)
```
```c++
// 将 jintArray 转成 C/C++ 的数组
jint* GetIntArrayElements(jintArray array, jboolean* isCopy)
```
```c++
// 释放生成的 C/C++ 的数组
// mode参数的意义
// 0:将 C/C++ 数组的数据回写到 Java 数组，并释放 C/C++ 数组
// JNI_COMMIT:将 C/C++ 数组的数据回写到 Java 数组，并不释放 C/C++ 数组
// JNI_ABORT:不回写数据，但释放 C/C++ 数组
void ReleaseIntArrayElements(jintArray array, jint* elems, jint mode)
```
示例代码
```c++
jintArray jarr = env->NewIntArray(size);
jint *carr = env->GetIntArrayElements(jarr, JNI_FALSE);
env->ReleaseIntArrayElements(jarr, carr, 0);
```
#### 4.2 引用类型数组

不支持将 `Java` 引用类型数组转换为 `C/C++` 数组：与基本类型数组不同，引用类型数组的元素 jobject 是一个指针，不存在转换为 `C/C++` 数组的概念。

```c++
// 获取数组中 index 的对象引用（指针）
jobject GetObjectArrayElement(jobjectArray array, jsize index)
```
```c++
// 设置数组中 index 的值
void SetObjectArrayElement(jobjectArray array, jsize index, jobject value)
```
构造一个 jobjectArray 数组
```c++
// 首先获取 jclass 类型，设置数组的类型
jclass jStringClazz = env->FindClass("java/lang/String"); 
// 构造新数组，nullptr 为每个元素初始的默认值，可以直接设置为 nullptr
jobjectArray jarr = env->NewObjectArray(size, jStringClazz, nullptr);
```
我们以 String[] 为例子写几个实例方法：

```c++
jclass jStringClazz = env->FindClass("java/lang/String");
jobjectArray jarr = env->NewObjectArray(size, jStringClazz, nullptr);
for (int i = 0; i < size; ++i) {
    std::string str = std::to_string(i);
    jstring s = env->NewStringUTF(str.c_str());
    env->SetObjectArrayElement(jarr,i,s);
}
```

## 三、Jni调用Java代码

Jni 调用 Java 代码可以分为修改字段属性和调用方法，因为 Jni 调用 Java 方法的相关Api中会用到字段/方法的描述符，我们先来看一下 `字段描述符` 和 `方法描述符`。

字段描述符：字段描述符其实就是描述字段的类型，JVM 对每种基础数据类型定义了固定的描述符，而引用类型则是以 L 开头的形式：

|Java类型|描述符|举例|
|:---:|:---:|:---:|
|boolean|Z||
|byte|B||
|char|	C||
|short|	S||
|int|	I||
|long|	J||
|floag|	F||
|double|	D||
|void|	V||
|object|以 "L"开头";"结尾，中间用"/"分隔的包名和类名。|例如 String 的字段描述符为 Ljava/lang/String;|
|数组|`[`开头+类型描述符|例如，`int[]`-> `[I`、`String[]`->`[Ljava/lang/String;`|
|多维数组|多个`[`开头+类型描述符|例如，`int[][]`-> `[[I`|

方法描述符：方法描述符其实就是描述方法的返回值类型和参数表类型，参数类型用一对圆括号括起来，按照参数声明顺序列举参数类型，返回值出现在括号后面。例如方法 void fun(); 的简单名称为 fun，方法描述符为 ()V

### 1、获取 Java 中 class

不管是处理Java中的字段还是调用Java中的方法，都需要通过 class 来获取对应的ID。我们可以通过下面这个API来获取对应的 class 类
```c++
// name:一个完全限定的类名，即包含“包名”+“/”+类名。如 java.lang.String -> java/lang/String
jclass FindClass(JNIEnv *env,const char *name);
```

### 2、JNI 访问 Java 字段
本地代码访问 Java 字段的流程分为 2 步：

1. 通过 jclass 获取字段 ID，例如：Fid = env->GetFieldId(clz, "name", "Ljava/lang/String;");
2. 通过字段 ID 访问字段，例如：Jstr = env->GetObjectField(thiz, Fid);

Java 字段分为静态字段和实例字段，相关方法如下：

```c++
// 获取实例方法的字段 ID
jfieldID GetFieldID(jclass clazz, const char* name, const char* sig)
// 获取静态方法的字段 ID
jfieldID GetStaticFieldID(jclass clazz, const char* name, const char* sig)
```

```c++
// 获取类型为 Type 的实例字段（例如 GetIntField）
jType Get<Type>Field(jobject obj, jfieldID fieldID)
// 设置类型为 Type 的实例字段（例如 SetIntField）
void Set<Type>Field(jobject obj, jfieldID fieldID, jType value)
// 获取类型为 Type 的静态字段（例如 GetStaticIntField）
jType GetStatic<Type>Field(jclass obj, jfieldID fieldID)
// 设置类型为 Type 的静态字段（例如 SetStaticIntField）
void SetStatic<Type>Field(jclass clazz, jfieldID fieldID, jType value)
```
示例程序
```c++
jclass clz = env->GetObjectClass(thiz);// 获取 jclass
// 访问静态字段
jfieldID sFieldId = env->GetStaticFieldID(clz, "staticField", "Ljava/lang/String;"); // 静态字段 ID
if (sFieldId) {
    // Java 方法的返回值 String 映射为 jstring
    jstring jStr = static_cast<jstring>(env->GetStaticObjectField(clz, sFieldId));
    const char *sStr = env->GetStringUTFChars(jStr, JNI_FALSE);
    env->ReleaseStringUTFChars(jStr, sStr);
    jstring newStr = env->NewStringUTF("静态字段修改");
    if (newStr) {
        env->SetStaticObjectField(clz, sFieldId, newStr);
    }
}
// 访问实例字段
jfieldID mFieldId = env->GetFieldID(clz, "strField", "Ljava/lang/String;");
if (mFieldId) {
    jstring jStr = static_cast<jstring>(env->GetObjectField(thiz, mFieldId));
    const char *sStr = env->GetStringUTFChars(jStr, JNI_FALSE);
    env->ReleaseStringUTFChars(jStr, sStr);
    jstring newStr = env->NewStringUTF("实例字段修改");
    if (newStr) {
        env->SetObjectField(thiz, mFieldId, newStr);
    }
}
```
### 3、JNI 调用 Java 方法
本地代码访问 Java 方法与访问 Java 字段类似，访问流程分为 2 步：

1. 通过 jclass 获取「方法 ID」，例如：Mid = env->GetMethodID(jclass, "helloJava", "()V");
2. 通过方法 ID 调用方法，例如：env->CallVoidMethod(thiz, Mid);

```c++
// 获取实例方法 ID
jmethodID GetMethodID(jclass clazz, const char* name, const char* sig)
// 获取静态方法 ID
jmethodID GetStaticMethodID(jclass clazz, const char* name, const char* sig)
```

```c++
Call<Type>Method：调用返回类型为 Type 的实例方法（例如 CallVoidMethod）
CallStatic<Type>Method：调用返回类型为 Type 的静态方法（例如 CallStaticVoidMethod）
CallNonvirtual<Type>Method：调用返回类型为 Type 的父类方法（例如 CallNonvirtualVoidMethod）
```
示例代码
```c++
// 获取 jclass
jclass clz = env->GetObjectClass(thiz);
// 调用 Java 静态方法
jmethodID sMethodId = env->GetStaticMethodID(clz, "sLogHelloJava", "()V");
if (sMethodId) {
    env->CallStaticVoidMethod(clz, sMethodId);
}
// 调用 Java 实例方法
jmethodID mMethodId = env->GetMethodID(clz, "logHelloJava", "()V");
if (mMethodId) {
    env->CallVoidMethod(thiz, mMethodId);
}
```
### 4、缓存ID

获取字段和方法 ID 需要基于字段和方法 ID 的名字和描述符进行符号查找。符号查找消耗相对较多，我们可以使用缓存的方式来存储字段和方法 ID。

#### 4.1 在使用时缓存

在使用时我们采用静态变量对方法 ID 进行缓存，以便在每次调用 cacheMethodId 方法时，不需要重新获取

```c++
JNIEXPORT void JNICALL
Java_com_egas_demo_JniDemoClass_cacheMethodId(JNIEnv *env, jobject obj) {
    static jfieldID fid_s = nullptr;  // 使用时缓存ID
    jclass cls = env->GetObjectClass(obj);
    jstring jstr;
    const char *str;

    if (fid_s == nullptr) {
        LOGE("nullptr = nullptr");
        fid_s = env->GetFieldID( cls, "strField", "Ljava/lang/String;");
        if (fid_s == nullptr) {
            return; /* exception already thrown */
        }
    }

    jstr = static_cast<jstring>(env->GetObjectField(obj, fid_s));
    str = env->GetStringUTFChars( jstr, JNI_FALSE);
    if (str == nullptr) {
        return; /* out of memory */
    }

    env->ReleaseStringUTFChars( jstr, str);
    jstr = env->NewStringUTF( "123");
    if (jstr == nullptr) {
        return; /* out of memory */
    }
    env->SetObjectField(obj, fid_s, jstr);
}
```
该静态变量初始化为 NUnullptrLL，当 cacheMethodId 方法第一次被调用时，它计算该字段 ID 然后将其缓存到该静态变量中以方便后续使用。


#### 4.2 在类的静态初始化块中执行缓存
当我们在使用时缓存字段或方法 ID 的时候，可能导致缓存和检查的重复工作。在许多情况下，在程序能够有机会调用本地方法前，初始化本地方法所需要的字段和方法 ID 会更为方便。虚拟机会在调用该类中的任何方法前，总是执行类的静态初始化器。因此，一个计算并缓存字段和方法 ID 的合适位置是在该字段和方法 ID 的类的静态初始化块中。

```java
static {
    System.loadLibrary("demo");
    initCacheMethodId();// 在执行任何任何方法（例如 nativeMethod 或 main）之前虚拟机先运行静态初始化块
}
// 类初始化时缓存方法ID
public static native void initCacheMethodId();
// 使用缓存的方法ID
public native void verifyInitCacheMethodId();
```
在 c++ 代码中
```c++
// 全局变量缓存ID
jmethodID MID_InstanceMethodCall_callback;
JNIEXPORT void JNICALL Java_com_egas_demo_JniDemoClass_initCacheMethodId
        (JNIEnv *env, jclass clz) {
    MID_InstanceMethodCall_callback = env->GetMethodID(clz, "logHelloJava", "()V");
}
// 使用缓存的 MethodID
JNIEXPORT void JNICALL Java_com_egas_demo_JniDemoClass_verifyInitCacheMethodId
        (JNIEnv *env, jobject thiz) {
    env->CallVoidMethod(thiz, MID_InstanceMethodCall_callback);
}
```

以上两节的的内容参见 demo [https://github.com/alinainai/AndroidDemo/tree/feature/feature_ndk_v2](https://github.com/alinainai/AndroidDemo/tree/feature/feature_ndk_v2)



## 参考

- [NDK 使用入门](https://developer.android.google.cn/ndk/guides)
- [NDK开发（二）- JNI](https://www.jianshu.com/p/b0260cf9370f)
- [NDK 系列（5）：JNI 从入门到实践，爆肝万字详解！](https://www.jianshu.com/p/5f48a9190d9d)
- [Android：JNI 与 NDK到底是什么？（含实例教学）](https://blog.csdn.net/carson_ho/article/details/73250163)
- [(译文) JNI编程指南与规范1~4章](https://juejin.cn/post/6930972583848312846)
- [Android JNI学习(四)——JNI的常用方法的中文API](https://www.jianshu.com/p/67081d9b0a9c)
