### 1.函数对象

重载**函数调用操作符**的类，其对象常称为**函数对象（仿函数）** 

仿函数可以作为参数进行传递，本身做为一个类还可以有自己的参数记录一些状态。

返回bool类型的仿函数称为**谓词**

operator()接受一个参数，叫做一元谓词，两个参数叫做二元谓词

```c++
class MyCompare {
public:
    bool operator()(int num1, int num2) {
        return num1 > num2;
    }
};
vector<int> v;
//使用函数对象改变算法策略，排序从大到小
sort(v.begin(), v.end(), MyCompare());
```

#### 1.1 stl内置仿函数

```c++
#include <functional>
```

* `template<class T> T plus<T>`                //加法仿函数
* `template<class T> T minus<T>`              //减法仿函数
* `template<class T> T multiplies<T>`    //乘法仿函数
* `template<class T> T divides<T>`         //除法仿函数
* `template<class T> T modulus<T>`         //取模仿函数
* `template<class T> T negate<T>`           //取反仿函数

* `template<class T> bool equal_to<T>`                    //等于
* `template<class T> bool not_equal_to<T>`            //不等于
* `template<class T> bool greater<T>`                      //大于
* `template<class T> bool greater_equal<T>`          //大于等于
* `template<class T> bool less<T>`                           //小于
* `template<class T> bool less_equal<T>`               //小于等于


```c++
sort(v.begin(), v.end(), greater<int>());
```

* `template<class T> bool logical_and<T>`              //逻辑与
* `template<class T> bool logical_or<T>`                //逻辑或
* `template<class T> bool logical_not<T>`              //逻辑非


### 2. stl常用算法


`for_each(iterator beg, iterator end, _func);  `

搬运容器到另一个容器中：`transform(iterator beg1, iterator end1, iterator beg2, _func);`
