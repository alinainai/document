## 1、前置知识点

### 1.1 Gradle 是什么

首先看下官方介绍：

>Gradle is an open-source build automation tool focused on flexibility and performance. Gradle build scripts are written using a Groovy or Kotlin DSL.

翻译过来就是：Gradle 是一个开源的自动化构建工具，专注于灵活性和性能。Gradle 构建脚本是使用 Groovy 或 Kotlin DSL 编写的。

我们先学习一下 Groovy 的语法，方便咱们看一些老项目的的 gradle 配置。当然还是推荐大家使用 Kotlin 去编写相关的 gradle 文件。

### 1.2 Groovy 是什么

Groovy 是基于 JVM 的脚本语言，它是基于Java扩展的动态语言。动态语言可以在运行时再去确认对象的类型，静态语言在编译时就需要确定好对象的类型。我们通过 Groovy 编写 Gradle 脚本。
Groovy 同样生成 .class 文件并在 JVM 上运行。

## 2、Groovy 语法

### 2.1 和Java的一些小差异

- 1.语句允许不以分号`;`结尾；
- 2.默认的访问修饰符为 public；
- 3.Groovy 会为每个 field 创建对应的 `getter/setter` 方法
- 4.Class 是一等公民，所有的 Class 类型可以省略 `.Class`
- 5.`==` 就相当于 Java 的 `equals`，使用 `.is()` 比较两个对象是否是同一个
- 6.使用 assert 来设置断言，当断言的条件为 false 时，程序将会抛出异常

**7.字符串**
- 单引号：不可扩展字符串
- 双引号：支持在引号内通过 $ 关键字直接引用变量值；
- 三引号：支持换行。

8.支持 `**` 次方运算符
```groovy
assert  2 ** 4 == 16
```
9.简洁的三元表达式

```groovy
//左边结果不为空则取左边的值，否则取右边的值
String str = obj ?: ""
```
10.简洁的非空判断
```groovy
obj?.group?.artifact
```
11.强大的 Switch
```groovy
def result = 'erdai666'
switch (result){
    case [1,2,'erdai666']:
        println "匹配到了result"
        break
    default:
        println 'default'
        break
}
```

12.判断是否为 null 和 非运算符
```groovy
if(name){ // 等价于 if (name != null && name.length > 0) 
}
```
### 2.2 函数

- Groovy 支持通过返回类型或 def 关键字定义函数。
- Groovy 支持不指定参数类型
- Groovy 支持指定函数参数默认值，默认参数必须放在参数列表末尾。
- 可以省略 return，默认返回最后一行语句的值。
```groovy
def methodName(param1, param2 = 1) {
    // Method Code
}
```

### 2.3 集合

Groovy 支持通过 [] 关键字定义 List 列表或 Map 集合：

```groove
def range = 1 .. 10 //
def range1 = 1..<10
def list = [1, 2, 3, 4]
def map = [’name’:’Tom’, ‘age’:18]，空集合 [:]
list.each { value ->}
list.eachWIthIndex { value, index ->}
```
## 3、闭包 Closure

Groovy 闭包是一个匿名代码块，可以作为值传递给变量或函数参数，也可以接收参数和提供返回值，形式上与 Java/Kotlin 的 lambda 表达式类似。

```groove
{ 123 }                                          
{ name -> println name }                            
{ String x, int y ->                                
    println "hey ${x} the value is ${y}"
}
// 定义
Closure a = { 123 }
def c = { 123 }
// 调用
c.call() // Closure#call() 调用
c() // 通过变量名调用
```
### 3.1 Closure 的 隐式参数
隐式参数： 闭包默认至少有一个形式参数，如果闭包没有显式定义参数列表（使用 →），Groovy 总是带有隐式添加一个参数 it。如果调用者没有使用任何实参，则 it 为空。
当你需要声明一个不接收任何参数的闭包，那么必须用显式的空参数列表声明。
```groove
def greeting = { "Hello, $it!" } // 带隐式参数 it
assert greeting('Patrick') == 'Hello, Patrick!'

def magicNumber = { -> 42 } // 不带隐式参数 it
magicNumber(11) // error 不允许传递参数
```

闭包参数简化： 函数的最后一个参数是闭包类型的化，在调用时可以简化，省略圆括号：
```groove
def methodName(String param1, Closure closure) {
    // Method Code
}
// 调用：
methodName("Hello") {
    // Closure Code
}
```
### 3.2 Closure 的 关键变量

闭包委托是 Groovy Closure 相比 Java Lambda 最大的区别，通过修改闭包的委托可以实现灵活多样的 DSL。先认识闭包中的三个关键变量：

- this：永远指向定义该闭包最近的类对象，就近原则，定义闭包时，哪个类离的最近就指向哪个，我这里的离得近是指定义闭包的这个类，包含内部类
- owner：永远指向定义该闭包的类对象或者闭包对象，顾名思义，闭包只能定义在类中或者闭包中
- delegate：默认情况 delegate 等同于 owner，this 和 owner 的语义无法修改，而 delegate 可以修改。

闭包委托策略: 在闭包中，如果一个属性没有显式声明接收者对象，则会通过闭包代理解析策略寻找定义的对象，例如：
```groove
class Person {
    String name
}
def p = new Person(name:'Igor')
def cl = { 
    name.toUpperCase() // 相当于 delegate.name.toUpperCase()
}                 
cl.delegate = p                                 
assert cl() == 'IGOR'
```

闭包定义了多种解析策略，可以通过 Closure#resolveStrategy=Closure.DELEGATE_FIRST 修改：

- Closure.OWNER_FIRST（默认）：优先在 owner 对象中寻找，再去 delegate 对象中寻找；
- Closure.DELEGATE_FIRST：优先在 delegate 对象中寻找，再去 owner 对象中寻找；
- Closure.OWNER_ONLY：只在 owner 对象中寻找；
- Closure.DELEGATE_ONLY：只在 delegate 对象中寻找；
- Closure.TO_SELF：只在闭包本身寻找；

## 4、IO文件处理
 
```groovy
def file = new File('testFile.txt')
// 1. 读取
file.eachLine { String line ->
    println line
}
file.withInputStream { InputStream inputStream ->
    inputStream.eachLine { String it ->
        println it
    }
}
file.withReader { BufferedReader it ->
    it.readLines().each { String it ->
        println it
    }
}
// 2. 输出
// 会把之前的内容给覆盖
file.withOutputStream { OutputStream outputStream ->
    outputStream.write("erdai999".getBytes())
}

// 会把之前的内容给覆盖
file.withWriter { BufferedWriter it ->
    it.write('erdai999')
}

//3. 文件拷贝
//1、通过 withOutputStream withInputStream 实现文件拷贝
def targetFile = new File('testFile1.txt')
targetFile.withOutputStream { OutputStream outputStream ->
    file.withInputStream { InputStream inputStream ->
        outputStream << inputStream
    }
}
//2、通过 withReader、withWriter 实现文件拷贝
targetFile.withWriter {BufferedWriter bufferedWriter ->
    file.withReader {BufferedReader bufferedReader ->
        bufferedReader.eachLine {String line ->
            bufferedWriter.write(line + "\r\n")
        }
    }
}
```
## 参考
- [Gradle 系列 （一）、Gradle相关概念理解，Groovy基础](https://juejin.cn/post/6939662617224937503#heading-52)

