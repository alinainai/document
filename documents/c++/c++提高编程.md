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

```c++
template<class NameType,class AgeType>
class Person{
public:
    Person(NameType name,AgeType age){
        this->m_Name = name;
        this->m_Age = age;
    }
    NameType m_Name;
    AgeType m_Age;
}
```


