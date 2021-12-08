### 1.模板：c++ 中的泛型编程

#### 1.1 函数模板：提高复用性

例子：交换两个数据类型
```c++
template<typename T>
void swap(T &a,T &b){
    T temp = a;
    a = b;
    b = temp;
}
```
使用
```c++
int a = 10;
int b = 20;
swap(a,b);//自定类型推导
swap<int>(a,b);//显示指定类型
```
typename 可用 class 代替。

#### 1.2 普通函数和函数模板的调用规则
1.如果普通函数和函数模板都可以实现，优先调用普通函数
2.可以通过空模板参数列表来强制调用函数模板
3.函数模板也可以发生重载
4.如果函数模板可以产生更好的匹配，优先调用函数模板

```c++
void print(int a,int b){
    cout<<"普通函数"<<endl;
}

template<class T>
void print(T a,T b){
    cout<<"模板函数"<<endl;
}

template<class T>
void print(T a,T b,T c){
    cout<<"重载的模板函数"<<endl;
}
```
使用
```c++
int a = 10;
int b = 20;
print(a,b); //普通函数
print<>(a,b); //函数模板
print(a,b，100); //重载的函数模板

char c1 = 'a';
cahr c2 = 'b';
//如果函数模板可以产生更好的匹配，优先调用函数模板
print(c1,c2); //函数模板 
```
#### 1.3 函数模板的具体化：解决自定义数据类型的通用化
```c++
template<class T>
bool compare(T &a,T &b){
    if(a==b){
        return true;
    }
    return false;
}
//如果类不能比较就是没有实现 == 操作符的重载，当然也可以让 Person 类重载运算符 ==
template<> bool compare(Person &p1,Person &p2)
```
#### 1.4 类模板

```c++
template<class NameType,class AgeType = int>
class Person{
public:
    Person(NameType name,AgeType age){
        this->m_Name = name;
        this->m_Age = age;
    }
    void showPerson(){
		cout << "name: " << this->mName << " age: " << this->mAge << endl;
	}
public:
    NameType m_Name;
    AgeType m_Age;
}
```
注意事项
```c++
Person <string ,int>p("孙悟空", 1000); //不可以用自动类型推导，必须使用显示指定类型的方式
Person <string> p("猪八戒", 999); //类模板中的模板参数列表 可以指定默认参数
```
类模板的成员函数创建时机：函数调用时才会创建成员函数

#### 1.5 类模板做函数参数
1.制定传入类型（整个最常用）
```c++
void printPerson(Person<string,int>&p){
    p.showPersion();
}
Person<string,int>p("孙悟空"，1000);
printPerson(p);
```
2.参数模板化
```c++
template<class T1,class T2 = int>
void printPerson(Person<T1,T2>&p){
    p.showPersion();
    cout << "T1 的类型为：" << typeid(T1).name() << endl;
    cout << "T2 的类型为：" << typeid(T2).name() << endl;
}
Person<string,int>p("孙悟空"，1000);
printPerson(p);
```
3.整个类模板化
```c++
template<class T>
void printPerson(T &p){
    p.showPersion();
    cout << "T 的类型为：" << typeid(T).name() << endl;//会带泛型的类型
}
Person<string,int>p("孙悟空"，1000);
printPerson(p);
```
#### 1.6 类模板与继承

```c++
template<class T>
class Base{
    T m;   
};
```
1.当子类继承的父类是一个类模板时，子类在声明的时候，要指定出父类中T的类型。如果不指定，编译器无法给子类分配内存。
```c++
class Son:public Base<Int>{
};
```
2.如果想灵活指定出父类中T的类型，子类也需变为类模板
```c++
template<class T1, class T2>
class Son2:public Base<T2>{
public:
    T1 obj;    
};
```
#### 1.7 类模板成员函数类外实现
```c++
template<class T1, class T2>
class Person{
public:
    Person(T1 name,T2 age)
    void showPerson();
    T1 m_Name;
    T2 m_Age;
};

template<class T1,class T2>
Person<T1,T2>::Person(T1 name,T2 age){
    this->m_Name = name;
    this->m_Age = age;
}

template<class T1,class T2>
Person<T1,T2>::showPerson(){
    cout<<"姓名："<<this->m_Name <<"年龄："<<this->m_Age<<endl;
}
```
#### 1.8 类模板的分文件编写

将声明 .h 和 .cpp 的内容写到一起，后缀名改为 .hpp

### 2.string 类

