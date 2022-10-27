## 一、引用

在Java中，对象在堆上分配，由垃圾回收器自动进行回收，在C/C++中，分配在堆上的对象需要手动调用 delete 去释放。JNI 层作为 Java 层和 C/C++ 层之间的桥接层，所以需要提供一套规则来处理这种情况。

在JNI规范中定义了三种引用：局部引用（Local Reference）、全局引用（Global Reference）、弱全局引用（Weak Global Reference）

### 1、局部引用

大部分 JNI 函数会创建局部引用，局部引用只有在创建引用的本地方法返回前有效，也只在创建局部引用的线程中有效。在方法返回后，局部引用会自动释放，也可以通过 DeleteLocalRef 函数手动释放；

局部引用会阻止GC回收所引用的对象，不能在本地函数中跨函数使用，也不能跨线程使用。我们可以通过 NewLocalRef 和各种JNI接口（FindClass、NewObject、GetObjectClass和NewCharArray等）来创建局部引用对象。

当使用大内存的Java对象，或者大量使用 Java 对象时，可以通过 DeleteLocalRef 函数手动释放局部对象。

### 2、全局引用： 

全局引用可以跨方法、跨线程使用，直到它被手动释放才会失效。同局部引用一样，全局引用也会阻止它所引用的对象被GC回收。

全局引用只能通过 NewGlobalRef 函数创建，不再使用对象时必须通过 DeleteGlobalRef 函数释放。

### 3、弱全局引用

弱引用与全局引用类似，区别在于弱全局引用不会持有强引用，因此不会阻止垃圾回收器回收引用指向的对象。弱全局引用通过 NewGlobalWeakRef 函数创建，不再使用对象时必须通过 DeleteGlobalWeakRef 函数释放。
```c++
//创建全局引用
jstring global_str;
JNIEXPORT void JNICALL Java_com_egas_demo_JniDemoClass_handlerDemo
        (JNIEnv *env, jobject thiz,jstring str) {
    // 局部引用
    jstring jstr = env->NewStringUTF("cc");
    auto local_str = (jstring)env->NewLocalRef(jstr);
    //释放局部引用
    env->DeleteLocalRef(local_str);

    // 全局引用
    global_str = (jstring)env->NewGlobalRef(jstr);
    env->DeleteGlobalRef(global_str);

    // 弱全局引用
    auto weakRefClz = (jstring)env->NewWeakGlobalRef(jstr);
    env->DeleteWeakGlobalRef(weakRefClz);  
}
```
### 4、JNI 引用的实现原理

在 JavaVM 和 JNIEnv 中，会分别建立多个表管理引用：

JavaVM 内有 globals 和 weak_globals 两个表管理全局引用和弱全局引用。由于 JavaVM 是进程共享的，因此全局引用可以跨方法和跨线程共享；
JavaEnv 内有 locals 表管理局部引用，由于 JavaEnv 是线程独占的，因此局部引用不能跨线程。另外虚拟机在进入和退出本地方法通过 Cookie 信息记录哪些局部引用是在哪些本地方法中创建的，因此局部引用是不能跨方法的。

### 5、比较引用是否指向相同对象

可以使用 JNI 函数 IsSameObject 判断两个引用是否指向相同对象（适用于三种引用类型），返回值为 JNI_TRUE 时表示相同，返回值为 JNI_FALSE 表示不同。例如：

示例程序

```c++
jclass localRef = ...
jclass globalRef = ...
bool isSampe = env->IsSamObject(localRef, globalRef）
```
另外，当引用与 NULL 比较时含义略有不同：

局部引用和全局引用与 NULL 比较： 用于判断引用是否指向 NULL 对象；
弱全局引用与 NULL 比较： 用于判断引用指向的对象是否被回收。

## 二、异常处理

### 1、异常处理机制

JNI： 程序使用 JNI 函数 ThrowNew 抛出异常，程序不会中断当前执行流程，而是返回 Java 层后，虚拟机才会抛出这个异常。因此，在 JNI 层出现异常时，有 2 种处理选择：

- 方法 1： 直接 return 当前方法，让 Java 层去处理这个异常（这类似于在 Java 中向方法外层抛出异常）；
- 方法 2： 通过 JNI 函数 ExceptionClear 清除这个异常，再执行异常处理程序（这类似于在 Java 中 try-catch 处理异常）。需要注意的是，当异常发生时，必须先处理-清除异常，再执行其他 
JNI 函数调用。 因为当运行环境存在未处理的异常时，只能调用 2 种 JNI 函数：异常护理函数和清理资源函数。

JNI 提供了以下与异常处理相关的 JNI 函数：

