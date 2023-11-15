1、一个 Native 进程只有一个 DartVM。

2、一个 DartVM (或说一个 Native 进程)可以有多个 FlutterEngine。

3、多个 FlutterEngine 运行在各自的Isolate中，他们的内存数据不共享，需要通过 Isolate 事先设置的 port (顶级函数)通讯。

4、FlutterEngine 可以后台运行代码，不渲染UI；也可以通过 FlutterRender 渲染UI。

5、初始化第一个 FlutterEngine 时，DartVM 会被创建，之后不会再有其他 DartVM 环境被创建。

6、FlutterEngine 可以通过 FlutterEngineCache 管理缓存，建议使用阿里闲鱼的 flutter_boost 来管理 Native&Flutter 页面混合的项目。

7、我们可以手动改动 Flutter 项目的入口函数、flutter_assets 资源路径、flutter 项目初始 Route 等参数。涉及到的 API 有F lutterLoader、DartExecutor、FlutterJNI、Host 等等。简单描述下，就是使用 BinaryMessager 传输数据，在修改入口函数、初始化 Route 参数之后在调用 DartExecutor 的执行代码

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

8、FlutterEngine 创建之后需要手动启动，调用 FlutterEngine.destory() 之后，该 Engine 就不能再使用了，并且需要清空 FlutterEngineCache 中的缓存。

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

