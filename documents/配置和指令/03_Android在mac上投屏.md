## 一、需求和场景
由于咱们的开发模式是远程对接，所以有的时候需要给产品演示一些咱们APP中的一些功能。直接使用视频通话拍摄很不方便，并且Android的模拟器不能运行咱们的项目。
为了方便演示效果，我们可以安装一个专门用来进行投屏的软件 'scrcpy'。

## 二、安装流程

我们使用的是 MacOS 系统，可以直接在 'scrcpy' 官网上查看 MacOS 的安装方式。

官方地址：[Genymobile/scrcpy#macos](https://github.com/Genymobile/scrcpy#macos)（可能需要翻墙）

这里贴一下安装步骤:

1、使用 homebrew 安装 scrcpy，如果没有安装 homebrew 先安装一下 homebrew。
```shell
brew install scrcpy
```
2、如果你没用安装 adb 环境，先使用下面的指令安装下 adb，并配置一下环境变量

You need adb, accessible from your PATH. If you don't have it yet:
```shell
brew install android-platform-tools
```
安装很简单，配置好以后直接使用下面的指令就可以开启投屏，当然你的手机要打开`开发者模式`。

```shell
scrcpy
```
## 三、OK，截个图

<img width="500" alt="image" src="https://user-images.githubusercontent.com/17560388/195013219-0e618cda-22bd-462e-acc8-48af73cd539a.png">
