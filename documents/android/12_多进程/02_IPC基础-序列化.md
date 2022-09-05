### Serializable vs Parcelable

这两者都是 Android 的序列化方式:

Serializable 的底层是通过 IO 来实现的，序列化是通过 ObjectOutputStream 来实现，比如字符串的序列化方法，就是标准的 IO 方法

java.io.ObjectOutputStream

```java
/**                                                                        
 * Writes given string to stream, using standard or long UTF format        
 * depending on string length.                                             
 */                                                                        
private void writeString(String str, boolean unshared) throws IOException {
    handles.assign(unshared ? null : str);                                 
    long utflen = bout.getUTFLength(str);                                  
    if (utflen <= 0xFFFF) {                                                
        bout.writeByte(TC_STRING);                                         
        bout.writeUTF(str, utflen);                                        
    } else {                                                               
        bout.writeByte(TC_LONGSTRING);                                     
        bout.writeLongUTF(str, utflen);                                    
    }                                                                      
}
```

Parcelable 的内部，是通过 Parcel 来实现的，本质是 native 层的共享内存，不涉及IO，性能更好，在 Android 中尽量避免使用 Serializable 来序列化

#### parcel 的 Java 层只是一个壳

先看下Java层的代码，首先通过 Parce.obtain() 来获取一个 parcel 对象

```java
/**                                                           
 * Retrieve a new Parcel object from the pool.                
 */                                                           
@NonNull                                                      
public static Parcel obtain() {                               
    final Parcel[] pool = sOwnedPool;                         
    synchronized (pool) {                                     
        Parcel p;                                             
        for (int i=0; i<POOL_SIZE; i++) {                     
            p = pool[i];                                      
            if (p != null) {                                  
                pool[i] = null;                     
                p.mReadWriteHelper = ReadWriteHelper.DEFAULT; 
                return p;                                     
            }                                                 
        }                                                     
    }                                                         
    return new Parcel(0);                                     
}
```

有一个缓存池，复用 Parcel 对象，第一次调用，返回新建的 Parcel 对象

```java
private Parcel(long nativePtr) { 
    init(nativePtr);                                                       
}                                                                          
                                                                           
private void init(long nativePtr) {                                        
    if (nativePtr != 0) {                                                  
        mNativePtr = nativePtr;                                            
        mOwnsNativeParcelObject = false;                                   
    } else {                                                               
        mNativePtr = nativeCreate();                                       
        mOwnsNativeParcelObject = true;                                    
    }                                                                      
}
```

走到了底层的nativeCreate方法

另外看下parcel Java层的其他方法，比如写入跟读取int、long

```java
public final void writeInt(int val) {                                       
    nativeWriteInt(mNativePtr, val);                                        
}                                                                           
                                                                                                                                                
public final void writeLong(long val) {                                     
    nativeWriteLong(mNativePtr, val);                                       
}  
```

可以发现，parcel 的 Java 层，只是一个壳，具体的实现，都是在native层处理的

### native层Parcel的初始化

```c++
#frameworks/base/core/jni/android_os_Parcel.cpp

static jlong android_os_Parcel_create(JNIEnv* env, jclass clazz)
{
    Parcel* parcel = new Parcel();
    //返回的是parcel指针的long值
    return reinterpret_cast<jlong>(parcel);
}
```

parcel* 是指针类型，reinterpret_cast 是返回这个指针转成 long 类型的值

对于 reinterpret_cast 的这个特性，可以用下面这个简单的 demo 验证下

```c++
using namespace std;   
int main() {                                           
    auto *data = new Sales_data();                     
    auto lValue = reinterpret_cast<long>(data);        
    cout << "hex value " << data << " long value " << lValue;
                                                       
    return EXIT_SUCCESS;                               
}
```

运行后的结果 hex value 0x7f9c184059a0 long value 140308398496160

16进制的值 0x7f9c184059a0 转成 10 进制，刚好就是 140308398496160，所以 nativeCreate() 方法，返回的值，就是这个 parcel 对象指针的值（也就是在内存中的位置）