```c++
ThrowNew： 向 Java 层抛出异常；
ExceptionDescribe： 打印异常描述信息；
ExceptionOccurred： 检查当前环境是否发生异常，如果存在异常则返回该异常对象；
ExceptionCheck： 检查当前环境是否发生异常，如果存在异常则返回 JNI_TRUE，否则返回 JNI_FALSE；
ExceptionClear： 清除当前环境的异常。
```

```c++
jni.h

struct JNINativeInterface {
    // 抛出异常
    jint        (*ThrowNew)(JNIEnv *, jclass, const char *);
    // 检查异常
    jthrowable  (*ExceptionOccurred)(JNIEnv*);
    // 检查异常
    jboolean    (*ExceptionCheck)(JNIEnv*);
    // 清除异常
    void        (*ExceptionClear)(JNIEnv*);
};
```
示例程序
```c++
// 异常
JNIEXPORT void JNICALL
Java_com_egas_demo_JniDemoClass_exceptingDemo
        (JNIEnv *env, jobject thiz) {
    jclass clz = env->GetObjectClass(thiz);// 获取 jclass
    jfieldID mFieldId = env->GetFieldID(clz, "strField1", "Ljava/lang/String;"); // 由于没有 strField1 这个属性，此处会向 Java 抛出一个 NoSuchFieldError 异常 
    if(env->ExceptionOccurred()){ // 1、通过 ExceptionOccurred 方法判断是否发生异常
        env->ExceptionDescribe(); // 2、打印异常信息
        env->ExceptionClear(); // 3、清楚异常
        jclass newExcCls;
        newExcCls = env->FindClass("java/lang/RuntimeException");
        if (newExcCls == nullptr) {
            return;
        }
        env->ThrowNew(newExcCls,"调用 strField1 属性发生了异常 "); // 4、抛出一个新异常
    }
}

```

## 三、JNI 与多线程

### 1、 不能跨线程的引用

在 JNI 中，JNIEnv 和局部引用是无法跨线程调用的

JNIEnv： JNIEnv 只在所在的线程有效，在不同线程中调用 JNI 函数时，必须使用该线程专门的 JNIEnv 指针，不能跨线程传递和使用。通过 AttachCurrentThread 函数将当前线程依附到 JavaVM 上，获得属于当前线程的 JNIEnv 指针。如果当前线程已经依附到 JavaVM，也可以直接使用 GetEnv 函数。

示例程序
```c++
JNIEnv * env_child;
vm->AttachCurrentThread(&env_child, nullptr);
// 使用 JNIEnv*
vm->DetachCurrentThread();
```
局部引用： 局部引用只在创建的线程和方法中有效，不能跨线程使用。可以将局部引用升级为全局引用后跨线程使用。

示例程序
```c++
// 局部引用
jclass localRefClz = env->FindClass("java/lang/String");
// 释放全局引用（非必须）
env->DeleteLocalRef(localRefClz);
// 局部引用升级为全局引用
jclass globalRefClz = env->NewGlobalRef(localRefClz);
// 释放全局引用（必须）
env->DeleteGlobalRef(globalRefClz);
```
### 2、监视器同步

在 JNI 中也会存在多个线程同时访问一个内存资源的情况，此时需要保证并发安全。在 Java 中我们会通过 synchronized 关键字来实现互斥块（背后是使用监视器字节码），在 JNI 层也提供了类似效果的 JNI 函数：
```c++
MonitorEnter： 进入同步块，如果另一个线程已经进入该 jobject 的监视器，则当前线程会阻塞；
MonitorExit： 退出同步块，如果当前线程未进入该 jobject 的监视器，则会抛出 IllegalMonitorStateException 异常。
```
```c++
jni.h

struct JNINativeInterface {
    jint        (*MonitorEnter)(JNIEnv*, jobject);
    jint        (*MonitorExit)(JNIEnv*, jobject);
}
```
示例程序
```c++
// 进入监视器
if (env->MonitorEnter(obj) != JNI_OK) {
    // 建立监视器的资源分配不成功等
}

// 此处为同步块
if (env->ExceptionOccurred()) {
    // 必须保证有对应的 MonitorExit，否则可能出现死锁
    if (env->MonitorExit(obj) != JNI_OK) {
        ...
    };
    return;
}

// 退出监视器
if (env->MonitorExit(obj) != JNI_OK) {
    ...
};
```
### 3、等待与唤醒

JNI 没有提供 Object 的 wati/notify 相关功能的函数，需要通过 JNI 调用 Java 方法的方式来实现：

