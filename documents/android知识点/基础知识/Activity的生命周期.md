## 1.A界面启动B界面（standard启动模式）

- onStart、onStop 针对可见性。
- onResume、onPause 针对可交互。

启动A界面
```shell
A: onCreate -> onStart -> onResume
```
A界面打开B界面
```shell
A: onPause

B: onCreate -> onStart -> onResume

A: onStop
```

从B界面返回A界面。

```shell
B: onPause

A: onRestart -> onStart -> onResume

B: onStop -> onDestroy
```
## 2.A界面启动B界面(透明，Theme 为 Dialog)

A界面启动透明主题的B界面，**不会调用A界面 onStop() 方法**。

```shell
A界面: onPause

B界面: onCreate -> onStart -> onResume
```

从B界面返回A界面，**不会调用A界面的 onRestart() 和 onStart()**。

```shell
B界面: onPause

A界面: onResume

B界面: onStop -> onDestroy
```
## 3.弹出 Dialog 对生命周期有什么影响

我们知道，生命周期回调都是 AMS 通过 Binder 通知应用进程调用的；而弹出 Dialog、Toast、PopupWindow 本质上都直接是通过 WindowManager.addView() 显示的（没有经过 AMS），所以不会对生命周期有任何影响。

如果是启动一个 Theme 为 Dialog 的 Activity , 则生命周期为： A.onPause -> B.onCreate -> B.onStart -> B.onResume 注意这边没有前一个 Activity 不会回调 onStop，因为只有在 Activity 切到后台不可见才会回调 onStop；而弹出 Dialog 主题的 Activity 时前一个页面还是可见的，只是失去了焦点而已所以仅有 onPause 回调。

## 4.onActivityResult 调用时机
onActivityResult 方法的注释：You will receive this call immediately before onResume() when your activity is re-starting. 跟一下代码（TransactionExecutor.execute 有兴趣的可以自己打断点跟一下），会发现 onActivityResult 回调先于该 Activity 的所有生命周期回调，从 B Activity 返回 A Activity 的生命周期调用为： B.onPause -> A.onActivityResult -> A.onRestart -> A.onStart -> A.onResume