**parcel 的本质其实是一个连续的内存空间**

### 先看下parcel的一些本地变量

```c++
#frameworks/native/libs/binder/include/binder/Parcel.h

uint8_t*            mData;     //内存空间的位置指针
size_t              mDataSize; //当前保存的内容大小
size_t              mDataCapacity;//总的容量大小
mutable size_t      mDataPos;  //当前位置的偏移量
```

mData就是保存内容的地方，其在这里赋值

```c++
#frameworks/native/libs/binder/Parcel.cpp

uint8_t* data = (uint8_t*)malloc(desired);
if (!data) {
    mError = NO_MEMORY;
    return NO_MEMORY;
}
mData = data;
```

malloc 方法，开辟了一个长度为 desired 的连续空间，mData 就是指向这个内存空间的指针，当这个连续空间不足的时候，就会扩容，开辟更大的空间

```c++
status_t Parcel::growData(size_t len)
{
    if (len > SIZE_MAX - mDataSize) return NO_MEMORY; // overflow
    if (mDataSize + len > SIZE_MAX / 3) return NO_MEMORY; // overflow
    size_t newSize = ((mDataSize+len)*3)/2;
    return (newSize <= mDataSize)
            ? (status_t) NO_MEMORY
            : continueWrite(std::max(newSize, (size_t) 128));
}
```

可以看到，新的空间大小，是按照现有的内容大小的 1.5 倍扩容

### 写入一个int值

Java层是调用的nativeWriteInt方法，实际走到了下面这里

```c++
static void android_os_Parcel_writeInt(JNIEnv* env, jclass clazz, jlong nativePtr, jint val) {
    Parcel* parcel = reinterpret_cast<Parcel*>(nativePtr);
    //nativePtr其实就是parcel指针的值，可以转成parcel指针
    if (parcel != NULL) {
        const status_t err = parcel->writeInt32(val);
        //parcel指针调用parcel的writeInt32方法
        if (err != NO_ERROR) {
            signalExceptionForError(env, clazz, err);
        }
    }
}

status_t Parcel::writeInt32(int32_t val)
{   //继续往下调用
    return writeAligned(val);
}

template<class T>
status_t Parcel::writeAligned(T val) {
    static_assert(PAD_SIZE_UNSAFE(sizeof(T)) == sizeof(T));

    if ((mDataPos+sizeof(val)) <= mDataCapacity) {
restart_write:
        *reinterpret_cast<T*>(mData+mDataPos) = val;
        //把val的值写入当前指针mDataPos偏移量位置
        return finishWrite(sizeof(val));
    }

    status_t err = growData(sizeof(val));
    if (err == NO_ERROR) goto restart_write;
    return err;
}

status_t Parcel::finishWrite(size_t len)
{
    //重新更新mDataPos的偏移量
    mDataPos += len;
    if (mDataPos > mDataSize) {
        mDataSize = mDataPos;
    }
    return NO_ERROR;
}
```

int的写入，先在当前偏移量的位置，写入int值，然后再更新偏移量mDataPos到int值后面

### 写入一个string

由于string的长度是不固定的，需要先写入 string 的长度，然后再写入 string 的内容

```c++
# frameworks/native/libs/binder/Parcel.cpp

status_t Parcel::writeString16(const String16& str)
{
    return writeString16(str.string(), str.size());
}

status_t Parcel::writeString16(const char16_t* str, size_t len)
{
    if (str == nullptr) return writeInt32(-1);

    // 先写入string的长度
    status_t err = writeInt32(len);
    if (err == NO_ERROR) {
        len *= sizeof(char16_t);
        //len是指string占用内存空间的长度，writeInplace返回合适的写入的位置
        uint8_t* data = (uint8_t*)writeInplace(len+sizeof(char16_t));
        //len后面要加sizeof(char16_t)，因为字符串的结尾，系统会默认加一个'\0'的结束符
        if (data) {
            memcpy(data, str, len);
            //mencpy拷贝字符串到内存中
            *reinterpret_cast<char16_t*>(data+len) = 0;
            return NO_ERROR;
        }
        err = mError;
    }
    return err;
}

void* Parcel::writeInplace(size_t len)
{
    if (len > INT32_MAX) {
        return nullptr;
    }

    //pad_size代表字符串占用的内存空间大小，下面专门说明
    const size_t padded = pad_size(len);

    if ((mDataPos+padded) <= mDataCapacity) {
restart_write:
        //printf("Writing %ld bytes, padded to %ld\n", len, padded);
        uint8_t* const data = mData+mDataPos;
        finishWrite(padded);
        //返回的data，就是strign要写入的位置的指针
        return data;
    }

    status_t err = growData(padded);
    if (err == NO_ERROR) goto restart_write;
    return nullptr;
}
```