示例程序
```c++
static jmethodID MID_Object_wait;
static jmethodID MID_Object_notify;
static jmethodID MID_Object_notifyAll;

void
JNU_MonitorWait(JNIEnv *env, jobject object, jlong timeout) {
    env->CallVoidMethod(object, MID_Object_wait, timeout);
}
void
JNU_MonitorNotify(JNIEnv *env, jobject object) {
    env->CallVoidMethod(object, MID_Object_notify);
}
void
JNU_MonitorNotifyAll(JNIEnv *env, jobject object) {
    env->CallVoidMethod(object, MID_Object_notifyAll);
}
```
### 4、创建线程的方法
在 JNI 开发中，有两种创建线程的方式：

- 方法 1 - 通过 Java API 创建： 使用我们熟悉的 Thread#start() 可以创建线程，优点是可以方便地设置线程名称和调试；
- 方法 2 - 通过 C/C++ API 创建： 使用 pthread_create() 或 std::thread 也可以创建线程
示例程序

```c++
// 
void *thr_fn(void *arg) {
    printids("new thread: ");
    return NULL;
}

int main(void) {
    pthread_t ntid;
    // 第 4 个参数将传递到 thr_fn 的参数 arg 中
    err = pthread_create(&ntid, NULL, thr_fn, NULL);
    if (err != 0) {
        printf("can't create thread: %s\n", strerror(err));
    }
    return 0;
}
```

## 四、动态注册

在我们第一篇NDK的文章中我们使用的是静态方式注册 jni 方法，方法名和类名绑定，不太好修改。

我们可通过动态注册的方法来规避这种缺点。

### 1、原理
在 Java 层调用 System.loadLibrary() 时，虚拟机会调用 jni 库中的JNI_OnLoad() 方法
```c++
jint JNI_OnLoad(JavaVM* vm, void* reserved);
```

返回值表示动态库需要的jni版本，如，JNI_VERSION_1_1, JNI_VERSION_1_2, JNI_VERSION_1_4, JNI_VERSION_1_6，如果动态库没有提供 JNI_OnLoad()函数会默认使用 JNI_VERSION_1_1 版本。

JNI_OnLoad() 用来做一些初始化操作，我们可以在该方法中调用 JNIEnv 的 RegisterNatives() 来动态注册方法

```c++
// clazz：Java Class 对象的表示
// methods：JNINativeMethod 结构体数组
// nMethods：JNINativeMethod 结构体数组长度
// 返回值 0代表成功，负值代表失败
jint RegisterNatives(JNIEnv *env, jclass clazz, const JNINativeMethod *methods, jint nMethods);
```
JNINativeMethod 是一个结构体
```c++
typedef struct {
    const char* name; // 代表 java 类中的 native 方法名
    const char* signature; // 方法签名
    void*       fnPtr; // 是一个函数指针，指向jni层的一个函数，也就是 java 层和 native 层建立联系的函数
} JNINativeMethod;
```
下面我们通过一个例子看下具体的用法

### 2、代码

```c++
// 需要注册的 Java 层类名
#define JNIREG_CLASS "com/egas/demo/JniDemoClass"

// 和java层方法对应的jni方法
JNIEXPORT jint JNICALL dynamicMethod
        (JNIEnv *env, jobject,jstring str ) {
    const char *javaStr = env->GetStringUTFChars(str, JNI_FALSE);
    LOGE("str=%s", javaStr);
    env->ReleaseStringUTFChars(str, javaStr);
    return 9;
}

// JNINativeMethod 结构体数组
static JNINativeMethod methods[] = {
        {"dynamicMethod", "(Ljava/lang/String;)I",(void*)dynamicMethod}, // JNINativeMethod 结构体
};

// 动态注册
static int registerNatives(JNIEnv* env)
{
    // 根据类名获取 jclass 对象
    jclass clazz = env->FindClass(JNIREG_CLASS);
    if (clazz == nullptr) {
        return JNI_FALSE;
    }
    // 调用 RegisterNatives() 进行方法注册
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

// 加载 so 库的回调
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = nullptr;
    jint result = -1;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    assert(env != nullptr);
    // 执行动态注册
    if (!registerNatives(env)) {
        return -1;
    }
    result = JNI_VERSION_1_6;
    return result;
}
```
代码很简单，

## 参考

- [NDK开发（二）- JNI](https://www.jianshu.com/p/b0260cf9370f)
- [NDK 系列（5）：JNI 从入门到实践，爆肝万字详解！](https://www.jianshu.com/p/5f48a9190d9d)
- [JNI/NDK开发指南（九）——JNI调用性能测试及优化](https://blog.csdn.net/xyang81/article/details/44279725)
- [NDK-JNI实战教程（二） JNI官方中文资料 ](https://www.cnblogs.com/jycboy/archive/2016/04/15/5396876.html#quanjujubuyinyong)
- [Android对so体积优化的探索与实践](https://mp.weixin.qq.com/s/7YVuouHAq2OfrowhoHVmnQ)
