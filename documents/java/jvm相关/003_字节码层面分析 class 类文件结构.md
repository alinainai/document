本课时我们从字节码层面分析 class 类文件结构。首先来看一道面试题：

> java中 String 字符串的长度有限制吗？

平时项目开发中，我们经常会用到 String 来声明字符串，比如 String str = “abc”， 但是你可能从来没有想过等于号之后的字符串常量到底有没有长度限制。要彻底答对这道题，就需要先学会今天所讲的内容——class 文件。

### 1.class 的来龙去脉

Java 能够实现"一次编译，到处运行”，这其中 class 文件要占大部分功劳。为了让 Java 语言具有良好的跨平台能力，Java 独具匠心的提供了一种可以在所有平台上都能使用的一种中间代码——字节码类文件（.class文件）。有了字节码，无论是哪种平台（如：Mac、Windows、Linux 等），只要安装了虚拟机都可以直接运行字节码。

并且，**有了字节码，也解除了 Java 虚拟机和 Java 语言之间的耦合。**这句话你可能不是很理解，这种解耦指的是什么？

其实，Java 虚拟机当初被设计出来的目的就不单单是只运行 Java 这一种语言。目前 Java 虚拟机已经可以支持很多除 Java 语言以外的其他语言了，如 Groovy、JRuby、Jython、Scala 等。之所以可以支持其他语言，是因为这些语言经过编译之后也可以生成能够被 JVM 解析并执行的字节码文件。而虚拟机并不关心字节码是由哪种语言编译而来的。如下图所示：

<img src="https://user-images.githubusercontent.com/17560388/174241685-fb9ed6e7-ba72-4804-8ecf-27ada460e34d.png" alt="图片替换文本" width="400"  align="center" />

### 2.上帝视角看 class 文件
如果从纵观的角度来看 class 文件，class 文件里只有两种数据结构：无符号数和表。

- 无符号数：属于基本的数据类型，以 u1、u2、u4、u8 来分别代表 1 个字节、2 个字节、4 个字节和 8 个字节的无符号数，无符号数可以用来描述数字、索引引用、数量值或者字符串（UTF-8 编码）。

- 表：表是由多个无符号数或者其他表作为数据项构成的复合数据类型，class文件中所有的表都以“_info”结尾。其实，整个 Class 文件本质上就是一张表。

这两者之间的关系可以用下面这张张图来表示：

<img src="https://user-images.githubusercontent.com/17560388/150632505-79642084-ac98-4ca6-9ef0-0907cd05aac3.png" alt="图片替换文本" width="400"  align="bottom" />

可以看出，在一张表中可以包含其他无符号数和其他表格。伪代码可以如下所示：

```java
// 无符号数
u1 = byte[1];
u2 = byte[2];
u4 = byte[4];
u8 = byte[8];

// 表
class_table {
    // 表中可以引用各种无符号数，
    u1 tag;
    u2 index2;
    ...

    // 表中也可以引用其它表
    method_table mt;
    ...
}
```

### 3.class 文件结构

刚才我们说在 class 文件中只存在无符号数和表这两种数据结构。而这些无符号数和表就组成了 class 中的各个结构。这些结构按照预先规定好的顺序紧密的从前向后排列，相邻的项之间没有任何间隙。如下图所示：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174242032-4b094b6c-b56e-45f1-acf7-926e93e03176.png">

当 JVM 加载某个 class 文件时，JVM 就是根据上图中的结构去解析 class 文件，加载 class 文件到内存中，并在内存中分配相应的空间。具体某一种结构需要占用大多空间，可以参考下图：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174242044-8712777b-d12b-469a-b5b6-088fa2502a1b.png">

> 看到这里你可能会有点概念混淆，分不清无符号数、表格以及上面的结构是什么关系。其实可以举一个简单的例子：人类的身体是由 H、O、C、N 等元素组成的。但是这些元素又是按照一定的规律组成了人类身体的各个器官。class 文件中的无符号数和表格就相当于人类身体中的 H、O、C、N 等元素，而 class 结构图中的各项结构就相当于人类身体的各个器官。并且这些器官的组织顺序是有严格顺序要求的，毕竟眼睛不能长在屁股上。

### 4.实例分析

理清这些概念之后，接下来通过一个 Java 代码实例，来看一下上面这几个结构的详细情况。首先编写一个简单的 Java 源代码 Test.java，如下所示：

```java
import java.io.Serializable;
 
public class Test implements Serializable, Cloneable{
      private int num = 1;
 
      public int add(int i) {
          int j = 10;
          num = num + i;
          return num;
     }
}
```
通过 javac 将其编译，生成 Test.class 字节码文件。然后使用 16 进制编辑器打开 class 文件，显示内容如下所示：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174242793-0a70d14f-8a84-4040-8568-e14ca82ac7ce.png">

