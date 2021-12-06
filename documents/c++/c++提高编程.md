### 1.模板：c++ 中的泛型编程

#### 函数模板：提高复用性

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

#### 普通函数和函数模板的调用规则
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
#### 函数模板的具体化：解决自定义数据类型的通用化
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
#### 类模板

#### 简单使用
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

#### 类模板做函数参数
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
#### 类模板与集成
```c++
template<class T>
class Base{
    T m;   
};

//指定父类模板类型
class Son:public Base<Int>{
};

//灵活配置
template<class T1, class T2>
class Son2:public Base<T2>{
public:
    T1 obj;    
};
```
#### 类模板成员函数类外实现
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
#### 类模板的分文件编写

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

#### 动态扩展

并不是在原空间之后续接新空间，而是找更大的内存空间，然后将原数据拷贝新空间，释放原空间

<img width="400" alt="vector" src="https://user-images.githubusercontent.com/17560388/144810574-2c70596a-046e-401f-8fbd-c473fa4065f3.jpg">

vector容器的迭代器是支持随机访问的迭代器

#### vector初始化