#### 2.1 string构造函数
```c++
string s1; //创建空字符串，调用无参构造函数
string s2(str); //把c_string转换成了string
string s3(s2); //调用拷贝构造函数，使用一个string对象初始化另一个string对象
string s4(10, 'a'); //使用n个字符c初始化 "aaaaaaaaaa"
```
#### 2.2 赋值操作
```c++
//char* 类型字符串 赋值给当前的字符串：string& operator=(const char* s)
string str1;
str1 = "hello world";

//把字符串s赋给当前的字符串：string& operator=(const string &s);
string str2;
str2 = str1;

//字符赋值给当前的字符串：string& operator=(char c);
string str3;
str3 = 'a';

//把字符串s赋给当前的字符串：string& assign(const char *s);
string str4;
str4.assign("hello c++");

//把字符串s的前n个字符赋给当前的字符串：string& assign(const char *s, int n);
string str5;
str5.assign("hello c++",5);

//把字符串s赋给当前字符串：string& assign(const string &s);
string str6;
str6.assign(str5);
	
//用n个字符c赋给当前字符串：string& assign(int n, char c);
string str7;
str7.assign(5, 'x');
```
#### 2.3 拼接字符
```c++
string str1 = "我";

//string& operator+=(const char* str);
str1 += "爱玩游戏";

//string& operator+=(const char c);
str1 += ':';

//string& operator+=(const string& str);
string str2 = "LOL DNF";
str1 += str2;

string str3 = "I";

//把字符串s连接到当前字符串结尾
str3.append(" love ");

//把字符串s的前n个字符连接到当前字符串结尾
str3.append("game abcde", 4);

//同operator+=(const string& str)
//str3.append(str2);

// 从下标4位置开始 ，截取3个字符，拼接到字符串末尾
str3.append(str2, 4, 3); 
```
#### 2.4 查找和替换

**查找**

find查找是从左往后，rfind从右往左，find找到字符串后返回查找的第一个字符位置，找不到返回-1

查找str第一次出现位置,从pos开始查找:`int find(const string& str, int pos = 0) const;`

查找s第一次出现位置,从pos开始查找:`int find(const char* s, int pos = 0) const;`

从pos位置查找s的前n个字符第一次位置:`int find(const char* s, int pos, int n) const;`

查找字符c第一次出现位置:`int find(const char c, int pos = 0) const;`

```c++
string str1 = "abcdefgde";
int pos = str1.find("de");//如果没有找到返回-1
```

**反向查找**

查找str最后一次位置,从pos开始查找:`int rfind(const string& str, int pos = npos) const;`

查找s最后一次出现位置,从pos开始查找:`int rfind(const char* s, int pos = npos) const;` 

从pos查找s的前n个字符最后一次位置:`int rfind(const char* s, int pos, int n) const;`

查找字符c最后一次出现位置:`int rfind(const char c, int pos = 0) const;`

**替换**

replace在替换时，要指定从哪个位置起，多少个字符，替换成什么样的字符串

替换从pos开始n个字符为字符串str:`string& replace(int pos, int n, const string& str);`

替换从pos开始的n个字符为字符串s:`string& replace(int pos, int n,const char* s);`  

```c++
//替换
string str1 = "abcdefgde";
str1.replace(1, 3, "1111");
```
#### 2.5 字符串比较
按字符的ASCII码进行对比
```c++
int compare(const string &s) const;
int compare(const char *s) const;

string s1 = "hello";
string s2 = "aello";
int ret = s1.compare(s2);
//ret == 0 s1 等于 s2
//ret > 0 s1 大于 s2
```
#### 2.6 字符存取
```c++
char& operator[](int n);//通过[]方式取字符
char& at(int n);//通过at方法获取字符
string str = "hello world";
str[0] = 'x';
cout << str[0] << endl;
```
#### 2.7 string插入和删除
```c++
string& insert(int pos, const char* s);//插入字符串
string& insert(int pos, const string& str);//插入字符串
string& insert(int pos, int n, char c);//在指定位置插入n个字符c
string& erase(int pos, int n = npos);//删除从Pos开始的n个字符 

string str = "hello";
str.insert(1, "111");
str.erase(1, 3);  //从1号位置开始3个字符
```
#### 2.8 子串
```c++
string str = "abcdefg";
string subStr = str.substr(1, 3);

string email = "hello@sina.com";
int pos = email.find("@");
string username = email.substr(0, pos);
```
### 3.vector容器

vector数据结构和**数组非常相似**，也称为**单端数组**，不同之处在于数组是静态空间，而vector可以**动态扩展**

#### 3.1 动态扩展

并不是在原空间之后续接新空间，而是找更大的内存空间，然后将原数据拷贝新空间，释放原空间

<img width="400" alt="vector" src="https://user-images.githubusercontent.com/17560388/144810574-2c70596a-046e-401f-8fbd-c473fa4065f3.jpg">

vector容器的迭代器是支持随机访问的迭代器

#### vector初始化和赋值操作
```c++
vector<int> v1; //无参构造
for (int i = 0; i < 10; i++){//添加数据
    v1.push_back(i);
}
vector<int> v2(v1.begin(), v1.end());//将v[begin(), end())区间中的元素拷贝给本身
vector<int> v3(10, 100);//构造函数将n个elem拷贝给本身
vector<int> v4(v3);//拷贝构造函数
```

赋值

