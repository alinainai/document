### 一、path转file
```java
File file = new File(path);
```
### 二、path转uri
```java
Uri uri = Uri.parse(path);  
```
### 三、uri转path
```java
    /**
     * 将URI路径转化为path路径
     */
    public static String getRealPathFromURI(Context context,Uri contentURI) {
        String result;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(contentURI, null, null, null, null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }
```
### 四、uri转file
```java
                    File file = null;   //图片地址
                    try {
                        file = new File(new URI(uri.toString()));
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
```
### 五、file转uri
```java
   private static Uri getUriForFile(Context context, File file) {
        if (context == null || file == null) {
            throw new NullPointerException();
        }
        Uri uri;
        if (Build.VERSION.SDK_INT >= 24) {
            uri = FileProvider.getUriForFile(context.getApplicationContext(), context.getPackageName()+".fileProvider", file);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }
```
### 六、file转path
```java
String path = file.getPath()；
```