上图中都是一些 16 进制数字，每两个字符代表一个字节。乍看一下各个字符之间毫无规律，但是在 JVM 的视角里这些 16 进制字符是按照严格的规律排列的。接下来就一步一步看下 JVM 是如何解析它们的。

#### 4.1 魔数 magic number

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174243033-29006d69-98ac-416e-9799-8a627e758de7.png">

如上图所示，在 class 文件开头的四个字节是 class 文件的魔数，它是一个固定的值--0XCAFEBABE。魔数是 class 文件的标志，也就是说它是判断一个文件是不是 class 格式文件的标准， 如果开头四个字节不是 0XCAFEBABE， 那么就说明它不是 class 文件， 不能被 JVM 识别或加载。

#### 4.2 版本号

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174243057-41c154c1-2fb2-474c-9816-e61afdfaae4f.png">

紧跟在魔数后面的四个字节代表当前 class 文件的版本号。前两个字节 0000 代表次版本号（minor_version），后两个字节 0034 是主版本号（major_version），对应的十进制值为 52，也就是说当前 class 文件的主版本号为 52，次版本号为 0。所以综合版本号是 52.0，也就是  jdk1.8.0

#### 4.3 常量池（重点）

紧跟在版本号之后的是一个叫作常量池的表（cp_info）。在常量池中保存了类的各种相关信息，比如类的名称、父类的名称、类中的方法名、参数名称、参数类型等，这些信息都是以各种表的形式保存在常量池中的。

常量池中的每一项都是一个表，其项目类型共有 14 种，如下表所示：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174243172-db8fa569-7328-4dff-902c-94548332a98c.png">

可以看出，常量池中的每一项都会有一个 u1 大小的 tag 值。tag 值是表的标识，JVM 解析 class 文件时，通过这个值来判断当前数据结构是哪一种表。以上 14 种表都有自己的结构，这里不再一一介绍，就以 CONSTANT_Class_info 和 CONSTANT_Utf8_info 这两张表举例说明，因为其他表也基本类似。

首先，CONSTANT_Class_info 表具体结构如下所示：

```java
table CONSTANT_Class_info {
    u1  tag = 7;
    u2  name_index;
}
```
解释说明。
- tag：占用一个字节大小。比如值为 7，说明是 CONSTANT_Class_info 类型表。
- name_index：是一个索引值，可以将它理解为一个指针，指向常量池中索引为 name_index 的常量表。比如 name_index = 2，则它指向常量池中第 2 个常量。

接下来再看 CONSTANT_Utf8_info 表具体结构如下：

```java
table CONSTANT_utf8_info {
    u1  tag;
    u2  length;
    u1[] bytes;
}    
```

解释说明：
- tag：值为1，表示是 CONSTANT_Utf8_info 类型表。
- length：length 表示 u1[] 的长度，比如 length=5，则表示接下来的数据是 5 个连续的 u1 类型数据。
- bytes：u1 类型数组，长度为上面第 2 个参数 length 的值。

