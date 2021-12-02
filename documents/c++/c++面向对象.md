
#### 3.class 和 struct 的区别

class 中的文件默认是私有的，struct 模式是 public 的。
struct 是 c++ 为了兼容 c 保留的。
使用习惯：一般涉及单独的类采用 struct，如果涉及到继承使用 class。

#### 4.引用的实质是常量指针 
表象：引用是变量的别名
实质：常量指针，就是不能改变指向的指针。



例如：
```c++
int b = 1;
int &a = b;//必须要有初值，实际是 int cont *a = b ;
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

#### 7.引用

1.引用必须初始化

2.引用在初始化后，不可以改变

```C++
int a = 10;
int& b = a;
int c = 20;
b = c;//这是赋值操作，而不是更改引用
```

引用的本质是常量指针，不可改变指向的指针。

所以初始化后不能更改引用。

常量引用

使用场景：用来修饰形参，防止误操作。

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
