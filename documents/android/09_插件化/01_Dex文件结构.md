## 一、DEX 文件的生成
在解析 `DEX` 文件结构之前，先来看看如何生成 `DEX` 文件。为了方便解析，本篇文章中就不从市场上的 `App` 里拿 `DEX` 文件过来解析了，而是手动生成一个最简单的 `DEX` 文件。还是以 `Class` 文件解析时候用的例子：

```java
public class Hello {

    private static String HELLO_WORLD = "Hello World!";

    public static void main(String[] args) {
        System.out.println(HELLO_WORLD);
    }
}
```

首先 `javac` 编译成 `Hello.class` 文件，然后利用 `Sdk` 自带的 `dx` 工具生成 `DEX` 文件：
```java
dx --dex --output=Hello.dex  Hello.class
```
`dx` 工具位于 `Sdk` 的 `build-tools` 目录下，可添加至环境变量方便调用。`dx` 也支持多 `Class` 文件生成 `dex`。

## 二、DEX 文件结构
### 1、概览
关于 DEX 文件结构的学习，给大家推荐两个资料。

第一个是看雪神图，出自非虫，

![image](https://user-images.githubusercontent.com/17560388/202062059-babdcf4b-82e4-4738-b7bb-e8c5dce712e9.png)


第二个是 `Android` 源码中对 DEX 文件格式的定义，[dalvik/libdex/DexFile.h](http://androidxref.com/9.0.0_r3/xref/dalvik/libdex/DexFile.h)，其中详细定义了 DEX 文件中的各个部分。

第三个是 010 Editor，在之前解析 AndroidManifest.xml 文件格式解析 也介绍过，它提供了丰富的文件模板，支持常见文件格式的解析，可以很方便的查看文件结构中的各个部分及其对应的十六进制。一般我在代码解析文件结构的时候都是对照着 010 Editor 来进行分析。下面贴一张 010 Editor 打开之前生成的 Hello.dex 文件的截图：

![image](https://user-images.githubusercontent.com/17560388/202062260-c7d7e76a-c773-4ab6-8f71-1da8485728b2.png)

第三个是 010 Editor，在之前解析 AndroidManifest.xml 文件格式解析 也介绍过，它提供了丰富的文件模板，支持常见文件格式的解析，可以很方便的查看文件结构中的各个部分及其对应的十六进制。一般我在代码解析文件结构的时候都是对照着 010 Editor 来进行分析。下面贴一张 010 Editor 打开之前生成的 Hello.dex 文件的截图：

我们可以一目了然的看到 DEX 的文件结构，着实是一个利器。在详细解析之前，我们先来大概给 DEX 文件分个层，如下图所示：

![image](https://user-images.githubusercontent.com/17560388/202062355-d0a4d10b-2a64-455c-97fc-c770f44a668b.png)

依次解释一下：

`header` : DEX 文件头，记录了一些当前文件的信息以及其他数据结构在文件中的偏移量
`string_ids` : 字符串的偏移量
`type_ids` : 类型信息的偏移量
`proto_ids` : 方法声明的偏移量
`field_ids` : 字段信息的偏移量
`method_ids` : 方法信息（所在类，方法声明以及方法名）的偏移量
`class_def` : 类信息的偏移量
`data` : ： 数据区
`link_data` : 静态链接数据区

从 `header` 到 `data` 之间都是偏移量数组，并不存储真实数据，所有数据都存在 `data` 数据区，根据其偏移量区查找。对 `DEX` 文件有了一个大概的认识之后，我们就来详细分析一下各个部分。
### 2、header

`DEX` 文件头部分的具体格式可以参考 DexFile.h 中的定义：
```c
struct DexHeader {
    u1  magic[8];           // 魔数
    u4  checksum;           // adler 校验值
    u1  signature[kSHA1DigestLen]; // sha1 校验值
    u4  fileSize;           // DEX 文件大小
    u4  headerSize;         // DEX 文件头大小
    u4  endianTag;          // 字节序
    u4  linkSize;           // 链接段大小
    u4  linkOff;            // 链接段的偏移量
    u4  mapOff;             // DexMapList 偏移量
    u4  stringIdsSize;      // DexStringId 个数
    u4  stringIdsOff;       // DexStringId 偏移量
    u4  typeIdsSize;        // DexTypeId 个数
    u4  typeIdsOff;         // DexTypeId 偏移量
    u4  protoIdsSize;       // DexProtoId 个数
    u4  protoIdsOff;        // DexProtoId 偏移量
    u4  fieldIdsSize;       // DexFieldId 个数
    u4  fieldIdsOff;        // DexFieldId 偏移量
    u4  methodIdsSize;      // DexMethodId 个数
    u4  methodIdsOff;       // DexMethodId 偏移量
    u4  classDefsSize;      // DexCLassDef 个数
    u4  classDefsOff;       // DexClassDef 偏移量
    u4  dataSize;           // 数据段大小
    u4  dataOff;            // 数据段偏移量
};
```
其中的 `u` 表示无符号数，`u1` 就是 `8` 位无符号数，`u4` 就是 `32` 位无符号数。  
`magic` 一般是常量，用来标记 `DEX` 文件，它可以分解为：
```css
文件标识 dex + 换行符 + DEX 版本 + 0
```
字符串格式为 `dex\n035\0`，十六进制为 `0x6465780A30333500`。

- `checksum` 是对去除 `magic` 、 `checksum` 以外的文件部分作 `alder32` 算法得到的校验值，用于判断 `DEX` 文件是否被篡改。
- `signature` 是对除去 `magic` 、 `checksum` 、 `signature` 以外的文件部分作 `sha1` 得到的文件哈希值。
- `endianTag` 用于标记 `DEX` 文件是大端表示还是小端表示。由于 `DEX` 文件是运行在 `Android` 系统中的，所以一般都是小端表示，这个值也是恒定值 `0x12345678`。
其余部分分别标记了 DEX 文件中其他各个数据结构的个数和其在数据区的偏移量。根据偏移量我们就可以轻松的获得各个数据结构的内容。下面顺着上面的 DEX 文件结构来认识第一个数据结构 

### 3、string_ids。
```c
struct DexStringId {
    u4 stringDataOff;
};
```
string_ids 是一个偏移量数组，stringDataOff 表示每个字符串在 data 区的偏移量。根据偏移量在 data 区拿到的数据中，第一个字节表示的是字符串长度，后面跟着的才是字符串数据。这块逻辑比较简单，直接看一下代码：
```java
private void parseDexString() {
    log("\nparse DexString");
    try {
        int stringIdsSize = dex.getDexHeader().string_ids__size;
        for (int i = 0; i < stringIdsSize; i++) {
            int string_data_off = reader.readInt();
            byte size = dexData[string_data_off]; // 第一个字节表示该字符串的长度，之后是字符串内容
            String string_data = new String(Utils.copy(dexData, string_data_off + 1, size));
            DexString string = new DexString(string_data_off, string_data);
            dexStrings.add(string);
            log("string[%d] data: %s", i, string.string_data);
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```
打印结果如下：
```shell
parse DexString
string[0] data: <clinit>
string[1] data: <init>
string[2] data: HELLO_WORLD
string[3] data: Hello World!
string[4] data: Hello.java
string[5] data: LHello;
string[6] data: Ljava/io/PrintStream;
string[7] data: Ljava/lang/Object;
string[8] data: Ljava/lang/String;
string[9] data: Ljava/lang/System;
string[10] data: V
string[11] data: VL
string[12] data: [Ljava/lang/String;
string[13] data: main
string[14] data: out
string[15] data: println
```
其中包含了变量名，方法名，文件名等等，这个字符串池在后面其他结构的解析中也会经常遇到。
### 4、type_ids
```java
struct DexTypeId {
    u4  descriptorIdx;
};
```
type_ids 表示的是类型信息，descriptorIdx 指向 string_ids 中元素。根据索引直接在上一步读取到的字符串池即可解析对应的类型信息，代码如下：
```java
private void parseDexType() {
    log("\nparse DexTypeId");
    try {
        int typeIdsSize = dex.getDexHeader().type_ids__size;
        for (int i = 0; i < typeIdsSize; i++) {
            int descriptor_idx = reader.readInt();
            DexTypeId dexTypeId = new DexTypeId(descriptor_idx, dexStringIds.get(descriptor_idx).string_data);
            dexTypeIds.add(dexTypeId);
            log("type[%d] data: %s", i, dexTypeId.string_data);
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```
解析结果：
```shell
parse DexType
type[0] data: LHello;
type[1] data: Ljava/io/PrintStream;
type[2] data: Ljava/lang/Object;
type[3] data: Ljava/lang/String;
type[4] data: Ljava/lang/System;
type[5] data: V
type[6] data: [Ljava/lang/String;
```
### 5、proto_ids
```c
struct DexProtoId {
    u4  shortyIdx;          /* index into stringIds for shorty descriptor */
    u4  returnTypeIdx;      /* index into typeIds list for return type */
    u4  parametersOff;      /* file offset to type_list for parameter types */
};
```
proto_ids 表示方法声明信息，它包含以下三个变量：

- `shortyIdx` : 指向 string_ids ，表示方法声明的字符串
- `returnTypeIdx` : 指向 type_ids ，表示方法的返回类型
- `parametersOff` ： 方法参数列表的偏移量

方法参数列表的数据结构在 DexFile.h 中用 DexTypeList 来表示：
```c
struct DexTypeList {
    u4  size;               /* #of entries in list */
    DexTypeItem list[1];    /* entries */
};

struct DexTypeItem {
    u2  typeIdx;            /* index into typeIds */
};
```
size 表示方法参数的个数，参数用 DexTypeItem 表示，它只有一个属性 typeIdx，指向 type_ids 中对应项。具体的解析代码如下：
```java
private void parseDexProto() {
    log("\nparse DexProto");
    try {
        int protoIdsSize = dex.getDexHeader().proto_ids__size;
        for (int i = 0; i < protoIdsSize; i++) {
            int shorty_idx = reader.readInt();
            int return_type_idx = reader.readInt();
            int parameters_off = reader.readInt();

            DexProtoId dexProtoId = new DexProtoId(shorty_idx, return_type_idx, parameters_off);
            log("proto[%d]: %s %s %d", i, dexStringIds.get(shorty_idx).string_data,
                    dexTypeIds.get(return_type_idx).string_data, parameters_off);

            if (parameters_off > 0) {
                parseDexProtoParameters(parameters_off);
            }

            dexProtos.add(dexProtoId);
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```java
解析结果：
```shell
parse DexProto
proto[0]: V V 0
proto[1]: VL V 412
parameters[0]: Ljava/lang/String;
proto[2]: VL V 420
parameters[0]: [Ljava/lang/String;
```
### 6、field_ids
```c
struct DexFieldId {
    u2  classIdx;           /* index into typeIds list for defining class */
    u2  typeIdx;            /* index into typeIds for field type */
    u4  nameIdx;            /* index into stringIds for field name */
};
```
field_ids 表示的是字段信息，指明了字段所在的类，字段的类型以及字段名称，在 DexFile.h 中定义为 DexFieldId , 其各个字段含义如下：

- `classIdx` : 指向 type_ids ，表示字段所在类的信息
- `typeIdx` : 指向 ype_ids ，表示字段的类型信息
- `nameIdx` : 指向 string_ids ，表示字段名称

代码解析很简单，就不贴出来了，直接看一下解析结果:
```shell
parse DexField
field[0]: LHello;->HELLO_WORLD;Ljava/lang/String;
field[1]: Ljava/lang/System;->out;Ljava/io/PrintStream;
```
### 7、method_ids
```c
struct DexMethodId {
    u2  classIdx;           /* index into typeIds list for defining class */
    u2  protoIdx;           /* index into protoIds for method prototype */
    u4  nameIdx;            /* index into stringIds for method name */
};
```
method_ids 指明了方法所在的类、方法声明以及方法名。在 DexFile.h 中用 DexMethodId 表示该项，其属性含义如下：

- `classIdx` : 指向 type_ids ，表示类的类型
- `protoIdx` : 指向 type_ids ，表示方法声明
- `nameIdx` : 指向 string_ids ，表示方法名

解析结果:
```shell
parse DexMethod
method[0]: LHello; proto[0] <clinit>
method[1]: LHello; proto[0] <init>
method[2]: LHello; proto[2] main
method[3]: Ljava/io/PrintStream; proto[1] println
method[4]: Ljava/lang/Object; proto[0] <init>
```
### 8、class_def
```c
struct DexClassDef {
    u4  classIdx;           /* index into typeIds for this class */
    u4  accessFlags;
    u4  superclassIdx;      /* index into typeIds for superclass */
    u4  interfacesOff;      /* file offset to DexTypeList */
    u4  sourceFileIdx;      /* index into stringIds for source file name */
    u4  annotationsOff;     /* file offset to annotations_directory_item */
    u4  classDataOff;       /* file offset to class_data_item */
    u4  staticValuesOff;    /* file offset to DexEncodedArray */
};
```
class_def 是 DEX 文件结构中最复杂也是最核心的部分，它表示了类的所有信息，对应 DexFile.h 中的 DexClassDef :

`classIdx` : 指向 type_ids ，表示类信息
`accessFlags` : 访问标识符
`superclassIdx` : 指向 type_ids ，表示父类信息
`interfacesOff` : 指向 DexTypeList 的偏移量，表示接口信息
`sourceFileIdx` : 指向 string_ids ，表示源文件名称
`annotationOff` : 注解信息
`classDataOff` : 指向 DexClassData 的偏移量，表示类的数据部分
`staticValueOff` :指向 DexEncodedArray 的偏移量，表示类的静态数据

#### DefCLassData

重点是 classDataOff 这个字段，它包含了一个类的核心数据，在 Android 源码中定义为 DexClassData ，它不在 DexFile.h 中了，而是在 DexClass.h 中：
```c
struct DexClassData {
    DexClassDataHeader header;
    DexField*          staticFields;
    DexField*          instanceFields;
    DexMethod*         directMethods;
    DexMethod*         virtualMethods;
};
```
`DexClassDataHeader` 定义了类中字段和方法的数目，它也定义在 `DexClass.h` 中：
```c
struct DexClassDataHeader {
    u4 staticFieldsSize;
    u4 instanceFieldsSize;
    u4 directMethodsSize;
    u4 virtualMethodsSize;
};
```
`staticFieldsSize` : 静态字段个数
`instanceFieldsSize` : 实例字段个数
`directMethodsSize` : 直接方法个数
`virtualMethodsSize` : 虚方法个数

在读取的时候要注意这里的数据是 LEB128 类型。它是一种可变长度类型，每个 LEB128 由 1~5 个字节组成，每个字节只有 7 个有效位。如果第一个字节的最高位为 1，表示需要继续使用第 2 个字节，如果第二个字节最高位为 1，表示需要继续使用第三个字节，依此类推，直到最后一个字节的最高位为 0，至多 5 个字节。除了 LEB128 以外，还有无符号类型 ULEB128。

那么为什么要使用这种数据结构呢？我们都知道 Java 中 int 类型都是 4 字节，32 位的，但是很多时候根本用不到 4 个字节，用这种可变长度的结构，可以节省空间。对于运行在 Android 系统上来说，能多省一点空间肯定是好的。下面给出了 Java 读取 ULEB128 的代码：

```java
public static int readUnsignedLeb128(byte[] src, int offset) {
    int result = 0;
    int count = 0;
    int cur;
    do {
        cur = copy(src, offset, 1)[0];
        cur &= 0xff;
        result |= (cur & 0x7f) << count * 7;
        count++;
        offset++;
        DexParser.POSITION++;
    } while ((cur & 0x80) == 128 && count < 5);
    return result;
}
```
继续回到 `DexClassData` 中来。`header` 部分定义了各种字段和方法的个数，后面跟着的分别就是 `静态字段` 、`实例字段` 、`直接方法` 、`虚方法` 的具体数据了。字段用 `DexField` 表示，方法用 `DexMethod` 表示。

#### DexField
```c
struct DexField {
    u4 fieldIdx;    /* index to a field_id_item */
    u4 accessFlags;
};
```
`fieldIdx` : 指向 field_ids ，表示字段信息
`accessFlags` ：访问标识符

#### DexMethod
```c
struct DexMethod {
    u4 methodIdx;    /* index to a method_id_item */
    u4 accessFlags;
    u4 codeOff;      /* file offset to a code_item */
};
```
method_idx 是指向 method_ids 的索引，表示方法信息。accessFlags 是该方法的访问标识符。codeOff 是结构体 DexCode 的偏移量。如果你坚持看到了这里，是不是发现说到现在还没说到最重要的东西，DEX 包含的代码，或者说指令，对应的就是 Hello.java 中的 main 方法。没错，DexCode 就是用来存储方法的详细信息以及其中的指令的。
```c
struct DexCode {
    u2  registersSize;  // 寄存器个数
    u2  insSize;        // 参数的个数
    u2  outsSize;       // 调用其他方法时使用的寄存器个数
    u2  triesSize;      // try/catch 语句个数
    u4  debugInfoOff;   // debug 信息的偏移量
    u4  insnsSize;      // 指令集的个数
    u2  insns[1];       // 指令集
    /* followed by optional u2 padding */  // 2 字节，用于对齐
    /* followed by try_item[triesSize] */
    /* followed by uleb128 handlersSize */
    /* followed by catch_handler_item[handlersSize] */
};
```
我们打开 010 Editor，定位到 main() 方法对应的 DexCode，对照进行分析：
```java
public class Hello {

    private static String HELLO_WORLD = "Hello World!";

    public static void main(String[] args) {
        System.out.println(HELLO_WORLD);
    }
}
```
main() 方法对应的 DexCode 十六进制表示为 ：
```shell
03 00 01 00 02 00 00 00 79 02 00 00 08 00 00 00
62 00 01 00 62 01 00 00 6E 20 03 00 10 00 0E 00
```
使用的寄存器个数是 3 个。参数个数是 1 个，就是 main() 方法中的 String[] args。调用外部方法时使用的寄存器个数为 2 个。指令个数是 8 。

终于说到指令了，main() 函数中有 8 条指令，就是上面十六进制中的第二行。尝试来解析一下这段指令。Android 官网就有 Dalvik 指令的相关介绍，链接。

第一个指令 `62 00 01 00`，查询文档 `62` 对应指令为 `sget-object vAA`, field@BBBB，AA 对应 00 , 表示 v0 寄存器。BBBB 对应 01 00 ，表示 field_ids 中索引为 1 的字段，根据前面的解析结果该字段为 `Ljava/lang/System;->out;Ljava/io/PrintStream`，整理一下，62 00 01 00 表示的就是：
```shell
sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
```
接着是 62 01 00 00。还是 sget-object vAA, field@BBBB, AA 对应 01 ，BBBB 对应 0000, 使用的是 v1 寄存器，field 位 field_ids 中索引为 0 的字段，即 LHello;->HELLO_WORLD;Ljava/lang/String，该句完整指令为：
```shell
sget-object v1, LHello;->HELLO_WORLD:Ljava/lang/String;
```
接着是 6E 20 03 00, 查看文档 6E 指令为 invoke-virtual {vC, vD, vE, vF, vG}, meth@BBBB。6E 后面一个十六位 2 表示调用方法是两个参数，那么 BBBB 就是 03 00，指向 method_ids 中索引为 3 方法。根据前面的解析结果，该方法就是 Ljava/io/PrintStream;->println(Ljava/lang/String;)V。完整指令为：
```shell
invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
```
最后的 0E，查看文档该指令为 return-void，到这 main() 方法就结束了。

将上面几句指令放在一起:
```shell
62 00 01 00 : sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
62 01 00 00 : sget-object v1, LHello;->HELLO_WORLD:Ljava/lang/String;
6E 20 03 00 : invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
OE OO : return-void
```
这就是 main() 方法的完整指令了。

## 参考 
- [Android逆向笔记 —— DEX 文件格式解析](https://juejin.cn/post/6844903847647772686)
- [一篇文章带你搞懂DEX文件的结构](https://blog.csdn.net/sinat_18268881/article/details/55832757)
