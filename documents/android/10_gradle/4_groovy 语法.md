## 1、groovy 语法

### 1.1 和Java的一些小差异

- 分号： 语句允许不以分号`;`结尾；
- public： 默认的访问修饰符为 public；
- getter / setter： Groovy 会为每个 field 创建对应的 getter / setter 方法
- 支持静态类型和动态类型： Groovy 既支持 Java 的静态类型，也支持通过 def 关键字声明动态类型（静态类型和动态类型的关键区别在于 ”类型检查是否倾向于在编译时执行“)
- 字符串： Groovy 支持三种格式定义字符串 —— 单引号、双引号和三引号

- 单引号：纯粹的字符串，与 Java 的双引号字符串类似；
- 双引号：支持在引号内通过 $ 关键字直接引用变量值；
- 三引号：支持换行。



### 1.2 函数

函数定义： Groovy 支持通过返回类型或 def 关键字定义函数。def 关键字定义的函数如果没有 return 关键字返回值，则默认会返回 null。例如：
```groovy
// 使用 def 关键字
def methodName() {
    // Method Code
}

String methodName() {
    // Method Code
}
```

参数名： Groovy 支持不指定参数类型。例如：
```groovy
// 省略参数类型
def methodName(param1, param2) {
    // Method Code
}

def methodName(String param1, String param2) {
    // Method Code
}
```

默认参数： Groovy 支持指定函数参数默认值，默认参数必须放在参数列表末尾。例如：

```groovy
def methodName(param1, param2 = 1) {
    // Method Code
}
```

返回值： 可以省略 return，默认返回最后一行语句的值。例如：
```groovy
def methodName() {
    return "返回值"
}
// 等价于
def methodName() {
    "返回值"
}
```

invokeMethod & methodMissing：

- invokeMethod： 分派对象上所有方法调用，包括已定义和未定义的方法，需要实现 GroovyInterceptable 接口；
- methodMissing： 分派对象上所有为定义方法的调用。

// 实现 GroovyInterceptable 接口，才会把方法调用分派到 invokeMethod。
class Student implements GroovyInterceptable{
    def name;

    def hello() {
        println "Hello ${name}"
    }

    @Override
    Object invokeMethod(String name, Object args) {
        System.out.println "invokeMethod : $name"
    }
}

def student = new Student(name: "Tom")

student.hello()
student.hello1()

输出：
invokeMethod : hello
invokeMethod : hello1

-------------------------------------------------------------

class Student {
    def name;

    def hello() {
        println "Hello ${name}"
    }

    @Override
    Object methodMissing(String name, Object args) {
        System.out.println "methodMissing : $name"
    }
}

def student = new Student(name: "Tom")

student.hello()
student.hello1()

输出：
Hello Tom
methodMissing hello1
复制代码


### 1.3 集合
Groovy 支持通过 [] 关键字定义 List 列表或 Map 集合：

列表： 例如 def list = [1, 2, 3, 4]
集合： 例如 def map = [’name’:’Tom’, ‘age’:18]，空集合 [:]
范围： 例如 def range = 1 .. 10
遍历：

```groove
// 列表
def list = [10, 11, 12]
list.each { value ->
}
list.eachWIthIndex { value, index ->
}

// 集合
def map = [’name’:’Tom’, ‘age’:18]
map.each { key, value ->
}
map.eachWithIndex { entry, index ->
}
map.eachWithIndex { key, value, index ->
}
```
### 1.4 闭包

Groovy 闭包是一个匿名代码块，可以作为值传递给变量或函数参数，也可以接收参数和提供返回值，形式上与 Java / Kotlin 的 lambda 表达式类似。例如以下是有效的闭包：

{ 123 }                                          

{ -> 123 }                                       

{ println it }

{ it -> println it }

{ name -> println name }                            

{ String x, int y ->                                
    println "hey ${x} the value is ${y}"
}

闭包类型： Groovy 将闭包定义为  groovy.lang.Closure 的实例，使得闭包可以像其他类型的值一样复制给变量。例如：

Closure c = { 123 }

// 当然也可以用 def 关键字
def c = { 123 }


闭包调用： 闭包可以像方法一样被调用，可以通过 Closure#call() 完成，也可以直接通过变量完成。例如：

def c = { 123 }

// 通过 Closure#call() 调用
c.call()

// 直接通过变量名调用
c()
复制代码

隐式参数： 闭包默认至少有一个形式参数，如果闭包没有显式定义参数列表（使用 →），Groovy 总是带有隐式添加一个参数 it。如果调用者没有使用任何实参，则 it 为空。
当你需要声明一个不接收任何参数的闭包，那么必须用显式的空参数列表声明。例如：

// 带隐式参数 it
def greeting = { "Hello, $it!" }
assert greeting('Patrick') == 'Hello, Patrick!'

// 不带隐式参数 it
def magicNumber = { -> 42 }
// error 不允许传递参数
magicNumber(11)


闭包参数简化： 函数的最后一个参数是闭包类型的化，在调用时可以简化，省略圆括号：

def methodName(String param1, Closure closure) {
    // Method Code
}

// 调用：
methodName("Hello") {
    // Closure Code
}


this、owner、delegate： 闭包委托是 Groovy Closure 相比 Java Lambda 最大的区别，通过修改闭包的委托可以实现灵活多样的 DSL。先认识闭包中的三个变量：

this： 定义闭包的外部类，this 一定指向类对象；
owner： 定义闭包的外部对象，owner 可能是类对象，也可能是更外一层的闭包；
delegate： 默认情况 delegate 等同于 owner，this 和 owner 的语义无法修改，而 delegate 可以修改。


闭包委托策略： 在闭包中，如果一个属性没有显式声明接收者对象，则会通过闭包代理解析策略寻找定义的对象，例如：

class Person {
    String name
}
def p = new Person(name:'Igor')
def cl = { 
    // 相当于 delegate.name.toUpperCase()
    name.toUpperCase() 
}                 
cl.delegate = p                                 
assert cl() == 'IGOR'

闭包定义了多种解析策略，可以通过 Closure#resolveStrategy=Closure.DELEGATE_FIRST 修改：

Closure.OWNER_FIRST（默认）： 优先在 owner 对象中寻找，再去 delegate 对象中寻找；
Closure.DELEGATE_FIRST： 优先在 delegate 对象中寻找，再去 owner 对象中寻找；
Closure.OWNER_ONLY： 只在 owner 对象中寻找；
Closure.DELEGATE_ONLY： 只在 delegate 对象中寻找；
Closure.TO_SELF： 只在闭包本身寻找；
