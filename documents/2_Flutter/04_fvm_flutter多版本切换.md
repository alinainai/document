### 1、安装 homebrew
网址：https://brew.sh/

设置当前账户最高权限
```shell
sudo whoami
```
设置 /opt 路径 有管理员权限
```shell
sudo chmod -R 777 /opt
# -R 是指级联应用到目录里的所有子目录和文件
# 777 是所有用户都拥有最高权限
```
### 2、使用 homebrew 安装 fvm
```shell
brew tap befovy/taps
brew install fvm
```
查看版本
```shell
fvm --version 
```
### 3、通过 bash_file 配置环境变量
```shell
open -e ~/.bash_profile
```
参考内容，其中 android 的环境变量配置过的可以忽略
```shell
export LC_ALL=en_US.UTF-8
export LANG=en_US.UTF-8

# android , 配置过的可以忽略
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer/
export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/platform-tools

# flutter
# 防墙
export FLUTTER_STORAGE_BASE_URL=https://storage.flutter-io.cn
export PUB_HOSTED_URL=https://pub.flutter-io.cn
# fvm 
export FLUTTER_ROOT=/opt/fvm/current
export FVM_HOME=/opt/fvm
# 指定 fvm 包中的 flutter 为默认
export PATH=$PATH:$HOME/.pub-cache/bin
export PATH=$PATH:$FLUTTER_ROOT/bin/cache/dart-sdk/bin
export PATH=$PATH:$FLUTTER_ROOT/bin
```
### 4、安装多个flutter 版本
```shell
fvm install 1.22.6
fvm install 3.6.0
```
### 5、设置 flutter 版本
查看 flutter release 版本 https://flutter.cn/docs/development/tools/sdk/releases

```shell
fvm use 1.22.6
fvm current # 查看当前版本
flutter --version #查看flutter版本，借助该指令更新 DART SDK
```
### 6、配置AS
打开 `Preferences` → `Languages & Frameworks` → `Flutter` 
设置 Flutter SDK path选择路径(/opt/fvm/current)

### 7、其他 fvm 指令
```shell
  current     Show current Flutter SDK info
  flutter     Proxies Flutter Commands
  help        Help about any command
  import      Import installed flutter into fvm
  install     Installs Flutter SDK Version
  list        Lists installed Flutter SDK Version
  remove      Removes Flutter SDK Version
  use         Which Flutter SDK Version you would like to use
```
### 8、配置项目相关内容
https://fvm.app/docs/getting_started/configuration/

