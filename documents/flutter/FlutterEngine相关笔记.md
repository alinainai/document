1.一个Native进程只有一个DartVM。

2.一个DartVM(或说一个Native进程)可以有多个FlutterEngine。

3.多个FlutterEngine运行在各自的Isolate中，他们的内存数据不共享，需要通过Isolate事先设置的port(顶级函数)通讯。

4.FlutterEngine可以后台运行代码，不渲染UI；也可以通过FlutterRender渲染UI。

5.初始化第一个FlutterEngine时，DartVM会被创建，之后不会再有其他DartVM环境被创建。

6.FlutterEngine可以通过FlutterEngineCache管理缓存，建议使用阿里闲鱼的flutter_boost来管理Native&Flutter页面混合的项目。

7.我们可以手动改动Flutter项目的入口函数、flutter_assets资源路径、flutter项目初始Route等参数。涉及到的API有FlutterLoader、DartExecutor、FlutterJNI、Host等等。简单描述下，就是使用BinaryMessager传输数据，在修改入口函数、初始化Route参数之后在调用DartExecutor的执行代码

```kotlin
/**
 * 创建一个新的engine
 */
fun createEngine(context: Application, dartVmArgs: Array<String>?) {
    if (flutterEngine != null) {
        throw IllegalStateException("Already has a unrelease engine!")
    }
    flutterEngine = FlutterEngine(context, dartVmArgs)
    flutterEngine?.navigationChannel?.setInitialRoute("/page/me")
    flutterEngine?.dartExecutor?.executeDartEntrypoint(
        DartExecutor.DartEntrypoint.createDefault()
    )
    FlutterEngineCache
        .getInstance()
        .put(FlutterConst.UNIQUE_ENGINE_NAME, flutterEngine)
}
```

8.FlutterEngine创建之后需要手动启动，调用FlutterEngine.destory()之后，该Engine就不能再使用了，并且需要清空FlutterEngineCache中的缓存。

```kotlin
/**
 * 释放flutter引擎
 */
fun releaseFlutterEngine() {
    flutterEngine?.let { engine ->
        FlutterEngineCache.getInstance().remove(FlutterConst.UNIQUE_ENGINE_NAME)
        engine.destroy()
    }
    flutterEngine = null
}
```