```c++
vector<int>v2;
v2 = v1;
vector<int>v3;
v3.assign(v1.begin(), v1.end()); //将[beg, end)区间中的数据拷贝赋值给本身
vector<int>v4;
v4.assign(10, 100);//将n个elem拷贝赋值给本身
```
#### 3.2 一些方法
```c++
v1.empty()//v1是否为空
v1.capacity()//v1的容量

//resize 重新指定大小 ，若指定的更大，默认用0填充新位置，可以利用重载版本替换默认填充
v1.resize(5);
v1.resize(15,10);
```
#### 3.3 插入删除
```c++
vector<int> v1;
//尾插
v1.push_back(10);
v1.push_back(20);
v1.push_back(30); // 10,20,30
//尾删
v1.pop_back(); // 10,20
//插入
v1.insert(v1.begin(), 100);//迭代器指向位置pos插入元素100 : 100,10,20
v1.insert(v1.begin(), 2, 1000);//迭代器指向位置pos插入2个元素1000 : 1000,1000,100,10,20
//删除
v1.erase(v1.begin()); //1000,100,10,20
//清空
v1.erase(v1.begin(), v1.end());
v1.clear();
```
#### 3.4 数据存取
```c++
 v1[0]//返回索引idx所指的数据
 v1.at(0)//返回索引idx所指的数据
 v1.front()//返回容器中第一个数据元素
 v1.back()//返回容器中最后一个数据元素
```
#### 3.5 互换容器

swap(vec)：将vec与本身的元素互换

```c++
vector<int>v1;
for (int i = 0; i < 10; i++){
    v1.push_back(i);
}
vector<int>v2;
for (int i = 10; i > 0; i--){
    v2.push_back(i);
}
//互换容器
v1.swap(v2);
```

实际作用：收缩内存

```c++
vector<int> v;
for (int i = 0; i < 100000; i++) {
    v.push_back(i);
}
cout << "v的容量为：" << v.capacity() << endl;
cout << "v的大小为：" << v.size() << endl;

v.resize(3); //size改变，容量不会变
cout << "v的容量为：" << v.capacity() << endl;
cout << "v的大小为：" << v.size() << endl;

//收缩内存
vector<int>(v).swap(v); //匿名对象
cout << "v的容量为：" << v.capacity() << endl;
cout << "v的大小为：" << v.size() << endl;
```
#### 3.6 预留空间

reserve(int len) : 容器预留len个元素长度，预留位置不初始化，元素不可访问。

作用：减少vector在动态扩展容量时的扩展次数

```c++
vector<int> v;

//预留空间
v.reserve(100000);

int num = 0;//num 表示 v 扩容的次数
int* p = NULL;
for (int i = 0; i < 100000; i++) {
    v.push_back(i);
    if (p != &v[0]) {
        p = &v[0];
        num++;
    }
}

cout << "num:" << num << endl;
```
### 4.deque容器
#### 4.1 基本概念
双端数组，可以对头端进行插入删除操作

特点


- deque对头部的插入删除速度回比vector快
- vector访问元素时的速度会比deque快
- deque的迭代器也是支持随机访问的

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/145016358-698404ca-9561-4a75-b2b3-6460a00746c7.jpg">

内部工作原理

eque内部有个**中控器**，维护每段缓冲区中的内容，缓冲区中存放真实数据

中控器维护的是每个缓冲区的地址，使得使用deque时像一片连续的内存空间

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/145016466-bff8900f-3657-4cc7-baa4-6002ee5dc57a.jpg">

#### 4.2构造函数和赋值

```c++
//1.无参构造函数
deque<int> d1; 

//2.构造函数将[beg, end)区间中的元素拷贝给本身
deque<int> d2(d1.begin(),d1.end());

//3.构造函数将n个elem拷贝给本身。
deque<int>d3(10,100);

//4.拷贝构造函数
deque<int>d4 = d3;
```

赋值操作

```c++
deque<int> d1;
for (int i = 0; i < 10; i++){
    d1.push_back(i);
}

//1.重载等号操作符 deque& operator=(const deque &deq)		       
deque<int>d2;
d2 = d1;  

//2.将[beg, end)区间中的数据拷贝赋值给本身	
deque<int>d3;
d3.assign(d1.begin(), d1.end()); 

//3.将n个elem拷贝赋值给本身。
deque<int>d4;
d4.assign(10, 100);
```
#### 4.3大小操作
```c++
deque<int> d1;
for (int i = 0; i < 10; i++){
    d1.push_back(i);
}

//判断容器是否为空
d1.empty()

//d1的大小为
d1.size()

//重新指定大小
d1.resize(15, 1);
d1.resize(5);
```
#### 4.4 插入和删除
```c++
deque<int> d;
	
//尾插:容器尾部添加一个数据
d.push_back(10);//10
d.push_back(20);//10,20

//头插:容器头部插入一个数据
d.push_front(100);//100,10,20
d.push_front(200);//200,100,10,20

//尾删:删除容器最后一个数据
d.pop_back();//200,100,10

//头删:删除容器第一个数据
d.pop_front();//100,10
```

指定位置操作：

* `insert(pos,elem);`         //在pos位置插入一个elem元素的拷贝，返回新数据的位置。

* `insert(pos,n,elem);`     //在pos位置插入n个elem数据，无返回值。

* `insert(pos,beg,end);`    //在pos位置插入[beg,end)区间的数据，无返回值。

* `clear();`                           //清空容器的所有数据

* `erase(beg,end);`             //删除[beg,end)区间的数据，返回下一个数据的位置。

* `erase(pos);`                    //删除pos位置的数据，返回下一个数据的位置。



