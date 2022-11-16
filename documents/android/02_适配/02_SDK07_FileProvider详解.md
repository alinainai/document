## 一、使用背景

Android 7.0 之前，文件的 `Uri` 以 `file:///` 形式提供给其他app访问。Android 7.0 之后，分享文件的 `Uri` 发生了变化。为了安全起见，`file:///` 形式的Uri不能正常访问。

官方提供了 `FileProvider`，`FileProvider` 生成的 `Uri` 会以 `content://` 的形式分享给其他 `app` 使用。

`content` 形式的 `Uri` 可以让其他 `app` 临时获得读取(Read)和写入(Write)权限，只要我们在创建 `Intent` 时，使用 `Intent.setFlags()` 添加权限。

只要接收 `Uri` 的 app 在接收的 `Activity` 任务栈中处于活动状态，添加的权限就会一直有效，直到 app 被任务栈移除。


## 二、代码 

1、在 `manifest` 的 `application` 标签中添加

```html
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="包名.fileprovider"
    android:exported="false" <!--是否到处 -->
    android:grantUriPermissions="true"> <!--是否可以设置权限 -->
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

2、resource文件下面新建 xml 文件夹，新建 filepaths 文件。

```html
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <!--物理路径相当于Context.getFilesDir() + /path/。-->
    <!--<files-path name="name" path="path" />-->

    <!--物理路径相当于Context.getCacheDir() + /path/。-->
    <!--<cache-path name="name" path="path" />-->

    <!--物理路径相当于Environment.getExternalStorageDirectory() + /path/。-->
    <!--<external-path name="name" path="path" />-->

    <!--物理路径相当于Context.getExternalFilesDir(String) + /path/。-->
    <!--<external-files-path name="name" path="path" />-->

    <!--物理路径相当于Context.getExternalCacheDir() + /path/。-->
    <!--<external-cache-path name="name" path="path" />-->
      
    <cache-path  name="imgs_dir"  path="imgs" />
<!-- <files-path  name="logs_dir"  path="logs" /> -->
       
    <!-- 可以在path中用.代替所有目录 -->
    <files-path  name="files"   path="." />

</paths>
```

## 三、使用

3.1 file 目录

```koltin
val logsDir = File(mContext.filesDir, "logs")
if (!logsDir.exists()) {
    logsDir.mkdirs()
}
val logFile = File(logsDir, "native.log")
if (!logFile.exists()) {
    logFile.createNewFile()
}
val logUri: Uri = FileProvider.getUriForFile(
    mContext,
    BuildConfig.APPLICATION_ID + ".fileprovider", logFile
)
Log.e("storage", logUri.toString())
```

3.2 cache 目录

```koltin
val imgsDir = File(mContext.cacheDir, "imgs")
if (!imgsDir.exists()) {
    imgsDir.mkdirs()
}
val headImg = File(imgsDir, "head.png")
if (!headImg.exists()) {
    headImg.createNewFile()
}
val imgUri: Uri = FileProvider.getUriForFile(
    mContext,
    BuildConfig.APPLICATION_ID + ".fileprovider", headImg
)
Log.e("storage", imgUri.toString())
```

3.3 使用

```koltin
Intent intent = new Intent(Intent.ACTION_SEND);
intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
// 邮箱分享文件
intent.putExtra(Intent.EXTRA_STREAM, contentUri);
// Intent.setDate或Intent.setClipData()
inetnt.setClipData.newRawUri("", contentUri)
```

## 四、源码分析

FileProvider 是继承自 ContentProvider。
```java
public class FileProvider extends ContentProvider {
```
对于增删改查接口仅支持 query 和 delete、以及 openInputStream 和 openOutputStream。

另外在 attachInfo 方法中检查了 exported 和 grantUriPermissions 属性：
```java
@Override
public void attachInfo(@NonNull Context context, @NonNull ProviderInfo info) {
    super.attachInfo(context, info);
    // Sanity check our security
    if (info.exported) {
        throw new SecurityException("Provider must not be exported");
    }
    if (!info.grantUriPermissions) {
        throw new SecurityException("Provider must grant uri permissions");
    }
    mStrategy = getPathStrategy(context, info.authority);
}
```
说明 FileProvider 不是公开的，不能像普通的 ContentProvider 那样的方式被外界的程序使用。