而我们在java代码中声明的String字符串最终在class文件中的存储格式就 CONSTANT_utf8_info。因此一个字符串最大长度也就是u2所能代表的最大值65536个，但是需要使用2个字节来保存 null 值，因此一个字符串的最大长度为 65536 - 2 = 65534。[参考 Java String最大长度分析](https://mp.weixin.qq.com/s/I16BlY9cJF-JZZReAjuRqg)。

不难看出，在常量池内部的表中也有相互之间的引用。用一张图来理解 CONSTANT_Class_info 和 CONSTANT_utf8_info 表格之间的关系，如下图所示：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174243237-7365ae99-0d71-49a2-985f-da4fcee8621f.png">

理解了常量池内部的数据结构之后，接下来就看一下实例代码的解析过程。因为开发者平时定义的 Java 类各式各样，类中的方法与参数也不尽相同。所以常量池的元素数量也就无法固定，因此 class 文件在常量池的前面使用 2 个字节的容量计数器，用来代表当前类中常量池的大小。如下图所示：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174243294-af3a7091-0133-449a-9d99-69481803566d.png">

红色框中的 001d 转化为十进制就是 29，也就是说常量计数器的值为 29。其中下标为 0 的常量被 JVM 留作其他特殊用途，因此 Test.class 中实际的常量池大小为这个计数器的值减 1，也就是 28个。

第一个常量，如下所示：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174243358-880c277a-a135-4887-9879-ab34507fa7f7.png">

0a 转化为 10 进制后为 10，通过查看常量池 14 种表格图中，可以查到 tag=10 的表类型为 CONSTANT_Methodref_info，因此常量池中的第一个常量类型为方法引用表。其结构如下：

```java
CONSTANT_Methodref_info {
    u1 tag = 10;
    u2 class_index;        指向此方法的所属类
    u2 name_type_index;    指向此方法的名称和类型
}
```

也就是说在“0a”之后的 2 个字节指向这个方法是属于哪个类，紧接的 2 个字节指向这个方法的名称和类型。它们的值分别是：

- 0006：十进制 6，表示指向常量池中的第 6 个常量。
- 0015：十进制 21，表示指向常量池中的第 21 个常量。

至此，第 1 个常量就解读完毕了。紧接着的就是第 2 个常量，如下所示：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174243393-2eb902e1-8f03-47d7-8ff0-3836d527051f.png">

tag 09 表示是字段引用表 CONSTANT_FIeldref_info ，其结构如下：

```java
CONSTANT_Fieldref_info{
    u1 tag;
    u2 class_index;        指向此字段的所属类
    u2 name_type_index;    指向此字段的名称和类型
}
```
同样也是 4 个字节，前后都是两个索引。

- 0005：指向常量池中第 5 个常量。
- 0016：指向常量池中第 22 个常量。

到现在为止我们已经解析出了常量池中的两个常量。剩下的 21 个常量的解析过程也大同小异，这里就不一一解析了。实际上我们可以借助 javap 命令来帮助我们查看 class 常量池中的内容：

```java
javap -v Test.class
```
上述命令执行后，显示结果如下：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174243438-495394d3-0e8f-4aca-8269-6844446d0e9a.png">

正如我们刚才分析的一样，常量池中第一个常量是 Methodref 类型，指向下标 6 和下标 21 的常量。其中下标 21 的常量类型为 NameAndType，它对应的数据结构如下：

```java
CONSTANT_NameAndType_info{
    u1 tag;
    u2 name_index;    指向某字段或方法的名称字符串
    u2 type_index;    指向某字段或方法的类型字符串
}
```
而下标在 21 的 NameAndType 的 name_index 和 type_index 分别指向了 13 和 14，也就是“<init>”和“()V”。因此最终解析下来常量池中第 1 个常量的解析过程以及最终值如下图所示：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174243584-33efe7be-66e1-46e7-a5d4-c9c1f4aa6f72.png">

仔细解析层层引用，最后我们可以看出，Test.class 文件中常量池的第 1 个常量保存的是 Object 中的默认构造器方法。

### 5.访问标志（access_flags）
  
紧跟在常量池之后的常量是访问标志，占用两个字节，如下图所示：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174243629-5c29c96b-4303-4ba3-b695-a99d272b235e.png">

访问标志代表类或者接口的访问信息，比如：该 class 文件是类还是接口，是否被定义成 public，是否是 abstract，如果是类，是否被声明成 final 等等。各种访问标志如下所示：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174243689-a427d4ee-778c-4976-a8ac-cc140cf25821.png">

我们定义的 Test.java 是一个普通 Java 类，不是接口、枚举或注解。并且被 public 修饰但没有被声明为 final 和 abstract，因此它所对应的 access_flags 为 0021（0X0001 和 0X0020 相结合）。

### 6.类索引、父类索引与接口索引计数器

在访问标志后的 2 个字节就是类索引，类索引后的 2 个字节就是父类索引，父类索引后的 2 个字节则是接口索引计数器。如下图所示：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174243715-3f809675-b58b-4423-9f77-4502999f2cba.png">

可以看出类索引指向常量池中的第 5 个常量，父类索引指向常量池中的第 6 个常量，并且实现的接口个数为 2 个。再回顾下常量池中的数据：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174243742-c6dccaa1-d635-411f-8d87-ad5abfbdf8ec.png">

从图中可以看出，第 5 个常量和第 6 个常量均为 CONSTANT_Class_info 表类型，并且代表的类分别是“Test”和“Object”。再看接口计数器，因为接口计数器的值是 2，代表这个类实现了 2 个接口。查看在接口计数器之后的 4 个字节分别为：

- 0007：指向常量池中的第 7 个常量，从图中可以看出第 7 个常量值为"Serializable"。
- 0008：指向常量池中的第 8 个常量，从图中可以看出第 8 个常量值为"Cloneable"。

综上所述，可以得出如下结论：当前类为 Test 继承自 Object 类，并实现了“Serializable”和“Cloneable”这两个接口。

### 7.字段表
紧跟在接口索引集合后面的就是字段表了，字段表的主要功能是用来描述类或者接口中声明的变量。这里的字段包含了类级别变量以及实例变量，但是不包括方法内部声明的局部变量。

同样, 一个类中的变量个数是不固定的，因此在字段表集合之前还是使用一个计数器来表示变量的个数，如下所示：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174243789-d37c77d9-eb8b-49cb-ad0a-c5b0d19864aa.png">

0002 表示类中声明了 2 个变量（在 class 文件中叫字段），字段计数器之后会紧跟着 2 个字段表的数据结构。

字段表的具体结构如下：

```java
CONSTANT_Fieldref_info{
    u2  access_flags    字段的访问标志
    u2  name_index          字段的名称索引(也就是变量名)
    u2  descriptor_index    字段的描述索引(也就是变量的类型)
    u2  attributes_count    属性计数器
    attribute_info
}
```
继续解析 Text.class 中的字段表，其结构如下图所示：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174264609-815a084f-cf94-4f41-9ef7-a6caf81626df.png">

#### 字段访问标志

对于 Java 类中的变量，也可以使用 public、private、final、static 等标识符进行标识。因此解析字段时，需要先判断它的访问标志，字段的访问标志如下所示：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174247354-4225ecfc-c60f-4936-8caf-3c0475a08b35.png">

字段表结构图中的访问标志的值为 0002，代表它是 private 类型。变量名索引指向常量池中的第 9 个常量，变量名类型索引指向常量池中第 10 个常量。第 9 和第 10 个常量分别为“num”和“I”，如下所示：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174247397-ddb6cc63-255f-47ef-9893-b209b97771e8.png">

因此可以得知类中有一个名为 num，类型为 int 类型的变量。对于第 2 个变量的解析过程也是一样，就不再过多介绍。

注意事项：
- 字段表集合中不会列出从父类或者父接口中继承而来的字段。
- 内部类中为了保持对外部类的访问性，会自动添加指向外部类实例的字段。
- 对于以上两种情况，建议你可以自行定义一个类查看并手动分析一下。

### 8.方法表

字段表之后跟着的就是方法表常量。相信你应该也能猜到了，方法表常量应该也是以一个计数器开始的，因为一个类中的方法数量是不固定的，如图所示：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174247441-02e55a6d-aac8-4756-ad88-74e3146daa64.png">

上图表示 Test.class 中有两个方法，但是我们只在 Test.java 中声明了一个 add 方法，这是为什么呢？这是因为默认构造器方法也被包含在方法表常量中。

方法表的结构如下所示：

```java
CONSTANT_Methodref_info{
    u2  access_flags;        方法的访问标志
    u2  name_index;          指向方法名的索引
    u2  descriptor_index;    指向方法类型的索引
    u2  attributes_count;    方法属性计数器
    attribute_info attributes;
}
```
可以看到，方法也是有自己的访问标志，具体如下：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174247479-be19a87e-bd19-4582-b8b1-4e4f969f494c.png">

我们主要来看下 add 方法，具体如下：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174247516-3aa06817-fe4c-4c1d-9ced-608e50246816.png">

从图中我们可以看出 add 方法的以下字段的具体值：

- access_flags = 0001 也就是访问权限为 public。
- name_index = 0X0011  指向常量池中的第 17 个常量，也就是“add”。
- type_index = 0X0012   指向常量池中的第 18 个常量，也即是 (I)。这个方法接收 int 类型参数，并返回 int 类型参数。

### 9.属性表
在之前解析字段和方法的时候，在它们的具体结构中我们都能看到有一个叫作 attributes_info 的表，这就是属性表。

属性表并没有一个固定的结构，各种不同的属性只要满足以下结构即可：

```java
CONSTANT_Attribute_info{
    u2 name_index;
    u2 attribute_length length;
    u1[] info;
}
```
  
JVM 中预定义了很多属性表，这里重点讲一下 Code 属性表。

Code属性表

我们可以接着刚才解析方法表的思路继续往下分析：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174248223-aa2761a6-fb94-42d7-87b6-455616f9fd65.png">

可以看到，在方法类型索引之后跟着的就是“add”方法的属性。0X0001 是属性计数器，代表只有一个属性。0X000f 是属性表类型索引，通过查看常量池可以看出它是一个 Code 属性表，如下所示：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174248174-fcab8995-7c9b-4518-ba60-acc0e2076104.png">

Code 属性表中，最主要的就是一些列的字节码。通过 javap -v Test.class 之后，可以看到方法的字节码，如下图显示的是 add 方法的字节码指令：

<img width="400" alt="类图" src="https://user-images.githubusercontent.com/17560388/174248134-19c5c26d-7fcd-4911-9512-c293c9aef423.png">

JVM 执行 add 方法时，就通过这一系列指令来做相应的操作。

### 10.总结：
本课时我们主要了解了一个 class 文件内容的数据结构到底长什么样子，并通过 Test.class 来模拟演示Java虚拟机解析字节码文件的过程。其中 class 常量池部分是重点内容，它就相当于是 class 文件中的资源仓库，其他的几种结构或多或少都会最终指向到这个资源仓库中。实际上平时我们不太会直接用一个 16 进制编辑器去打开一个 .class 文件。我们可以使用 javap 等命令或者是其他工具，来帮助我们查看 class 内部的数据结构。只不过自己亲手操作一遍是很有助于理解 JVM 的解析过程，并加深对 class 文件结构的记忆。

 
