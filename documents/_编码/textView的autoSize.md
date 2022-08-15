```html
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

  <TextView
      android:layout_width="match_parent"
      android:layout_height="200dp"
      app:autoSizeTextType="uniform"
      app:autoSizeMinTextSize="12sp"
      app:autoSizeMaxTextSize="100sp"
      app:autoSizeStepGranularity="2sp" />

</LinearLayout>
```

[设置 TextView 自动调整大小](https://developer.android.com/guide/topics/ui/look-and-feel/autosizing-textview#setting-textview-autosize)
