问题：
```shell
Could not resolve all files for cCould not resolve all files for configuration ':app:androidApis'onfiguration ':app:androidApis'
```
解决：
查看一下 sdk/plateforms/android-xx 中是否有 android.jar，如果没有需要通过 SDKManager 重新安装一下对应版本
