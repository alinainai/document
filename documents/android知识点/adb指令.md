## 查看activity栈

1、获取所用应用
```shell
adb shell dumpsys activity
```
2、获取自己的应用
```shell
adb shell dumpsys activity | grep com.xxx.xxx.xx
```
3、获取处于栈顶的activity
```shell
adb shell dumpsys activity | grep mFocusedActivity
```
