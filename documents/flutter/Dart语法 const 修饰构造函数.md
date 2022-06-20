
## const修复构造函数，该构造函数为常量构造函数

- 1.类的成员变量都是final类型
- 2.实例化时不加const修饰，实例化的对象不是常量实例
- 3.定义一个const对象，调用的构造函数必须是常量构造函数

#### 1.正确的常量构造函数定义
根据以上的总结，定义一个Point类，包含一个常量构造函数，注意其成员变量都是final类型，且构造函数用const修饰

class Point {
  final int x;
  final int y;
  const Point(this.x, this.y);
}
#### 2.常量构造函数需以const关键字修饰
如下代码定义一个const对象，但是调用的构造方法不是const修饰的，则会报The constructor being called isn't a const constructor.错误

void main() {
  const point = Point(1, 2); // 报错
}
 
class Point {
  final int x;
  final int y;
  Point(this.x, this.y);
}
#### 3.const构造函数必须用于成员变量都是final的类
如下代码中成员变量x为非final，会报Can't define a const constructor for a class with non-final fields.错误

class Point {
  int x;
  final int y;
  const Point(this.x, this.y);
}
 

#### 4.构建常量实例必须使用定义的常量构造函数
如下代码，定义一个const对象，但是调用的却是非常量构造函数，会报The constructor being called isn't a const constructor.错误

void main() {
  var point = const Point(1, 2); // 报错
  print(point.toString());
}
 
class Point {
  int x;
  int y;
  Point(this.x, this.y); // 非const构造函数
  
  String toString() {
    return 'Point(${x}, ${y})';
  }
}
 

#### 5.如果实例化时不加const修饰符，即使调用的是常量构造函数，实例化的对象也不是常量实例
如下代码，用常量构造函数构造一个对象，但是未用const修饰，那么该对象就不是const常量，其值可以再改变

void main() {
  var point = Point(1, 2); // 调用常量构造函数，但是未定义成常量
  print(point.toString());
  point = Point(10, 20); // 可以重新赋值，说明定义的变量为非常量
  print(point.toString());
}
 
class Point {
  final int x;
  final int y;
  const Point(this.x, this.y);
  
  String toString() {
    return 'Point(${x}, ${y})';
  }
}
