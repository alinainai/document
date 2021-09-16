### Bitmap 占用内存分析

#### getAllocationByteCount 探索

<img src="https://user-images.githubusercontent.com/17560388/132435360-0ed6c521-c52e-44a1-a716-69706c251338.png" alt="图片替换文本" width="600"  align="bottom" />

上图中 rodman 是保存在 res/drawable-xhdpi 目录下的一张 600*600，大小为 65Kb 的图片。打印结果如下：

>I/Bitmap  ( 5673): bitmap size is 1440000  
#### 分析
默认情况下 BitmapFactory 使用 Bitmap.Config.ARGB_8888 的存储方式来加载图片内容，而在这种存储模式下，每一个像素需要占用 4 个字节。因此上面图片 rodman 的内存大小可以使用如下公式来计算：
>宽 * 高 * 4 = 600 * 600 * 4 = 1440000

#### 屏幕自适应

将图片 rodman 移动到（注意是移动，不是拷贝）res/drawable-hdpi 目录下，重新运行代码，则打印日志如下：

>I/Bitmap  ( 6047): bitmap size is 2560000

可以看出我们只是移动了图片的位置，Bitmap 所占用的空间竟然上涨了 77%。这是为什么呢？

实际上 BitmapFactory 在解析图片的过程中，会根据当前设备屏幕密度和图片所在的 drawable 目录来做一个对比，根据这个对比值进行缩放操作。具体公式为如下所示：

- 1.缩放比例 scale = 当前设备屏幕密度 / 图片所在 drawable 目录对应屏幕密度
- 2.Bitmap 实际大小 = 宽 * scale * 高 * scale * Config 对应存储像素数

在 Android 中，各个 drawable 目录对应的屏幕密度分别为下：

![image](https://user-images.githubusercontent.com/17560388/132436812-dc00a56b-e534-4f81-9f94-38d551204889.png)

我运行的设备是 Nexus 4，屏幕密度为 320。如果将 rodman 放到 drawable-hdpi 目录下，最终的计算公式如下：

rodman 实际占用内存大小 = 600 * (320 / 240) * 600 * (320 / 240) * 4 = 2560000

#### assets 中的图片大小

加载 assets 目录中的图片，系统并不会对其进行缩放操作

#### Bitmap 加载优化

```java
BitmapOptions options = new BitmapOptions();
options.inPreferredConfig = Bitmap.Config.RGB_565; //一个像素两字节
options.nSampleSize = 2;//宽高维度上每隔 inSampleSize 个像素进行一次采集
Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ic_laucher,options);
```
#### BitmapRegionDecoder 基本使用

大图加载

![image](https://user-images.githubusercontent.com/17560388/132437819-2c3bed7b-876a-43bd-b752-699941d4b51a.png)


