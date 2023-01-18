Dart 在静态语法方面和 Java 非常相似，如类型定义、函数声明、泛型等，而在动态特性方面又和 JavaScript 很像，如函数式特性、异步支持等。
除了融合 Java 和 JavaScript 语言之所长之外，Dart 也具有一些其他很有表现力的语法，如`可选命名参数`、`..（级联运算符）`和`?.（条件成员访问运算符`）以及`??（判空赋值运算符）`。

## 一、基础变量

### 1、num类型

`int`：整数值，长度不超过64位。  
`double`: 64位双精度。  
`int` 和 `double` 都是 `num` 的子类。

```dart
var x = 1;// int
var y = 1.1; // double

num x = 1;// x can have both int and double values
x+=2.5;

double z = 1;// z = 1.0。注意 Dart 2.1 之前，在浮点数上下文中使用整数字面量是错误的
```

### 2、String类型

`String` 字符串包含了 `UTF-16` 编码的字符序列。创建方式：

```dart
var s1 = '单引号\''; // 注意单引号前面的转义字符
var s2 = "双引号"; // 双引号声明中不需要使用转义与单引号冲突的字符串
```

Dart 支持字符串插值 `${表达式}`

```dart
var s = '字符串插值';
var s = '${s.substring(3,5)}';
```

可以使用 `+` 运算符或并列放置多个字符串来连接字符串

```dart
var s1 = '可以拼接'
    '字符串'
    "即便它们不在同一行。";
    
var s2 = '使用加号 + 运算符' + '也可以达到相同的效果。';
```

使用三个单引号或者三个双引号也能创建多行字符串：

```dart
var s1 = '''
你可以像这样创建多行字符串。
''';
var s2 = """这也是一个多行字符串。""";
```

在字符串前加上 `r` 作为前缀创建 “raw” 字符串（即不会被做任何处理（比如转义）的字符串）：

```dart
var s = r'在 raw 字符串中，转义字符串 \n 会直接输出 “\n” 而不是转义为换行。';
```

字符串字面量是一个编译时常量，只要是编译时常量 (null、数字、字符串、布尔) 都可以作为字符串字面量的插值表达式：

```dart
// These work in a const string.
const aConstNum = 0;
const aConstBool = true;
const aConstString = 'a constant string';

// These do NOT work in a const string.
var aNum = 0;
var aBool = true;
var aString = 'a string';
const aConstList = [1, 2, 3];

const validConstString = '$aConstNum $aConstBool $aConstString';
// const invalidConstString = '$aNum $aBool $aString $aConstList';
```

### 3、bool类型

`bool` 类型只有两个对象 `true` 和 `false`，两者都是编译时常量。

Dart 的类型安全不允许使用类似 `if (nonbooleanValue)` 或者 `assert (nonbooleanValue)` 这样的代码检查布尔值。

```dart
const b1 = true;
const b2 = false;
```

### 4、集合类型

List、set、map是dart中的集合类型，和Java、Kotlin相似

```dart
// List (也被称为 Array)
var list = [1,2,3];
list.add(4);
list.removeAt(0);
print("list= ${list}");

var list1 = List<String>();
list1.add("1");
list1.add("2");
print("list1= ${list1}");

// Set
var halogens = {'fluorine', 'chlorine', 'bromine', 'iodine', 'astatine'};
halogens.add("'fluorine'");
print("set1= $halogens");

var set = Set<String>();
set.add("set1");
set.add("set2");
print("set2= $set");
// Map
var gift = Map<String,String>();
final gifts = {"1":"1","2":"2"};
gifts['3'] = '3';
print("Map1= $gift");
print("Map2= $gifts");
```



## 二、变量声明

### 1、var

var 变量一旦赋值，类型便会确定，则不能再改变其类型，当用 var 声明一个变量后，Dart 在编译时会根据第一次赋值数据的类型来推断其类型

```dart
var t = "hi world";
t = 1000; // 报错，类型一旦确定后则不能再更改其类型
```

### 2、dynamic 和 Object

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

// dynamic 声明的对象编译器会提供所有可能的组合，这个特点很容易引入一个运行时错误，a是字符串，没有"xx"属性，编译时不会报错，运行时会报错
dynamic a;
a = "";
print(a.xx); 
```

### 3、final 和 const

`const` 变量是一个编译时常量。  
`final` 变量需要在第一次使用时被初始化。  
被 `final` 或者 `const` 修饰的变量，变量类型可以省略  

```dart
final str = "hi world";
const str1 = "hi world";
```
### 4、空安全（null-safety）

```dart
int? j; // 定义为可空类型，使用前要判空。可以通过变量后面加一个”!“符号，显示告诉预处理器它已经不是null了
if(i!=null) {
    print(i! * 8); //因为已经判过空，所以能走到这 i 必不为null，如果没有显式申明，则 IDE 会报错
}
```

如果我们预期变量不能为空，但在定义时不能确定其初始值，则可以加上` late` 关键字
```dart
late int k;
k=9;
```
如果函数变量可空时，调用的时候可以用语法糖：
```dart
fun?.call() // fun 不为空时则会被调用
```

## 三、const修饰构造函数

1、const修饰构造函数时，该构造函数为常量构造函数，常量构造函数有下面几点需求：

- 1.const构造函数必须用于成员变量都是final的类
- 2.实例化对象时如果不加 `const` 修饰，那么实例化的对象不是常量实例
- 3.构建常量实例必须使用定义的常量构造函数

```dart
class Point {
  final int x;
  final int y;
  const Point(this.x, this.y);
}
```

## 四、函数

Dart 作为一个高级语言，函数也是 Dart 的基本公民，函数即可以赋值给变量或者当做参数传递给其他函数。

### 1、函数声明

```dart
// 声明返回值为 bool 类型的一个函数
bool isNoble(int atomicNum){
  return _nobleGases[atomicNum]!=null;
}
```

Dart函数声明如果没有显示声明返回值类型时会默认当做 `dynamic` 处理

