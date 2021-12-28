### 1.LruCache的使用

Least Recently Used

初始化
```java
LruCache<String, Bitmap> mLruCache;
//获取手机最大内存 单位 kb
int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
//一般都将1/8设为LruCache的最大缓存
int cacheSize = maxMemory / 8;
mLruCache = new LruCache<String, Bitmap>(maxMemory / 8) {
    @Override
    protected int sizeOf(String key, Bitmap value) {
        //返回图片Size的大小
        return value.getByteCount() / 1024;
    }
};
```
使用
```java
//加入缓存
mLruCache.put("key", BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
//从缓存中读取
Bitmap bitmap = mLruCache.get("key");
```

### 2.LruCache 分析

构造方法，maxSize 为传入的 LruCache 存储的最大容量。内部采用 LinkedHashMap 的 aceessOrder 为 true 实现的。
```java
    public LruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<K, V>(0, 0.75f, true);
    }
```
这里注意一下 trimToSize() 方法的使用即可。
```java
    public void resize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }

        synchronized (this) {
            this.maxSize = maxSize;
        }
        trimToSize(maxSize);
    }
```
#### get()方法
通过key获取缓存的数据，如果通过这个方法得到的需要的元素，那么这个元素会被放在缓存队列的头部，可以理解成最近常用的元素，不会在缓存空间不够的时候自动清理掉
```java
    public final V get(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        V mapValue;
        //这里用同步代码块，
        synchronized (this) {
            //从LinkedHashMap中获取数据。
            mapValue = map.get(key);
            if (mapValue != null) {
                hitCount++;
                return mapValue;
            }
            missCount++;
        }

        /**
         * 如果通过key从缓存集合中获取不到缓存数据，就尝试使用creat(key)方法创造一个新数据。
         * create(key)默认返回的也是null，需要的时候可以重写这个方法。
         */
        V createdValue = create(key);
        if (createdValue == null) {
            return null;
        }

        //如果重写了create(key)方法，创建了新的数据，就讲新数据放入缓存中。
        synchronized (this) {
            createCount++;
            mapValue = map.put(key, createdValue);

            if (mapValue != null) {
                // There was a conflict so undo that last put
                map.put(key, mapValue);
            } else {
                size += safeSizeOf(key, createdValue);
            }
        }

        if (mapValue != null) {
            entryRemoved(false, key, createdValue, mapValue);
            return mapValue;
        } else {
            trimToSize(maxSize);
            return createdValue;
        }
    }
```
#### put()方法
往缓存中添加数据
```java
    public final V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }

        V previous;
        synchronized (this) {
            putCount++;
            //safeSizeOf(key, value)。
            // 这个方法返回的是1，也就是将缓存的个数加1.
            // 当缓存的是图片的时候，这个size应该表示图片占用的内存的大小，
            // 所以应该重写里面调用的sizeOf(key, value)方法
            size += safeSizeOf(key, value);
            //将创建的新元素添加进缓存队列，并添加成功后返回这个元素
            previous = map.put(key, value);
            if (previous != null) {
                //如果返回的是null，说明添加缓存失败，在已用缓存大小中减去这个元素的大小。
                size -= safeSizeOf(key, previous);
            }
        }

        if (previous != null) {
            entryRemoved(false, key, previous, value);
        }

        trimToSize(maxSize);
        return previous;
    }
```
#### trimToSize() 方法

修改缓存大小，使已用的缓存不大于设置的缓存最大值，核心代码 map.eldest() 元素的 remove.

```java
    public void trimToSize(int maxSize) {
        while (true) { //开启一个死循环
            K key;
            V value;
            synchronized (this) {
                if (size < 0 || (map.isEmpty() && size != 0)) {
                    throw new IllegalStateException(getClass().getName()
                            + ".sizeOf() is reporting inconsistent results!");
                }

                //当已用的缓存小于最大缓存，完成任务，退出循环
                if (size <= maxSize) {
                    break;
                }

                //否则就在缓存队列中先找到最近最少使用的元素，调用LinkedHashMap的eldest()方法返回最不经常使用的方法。
                Map.Entry<K, V> toEvict = map.eldest();
                if (toEvict == null) {
                    break;
                }
                //然后删掉这个元素，并减少已使用的缓存空间
                key = toEvict.getKey();
                value = toEvict.getValue();
                map.remove(key);
                size -= safeSizeOf(key, value);
                evictionCount++;
            }

            entryRemoved(true, key, value, null);
        }
    }
```
#### remove()方法
```java
    public final V remove(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        V previous;
        synchronized (this) {
            previous = map.remove(key);
            if (previous != null) {
                size -= safeSizeOf(key, previous);
            }
        }

        if (previous != null) {
            entryRemoved(false, key, previous, null);
        }

        return previous;
    }
```
#### entryRemoved()方法
这个方法在前面很多地方都会被调用，默认是空方法，有需要的时候自己实现evicted如果是true，则表示这个元素是因为空间不够而被自动清理了，所以可以在这个地方对这个被清理的元素进行再次缓存
```java
    protected void entryRemoved(boolean evicted, K key, V oldValue, V newValue) {}
```
#### create()方法
一个空方法，也是在需要的时候重写实现
```java
    protected V create(K key) {
        return null;
    }
```
```java
    private int safeSizeOf(K key, V value) {
        int result = sizeOf(key, value);
        if (result < 0) {
            throw new IllegalStateException("Negative size: " + key + "=" + value);
        }
        return result;
    }
```
#### sizeOf()方法
这个方法可以说是用来定义已用缓存的数量算法，默认是返回数量
```java
    protected int sizeOf(K key, V value) {
        return 1;
    }
```    
#### evictAll() 清空所有缓存
```java
    public final void evictAll() {
        trimToSize(-1); // -1 will evict 0-sized elements
    }
```



