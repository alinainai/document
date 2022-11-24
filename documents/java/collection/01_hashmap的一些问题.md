### 1、HashMap的初始容量为16（2的4次方）
```java
static fnal int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16
```
### 2、HashMap的容量初始值必须为2的倍数
因为在求 index 的时候要把取余运算改为位运算来提高查询效率，改为2的幂之后，两种运算方式等价，例如：12%16 == 12&(16-1)，下面是index的算法。

### 3、HashMap 的 entry 的 index 计算
```java
int hash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16)
```
">>>" : 无符号右移16位，高16位和低16位都可以参与
```java
int index =hash & (table.length-1) 
```

![image](https://user-images.githubusercontent.com/17560388/118071960-6ba33580-b3db-11eb-8507-8099d5021fda.png)


### 4、key和value可以为空，但是要注意 key 的唯一性
### 5、采用数组加链表的存储方式（JDK1.7,1.8之后链表改为红黑树）
JDK1.8:当链表的 长度>8 && 数组长度>64时 链表转为红黑树。
当 resize() 树的节点数 <6 时会再次转为链表。数据查询效率要高一些。
### 6、举例其他的求index的方法
平方取中法、取余法（HashMap使用）、伪随机数法
### 7、HashMap通过拉链法来处理hash碰撞

