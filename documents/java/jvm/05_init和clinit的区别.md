### 1.执行时机不同

- `init` 是对象构造器方法，也就是说在程序执行 `new`一个对象调用该对象类的 `constructor` 方法时才会执行 `init` 方法，
- 而 `clinit` 是类构造器方法（Class 类的 init），也就是在 `jvm` 进行 `类加载的初始化`阶段调用`clinit`方法。

### 2.执行目不同

- init is the (or one of the) constructor(s) for the instance, and non-static field initialization.
- clinit are the static initialization blocks for the class, and static field initialization.

上面这两句是 `Stack Overflow` 上 的解析，很清楚`init`是`instance`实例构造器，对非静态变量解析初始化，而`clinit`是`class`类构造器对静态变量，静态代码块进行初始化。

看看下面的这段程序就很清楚了。

```java
class X {
   static Log log = LogFactory.getLog(); // <clinit>
   private int x = 1;   // <init>
   X(){
      // <init>
   }
   static {
      // <clinit>
   }
}
```
### 3.clinit 详解

在准备阶段，变量已经赋过一次系统要求的初始值（零值），而在初始化阶段，则根据程序员通过程序制定的主观计划去初始化 类变量 和 其他资源，
或者可以从另外一个角度来表达：初始化阶段是执行类构造器`＜clinit＞()`方法的过程。

① `＜clinit＞()` 方法是由编译器自动收集类中的所有类变量的赋值动作和静态语句块`（static{}块）`中的语句合并产生的，编译器收集的顺序是由语句在源文件中出现的顺序所决定的，
静态语句块中只能访问到定义在静态语句块之前的变量，定义在它之后的变量，在前面的静态语句块可以赋值，但是不能访问如下代码
```java
public class Test{
  static{
    i=0；//给变量赋值可以正常编译通过
    System.out.print（i）；//这句编译器会提示"非法向前引用"
  }
  static int i=1；
}
```
② 虚拟机会保证在子类的`＜clinit＞()`方法执行之前，父类的`＜clinit＞()`方法已经执行完毕。 
因此在虚拟机中第一个被执行的`＜clinit＞()`方法的类肯定是`java.lang.Object`。
由于父类的`＜clinit＞()`方法先执行，也就意味着父类中定义的静态语句块要优先于子类的变量赋值操作，如下代码中，字段B的值将会是2而不是1。
```java
static class Parent{
    public static int A=1；
    static{
      A=2；
    }
    static class Sub extends Parent{
      public static int B=A；
    }
    public static void main（String[]args）{
      System.out.println（Sub.B）；
    }
}
```
③ 接口中不能使用静态语句块，但仍然有变量初始化的赋值操作，因此接口与类一样都会生成`＜clinit＞()`方法。 
但接口与类不同的是，执行接口的`＜clinit＞()`方法不需要先执行父接口的`＜clinit＞()`方法。 
只有当父接口中定义的变量使用时，父接口才会初始化。 另外，接口的实现类在初始化时也一样不会执行接口的`＜clinit＞()`方法。

注意：接口中的属性都是 static final 类型的常量，因此在准备阶段就已经初始化话。
