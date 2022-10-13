## 一、Protobuf 介绍

Protobuf 全称：Protocol Buffers，是 Google 推出的一种与平台无关、语言无关、可扩展的轻便高效的序列化数据存储格式，类似于我们常用的 xml 和 json。

三种方式的性能对比

|              | xml  | json | protobuf |
| :----------- | :--- | :--- | :------: |
| 数据保存方式 | 文本 | 文本 |  二进制  |
| 数据保存大小 | 大   | 一般 |    小    |
| 解析效率     | 慢   | 一般 |    快    |

## 二、安装 Protobuf 

方式一：下载安装包

Protobuf Github 下载地址：*https://github.com/protocolbuffers/protobuf/releases/tag/v3.19.2*

方式二：通过 Homebrew 安装 

```shell
brew install protobuf
brew uninstall protobuf
```

安装完成配置一下环境变量

```shell
# protobuf 环境变量
export PROTOBUF_HOME=protobuf安装路径
export PATH=${PATH}:${PROTO_BUF_HOME}/bin
```

使用 `protoc --version`验证一下

```shell
libprotoc 3.21.7
```

在 AS 中安装 `Protocol Buffers`插件

## 三、语法基础

### 3.1  常用关键字介绍

| 关键字               | 说明                                                         |
| :------------------- | :----------------------------------------------------------- |
| syntax               | 指定 Protobuf 的版本，Protobuf 目前有 proto2 和 proto3 两个常用版本，如果没有声明，则默认是proto2 |
| package              | 指定文件包名                                                 |
| import               | 导包，和 Java 的 import 类似                                 |
| message              | 定义消息类，和 Java 的 class 关键字类似，消息类之间可以嵌套  |
| repeated             | 定义一个集合，和 Java 的集合类似                             |
| reserved             | 保留字段，如果使用了这个关键字修饰，用户就不能使用这个字段编号或字段名 |
| option               | option 可以用在 Protobuf 的 scope 中，或者 message、enum、service 的定义中，Protobuf 定义的 option 有 java_package，java_outer_classname，java_multiple_files 等等 |
| optional             | 表示该字段是可选的                                           |
| java_package         | 指定生成类所在的包名，需配合                                 |
| java_outer_classname | 定义当前文件的类名，如果没有定义，则默认为文件的首字母大写名称 |
| java_multiple_files  | 指定编译过后 Java 的文件个数，如果是 true，那么将会一个 Java 对象一个类，如果是 false，那么定义的Java 对象将会被包含在同一个文件中 |

注意：在 Proto3 中不支持 required (必须)字段。

### 3.2  基本数据类型

| Protobuf Type | 说明                                                         | 对应 Java/Kotlin Type |
| :------------ | :----------------------------------------------------------- | :-------------------- |
| double        | 固定 8 字节长度                                              | double                |
| float         | 固定 4 字节长度                                              | float                 |
| int32         | 可变长度编码，对负数编码低效，如果字段可能为负数，用 sint32 代替 | int                   |
| int64         | 可变长度编码，对负数编码低效，如果字段可能为负数，用 sint64 代替 | long                  |
| uint32        | 可变长度编码，无符号整数                                     | int                   |
| uint64        | 可变长度编码，无符号整数                                     | long                  |
| sint32        | 可变长度编码，有符号整数                                     | int                   |
| sint64        | 可变长度编码，有符号整数                                     | long                  |
| fixed32       | 固定 4 字节长度，无符号整数                                  | int                   |
| fixed64       | 固定 8 字节长度，无符号整数                                  | long                  |
| sfixed32      | 固定 4 字节长度，有符号整数                                  | int                   |
| sfixed64      | 固定 8 字节长度，有符号整数                                  | long                  |
| bool          | 布尔类型，值为 true 或 false                                 | boolean               |
| string        | 字符串类型                                                   | String                |

可变长度编码和固定长度编码区别：

```java
// 例如说我在 Java 里面进行如下定义：
int a = 1;
// 因为 int 类型占 4 个字节，1个字节占 8 位，我们把 1 的字节占位给列出来：
00000000 00000000 00000000 00000001
// 可以看到 1 的前面 3 个字节占位都是 0，在 Protobuf 里面是可以去掉的，于是就变成了：
00000001
// 因此 1 在 Protobuf 里面就只占用了一个字节，节省了空间
```

上面这种就是可变长度编码。而固定长度编码就是即使前面的字节占位是 0，也不能去掉，我就是要占这么多字节。

