ListWiew 嵌套 ListWiew 时，子级 ListWiew 需要如下设置

```dart
ListView(
      children: <Widget>[
          ListView(
                shrinkWrap: true, //为true可以解决子控件必须设置高度的问题，ListView 会先计算所有的 childen 的高度作为自己的高度
                physics:NeverScrollableScrollPhysics(),//禁用滑动事件
          ),
    ],
)
```
