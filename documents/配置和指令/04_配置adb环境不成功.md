#### 14.MacOS 下 adb 指令无法识别
 首先确保adb环境变量已经配置
 - 1.打开终端输入命令行输入 open .zshrc 回车。
- 2.打开的文件中找到 # User configuration 部分，紧贴其后添加 source ~/.bash_profile。
- 3.点击文件的完成，命令行再输入source .zshrc 回车。
- 4.在执行adb命令成功解决。
#### 15.MacOS 下 拷贝android数据库到电脑
 - 1.插入设备，然后从命令行或终端运行 adb shell。
 - 2.在 shell 中 run-as com.myapplication.packagname（你想要拷贝的包名）。
 - 3.然后 CD 进入数据库文件夹 cd databases。
 - 4.然后运行 cat my_datbase_name.db（数据库名） > /sdcard/my_database_name_temp.db（拷贝的数据库名）。
 - 5.运行exit ，然后再次 exit 以返回正常的终端提示符。
 - 6.cd 到指定目录，运行 adb pull /sdcard/my_database_name_temb.db ，OK！
