
### SparseArray 的使用

```java
//创建
SparseArray sparseArray = new SparseArray<>();
SparseArray sparseArray = new SparseArray<>(capacity);

//put()
sparseArray.put(int key,Student value);

//get()
sparseArray.get(int key);
sparseArray.get(int key,Student valueIfNotFound);

//remove()
sparseArray.remove(int key);
```

**index**

前面几个都跟HashMap没有什么太大区别，而这个index就是SparseArray所特有的属性了，这里为了方便理解先提一嘴，SparseArray从名字上看就能猜到跟数组有关系，事实上他底层是两条数组，一组存放key，一组存放value，知道了这一点应该能猜到index的作用了。
index — key在数组中的位置。SparseArray提供了一些跟index相关的方法：

```java
sparseArray.indexOfKey(int key);
sparseArray.indexOfValue(T value);
sparseArray.keyAt(int index);
sparseArray.valueAt(int index);
sparseArray.setValueAt(int index);
sparseArray.removeAt(int index);
sparseArray.removeAt(int index,int size);
```

### SparseArray 实现原理

**官方优点：** 
- 避免装箱
- 不使用额外的结构体（Entry)

#### 初始化

SparseArray没有继承任何其他的数据结构，实现了Cloneable接口。

```java
private int[] mKeys;
private Object[] mValues;
private int mSize;
public SparseArray() {this(10);}
public SparseArray(int initialCapacity) {
        if (initialCapacity == 0) {
            mKeys = EmptyArray.INT;
            mValues = EmptyArray.OBJECT;
        } else {
            mValues = ArrayUtils.newUnpaddedObjectArray(initialCapacity);
            mKeys = new int[mValues.length];
        mSize = 0;
```

初始化SparseArray只是简单的创建了两个数组。

#### put()方法

接下来就是往SparseArray中存放数据。

```java
public void put(int key, E value) {
    int i = ContainerHelpers.binarySearch(mKeys, mSize, key);
    if (i >= 0) { 
        mValues[i] = value;
    } else {
        i = ~i;
        if (i < mSize && mValues[i] == DELETED) {
            mKeys[i] = key;
            mValues[i] = value;
            return;
        if (mGarbage && mSize >= mKeys.length) {
            gc();
            i = ~ContainerHelpers.binarySearch(mKeys, mSize, key);
        mKeys = GrowingArrayUtils.insert(mKeys, mSize, i, key);
        mValues = GrowingArrayUtils.insert(mValues, mSize, i, value);
        mSize++;
```

**冲突直接覆盖**

<img src="https://user-images.githubusercontent.com/17560388/132646961-a386faf3-865e-447c-bf41-3165d6b9f3da.png" alt="图片替换文本" width="600"  align="bottom" />

上面这个图，插入一个key=3的元素，因为在mKeys中已经存在了这个值，则直接覆盖。

**插入索引上为DELETED**

<img src="https://user-images.githubusercontent.com/17560388/132647060-5bc86fad-2641-41db-a920-0ed9f61ebbb5.png" alt="图片替换文本" width="600"  align="bottom" />

注意mKeys中并没有 3 这个值，但是通过二分查找得出来，目前应该插入的索引位置为 2 ，即key=4所在的位置，而当前这个位置上对应的value标记为DELETED了，所以会直接将该位置上的key赋值为 3 ，并且将该位置上的value赋值为put()传入的对象。

**索引上有值，但是应该触发 `gc()`**

<img src="https://user-images.githubusercontent.com/17560388/132647381-30ee7f59-79e6-46b6-9029-53312691f1c8.png" alt="图片替换文本" width="600"  align="bottom" />

注意这个图跟前面的几个又一个区别，那就是数组已经满容量了，而且 3 应该插入的位置已经有 4 了，而 5 所指向的值为DELETED，这种情况下，会先去回收DELETED,重新调整数组结构，图中的例子则会回收 5 ,然后再重新计算 3 应该插入的位置

