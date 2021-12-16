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


#### 2.1 for_each 和 transform

`for_each(iterator beg, iterator end, _func);`

`transform(iterator beg1, iterator end1, iterator beg2, _func);` //搬运容器到另一个容器中

```c++
//打印
void print_int(int val){
    cout<<val<<" ";
}
//处理数据
int num_plus(int val){
    return val*2;
}
//使用
vector<int> v ;
v.reserve(10);
for(int i = 0;i<10;i++){
    v.push_back(i);
}
vector<int> target;
target.resize(v.size());//搬运的目标容器必须要提前开辟空间，否则无法正常搬运
transform(v.begin(),v.end(),target.begin(),num_plus);
for_each(target.begin(),target.end(),print_int);
```

#### 2.2 常用查找算法

**find**：按值查找元素，找到返回指定位置迭代器，找不到返回结束迭代器位置
```c++
// 按值查找元素，找到返回指定位置迭代器，找不到返回结束迭代器位置
// beg 开始迭代器
// end 结束迭代器
// value 查找的元素
find(iterator beg, iterator end, value); 
```
**find_if**：按值查找元素，找到返回指定位置迭代器，找不到返回结束迭代器位置
```c++
// 按值查找元素，找到返回指定位置迭代器，找不到返回结束迭代器位置
// beg 开始迭代器
// end 结束迭代器
// _Pred 函数或者谓词（返回bool类型的仿函数）
find_if(iterator beg, iterator end, _Pred);  
```
**adjacent_find**：查找相邻重复元素,返回相邻元素的第一个位置的迭代器
```c++
// 查找相邻重复元素,返回相邻元素的第一个位置的迭代器
// beg 开始迭代器
// end 结束迭代器
adjacent_find(iterator beg, iterator end); 
```
**binary_search**：有序列表中查找指定的元素，查到 返回true  否则false
```c++
// 有序列表中查找指定的元素，查到 返回true  否则false
bool binary_search(iterator beg, iterator end, value);
```
**count**：统计元素value出现的次数
```c++
// 统计元素value出现的次数
int count(iterator beg, iterator end, value); 
```
**count_if**：按条件统计元素出现次数
```c++
// 按条件统计元素出现次数
int count_if(iterator beg, iterator end, _Pred); 
```

#### 2.3 常用排序算法

**sort**：对容器内元素进行排序

```c++
sort(v.begin(), v.end(), greater<int>());
```

**shuffle**：洗牌，指定范围内的元素随机调整次序

```c++
std::random_device rd;
std::mt19937 g(rd());	// 随机数引擎:基于梅森缠绕器算法的随机数生成器
std::shuffle(v.begin(), v.end(), g);
for_each(v.begin(), v.end(), print_int);
```
**merge**：容器元素合并，并存储到另一容器中

```c++
//merge(iterator beg1, iterator end1, iterator beg2, iterator end2, iterator dest);
vector<int> v1;
vector<int> v2;
for (int i = 0; i < 10 ; i++) {
	v1.push_back(i);
	v2.push_back(i + 1);
}
vector<int> vtarget;
//目标容器需要提前开辟空间
vtarget.resize(v1.size() + v2.size());
//合并  需要两个有序序列
merge(v1.begin(), v1.end(), v2.begin(), v2.end(), vtarget.begin());
for_each(vtarget.begin(), vtarget.end(), myPrint());
cout << endl;
```
**reverse**：反转指定范围的元素

```c++
reverse(v.begin(), v.end());
```
#### 2.4 常用拷贝和替换算法

**copy：** 容器内指定范围的元素拷贝到另一容器中，目标容器记得提前开辟空间
```c++
copy(iterator beg, iterator end, iterator dest);
```
**replace：** 将容器内指定范围的旧元素修改为新元素
```c++
// 将区间内旧元素 替换成 新元素
replace(iterator beg, iterator end, oldvalue, newvalue); 
```

**replace_if：** 容器内指定范围满足条件的元素替换为新元素
```c++
replace_if(iterator beg, iterator end, _pred, newvalue);
```
**swap：** 互换两个容器的元素 

```c++
swap(container c1, container c2);
```
#### 2.5 常用算术生成算法

**accumulate**：计算容器元素累计总和
```c++
#include <numeric>
//value 起始值
accumulate(iterator beg, iterator end, value);  

vector<int> v;
for (int i = 0; i <= 100; i++) {
    v.push_back(i);
}
int total = accumulate(v.begin(), v.end(), 0);
```

**fill：** 向容器中添加元素

```c++
vector<int> v;
v.resize(10);
//填充：将容器区间内元素填充为 指定的值
fill(v.begin(), v.end(), 100);
```
#### 2.6 常用集合算法

**两个集合必须是有序序列**

**set_intersection：** 求两个容器的交集

```c++
vector<int> v1;
vector<int> v2;
for (int i = 0; i < 10; i++) {
    v1.push_back(i);
    v2.push_back(i + 5);
}
vector<int> vTarget;
vTarget.resize(min(v1.size(), v2.size()));
//返回目标容器的最后一个元素的迭代器地址
auto itEnd = set_intersection(v1.begin(), v1.end(), v2.begin(), v2.end(), vTarget.begin());
for_each(vTarget.begin(), itEnd, print_int);
```

**set_union：** 求两个容器的并集
```c++
vector<int> v1;
vector<int> v2;
for (int i = 0; i < 10; i++) {
    v1.push_back(i);
    v2.push_back(i + 5);
}
vector<int> vTarget;
//取两个容器的和给目标容器开辟空间
vTarget.resize(v1.size() + v2.size());
//返回目标容器的最后一个元素的迭代器地址
auto itEnd = set_union(v1.begin(), v1.end(), v2.begin(), v2.end(), vTarget.begin());
for_each(vTarget.begin(), itEnd, print_int);
cout << endl;
```
**set_difference：** 求两个容器的差集
```c++
vector<int> v1;
vector<int> v2;
for (int i = 0; i < 10; i++) {
    v1.push_back(i);
    v2.push_back(i + 5);
}
vector<int> vTarget;
//取两个里面较大的值给目标容器开辟空间
vTarget.resize(max(v1.size(), v2.size()));
//返回目标容器的最后一个元素的迭代器地址
cout << "v1与v2的差集为： " << endl;
auto itEnd = set_difference(v1.begin(), v1.end(), v2.begin(), v2.end(), vTarget.begin());
for_each(vTarget.begin(), itEnd, print_int);
cout << endl;
cout << "v2与v1的差集为： " << endl;
itEnd = set_difference(v2.begin(), v2.end(), v1.begin(), v1.end(), vTarget.begin());
for_each(vTarget.begin(), itEnd, print_int);
cout << endl;
```
日志
```c++
v1与v2的差集为： 
0 1 2 3 4 
v2与v1的差集为： 
10 11 12 13 14
```



