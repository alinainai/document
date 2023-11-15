带样式的 Container
```dart
Container(
      margin: EdgeInsets.only(left: 40, top: 40),
      //设置child居中 Alignment.center
      alignment: Alignment.center,
      height: 60,
      width: 300,
      decoration: BoxDecoration(
        color: Colors.cyanAccent, //设置背景颜色
        borderRadius: BorderRadius.all(Radius.circular(10.0)), //设置Container圆角
        border: Border.all(width: 2, color: Colors.yellowAccent),//设置Container边框
      ),
      child: Text("圆角边框的Container"),
    )
```
