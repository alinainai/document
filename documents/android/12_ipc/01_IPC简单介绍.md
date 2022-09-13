## 1、IPC简单介绍

IPC 进程间通信技术。

因为有进程隔离，进程间的数据是不共享的。所以需要进程间通信技术来实现进程间数据的交互。

## 2、Android 多进程

### 2.1 如何开启多进程

1. 通过 JNI 在 native 层 fork 一个新的进程。忽略掉
2. 给四大组件在 Manifest 指定 android:process。

咱们以 Activity 为例子，3个 Activity 指定不同的进程，如下：
```html
<activity
    android:name=".MainActivity"
    android:exported="true" />
<activity android:name=".Remote1Activity"
    android:exported="false"
    android:process=":remote" />
<activity android:name=".Remote2Activity"
    android:exported="false"
    android:process="com.egas.demo.remote" />
```

使用 adb 指令查看当前 app 所运行的线程

```shell 
adb shell ps | grep yourpackagename
```
```shell 
u0_a911   26215   701 5307828  78088 0  0 S com.egas.demo
u0_a911   26216   701 5307852  79988 0  0 S com.egas.demo:remote
u0_a911   26217   701 5272876  77828 0  0 S com.egas.demo.remote
```

`:remote` 表示进程全路径为 `包名:remote` 
- 以 ":" 开头的进程属于当前应用的私有进程，其他进程不可以和它跑在同一个进程中
- 不以 ":" 开头的进程属于全局进程，其他应用通过 ShareUID 方式和它跑在同一个进程中。

>Android 会为每个应用分配一个唯一的UID，具有相同UID的应用才能共享数据。
>两个应用有相同的ShareUID和签名才能跑在同一个进程，他们才能共享data 目录、组件信息还有内存数据。

### 2.2 多进程带来的问题
1. 静态成员和单例模式完全失效
2. 线程同步机制完全失效
3. SharedPreferences 的可靠性下降
4. Application 会多次创建

### 2.3 解决 Application 会多次创建的问题

```java
class App:Application() {

    override fun onCreate() {
        super.onCreate()
        val processName = getProcessName(this, android.os.Process.myPid())
        if (!TextUtils.isEmpty(processName) && processName.equals(this.packageName)) {
            Log.e("app","app main") //判断进程名，保证只有主进程运行
        }
    }

    private fun getProcessName(cxt: Context, pid: Int): String? {
        val am = cxt.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningApps = am.runningAppProcesses ?: return null
        for (procInfo in runningApps) {
            if (procInfo.pid == pid) {
                return procInfo.processName
            }
        }
        return null
    }
```







