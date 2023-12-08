1、通过brew安装ruby
```shell
brew install ruby
```
查看ruby位置
```shell
which ruby
```
优先会显示系统ruby位置，这个是 MacOS 自带的，一般版本是 2.x

2、修改 ruby 全局环境变量配置，在 .bash_profile 改为使用 brew 下载的 ruby。
```shell
export PATH="/usr/local/opt/ruby/bin:/usr/local/lib/ruby/gems/3.2.2_1/bin:$PATH"
```
可以通过 brew list ruby 查看安装版本

3、通过 ruby 安装 cocospods
```shell
sudo gem install -n /usr/local/bin cocoapods
```
查看 cocospods 版本
```shell
pod --version
```

