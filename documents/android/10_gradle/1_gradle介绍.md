## 1、前置知识点

### 1.1 Gradle 是什么

首先看下官方介绍：

>Gradle is an open-source build automation tool focused on flexibility and performance. Gradle build scripts are written using a Groovy or Kotlin DSL.

翻译过来就是：Gradle 是一个开源的自动化构建工具，专注于灵活性和性能。Gradle 构建脚本是使用 Groovy 或 Kotlin DSL 编写的。

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
### 2.4 闭包 Closure

Groovy 闭包是一个匿名代码块，可以作为值传递给变量或函数参数，也可以接收参数和提供返回值，形式上与 Java / Kotlin 的 lambda 表达式类似。
```groove
{ 123 }                                          
{ name -> println name }                            
{ String x, int y ->                                
    println "hey ${x} the value is ${y}"
}
Closure a = { 123 }
def c = { 123 }
c.call() // Closure#call() 调用
c() // 通过变量名调用
```
### 2.5 Closure 的 隐式参数
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
### 2.5 Closure 的 关键变量

闭包委托是 Groovy Closure 相比 Java Lambda 最大的区别，通过修改闭包的委托可以实现灵活多样的 DSL。先认识闭包中的三个关键变量：

- this：永远指向定义该闭包最近的类对象，就近原则，定义闭包时，哪个类离的最近就指向哪个，我这里的离得近是指定义闭包的这个类，包含内部类
- owner：永远指向定义该闭包的类对象或者闭包对象，顾名思义，闭包只能定义在类中或者闭包中
- delegate：默认情况 delegate 等同于 owner，this 和 owner 的语义无法修改，而 delegate 可以修改。


闭包委托策略： 在闭包中，如果一个属性没有显式声明接收者对象，则会通过闭包代理解析策略寻找定义的对象，例如：
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

## 3、IO文件处理
 
### 3.1 IO 常用的 Api
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
//会把之前的内容给覆盖
file.withOutputStream { OutputStream outputStream ->
    outputStream.write("erdai999".getBytes())
}

// 会把之前的内容给覆盖
file.withWriter { BufferedWriter it ->
    it.write('erdai999')
}

//3. 通过输入输出流实现文件拷贝功能
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
### 3.2 XML 文件操作

1）、解析 XML 文件
```groovy
//定义一个带格式的 xml 字符串
def xml = '''
    <response>
        <value>
            <books id="1" classification="android">
                <book available="14" id="2">
                   <title>第一行代码</title>
                   <author id="2">郭霖</author>
               </book>
               <book available="13" id="3">
                   <title>Android开发艺术探索</title>
                   <author id="3">任玉刚</author>
               </book>
           </books>
       </value>
    </response>
'''
//创建 XmlSlurper 类对象，解析 XML 文件主要借助 XmlSlurper 这个类
def xmlSlurper = new XmlSlurper()
//解析 mxl 返回 response 根结点对象
def response = xmlSlurper.parseText(xml)

//1、使用迭代器解析
response.value.books.each{ books ->
    books.book.each{ book ->
        println book.title
        println book.author 
    }
}
//2、深度遍历 XML 数据
def str1 = response.depthFirst().findAll { book ->
    return book.author == '郭霖' //[第一行代码 郭霖][第一行代码郭霖]
}
//3、广度遍历 XML 数据
def str2 = response.value.books.children().findAll{ node ->
    node.name() == 'book' && node.@id == '2'
}.collect { node ->
    "$node.title $node.author" // [第一行代码 郭霖]
}
```
2)、生成 XML 文件
上面我们使用 XmlSlurper 这个类解析了 XML，现在我们借助 MarkupBuilder 来生成 XML ,代码如下：
```groovy
//方式1：通过下面这种方式 就可以实现上面的效果，但是这种方式有个弊端，数据都是写死的
def sw = new StringWriter()
def xmlBuilder = new MarkupBuilder(sw)
xmlBuilder.response{
    value{
        books(id: '1',classification: 'android'){
            book(available: '14',id: '2'){
                title('第一行代码')
                author(id: '2' ,'郭霖')
            }
            book(available: '13',id: '3'){
                title('Android开发艺术探索')
                author(id: '3' ,'任玉刚')
            }
        }
    }
}
println sw

//方式2：将 XML 数据对应创建相应的数据模型，就像我们解析 Json 创建相应的数据模型是一样的
class Response {
    def value = new Value()
    
    class Value {
        def books = new Books(id: '1', classification: 'android')
        class Books {
            def id
            def classification
            def book = [new Book(available: '14', id: '2', title: '第一行代码', authorId: 2, author: '郭霖'),
               new Book(available: '13', id: '3', title: 'Android开发艺术探索', authorId: 3, author: '任玉刚')]

            class Book {
                def available
                def id
                def title
                def authorId
                def author
            }
        }
    }
}

//创建 response 对象
def response = new Response()
//构建 XML 
xmlBuilder.response{
    value{
        books(id: response.value.books.id,classification: response.value.books.classification){
            response.value.books.book.each{
                def book1 = it
                book(available: it.available,id: it.id){
                    title(book1.title)
                    author(authorId: book1.authorId,book1.author)
                }
            }
        }
    }
}
println sw
```
### 3.3 Json 解析
Json解析主要是通过 JsonSlurper 这个类实现的，这样我们在写插件的时候就不需要额外引入第三方的 Json 解析库了，其示例代码如下所示：

