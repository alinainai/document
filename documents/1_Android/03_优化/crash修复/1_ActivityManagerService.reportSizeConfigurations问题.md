## ActivityManagerService.reportSizeConfigurations异常问题规避方案

在crash平台上上报了以下crash:

```shell
android.os.RemoteException: Remote stack trace:
com.android.server.am.ActivityManagerService.reportSizeConfigurations(ActivityManagerService.java:9443)
android.app.IActivityManager$Stub.onTransact(IActivityManager.java:2837)
com.android.server.am.ActivityManagerService.onTransact(ActivityManagerService.java:3654)
com.android.server.am.HwActivityManagerService.onTransact(HwActivityManagerService.java:609)
android.os.Binder.execTransact(Binder.java:739)
java.lang.IllegalArgumentException:reportSizeConfigurations: ActivityRecord not found for: Token{f7b56df ActivityRecord{a06a17e u0 com.tencent.qqlive/.ona.activity.SplashVideoDetailActivity t-1 f}}
android.os.Parcel.createException(Parcel.java:1957)
......

android.os.RemoteException:Remote stack trace:
com.android.server.am.ActivityManagerService.reportSizeConfigurations(ActivityManagerService.java:9443)
android.app.IActivityManager$Stub.onTransact(IActivityManager.java:2837)
com.android.server.am.ActivityManagerService.onTransact(ActivityManagerService.java:3654)
com.android.server.am.HwActivityManagerService.onTransact(HwActivityManagerService.java:609)
android.os.Binder.execTransact(Binder.java:739)
并且看crash机型都是出现了Android 9.0手机上。因此，我们就从Android 9.0源码下手开始分析：
```
## 分析过程
找到ActivityManagerService.reportSizeConfigurations方法源码：

```java
@Override
public void reportSizeConfigurations(IBinder token, int[] horizontalSizeConfiguration,
        int[] verticalSizeConfigurations, int[] smallestSizeConfigurations) {
    if (DEBUG_CONFIGURATION) Slog.v(TAG, "Report configuration: " + token + " "
            + horizontalSizeConfiguration + " " + verticalSizeConfigurations);
    synchronized (this) {
        ActivityRecord record = ActivityRecord.isInStackLocked(token);
        if (record == null) {
            throw new IllegalArgumentException("reportSizeConfigurations: ActivityRecord not "
                    + "found for: " + token);
        }
        record.setSizeConfigurations(horizontalSizeConfiguration,
                verticalSizeConfigurations, smallestSizeConfigurations);
    }
}
```
结合crash堆栈：

```shell
java.lang.IllegalArgumentException:reportSizeConfigurations: ActivityRecord not found for: Token{f7b56df ActivityRecord{a06a17e u0 com.tencent.qqlive/.ona.activity.SplashVideoDetailActivity t-1 f}}
可以看出应该是从通过token从Activity堆栈里面找不到了ActivityRecord，导致抛出了crash，并且看了下只有9.0源码里面有这一段代码，其他版本没有这个代码。
```

既然是从堆栈里面找不到ActivityRecord，那说明Activity被finish掉了，导致从栈里面移除掉了？

由于Activity相关操作都是跨进程的，ActivityManagerService对应的Client为ActivityManager,找到Client端调用reportSizeConfigurations方法的地方，在ActivityThread的reportSizeConfigurations方法里面：

