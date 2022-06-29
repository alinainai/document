## 1.更新相关参数和环境

### 1.1 首先更新 SDK Manager 和相关版本号

更新 SDK 依赖包

<img width="500" alt="image" src="https://user-images.githubusercontent.com/17560388/174991242-e2678462-d79f-4c4a-b3e8-ca4c33dcf9ac.png">

<img width="500" alt="image" src="https://user-images.githubusercontent.com/17560388/174991446-3956de8e-3ef7-4850-839b-2d0eb6623e95.png">

Android-12 的 版本code 为31、32，本次适配直接将 SDK_VERSION 升到 32。build-tools 升级为 32.0.0 。

```shell
TARGET_SDK_VERSION=30->32
COMPILE_SDK_VERSION=30->32
BUILD_TOOLS_VERSION=30.0.2->32.0.0
```
### 1.2 升级 macOS 的 jdk 版本

由于使用指令运行项目时，gradle 依赖的是 macOS 的 JDK，所以我们需要把环境变量升级为 JDK_11 版本。这用使用指令运行时 gradle 的 jdk 依赖就是 11 版本了。

- 先从华为的镜像地址下载 [jdk-11](https://repo.huaweicloud.com/java/jdk/)，我下载的版本是 `Index of java-local/jdk/11.0.2+9`
- 也可以从[官网下载](https://www.oracle.com/java/technologies/downloads/#java11)

下载之后直接按照步骤安装，会安装到 `/Library/Java/JavaVirtualMachines/jdk-11.0.2.jdk`

然后更新我们的 java_home 环境变量

```shell
open ~/.bash_profile
```
更新 .bash_profile 文件中的 JAVA_HOME
```shell
export JAVA_8_JDK=/Library/Java/JavaVirtualMachines/jdk1.8.0_311.jdk/Contents/Home
export JAVA_11_JDK=/Library/Java/JavaVirtualMachines/jdk-11.0.2.jdk/Contents/Home
export JAVA_HOME=$JAVA_11_JDK
```
刷新 .bash_profile
```shell
source ~/.bash_profile
```
查看一下 java 版本

```shell
java -version
```

~ok，基础操作完成，试着运行一下你的项目

**讨论点：这个要在确定一下，要不要 回退到 31 版本，Android12L 和 Android12 也是有区别的**

## 2.适配代码

可以先看下官网的行为变更：[Android12:行为变更：所有应用](https://developer.android.google.cn/about/versions/12/behavior-changes-all)

还可以参看下面几个文章：

[Android 12 保姆级适配指南](https://juejin.cn/post/7093787313095999502#heading-9)

咱们项目目前需要适配主要是下面几个

- 1.三方库升级: Okhttp、Retrofit、Rxjava(暂时放弃)、系统库
- 2.三方服务的适配追踪: 客服、Firebase、神策
- 3.新版应用启动画面
- 4.Manifest 适配 exported 属性
- 5.过时的API处理

上面几个方面可以细讲一下

### 2.1 三方库升级: Okhttp、Retrofit、Rxjava(暂时放弃)、系统库

讨论下升级的必要性

### 2.2 三方服务的适配追踪: 客服、Firebase、神策


客服：目前一对客服的aar包进行了 exported 适配，可以在找下官方查看版本适配

Firebase：已升级新包

神策：找客服对接一下。

哆啦A梦：已升级为最新版本

### 2.3 新版应用启动画面

目前已经和雪花沟通，等待雪花最终的UI效果

我的意思：

分 12 和 12 以前的版本分别适配。主要方法：建立 values-v31 theme 主题分别适配

### 2.4 Manifest 适配 exported 属性

针对app内和第三方的 aar manifest 中有 intent-filter 的 Activity/Service/BroadcastReceiver 要设置 exported 属性。

目前已做处理，处理方式如下：
- 针对 app 内根据组件的使用情况设置 exported 属性，需要被外部调用的设置为 true，不需要被外部调用的设置为 false
- 针对三方的 aar 可以在 app 的 manifest 使用merge属性再声明一次组件，并设置exported 属性，需要被外部调用的设置为 true，不需要被外部调用的设置为 false

### 2.5 过时的API处理

可以先不处理，过时不代表去掉

## 3.测试重点

1. 有些图片上传的页面要过一下，找一个主要界面过一下流程不崩溃即可，内部逻辑都是一样的。
2. 主要流程要测试一下，保证app的稳定性。


