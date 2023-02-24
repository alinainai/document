
1、查看所用应用的activity栈
```shell
adb shell dumpsys activity
```
2、查看自己的应用的activity栈
```shell
adb shell dumpsys activity | grep com.xxx.xxx.xx
```
3、查看处于栈顶的activity
```shell
adb shell dumpsys activity | grep mFocusedActivity
```
4、打印log日志
```shell
adb logcat -e time > /Users/lijiaxing/Downloads/log.txt
```
5、查看应用进程
```shell
adb shell ps | grep 包名
```
6、查看当前连接设备或者虚拟机的所有包
```shell
adb shell pm list packages   
```