```java
private void reportSizeConfigurations(ActivityClientRecord r) {
    Configuration[] configurations = r.activity.getResources().getSizeConfigurations();
    if (configurations == null) {
        return;
    }
    SparseIntArray horizontal = new SparseIntArray();
    SparseIntArray vertical = new SparseIntArray();
    SparseIntArray smallest = new SparseIntArray();
    for (int i = configurations.length - 1; i >= 0; i--) {
        Configuration config = configurations[i];
        if (config.screenHeightDp != Configuration.SCREEN_HEIGHT_DP_UNDEFINED) {
            vertical.put(config.screenHeightDp, 0);
        }
        if (config.screenWidthDp != Configuration.SCREEN_WIDTH_DP_UNDEFINED) {
            horizontal.put(config.screenWidthDp, 0);
        }
        if (config.smallestScreenWidthDp != Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED) {
            smallest.put(config.smallestScreenWidthDp, 0);
        }
    }
    try {
        ActivityManager.getService().reportSizeConfigurations(r.token,
                horizontal.copyKeys(), vertical.copyKeys(), smallest.copyKeys());
    } catch (RemoteException ex) {
        throw ex.rethrowFromSystemServer();
    }

}
```
然后来看下这个reportSizeConfigurations方法调用的地方:

```java
public Activity handleLaunchActivity(ActivityClientRecord r,
           PendingTransactionActions pendingActions, Intent customIntent) {
     //...........
       final Activity a = performLaunchActivity(r, customIntent);

       if (a != null) {
           r.createdConfig = new Configuration(mConfiguration);
           reportSizeConfigurations(r);
```
可以发现，在ActivityThread的handleLaunchActivity方法里面会先调用performLaunchActivity方法来启动一个Activity，这里面也是涉及到跨进程操作的。如果performLaunchActivity里面跨进程操作执行比较慢，在这个过程中杀死App，然后结合前面的分析，应该就能复现这个crash？

为了验证这个猜想，执行复现路径：找到crash列表中的某一个机型，启动App的时候，马上切后台杀死App。果然就复现了这个crash，crash现象是：杀死app之后过一会app又回自动起来白屏，然后就crash了，看堆栈也可以完全对应上!

## 规避方案

使用Hook方案，针对9.0的机型进行特殊处理：

```java
public class HookActivityManager {

    private static final String TAG = "HookActivityManager";

    public static void hook() {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.P) {
            Log.i(TAG, "hook return, not match version");
            return;
        }
        try {
            Field am = ActivityManager.class.getDeclaredField("IActivityManagerSingleton");
            am.setAccessible(true);
            Object iActivityManagerSingleton = am.get(null);
            if (iActivityManagerSingleton == null) {
                return;
            }

            Class<?> singletonCls = iActivityManagerSingleton.getClass().getSuperclass();
            if (singletonCls == null) {
                return;
            }

            Field instance = singletonCls.getDeclaredField("mInstance");
            instance.setAccessible(true);
            Object iActivityManager = instance.get(iActivityManagerSingleton);

            Class<?> iActivityManagerCls = Class.forName("android.app.IActivityManager");
            Class<?>[] classes = {iActivityManagerCls};
            Object iActivityManageProxy = Proxy.newProxyInstance(
                    iActivityManagerCls.getClassLoader(),
                    classes,
                    new IActivityManagerProxy(iActivityManager));
            instance.set(iActivityManagerSingleton, iActivityManageProxy);
            Log.i(TAG, "hook success!");
        } catch (Exception e) {
            Log.w(TAG, "" + e);
        }
    }

    private static class IActivityManagerProxy implements InvocationHandler {
        private final Object mActivityManager;

        public IActivityManagerProxy(Object iActivityManager) {
            mActivityManager = iActivityManager;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("reportSizeConfigurations".equals(method.getName())) {
                try {
                    Log.w(TAG, "reportSizeConfigurations invoke execute ");
                    return method.invoke(mActivityManager, args);
                } catch (Exception e) {
                    Log.w(TAG, "reportSizeConfigurations exception: " + e.getMessage());
                    return null;
                }
            }
            return method.invoke(mActivityManager, args);
        }
    }
}
```
通过动态代理的方式对这个ActivityManagerService.reportSizeConfigurations方法加上try_catch处理，然后在Application的attachBaseContext方法里面执行hook：

```java
 public void attachBaseContext(Context base) {
       if (ApplicationProcessUtils.getInstance().isMainProcess()) {
           HookActivityManager.hook();
       }
}
```