#### 3.2.1 基本数据类型默认值

| 类型     | 默认值           |
| :------- | :--------------- |
| 数值类型 | 0                |
| bool     | false            |
| string   | 空字符串         |
| enum     | 默认为第一个元素 |

### 3.3 消息类型定义

我们创建一个学生的 Protobuf 文件，属性有姓名、年龄、邮箱和课程

```protobuf
//指定 Protobuf 版本
syntax = "proto3";

//指定包名
package erdai;

//定义一个学生的消息类
message Student{
  //姓名
  string name = 1;
  //年龄
  int32 age = 2;
  //邮箱
  string email = 3;
  //课程
  repeated string course = 4; //相当于 Java 的 List<String>
}
```

注意：

1. 一个 Protobuf 文件里面可以添加多个消息类，也可以进行嵌套。

2. 上面的 1，2，3，4 并不是给字段赋值，而是给每个字段定义一个唯一的编号。这些编号用于二进制格式中标识你的字段，并且在使用你的消息类型后不应更改。

3. 1-15 的字段编号只占一个字节进行编码，16-2047 的字段编号占两个字节，包括字段编号和字段类型，因此建议更多的使用 1-15 的字段编号。

4. 可以指定最小字段编号为 1，最大字段编号为 2^29 - 1 或 536870911。另外不能使用 19000-19999 的标识号，因为 protobuf 协议实现对这些进行了预留，同样，也不能使用任何以前保留（reserved） 的字段编号。

### 3.4 枚举类型

```protobuf
message Weather{
  int32 query = 1;

  //季节
  enum Season{
    //允许对枚举常量设置别名
    option allow_alias = true;
    //枚举里面的 = 操作是对常量进行赋值操作
    //春
    SPRING = 0;
    //夏
    SUMMER = 1;
    //秋 如果不设置别名，不允许存在两个相同的值
    FALL = 2;
    AUTUMN = 2;
    //冬
    WINTER = 3;
  }

  //对 season 进行编号
  Season season = 2;
}
```

1. 定义枚举类型使用 enum 关键字。

2. 枚举类型第一个字段的值为必须 0，否则编译会报错。

3. 枚举常量值必须在 32 位整型值的范围内。因为 enum 值是使用可变编码方式的，对负数不够高效，因此不推荐在 enum 中使用负数。

4. 枚举里面的 = 操作是对常量进行赋值操作，而枚举外面的 = 则是对当前字段进行编号。

### 3.5 集合

```protobuf
repeated string list = 1; //类似 Java 的 List<String>
map<string,string> = 2; //类似 Java 的 Map<String,String>

```

### 3.6 reserved 保留字段

可以通过 `reserved` 将字段和名称设置为保留项 。protocol buffer 编译器在用户使用这些保留字段时会发出警告。

```protobuf
message Foo {
  reserved 2, 15, 9 to 11;
  reserved "foo", "bar";
}
```

注意，不能在同一条 `reserved` 语句中同时使用字段编号和名称。

## 四、使用

首先在AS中集成我们需要的开发环境（先这样吧，用的时候再深入研究）

根 build.gradle 中添加 classpath

```groovy
classpath 'com.google.protobuf:protobuf-gradle-plugin:0.8.19'
```

在 app 的 build.gradle 添加相关配置
```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'com.google.protobuf'
}

android {
    ...
    sourceSets {
        main {
            proto {
                srcDir 'src/main/proto'
                include '**/*.proto'
            }
            java {
                srcDir 'src/main/java'
            }
        }
    }
}
protobuf {
    protoc {
        // You still need protoc like in the non-Android case
        artifact = 'com.google.protobuf:protoc:3.0.0'
    }
    plugins {
        javalite {
            // The codegen for lite comes as a separate artifact
            artifact = 'com.google.protobuf:protoc-gen-javalite:3.0.0'
        }
    }
    generateProtoTasks {
        all().each { task ->
            task.builtins {
                // In most cases you don't need the full Java output
                // if you use the lite output.
                remove java
            }
            task.plugins {
                javalite { }
            }
        }
    }
}

dependencies {
    ...
    implementation 'com.google.protobuf:protobuf-lite:3.0.0'
}
```
## 参考
- ["一篇就够"系列：Android 中使用 Protobuf](https://mp.weixin.qq.com/s/sgFFoWM0-AAvwFqmfFj6Jg)
