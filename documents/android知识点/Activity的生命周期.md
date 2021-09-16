### MainActivity 启动 SecondActivity

启动 MainActivity。

onStart、onStop 针对可见性。
onResume、onPause 针对可交互。

```shell
04-14 23:34:52.692 22963-22963/com.burjal.performancetest I/MainActivity: ====onCreate=====
04-14 23:34:52.692 22963-22963/com.burjal.performancetest I/MainActivity: ====onStart=====
04-14 23:34:52.692 22963-22963/com.burjal.performancetest I/MainActivity: ====onResume=====
```

从 MainActivity 打开 SecondActivity。

```shell
04-14 23:34:57.532 22963-22963/com.burjal.performancetest I/MainActivity: ====onPause=====
04-14 23:34:57.562 22963-22963/com.burjal.performancetest I/SecondActivity: ====onCreate=====
04-14 23:34:57.562 22963-22963/com.burjal.performancetest I/SecondActivity: ====onStart=====
04-14 23:34:57.562 22963-22963/com.burjal.performancetest I/SecondActivity: ====onResume=====
04-14 23:34:57.972 22963-22963/com.burjal.performancetest I/MainActivity: =====onStop====
```

从 SecondActivity 返回 MainActivity。

```shell
04-14 23:48:10.692 22963-22963/com.burjal.performancetest I/SecondActivity: ====onPause=====
04-14 23:48:10.692 22963-22963/com.burjal.performancetest I/MainActivity: ====onRestart=====
04-14 23:48:10.692 22963-22963/com.burjal.performancetest I/MainActivity: ====onStart=====
04-14 23:48:10.692 22963-22963/com.burjal.performancetest I/MainActivity: ====onResume=====
04-14 23:48:11.062 22963-22963/com.burjal.performancetest I/SecondActivity: =====onStop====
04-14 23:48:11.062 22963-22963/com.burjal.performancetest I/SecondActivity: ====onDestroy=====
```
### MainActivity 启动 TransActivity 问题

MainActivity 启动透明主题的 TransActivity，**不会调用 MainActivity onStop() 方法**。

```shell
2021-09-11 14:41:58.447 3360-3360/com.gas.app D/lifecycle: MainActivity_onCreate
2021-09-11 14:41:58.460 3360-3360/com.gas.app D/lifecycle: MainActivity_onStart
2021-09-11 14:41:58.460 3360-3360/com.gas.app D/lifecycle: MainActivity_onResume
2021-09-11 14:42:00.497 3360-3360/com.gas.app D/lifecycle: MainActivity_onPause

2021-09-11 14:42:00.530 3360-3360/com.gas.app D/lifecycle: TransActivity_onCreate
2021-09-11 14:42:00.545 3360-3360/com.gas.app D/lifecycle: TransActivity_onStart
2021-09-11 14:42:00.546 3360-3360/com.gas.app D/lifecycle: TransActivity_onResume
```

从 TransActivity 返回 MainActivity，**不会调用 onRestart() 和 onStart()**。

```shell
2021-09-11 14:42:17.447 3360-3360/com.gas.app D/lifecycle: TransActivity_onPause
2021-09-11 14:42:17.472 3360-3360/com.gas.app D/lifecycle: MainActivity_onResume
2021-09-11 14:42:17.483 3360-3360/com.gas.app D/lifecycle: TransActivity_onStop
2021-09-11 14:42:17.485 3360-3360/com.gas.app D/lifecycle: TransActivity_onDestroy
```
