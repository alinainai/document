## 1. Android_Q 的分区存储

在Android使用 fuse 文件系统开始，Android针对外置存储支持了独立的沙箱存储空间，一般通过Context.getExternalFilesDir() Api 获取，该空间内的数据为应用独有，并且不需要申请任何权限即可使用。
但是当时并没有限制应用读写非沙箱内的数据。从Android Q开始，出于数据隐私问题，Android 希望禁止应用程序操作非沙箱内的数据。为了过度，Android提供了requestLegacyExternalStorage机制，来帮助应用使用原来的机制继续读写存储卡。
Android 应用程序即使获取了读写存储卡权限也不能读写非沙盒路径下的数据。除非Android 应用程序获得读写存储卡权限的情况下，必须在 AndroidManifest.xml 的 application 标签下声明 requestLegacyExternalStorage=true，才可以访问非沙盒路径下的数据。
targetSdkVersion < 29 的应用程序默认带有requestLegacyExternalStorage=true属性。

## 2. Androd 11 无效的 requestLegacyExternalStorage=true

当targetSdk 升级到 30 之后，requestLegacyExternalStorage=true 无效，所以必须适配。

## 3. 文件迁移的策略

在数据迁移的时候，有个很重要的前提是，app能够访问旧存储模型。我们看看什么情况能访问旧存储模型，得分几种情况讨论：

targetSdkVersion 28的app安装在Android 9（28）的手机上，手机系统升级到Android10或11，app正常访问旧存储模型。这种情况和把targetSDKVersion 28的app安装到Android10或11系统手机上一样的情况。

* target 28在Android 9上，app target升级到30，覆盖安装，旧存储模型访问正常；
* target 28在Android10上，app target升级到30，覆盖安装，旧存储模型访问正常。
* requestLegacyExternalStorage设置成true，在 Android 10上新安装的target 30 app，也可以正常访问旧存储模型。
* target28在Android11上，app target升级到30，覆盖安装，==旧存储模型不能访问了==，需要preserveLegacyExternalStorage设置成true。

怎么进行数据迁移最好呢？targetSDKVersion 28的时候，先大规模的升级一次，此app就包含数据迁移功能，同时共享媒体的方式也按照分区存储模型的规范来，这样不论什么版本系统的用户，都能完成数据迁移，同时进行共享媒体的方式也正确。但是，有部分用户就是不升级我们的app，可是我们app以后也得发版，而且target也得升级，假如有一部分用户没升级，等升级的时候，我们的app的target已经是30了，这些用户的系统如果是小于29的，可以正常迁移，如果这些用户的系统版本是29或者30，target30的app在29的系统上正常迁移，target30的app在30系统上，preserveLegacyExternalStorage设置成true，正常迁移。

所以我们的数据迁移方案就是，做好数据迁移功能和共享媒体功能，requestLegacyExternalStorage 和 preserveLegacyExternalStorage 都设置成 true，target 升级不升级都没问题。不过前提是compileSdkVersion 得是 30

## 4. 实战

在 8.0 及以上的系统，采用 Files.move 进行数据迁移，8.0 以下的系统采用 File.rename 进行数据迁移。Files 的 move 方法既可以作用于文件也可以作用于文件夹。

我们项目中需要 move 的是文件夹，首先看看对move文件夹的定义：Empty directories can be moved. If the directory is not empty, the move is allowed when the directory can be moved without moving the contents of that directory. On UNIX systems, moving a directory within the same partition generally consists of renaming the directory. In that situation, this method works even when the directory contains files. 从定义中，我们知道在UNIX系统（linux源自UNIX）上同一个partition上，即便被move的文件夹中有内容，也是可以 move 的，实际就是重命名了一下。

我们的需求：在分区存储模型下，SD卡的公共区域是禁止app使用的，为了保证我们app之前下载到SD的视频在分区存储模型下还能被app识别，所以，在app还是采用旧存储模型的时候，我们需要把这些视频迁移到app在 SD卡 的私有目录下。这两个目录都在SD卡上，属于同一个partition。说明一下，targetSDKVersion 29 或 30 的 app 在 Android 10和 Android 11上，也是有办法让app采用旧存储模型的；

### 4.1 共享数据迁移 

把之前保存的需要分享的视频从app自建的目录迁移到分区存储模型下 app 也能访问到 Movies 目录，这样做的目的是在分区存储模型下，自己和别的app还是访问到这些视频。

从/storage/emulated/0/shvdownload/video/VideoGallery 迁移到 /storage/emulated/0/Movies/SHVideo

VideoGallery目录中有文件，SHVideo目录不存在，move可以成功。app在分区存储模型下，在任何版本系统上上述迁移都正常。

### 4.2 私有数据迁移

从 /storage/emulated/0/xxx/data 迁移到 /storage/emulated/0/Android/data/包名/files/data

xxx/data 目录中有文件，files/data目录不存在，在Android 10及以下的系统上，可以move成功；
在Android 11的系统上 ，move失败了，报 DirectoryNotEmptyException。== 猜测可能是Android 11对 Android/data 目录有了限制吧！如果，在Android 11上还需要进行这种迁移的话，可以采用遍历文件夹输入输出流拷贝的方式。

```shell
java.nio.file.DirectoryNotEmptyException: /storage/emulated/0/xxx/data
 at sun.nio.fs.UnixCopyFile.move(UnixCopyFile.java:498)
 at sun.nio.fs.UnixFileSystemProvider.move(UnixFileSystemProvider.java:262)
 at java.nio.file.Files.move(Files.java:1395)
 at com.xxx.sdk.android.storage.SHDataMigrateUtil.moveData(SHDataMigrateUtil.java:257)
    ...
```

