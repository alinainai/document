Dart 在静态语法方面和 Java 非常相似，如类型定义、函数声明、泛型等，而在动态特性方面又和 JavaScript 很像，如函数式特性、异步支持等。
除了融合 Java 和 JavaScript 语言之所长之外，Dart 也具有一些其他很有表现力的语法，如可选命名参数、..（级联运算符）和?.（条件成员访问运算符）以及??（判空赋值运算符）。

## 一、变量声明

### 1.1 var

var 变量一旦赋值，类型便会确定，则不能再改变其类型，当用 var 声明一个变量后，Dart 在编译时会根据第一次赋值数据的类型来推断其类型

```dart
var t = "hi world";
t = 1000; // 报错，类型一旦确定后则不能再更改其类型
```

### 1.2 dynamic 和 Object

- dynamic 与 Object 声明的变量都可以赋值任意对象，且后期可以改变赋值的类型
- Object 是 Dart 所有对象的根基类，任何类型的数据都可以赋值给 Object 声明的对象。
- dynamic 声明的对象编译器会提供所有可能的组合，Object 声明的对象只能使用 Object 的属性与方法

```dart
dynamic t = 1000;
Object x = 900;
t = "hi world";
x = 'Hello Object';
print(t.length);// 正常
print(x.length); // 报错 The getter 'length' is not defined for the class 'Object'
print(a.xx); // dynamic 声明的对象编译器会提供所有可能的组合，这个特点很容易引入一个运行时错误，a是字符串，没有"xx"属性，编译时不会报错，运行时会报错
```

### 1.3 final 和 const

const 变量是一个编译时常量
final 变量在第一次使用时被初始化。
被 final 或者 const 修饰的变量，变量类型可以省略
```dart
final str = "hi world";
const str1 = "hi world";
```
### 1.4. 空安全（null-safety）

```dart
int? j; // 定义为可空类型，使用前要判空。可以通过变量后面加一个”!“符号，显示告诉预处理器它已经不是null了
if(i!=null) {
    print(i! * 8); //因为已经判过空，所以能走到这 i 必不为null，如果没有显式申明，则 IDE 会报错
}
```

// 如果我们预期变量不能为空，但在定义时不能确定其初始值，则可以加上 late 关键字
```dart
late int k;
k=9;
```
如果函数变量可空时，调用的时候可以用语法糖：
```dart
fun?.call() // fun 不为空时则会被调用
```

## 二、const修复构造函数时，该构造函数为常量构造函数

- 1.类的成员变量都是final类型
- 2.实例化对象时如果不加 const 修饰，那么实例化的对象不是常量实例
- 3.定义一个const对象，调用的构造函数必须是常量构造函数

### 1、正确的常量构造函数定义

根据以上的总结，定义一个Point类，包含一个常量构造函数，注意其成员变量都是final类型，且构造函数用const修饰

class Point {
  final int x;
  final int y;
  const Point(this.x, this.y);
}
### 2、常量构造函数需以const关键字修饰
如下代码定义一个const对象，但是调用的构造方法不是const修饰的，则会报The constructor being called isn't a const constructor.错误

void main() {
  const point = Point(1, 2); // 报错
}
 
class Point {
  final int x;
  final int y;
  Point(this.x, this.y);
}
### 3、const构造函数必须用于成员变量都是final的类
如下代码中成员变量x为非final，会报Can't define a const constructor for a class with non-final fields.错误

```dart
class Point {
  int x;
  final int y;
  const Point(this.x, this.y);
}
```
 
### 4、构建常量实例必须使用定义的常量构造函数
如下代码，定义一个const对象，但是调用的却是非常量构造函数，会报The constructor being called isn't a const constructor.错误
```dart
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
```
 

### 5.如果实例化时不加const修饰符，即使调用的是常量构造函数，实例化的对象也不是常量实例
如下代码，用常量构造函数构造一个对象，但是未用const修饰，那么该对象就不是const常量，其值可以再改变
```dart
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
```
