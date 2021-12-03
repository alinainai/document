### 1.内存四区

#### 运行前

代码区：存放二进制代码，共享和只读

全局区：存放全局变量、静态变量、字符串常量和 const 修饰的全局常量，该区域的数据在程序结束后由操作系统释放

#### 运行后
栈区：由编译器自动分配释放，存放函数的参数值，局部变量等。

堆区：由程序员分配和释放，若程序员不释放，程序结束时由操作系统回收。

C++中主要利用new在堆区开辟内存

```c++
int* a = new int(10);
//利用delete释放堆区数据
delete p;
//cout << *p << endl; //报错，释放的空间不可访问
```
堆区开辟数组
```c++
int* arr = new int[10];
for (int i = 0; i < 10; i++){
    arr[i] = i + 100;
}
for (int i = 0; i < 10; i++){
    cout << arr[i] << endl;
}
//释放数组 delete 后加 []
delete[] arr;
```
### 2.引用

1.引用必须初始化

2.引用在初始化后，不可以改变

```c++
int a = 10;
int& b = a;
int c = 20;
b = c;//这是赋值操作，而不是更改引用:b=20,a=20,c=20
```

引用的本质是**常量指针**，不可改变指向的指针。所以初始化后不能更改引用。

```c++
//发现是引用，转换为 int* const ref = &a;
void func(int& ref){
	ref = 100; // ref是引用，转换为*ref = 100
}

int a = 10;
//自动转换为 int* const ref = &a; 指向不可改，也说明为什么引用不可更改
int& ref = a; 
ref = 20; //内部发现ref是引用，自动帮我们转换为: *ref = 20;
```

#### 常量引用

使用场景：用来修饰形参，防止误操作。
```c++
void showValue(const int& v) {
	//v += 10;
	cout << v << endl;
}

//int& ref = 10; 引用本身需要一个合法的内存空间，因此这行错误，加入const就可以了
//因为编译器会优化代码，int temp = 10; const int& ref = temp;
const int& ref = 10;
```
### 3.函数提高
#### 函数默认参数
```c++
int func(int a, int b = 10, int c = 10) {
	return a + b + c;
}
```

**注意点**

1)如果某个位置参数有默认值，那么从这个位置往后，从左向右，必须都要有默认值

2)如果函数声明有默认值，函数实现的时候就不能有默认参数

```c++
int func2(int a = 10, int b = 10);
int func2(int a, int b) {
	return a + b;
}
```

#### 函数占位参数，先了解一下
```c++
//函数占位参数 ，占位参数也可以有默认参数
void func(int a, int) {
	cout << "this is func" << endl;
}
func(10,10); //占位参数必须填补
```

#### 函数重载
同一个作用域下，函数名称相同，函数参数**类型不同**  或者 **个数不同** 或者 **顺序不同**
```c++
//函数重载需要函数都在同一个作用域下
void func(){
	cout << "func 的调用！" << endl;
}
void func(int a){
	cout << "func (int a) 的调用！" << endl;
}
void func(double a){
	cout << "func (double a)的调用！" << endl;
}
void func(int a ,double b){
	cout << "func (int a ,double b) 的调用！" << endl;
}
void func(double a ,int b){
	cout << "func (double a ,int b)的调用！" << endl;
}

func();
func(10);
func(3.14);
func(10,3.14);
func(3.14 , 10);
```
#### 函数重载注意事项
1、引用作为重载条件
```c++
void func(int &a){
    cout << "func (int &a) 调用 " << endl;
}
void func(const int &a){
    cout << "func (const int &a) 调用 " << endl;
}
int a = 10;
func(a); //调用无const
func(10);//调用有const
```
2、函数重载碰到函数默认参数
```c++
void func2(int a, int b = 10){
    cout << "func2(int a, int b = 10) 调用" << endl;
}
void func2(int a){
    cout << "func2(int a) 调用" << endl;
}
//func2(10); //碰到默认参数产生歧义，需要避免
```

### 3.类和对象
#### class 和 struct 的区别

class 中的文件默认是私有的，struct 默认是 public 的。
struct 是 c++ 为了兼容 c 保留的。
使用习惯：一般涉及单独的类采用 struct，如果涉及到继承使用 class。

#### class 关键字的使用
```C++
class Person {
public:
    Person() {
        cout << "无参构造函数!" << endl;
    }
    Person(int a) {//有参构造函数
        age = a;
        cout << "有参构造函数!" << endl;
    }
    Person(const Person &p) {//拷贝构造函数
        age = p.age;
        cout << "拷贝构造函数!" << endl;
    }
    ~Person() {//析构函数
        cout << "析构函数!" << endl;
    }
public:
    int age;
};

int main() {
    //------无参构造函数------
    Person p;
    //Person p2(); //调用无参构造函数不能加括号，否则编译器认为这是一个函数声明
    //------有参的构造函数------
    Person p1(10);
    Person p2 = Person(10);
    Person p3 = Person(p2);
    //隐式转换法
    Person p4 = 10; // Person p4 = Person(10);
    Person p5 = p4; // Person p5 = Person(p4);
    //拷贝构造
    Person p6(p5); 
}
```
#### 拷贝构造函数调用时机

1.使用一个已经创建完毕的对象来初始化一个新对象
2.值传递的方式给函数参数传值
3.以值方式返回局部对象

深拷贝解决重复释放堆区问题

```C++
class Person {
public:
    Person() {
        cout << "无参构造函数!" << endl;
    }
    Person(int age ,int height) {
        cout << "有参构造函数!" << endl;
        m_age = age;
        m_height = new int(height);
    }
    Person(const Person& p) {
        cout << "拷贝构造函数!" << endl;
        m_age = p.m_age;
        m_height = new int(*p.m_height);//如果不利用深拷贝在堆区创建新内存，会导致浅拷贝带来的重复释放堆区问题
    }
    ~Person() {//析构函数
        cout << "Person的析构函数调用" << endl;
        if (m_height != NULL)
        {
            delete m_height;
        }
    }
public:
    int m_age;
    int* m_height;
};
```

#### 5.static 关键字
static 关键字，定义在 class 中表示变量或者方法是类级别的。
定义在全局变量中表示变量是当前编译单元（该 cpp 文件）私有的，别的文件 link 不到。

#### 6.虚函数关键字 virtual
```C++
//纯虚函数后面为 =0，表示没有方法体
virtual int getArea() = 0;
```
有一个纯虚函数的类为抽象类