字符串的写入，也是往内存中写，跟int一样，都是在同个内存空间操作，其他类型的写入，包括boolean、long等，都差不多

这里专门说下 pad_size 方法

```c++
#define PAD_SIZE_UNSAFE(s) (((s)+3)&~3)

static size_t pad_size(size_t s) {
    return PAD_SIZE_UNSAFE(s);
}
```

因为写入的空间，必须以4对齐，就是4个字节，作为最小单位，比如 s = 3, padSize = 4; s = 4, pasSize = 4; s = 5, padSize = 8; 正常这种，也可以用余数的来计算 (s+3)/4，而源码用((s)+3)&~3来计算，采用纯粹的位运算，更高效，更有逼格，这里还可以拓展，计算跟4、8、16、32等的除数和余数，都可以采用这种位运算

比如要计算 78 跟 8 的余数，可以这样写 78&7

### 读取一个字符串

看下源码是如何读取的

android.os.parcel.java

```java
public final String readString() {              
    return mReadWriteHelper.readString(this);   
}

public static class ReadWriteHelper {                                                
    public static final ReadWriteHelper DEFAULT = new ReadWriteHelper();                                                            
    public String readString(Parcel p) {                                             
        return nativeReadString(p.mNativePtr);                                       
    }                                                                             
}                                                                                    
```

```c++
# frameworks/native/libs/binder/Parcel.cpp

static jstring android_os_Parcel_readString8(JNIEnv* env, jclass clazz, jlong nativePtr)
{
    Parcel* parcel = reinterpret_cast<Parcel*>(nativePtr);
    //一样的，通过指针的long类型值转成parcel指针
    if (parcel != NULL) {
        size_t len;
        //这里是实际读取的地方
        const char* str = parcel->readString8Inplace(&len);
        if (str) {
            return env->NewStringUTF(str);
        }
        return NULL;
    }
    return NULL;
}

const char* Parcel::readString8Inplace(size_t* outLen) const
{
    int32_t size = readInt32();
    // size就是读取的string的长度
    if (size >= 0 && size < INT32_MAX) {
        *outLen = size;
        //size+1是因为写入string的时候，默认的尾部'\0'也写入了
        const char* str = (const char*)readInplace(size+1);
        if (str != nullptr) {
            if (str[size] == '\0') {
                //代表是正确的string
                return str;
            }
            android_errorWriteLog(0x534e4554, "172655291");
        }
    }
    *outLen = 0;
    return nullptr;
}
```

读上面源码可以发现，parcel的写跟读，都是按照顺序去操作的，所以这里也可以解释，为什么在实现 parcelable 接口的时候，writeToParcel 和 createFromParcel 顺序一定要相匹配，比如先写入 int，再写入 strign，读取的时候，也要先读取 int，再读取 string

### parcel作为IPC通信的数据媒介

不同的进程，数据本身是无法互通的，parcel的数据虽然是存在native层，属于用户空间，也是不能直接跟其他进程直接通信的

Android是基于Linux系统，有一个mmap函数，可以把用户空间映射到内核空间，在用户空间的修改直接映射到内核空间，内核空间全局只有一个，
为所有进程共享，从而可以实现跨进程通信，微信开源的MMKV也是基于类似的机制实现的

### 参考

https://zhuanlan.zhihu.com/p/402790867