### 3.为什么用LinkedHashMap

LinkedHashMap初始化时，accessOrder = true的时候，数据在被访问的时候进行排序，将最近访问的数据放到集合的最后面。

#### 1.构造方法
LinkedHashMap有个构造方法是这样的：
```java
    /**
     * Constructs an empty <tt>LinkedHashMap</tt> instance with the
     * specified initial capacity, load factor and ordering mode.
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @param  accessOrder     the ordering mode - <tt>true</tt> for
     *         access-order, <tt>false</tt> for insertion-order
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    public LinkedHashMap(int initialCapacity,
                         float loadFactor,
                         boolean accessOrder) {
        super(initialCapacity, loadFactor);
        this.accessOrder = accessOrder;
    }
```
#### 2.Entity的定义
LinkedHashMap内部是使用**双向循环链表**来存储数据的。也就是每一个元素都持有他上一个元素的地址和下一个元素的地址，看Entity的定义：
```java
    /**
     * LinkedHashMap entry.
     */
    private static class LinkedHashMapEntry<K,V> extends HashMapEntry<K,V> {
        // These fields comprise the doubly linked list used for iteration.
        LinkedHashMapEntry<K,V> before, after;

        LinkedHashMapEntry(int hash, K key, V value, HashMapEntry<K,V> next) {
            super(hash, key, value, next);
        }

        /**
         * 从链表中删除这个元素
         */
        private void remove() {
            before.after = after;
            after.before = before;
        }
        
        /**
         * Inserts this entry before the specified existing entry in the list.
         */
        private void addBefore(LinkedHashMapEntry<K,V> existingEntry) {
            after  = existingEntry;
            before = existingEntry.before;
            before.after = this;
            after.before = this;
        }

        /**
         * 当集合的get方法被调用时，会调用这个方法。
         * 如果accessOrder为true，就把这个元素放在集合的最末端。
         */
        void recordAccess(HashMap<K,V> m) {
            LinkedHashMap<K,V> lm = (LinkedHashMap<K,V>)m;
            if (lm.accessOrder) {
                lm.modCount++;
                remove();
                addBefore(lm.header);
            }
        }

        void recordRemoval(HashMap<K,V> m) {
            remove();
        }
    }
```
#### 3.get方法的排序过程

看LinkedHashMap的get方法：
```java
public V get(Object key) {
        LinkedHashMapEntry<K,V> e = (LinkedHashMapEntry<K,V>)getEntry(key);
        if (e == null)
            return null;
        e.recordAccess(this);
        return e.value;
    }
```
当LinkedHashMap初始化的时候，会有一个头节点header。
```java
    void init() {
        header = new LinkedHashMapEntry<>(-1, null, null, null);
        header.before = header.after = header;
    }
```
#### 4.元素的添加过程

<img src="https://user-images.githubusercontent.com/17560388/132780867-48430d0a-db54-49a7-a647-b485a2001586.png" alt="图片替换文本" width="180"  align="bottom" />

<img src="https://user-images.githubusercontent.com/17560388/132780841-3a7ce00c-d140-4701-9f1c-dac778d4ae04.png" alt="图片替换文本" width="400"  align="bottom" />

<img src="https://user-images.githubusercontent.com/17560388/132780935-e5d04ff9-41bd-49b8-a4e2-0f4b3667526e.png" alt="图片替换文本" width="650"  align="bottom" />

<img src="https://user-images.githubusercontent.com/17560388/132780989-6e8d0658-64ed-44bd-8242-5b7d047bb610.png" alt="图片替换文本" width="800"  align="bottom" />

#### 5.recordAccess()方法模拟

<img src="https://user-images.githubusercontent.com/17560388/132781861-479b887b-a03b-4f56-a671-3f50d1a744fa.png" alt="图片替换文本" width="850"  align="bottom" />

<img src="https://user-images.githubusercontent.com/17560388/132781876-b18c223b-8451-4167-843e-7ec66285e8b8.png" alt="图片替换文本" width="950"  align="bottom" />

#### 6.eldest() 获取最近最少使用的元素

LinkedHashMap还有一个方法eldest()，提供的就是最近最少使用的元素：

```java
    public Map.Entry<K, V> eldest() {
        Entry<K, V> eldest = header.after;
        return eldest != header ? eldest : null;
    }
```  
