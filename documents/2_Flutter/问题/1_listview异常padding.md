ListView 头部有一段空白区域，是因为当 ListView 没有和 AppBar 一起使用时，头部会有一个 padding，为了去掉 padding，可以使用 MediaQuery.removePadding
```dart
Widget _listView(BuildContext context){
    return MediaQuery.removePadding(
      removeTop: true,
      context: context,
      child: ListView.builder(
        itemCount: 10,
        itemBuilder: (context,index){
          return _item(context,index);
        },

      ),
    );
}
```