```groovy
//发送请求获取服务器响应的数据
def response = getNetWorkData("https://www.wanandroid.com/banner/json")
println response.data[0].desc
println response.data[0].imagePath

def getNetWorkData(String url){
    def connect = new URL(url).openConnection()
    connect.setRequestMethod("GET")
    //这个会阻塞线程 在Android中不能这样操作 但是在桌面程序是可以的
    connect.connect()
    def response = connect.content.text

    //json转实体对象
    def jsonSlurper = new JsonSlurper()
    jsonSlurper.parseText(response)
}
//打印结果
扔物线
https://wanandroid.com/blogimgs/8a0131ac-05b7-4b6c-a8d0-f438678834ba.png
```

## 3、Gradle 构建生命周期

- 初始化阶段: 执行 settings.gradle，确定主 project 和⼦ project 
- 定义阶段: 执⾏每个 project 的 bulid.gradle（将每个 build.gradle 文件实例化为一个 Gradle 的 project 对象），确定出所有 task 所组成的有向无环图
- 执行阶段: 按照上一阶段所确定出的有向⽆环图来执行指定的 task

### 3.1 在阶段之间插⼊入代码:

⼀二阶段之间:
```groovy
settings.gradle 的最后 
```
⼆三阶段之间:
```groovy
afterEvaluate { 
    插⼊入代码
}
```

### 3.2 Gradle 的 Project 和 Tasks

每次构建（build）至少由一个 project 构成，一个 project 由一到多个 task 构成。

项目结构中的每个 build.gradle 文件代表一个 project ，在这些编译脚本文件中可以定义一系列的task；

task 本质上又是由一组被顺序执行的 Action 对象构成，Action其实是一段代码块，类似于Java中的方法。

```groovy
task taskName { 
    //初始化代码，初始化话的时候会执行
    doFirst { //Closure
     //初始化的时候不会执行
    }
    doLast {
    //初始化的时候不会执行
    }
}
```
### 1.2 task的执行: 

使用 gradlew 执行命令的时候，就是执行 gradle-wrapper 的指令。每个 gradle 版本有差异性，gradle-wrapper 对 gradle 进行封装。

```shell
./gradlew taskName1 taskName2 //顺序执行 taskName1 taskName2
```
### 1.3 doFirst() doLast() 和普通代码段的区别:

普通代码段:在 task 创建过程中就会被执行，发在 configuration 阶段