**满容且无法 `gc()`**

<img src="https://user-images.githubusercontent.com/17560388/132647505-44342e4e-5733-40e2-8269-1725666955af.png" alt="图片替换文本" width="600"  align="bottom" />

这种情况下，就只能对数组进行扩容，然后插入数据。

结合这几个图，插入的流程应该很清晰了，但是`put()`还有几个值得我们探索的点，首先就是**二分查找**的算法，这是一个很普通的二分算法，注意最后一行代码，当找不到这个值的时候`return ~lo`，实际上到这一步的时候，理论上`lo==mid==hi`。所以这个位置是最适合插入数据的地方。但是为了让能让调用者既知道没有查到值，又知道索引位置，做了一个取反操作，返回一个负数。这样调用处可以首先通过正负来判断命中，之后又可以通过取反获取索引位置。

```java
static int binarySearch(int[] array, int size, int value) {
    int lo = 0;
    int hi = size - 1;
    while (lo <= hi) {
        final int mid = (lo + hi) >>> 1;
        final int midVal = array[mid];
        if (midVal < value) {
            lo = mid + 1;
        } else if (midVal > value) {
            hi = mid - 1;
        } else {
            return mid;  
    return ~lo;  
```   

**第二个点就是，插入数据具体是怎么插入的。**

```java
mKeys = GrowingArrayUtils.insert(mKeys, mSize, i, key);
mValues = GrowingArrayUtils.insert(mValues, mSize, i, value);
```

```java
public static int[] insert(int[] array, int currentSize, int index, int element) {
    assert currentSize <= array.length;
    if (currentSize + 1 <= array.length) {
        System.arraycopy(array, index, array, index + 1, currentSize - index);
        array[index] = element;
        return array;
    int[] newArray = ArrayUtils.newUnpaddedIntArray(growSize(currentSize));
    System.arraycopy(array, 0, newArray, 0, index);
  	newArray[index] = element;
    System.arraycopy(array, index, newArray, index + 1, array.length - index);
    return newArray;
```

`put()`部分的代码就全部完毕了，接下来先来看看`remove()`是怎么处理的？

```java
private static final Object DELETED = new Object();

public void remove(int key) {
    delete(key);
    public void delete(int key) {
    int i = ContainerHelpers.binarySearch(mKeys, mSize, key);
    if (i >= 0) {
        if (mValues[i] != DELETED) {
            mValues[i] = DELETED;
            mGarbage = true;
```
将 remove 的元素的 value 值为 DELETE，并将 mGarbage 标记位值为 true。`gc()`方法调用的时候会回收 value 值为 DELETE 的值。

**gc()方法**

采用双指针清空 val != DELETED 的值。

```java
private void gc() {
    int n = mSize;
    int o = 0;
    int[] keys = mKeys;
    Object[] values = mValues;
    for (int i = 0; i < n; i++) {
        Object val = values[i];
        if (val != DELETED) {
            if (i != o) {
                keys[o] = keys[i];
                values[o] = val;
                values[i] = null;
            o++;
    mGarbage = false;
    mSize = o;
```

**get()方法**

```java
public E get(int key, E valueIfKeyNotFound) {
    int i = ContainerHelpers.binarySearch(mKeys, mSize, key);
    if (i < 0 || mValues[i] == DELETED) {
        return valueIfKeyNotFound;
    } else {
        return (E) mValues[i];
```
get()中的代码就比较简单了，通过二分查找获取到key的索引，通过该索引来获取到value

### 总结
了解了SparseArray的实现原理，就该来总结一下它与HashMap之间来比较的优缺点

优势：

- 避免了基本数据类型的装箱操作
- 不需要额外的结构体，单个元素的存储成本更低
- 数据量小的情况下，随机访问的效率更高

有优点就一定有缺点

- 插入操作需要复制数组，增删效率降低
- 数据量巨大时，复制数组成本巨大，gc()成本也巨大
 -数据量巨大时，查询效率也会明显下降






