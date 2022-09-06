## 1、前台服务

后台运行的 Service 系统优先级相对较低，当系统内存不足时，后台的Service有可能被回收。
为了保持后台服务的正常运行及相关操作，可以选择将需要保持运行的 Service 设置为前台服务，从而使 APP 长时间处于后台或者关闭（进程未被清理）时，服务能够保持工作。

## 2、具体使用 

在 Manifest 中申请前台服务的权限

```html
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" /> 
```

创建一个 ForegroundService，前台服务运行时必须设置一个和服务绑定的 notification。

```java
public class ForegroundService extends Service {
  
  private static final String TAG = ForegroundService.class.getSimpleName();

 @Override
  public void onCreate() {
    super.onCreate();
    Notification notification = createForegroundNotification(); // 创建通知
    startForeground(NOTIFICATION_ID, notification); // 设置 startForeground 相关参数
    Log.e(TAG, "onCreate");
  }
  
  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    Log.e(TAG, "onBind");
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.e(TAG, "onStartCommand");
    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public void onDestroy() {
    Log.e(TAG, "onDestroy");
    stopForeground(true); // 移除前台设置
    super.onDestroy();
  }  
}
```

通知相关的方法
```java
private Notification createForegroundNotification() {
  NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

  // 唯一的通知通道的id.
  String notificationChannelId = "notification_channel_id_01";

  // Android8.0以上的系统，新建消息通道
  if (Build.VERSION.SDK_INT  = Build.VERSION_CODES.O) {
    //用户可见的通道名称
    String channelName = "Foreground Service Notification";
    //通道的重要程度
    int importance = NotificationManager.IMPORTANCE_HIGH;
    NotificationChannel notificationChannel = new NotificationChannel(notificationChannelId, channelName, importance);
    notificationChannel.setDescription("Channel description");
    //LED灯
    notificationChannel.enableLights(true);
    notificationChannel.setLightColor(Color.RED);
    //震动
    notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
    notificationChannel.enableVibration(true);
    if (notificationManager != null) {
      notificationManager.createNotificationChannel(notificationChannel);
    }
  }

  NotificationCompat.Builder builder = new NotificationCompat.Builder(this, notificationChannelId);
  //通知小图标
  builder.setSmallIcon(R.drawable.ic_launcher);
  //通知标题
  builder.setContentTitle("ContentTitle");
  //通知内容
  builder.setContentText("ContentText");
  //设定通知显示的时间
  builder.setWhen(System.currentTimeMillis());
  //设定启动的内容
  Intent activityIntent = new Intent(this, NotificationActivity.class);
  PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
  builder.setContentIntent(pendingIntent);

  //创建通知并返回
  return builder.build();
}
```

## 3、服务的启动和停止 

启动服务
```java
if (Build.VERSION.SDK_INT  = Build.VERSION_CODES.O) {
  startForegroundService(mForegroundService);
} else {
  startService(mForegroundService);
}
```
停止服务
```java
mForegroundService = new Intent(this, ForegroundService.class);
stopService(mForegroundService);
```