doFirst() 和 doLast(): 在 task 执⾏过程中被执行，发⽣在 execution 阶段。如果⽤用户没有直接或间接执⾏ task，那么它的 doLast() doFirst() 代码不会被执行

doFirst() 和 doLast() 都是 task 代码，其中 doFirst() 是往队列的前⾯插入代码，doLast() 是往队列的后面插⼊代码

### 1.4 task 的依赖:

可以使⽤ task taskA(dependsOn: b) 的形式来指定依赖。指定依赖后，task 会在⾃⼰执行前先执行⾃己依赖的 task。
```groovy
task task1 {
    doLast { println "执行task1----"}
}
task task2 {
    doLast { println "执行task2----"}
}
//task2 依赖 task1, 执行task2之前会先执行task1
task2.dependsOn task1
```

执行和日志

```shell
./gradlew task2

> Task :task1
执行task1----

> Task :task2
执行task2----
```

## 2. Android Gradle 配置
 
### 2.1 gradle 项⽬结构

- 单 project: build.gradle
- 多 project: 由 settings.gradle 配置多个 
 
查找 settings 的顺序:

1. 当前⽬录
2. 兄弟目录 master 
3. ⽗目录

### 2.2 根目录下的 build.gradle

```groovy
buildscript {//配置构建脚本所用到的代码库和依赖关系}

allprojects {//所有模块需要用到的一些公共属性}

task clean(type: Delete) {
    delete rootProject.buildDir
}
```

### 2.3 module 下的 build.gradle

android{} 是 pluagins {id 'com.android.application'} 中的方法 

常用的依赖:

compile, implementation 和 api

implementation: 不会传递依赖

compile / api: 会传递依赖; api 是 compile 的替代品，效果完全等同。

当依赖被传递时，⼆级依赖的改动会导致 0 级项⽬重新编译;
当依赖不传递时，⼆级依赖的改动不会导致 0 级项⽬重新编译


## 3. 在 build.gradle 中自定义 task

### 3.1 依赖 copy
```groovy
task copyImage(type: Copy) {
    from 'C:\\Users\\yiba_zyj\\Desktop\\gradle\\copy'
    into 'C:\\Users\\yiba_zyj\\Desktop'
    include "*.jpg"
    exclude "image1.jpg"
    rename("image2.jpg","123.jpg")
}
```
### 3.2 依赖 delete
```groovy
task deleteFile(type: Delete) {
    //删除系统桌面 delete 
    delete "path"
}
```
### 3.3 执行 shell

1. Use Gradle [Exec](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Exec.html) task type
```groovy
task fooExec(type: Exec) {
    workingDir "${buildDir}/foo"
    commandLine 'echo', 'Hello world!'
    doLast {
        println "Executed!"
    }
}
```
2. Use Gradle [Project.exec](https://docs.gradle.org/current/javadoc/org/gradle/api/Project.html#exec-groovy.lang.Closure-) method
```groovy
task execFoo {
    doLast {
        exec {
            workingDir "${buildDir}/foo"
            executable 'echo'
            args 'Hello world!'
        }
        println "Executed!"
    }
}
```

## 4. 将 task 放到单独的文件

```groovy
// ../代表根目录
apply from:"../utils.gradle"
```

## 参考：
- [Android Gradle 自定义Task 详解](https://blog.csdn.net/zhaoyanjun6/article/details/76408024)
- [Android Gradle使用总结](https://blog.csdn.net/zhaoyanjun6/article/details/77678577)
- [Gradle 使用指南 -- Gradle 生命周期](https://www.heqiangfly.com/2016/03/18/development-tool-gradle-lifecycle/)
- [Gradle execute command lines in custom task](https://stackoverflow.com/questions/38250735/gradle-execute-command-lines-in-custom-task)
- [Gradle 系列 （一）、Gradle相关概念理解，Groovy基础](https://juejin.cn/post/6939662617224937503#heading-52)

