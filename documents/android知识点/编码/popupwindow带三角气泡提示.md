#### 如图白色弹窗

<img width="300" alt="类图" src="https://user-images.githubusercontent.com/17560388/147737941-b923619c-5cac-4fc4-a46f-9dbfaea6bb91.png">

#### 展示代码

```java
private PopupWindow mHideAssetPopup;
private void showHideAssetPopupTip(){
    if(mHideAssetPopup==null){
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_popup_hide_asset_tip,null);//PopupWindow对象
        mHideAssetPopup=new PopupWindow(getActivity());//初始化PopupWindow对象
        mHideAssetPopup.setContentView(view);//设置PopupWindow布局文件
        mHideAssetPopup.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);//设置PopupWindow宽
        mHideAssetPopup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);//设置PopupWindow高
        mHideAssetPopup.setBackgroundDrawable(new ColorDrawable(0x00000000));
        mHideAssetPopup.setOutsideTouchable(true);//点击外部布局取消弹窗
        mHideAssetPopup.setFocusable(true); //设置焦点，让外部布局不响应点击事件
        view.findViewById(R.id.guide_index_notice_tv).setOnClickListener(v -> mHideAssetPopup.dismiss());
    }
    mHideAssetPopup.showAsDropDown(imgAssetHideTip,-151,-49);
}
```

#### 布局代码
```html
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:orientation="vertical">

    <ImageView
        android:layout_marginStart="60dp"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:src="@drawable/shape_indicator_index_notice_guide" />

    <TextView
        android:id="@+id/guide_index_notice_tv"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center_horizontal"
        android:background="@drawable/bg_index_top_scroll_view"
        android:gravity="center"
        android:maxWidth="@dimen/dimen_279"
        android:padding="@dimen/dimen_12"
        android:textColor="@color/guide_text_color"
        android:textSize="@dimen/global_text_size_12"
        android:textStyle="bold"
        android:text="@string/str_hide_asset_popup_tip"/>

</LinearLayout>
```

#### 正三角
```html
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <rotate
            android:fromDegrees="45"
            android:pivotX="-45%"
            android:pivotY="90%">
            <shape android:shape="rectangle">
                <size
                    android:width="16dp"
                    android:height="16dp" />
                <solid android:color="@color/index_top_scroll_view" />
            </shape>
        </rotate>
    </item>
</layer-list>
