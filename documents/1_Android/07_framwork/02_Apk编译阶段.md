
## 一、编译阶段 

编译阶段是指我们的 android 项目从`源码`编译开始到生成`.apk`的过程

### 1.1 资源文件

资源文件包括项目中 `res` 目录下的各种 `XML` 文件、动画、`drawable` 图片、音视频等。`AAPT` 负责编译这些资源文件，其中

- `XML`文件被编译成二进制文件，所以解压 apk 之后无法直接打开 XML 文件。
- `assets 和 raw` 目录下的资源并不会被编译，会被原封不动的打包到 apk 压缩包中。

资源文件编译之后的产物包括两部分：

- resources.arsc：资源索引表（Map结构，key：R.java 中的资源 ID；value ：对应的资源所在路径）。
- R.java：各个资源 ID 常量（用 4 字节的无符号整数表示，其中高 1 字节表示 `Package ID`，次高 1 个字节表示 `Type ID`，最低 2 字节表示 `Entry ID`）。

<img src="https://user-images.githubusercontent.com/17560388/123922090-45048280-d9ba-11eb-96c5-8950ffe21730.png" alt="图片替换文本" width="600"  align="bottom" />

### 1.2 源码

源码首先会通过 javac 编译为 `class` 字节码文件，然后这些 `class` 文件连同依赖的三方库中的 `class` 文件一同被 `dx` 工具优化为 `dex` 文件。也包括 `AIDL` 接口文件编译之后生成的 `.java` 文件

如果有分包，那么也可能会生成多个 `dex` 文件。

<img src="https://user-images.githubusercontent.com/17560388/123921565-b98af180-d9b9-11eb-812a-7ed0bacff8cc.png" alt="图片替换文本" width="400"  align="bottom" />

## 二、打包阶段

使用 `APK Builder` 工具将经过编译之后的 `resource` 和 `dex` 文件一起打包到 `apk` 中，实际上被打包到 apk 中的还有一些其他资源，比如 `AndroidManifest.xml` 和三方库中使用的动态库 `.so` 文件。  
apk 创建好之后，还不能直接使用。需要使用工具 `jarsigner` 对其进行签名，因为 `Android` 系统不会安装没有进行签名的程序。签名之后会生成` META_INF` 文件夹，此文件夹中保存着跟签名相关的各个文件。

- CERT.SF：生成每个文件相对的密钥
- MANIFEST.MF：数字签名信息
- xxx.SF：这是 JAR 文件的签名文件
- xxx.DSA：对输出文件的签名和公钥。

PMS 在安装过程中会检查 apk 中的签名证书的合法性，具体内容在讲解安装时介绍。

**资源优化**

就是使用工具 `zipalign` 对 `apk` 中的未压缩资源（图片、视频等）进行对齐操作，让资源按照 4 字节的边界进行对齐。

这种思想同 Java 对象内存布局中的对齐空间非常类似，主要是为了加快资源的访问速度。如果每个资源的开始位置都是上一个资源之后的 4n 字节，那么访问下一个资源就不用遍历，直接跳到 4n 字节处判断是不是一个新的资源即可。

至此一个完整的 apk 安装包就创建成功，一个完整的 apk 解压缩之后的内容如下所示：

<img src="https://user-images.githubusercontent.com/17560388/124059705-1b9d3280-da5e-11eb-874d-151b900b8daa.png" alt="图片替换文本" width="300"  align="bottom" />

## 三、编译打包流程图

整个编译打包流程可以用下图来描述：

<img src="https://user-images.githubusercontent.com/17560388/186102996-7276222f-784f-41dc-b9e1-ff548b97f285.png" alt="编译打包流程图" width="400"  align="bottom" />


## 参考
- [从构建工具看 Android APK 编译打包流程](https://cloud.tencent.com/developer/article/1814090)





