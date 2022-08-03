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
## 2.A界面启动一个主题为Dialog的B界面

A界面启动主题为Dialog(也是我们常说的透明主题)的B界面，**B界面启动后不会回调A界面 onStop() 方法**。

```shell
A界面: onPause

B界面启动: onCreate -> onStart -> onResume
```

从`B界面`返回`A界面`，**不会调用`A界面`的 `onRestart()` 和 `onStart()`**。

```shell
B界面: onPause

A界面: onResume

B界面: onStop -> onDestroy
```
弹出 Dialog 主题的 Activity 时前一个页面还是可见的，只是失去了焦点而已所以仅有 onPause 回调。

## 3.弹出 Dialog 对生命周期有什么影响

我们知道，生命周期回调都是 AMS 通过 Binder 通知应用进程调用的；而弹出 Dialog、Toast、PopupWindow 本质上都直接是通过 WindowManager.addView() 显示的（没有经过 AMS），所以不会对生命周期有任何影响。


## 4.onActivityResult 调用时机
onActivityResult 方法的注释：You will receive this call immediately before onResume() when your activity is re-starting. 跟一下代码（TransactionExecutor.execute 有兴趣的可以自己打断点跟一下），会发现 onActivityResult 回调先于该 Activity 的所有生命周期回调，从 B Activity 返回 A Activity 的生命周期调用为： B.onPause -> A.onActivityResult -> A.onRestart -> A.onStart -> A.onResume