File.move 文件夹的时候，如果目标文件夹存在，那么会报java.nio.file.FileAlreadyExistsException异常

```java
private boolean moveData(File source, File target) {
        long start = System.currentTimeMillis();
        // 只有目标文件夹不存在的时候，move文件夹才能成功
        if (target.exists() && target.isDirectory() && (target.list() == null || target.list().length == 0)) {
            target.delete();
        }
        boolean isSuccess;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Path sourceP = source.toPath();
            Path targetP = target.toPath();
 
            if (target.exists()) {
                isSuccess = copyDir(source, target);
                LogUtils.i(TAG, "moveData copyDir");
            } else {
                try {
                    Files.move(sourceP, targetP);
                    isSuccess = true;
                    LogUtils.i(TAG, "moveData Files.move");
                } catch (IOException e) {
                    e.printStackTrace();
                    LogUtils.i(TAG, Log.getStackTraceString(e));
                    //在Android11上，move ATOMIC_MOVE会报AtomicMoveNotSupportedException异常
                    //在Android11上，move REPLACE_EXISTING会报DirectoryNotEmptyException异常
                    isSuccess = copyDir(source, target);
                    LogUtils.i(TAG, "moveData move fail, use copyDir");
                }
            }
        } else {
            if (target.exists()) {
                isSuccess = copyDir(source, target);
                LogUtils.i(TAG, "moveData copyDir");
            } else {
                isSuccess = source.renameTo(target);
                LogUtils.i(TAG, "moveData renameTo result " + isSuccess);
            }
        }
        long end = System.currentTimeMillis();
        long val = end - start;
        LogUtils.i(TAG, "moveData migrate data take time " + val +" from " + source.getAbsolutePath() + " to " + target.getAbsolutePath());
 
        return isSuccess;
    } 
```
## 5. requestLegacyExternalStorage和preserveLegacyExternalStorage的理解

requestLegacyExternalStorage是 Android10 引入的，preserveLegacyExternalStorage 是 Android11 引入的。

如果你已经适配Android 10，如果应用通过升级安装，那么还会使用以前的储存模式（Legacy View），只有通过首次安装或是卸载重新安装才能启用新模式（Filtered View）。经过测试，确实是这样，我们在Android10的手机上安装了一个 targetSDKVersion 是 27 的app，旧的存储模型是可以正常使用的，然后覆盖安装了 target 是 29 的新包，旧存储模型也是可以访问的，但是，卸载重新安装旧存储模型就不能访问了。requestLegacyExternalStorage 让 targetSDKVersion 是 29（适配了Android 10）的 app 新安装在 Android 10 系统上也继续访问旧的存储模型。

==如果某个应用在安装时启用了传统外部存储，则该应用会保持此模式，直到卸载为止。无论设备后续是否升级为搭载 Android 10 或更高版本，或者应用后续是否更新为以 Android 10 或更高版本为目标平台，此兼容性行为均适用。==

这句话是有些问题的，估计当时说这话的时候，是Android10的时候。在Android11中引入了preserveLegacyExternalStorage，看下面的解释

按照文档说targetSDKVersion<29时，requestLegacyExternalStorage默认是true的，也就是说这些app是采用旧的存储模型运行的，targetSDKVersion升级到29后，requestLegacyExternalStorage默认是false的，但是覆盖安装的，还是采用旧的存储模式运行。重新安装的，由于requestLegacyExternalStorage是false，就采用分区存储模式运行了，除非requestLegacyExternalStorage显示设置成true。

也就是说requestLegacyExternalStorage给了app，在Android 10的系统上，无论是覆盖安装还是重新安装都能使用旧存储模式的机会。

targetSDKVersion升级到30后，在Android 11设备上，requestLegacyExternalStorage会被忽略掉，在Android 10的系统上requestLegacyExternalStorage依旧有效。preserveLegacyExternalStorage 只是让覆盖安装的app能继续使用旧的存储模型，如果之前是旧的存储模型的话。如果您使用 preserveLegacyExternalStorage，旧版存储模型只在用户卸载您的应用之前保持有效。如果用户在搭载 Android 11 的设备上安装或重新安装您的应用，那么无论 preserveLegacyExternalStorage 的值是什么，您的应用都无法停用分区存储模型。

app targetSDKVersion 适配到 30，在 Android 11 的系统上首次安装，是没有任何机会让app能继续使用旧存储模型的。



## 参考

[Android 10适配要点，作用域存储](https://guolin.blog.csdn.net/article/details/105419420)

[Android 10 中的隐私权变更](https://developer.android.com/about/versions/10/privacy/changes?hl=zh-cn#scoped-storage)

[Android 11 中的存储机制更新](https://developer.android.com/about/versions/11/privacy/storage)

[数据和文件存储概览](https://developer.android.com/training/data-storage?hl=zh-cn)

[RELATIVE_PATH的使用](https://developer.android.com/reference/android/provider/MediaStore.MediaColumns#RELATIVE_PATH)

[携程Android 10适配踩坑指南](https://zhuanlan.zhihu.com/p/128558892)

[Android使用MediaStore获取手机上的文件](https://blog.csdn.net/yann02/article/details/92844364)

[AndroidQ(10)分区存储完美适配](https://www.jianshu.com/p/271bbd13bfcf)

[Moving a File or Directory](https://docs.oracle.com/javase/tutorial/essential/io/move.html)

[Android 10 适配攻略](https://juejin.im/post/6844904073024503822)

[Android 11 (R) 分区存储](http://shoewann0402.github.io/2020/03/17/android-R-scoped-storage/)

[Android MediaStore Api 使用](https://ppting.me/2020/04/19/2020_04_19_how_to_use_Android_MediaStore_Api/)
 
 

 
 


