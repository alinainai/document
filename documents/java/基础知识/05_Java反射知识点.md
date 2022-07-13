## 1. 获取 Class 对象的三种方式

### 1.1 通过对象获取

通过实例化对象获取该对象的 Class

```java
new Candy().getClass();
```

### 1.2 通过 Class.forName 方法

`Class.forName()` 方法也会返回一个对应类的`Class`对象，无需通过持有该类的实例对象引用而去获取`Class`对象。
```java
Class clazz=Class.forName("com.suger.Candy");
```
### 1.3 通过字面常量
通过字面常量的方式获取 Class 对象
```java
Class clazz = Gum.class;
```
通过字面量的方法获取`Class`对象的引用不会自动初始化该类，该方式不仅可以应用于普通的类，也可以应用用接口，数组以及基本数据类型。

由于基本数据类型还有对应的基本包装类型，其包装类型有一个标准字段`TYPE`，而这个`TYPE`就是一个引用，指向基本数据类型的`Class`对象。

```java
boolean.class = Boolean.TYPE;
char.class = Character.TYPE;
byte.class = Byte.TYPE;
short.class = Short.TYPE;
int.class = Integer.TYPE;
long.class = Long.TYPE;
float.class = Float.TYPE;
double.class = Double.TYPE;
void.class = Void.TYPE;
```
## 2. 类的加载过程

前面提到过，使用字面常量的方式获取Class对象的引用不会触发类的初始化，这里我们可能需要简单了解一下类加载的过程，如下：

![在这里插入图片描述](https://img-blog.csdn.net/20170430160610299?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvamF2YXplamlhbg==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast#pic_center)

由此可知，我们获取字面常量的Class引用时，触发的应该是加载阶段，因为在这个阶段Class对象已创建完成，获取其引用并不困难，而无需触发类的最后阶段初始化。下面通过小例子来验证这个过程：
```java
class Initable {
  //编译期静态常量
  static final int staticFinal = 47;
  
  //非编期静态常量
  static final int staticFinal2 = ClassInitialization.rand.nextInt(1000);
    
  static {
    System.out.println("Initializing Initable");
  }
}

class Initable2 {
  //静态成员变量
  static int staticNonFinal = 147;
  static {
    System.out.println("Initializing Initable2");
  }
}

class Initable3 {
  //静态成员变量
  static int staticNonFinal = 74;
  static {
    System.out.println("Initializing Initable3");
  }
}

public class ClassInitialization {
  public static Random rand = new Random(47);
  public static void main(String[] args) throws Exception {
    //字面常量获取方式获取Class对象
    Class initable = Initable.class;
    System.out.println("After creating Initable ref");
    //不触发类初始化
    System.out.println(Initable.staticFinal);
    //会触发类初始化
    System.out.println(Initable.staticFinal2);
    //会触发类初始化
    System.out.println(Initable2.staticNonFinal);
    //forName方法获取Class对象
    Class initable3 = Class.forName("Initable3");
    System.out.println("After creating Initable3 ref");
    System.out.println(Initable3.staticNonFinal);
  }
}
```
执行结果：
```shell
After creating Initable ref
47
Initializing Initable
258
Initializing Initable2
147
Initializing Initable3
After creating Initable3 ref
74
```
从输出结果来看，可以发现
- 1.通过字面常量获取方式获取 Initable 类的 Class 对象并没有触发 Initable 类的初始化
- 2.同时发现调用 Initable.static final 变量时也没有触发初始化，这是因为 static final 属于编译期静态常量，在编译阶段通过常量传播优化的方式将 Initable 类的常量 static final 存储到了一个称为 NotInitialization 类的常量池中，在以后对 Initable 类常量 static final 的引用实际都转化为对 NotInitialization 类对自身常量池的引用，所以在编译期后，对编译期常量的引用都将在NotInitialization 类的常量池获取，这也就是引用编译期静态常量不会触发 Initable 类初始化的重要原因。但在之后调用了 Initable.staticFinal2 变量后就触发了 Initable 类的初始化，注意staticFinal2 虽然被 static 和 final 修饰，但其值在编译期并不能确定，因此 staticFinal2 并不是编译期常量，使用该变量必须先初始化 Initable 类。
- 3.Initable2 和 Initable3 类中都是静态成员变量并非编译期常量，引用都会触发初始化。至于forName方法获取Class对象，肯定会触发初始化，这点在前面已分析过。

到这几种获取Class对象的方式也都分析完，ok~,到此这里可以得出小结论：

- 获取Class对象引用的方式3种，通过继承自Object类的getClass方法，Class类的静态方法forName以及字面常量的方式”.class”。

- 其中实例类的getClass方法和Class类的静态方法forName都将会触发类的初始化阶段，而字面常量获取Class对象的方式则不会触发初始化。

- 初始化是类加载的最后一个阶段，也就是说完成这个阶段后类也就加载到内存中(Class对象在加载阶段已被创建)，此时可以对类进行各种必要的操作了（如new对象，调用静态成员等），注意在这个阶段，才真正开始执行类中定义的Java程序代码或者字节码。

关于类加载的初始化阶段，在虚拟机规范严格规定了有且只有5种场景必须对类进行初始化：

- 使用new关键字实例化对象时、读取或者设置一个类的静态字段(不包含编译期常量)以及调用静态方法的时候，必须触发类加载的初始化过程(类加载过程最终阶段)。

- 使用反射包(java.lang.reflect)的方法对类进行反射调用时，如果类还没有被初始化，则需先进行初始化，这点对反射很重要。

- 当初始化一个类的时候，如果其父类还没进行初始化则需先触发其父类的初始化。

- 当Java虚拟机启动时，用户需要指定一个要执行的主类(包含main方法的类)，虚拟机会先初始化这个主类

- 当使用JDK 1.7 的动态语言支持时，如果一个java.lang.invoke.MethodHandle 实例最后解析结果为REF_getStatic、REF_putStatic、REF_invokeStatic的方法句柄，并且这个方法句柄对应类没有初始化时，必须触发其初始化(这点看不懂就算了，这是1.7的新增的动态语言支持，其关键特征是它的类型检查的主体过程是在运行期而不是编译期进行的，这是一个比较大点的话题，这里暂且打住)

### 3. 理解泛化的Class对象引用
由于Class的引用总数指向某个类的Class对象，利用Class对象可以创建实例类，这也就足以说明Class对象的引用指向的对象确切的类型。在Java SE5引入泛型后，使用我们可以利用泛型来表示Class对象更具体的类型，即使在运行期间会被擦除，但编译期足以确保我们使用正确的对象类型。如下：
```java
public class ClazzDemo {
    public static void main(String[] args){
        //没有泛型
        Class intClass = int.class;
        //带泛型的Class对象
        Class<Integer> integerClass = int.class;
        integerClass = Integer.class;
        //没有泛型的约束,可以随意赋值
        intClass= double.class;
        //编译期错误,无法编译通过
        //integerClass = double.class
    }
}
```
从代码可以看出，声明普通的Class对象，在编译器并不会检查Class对象的确切类型是否符合要求，如果存在错误只有在运行时才得以暴露出来。但是通过泛型声明指明类型的Class对象，编译器在编译期将对带泛型的类进行额外的类型检查，确保在编译期就能保证类型的正确性，实际上Integer.class就是一个Class<Integer>类的对象。面对下述语句，确实可能令人困惑，但该语句确实是无法编译通过的。
```java
//编译无法通过
Class<Number> numberClass=Integer.class;
```
我们或许会想Integer不就是Number的子类吗？然而事实并非这般简单，毕竟Integer的Class对象并非Number的Class对象的子类，前面提到过，所有的Class对象都只来源于Class类，看来事实确实如此。当然我们可以利用通配符“?”来解决问题：
```java
Class<?> intClass = int.class;
intClass = double.class;
```
这样的语句并没有什么问题，毕竟通配符指明所有类型都适用，那么为什么不直接使用Class还要使用Class<?>呢？这样做的好处是告诉编译器，我们是确实是采用任意类型的泛型，而非忘记使用泛型约束，因此Class<?>总是优于直接使用Class，至少前者在编译器检查时不会产生警告信息。当然我们还可以使用extends关键字告诉编译器接收某个类型的子类，如解决前面Number与Integer的问题：
```java
//编译通过！
Class<? extends Number> clazz = Integer.class;
//赋予其他类型
clazz = double.class;
clazz = Number.class;
```
上述的代码是行得通的，extends关键字的作用是告诉编译器，只要是Number的子类都可以赋值。这点与前面直接使用Class<Number>是不一样的。实际上，应该时刻记住向Class引用添加泛型约束仅仅是为了提供编译期类型的检查从而避免将错误延续到运行时期。

#### 3.1 关于类型转换的问题
在许多需要强制类型转换的场景，我们更多的做法是直接强制转换类型：
```java
public class ClassCast {
   public void cast(){
     Animal animal= new Dog();
     //强制转换
     Dog dog = (Dog) animal;
   }
}
interface Animal{ }
class Dog implements  Animal{ }
```
之所可以强制转换，这得归功于RTTI，要知道在Java中，所有类型转换都是在运行时进行正确性检查的，利用RTTI进行判断类型是否正确从而确保强制转换的完成，如果类型转换失败，将会抛出类型转换异常。除了强制转换外，在Java SE5中新增一种使用Class对象进行类型转换的方式，如下：
```java
Animal animal= new Dog();
//这两句等同于Dog dog = (Dog) animal;
Class<Dog> dogType = Dog.class;
Dog dog = dogType.cast(animal)
```
利用Class对象的cast方法，其参数接收一个参数对象并将其转换为Class引用的类型。这种方式似乎比之前的强制转换更麻烦些，确实如此，而且当类型不能正确转换时，仍然会抛出ClassCastException异常。源码如下：
```java
public T cast(Object obj) {
    if (obj != null && !isInstance(obj))
         throw new ClassCastException(cannotCastMsg(obj));
     return (T) obj;
  }
```

#### 3.2 instanceof 关键字与isInstance方法
关于instanceof 关键字，它返回一个boolean类型的值，意在告诉我们对象是不是某个特定的类型实例。如下，在强制转换前利用instanceof检测obj是不是Animal类型的实例对象，如果返回true再进行类型转换，这样可以避免抛出类型转换的异常(ClassCastException)
```java
public void cast2(Object obj){
     if(obj instanceof Animal){
          Animal animal= (Animal) obj;
      }
}
```
而isInstance方法则是Class类中的一个Native方法，也是用于判断对象类型的，看个简单例子：
```java
public void cast2(Object obj){
        //instanceof关键字
        if(obj instanceof Animal){
            Animal animal= (Animal) obj;
        }

        //isInstance方法
        if(Animal.class.isInstance(obj)){
            Animal animal= (Animal) obj;
        }
  }
```
事实上instanceOf 与isInstance方法产生的结果是相同的。对于instanceOf是关键字只被用于对象引用变量，检查左边对象是不是右边类或接口的实例化。如果被测对象是null值，则测试结果总是false。一般形式：
```java
//判断这个对象是不是这种类型
obj.instanceof(class)
```
而isInstance方法则是Class类的Native方法，其中obj是被测试的对象或者变量，如果obj是调用这个方法的class或接口的实例，则返回true。如果被检测的对象是null或者基本类型，那么返回值是false;一般形式如下：
```java
//判断这个对象能不能被转化为这个类
class.isInstance(obj)
```
最后这里给出一个简单实例，验证isInstance方法与instanceof等价性：
```java
class A {}

class B extends A {}

public class C {
  static void test(Object x) {
    print("Testing x of type " + x.getClass());
    print("x instanceof A " + (x instanceof A));
    print("x instanceof B "+ (x instanceof B));
    print("A.isInstance(x) "+ A.class.isInstance(x));
    print("B.isInstance(x) " +
      B.class.isInstance(x));
    print("x.getClass() == A.class " +
      (x.getClass() == A.class));
    print("x.getClass() == B.class " +
      (x.getClass() == B.class));
    print("x.getClass().equals(A.class)) "+
      (x.getClass().equals(A.class)));
    print("x.getClass().equals(B.class)) " +
      (x.getClass().equals(B.class)));
  }
  public static void main(String[] args) {
    test(new A());
    test(new B());
  } 
}
```
执行结果：
```java
Testing x of type class com.zejian.A
x instanceof A true
x instanceof B false //父类不一定是子类的某个类型
A.isInstance(x) true
B.isInstance(x) false
x.getClass() == A.class true
x.getClass() == B.class false
x.getClass().equals(A.class)) true
x.getClass().equals(B.class)) false
---------------------------------------------
Testing x of type class com.zejian.B
x instanceof A true
x instanceof B true
A.isInstance(x) true
B.isInstance(x) true
x.getClass() == A.class false
x.getClass() == B.class true
x.getClass().equals(A.class)) false
x.getClass().equals(B.class)) true
```
到此关于Class对象相关的知识点都分析完了，下面将结合Class对象的知识点分析反射技术。



### 4. 理解反射技术
反射机制是在运行状态中，对于任意一个类，都能够知道这个类的所有属性和方法；对于任意一个对象，都能够调用它的任意一个方法和属性，这种动态获取的信息以及动态调用对象的方法的功能称为java语言的反射机制。一直以来反射技术都是Java中的闪亮点，这也是目前大部分框架(如Spring/Mybatis等)得以实现的支柱。在Java中，Class类与java.lang.reflect类库一起对反射技术进行了全力的支持。在反射包中，我们常用的类主要有Constructor类表示的是Class 对象所表示的类的构造方法，利用它可以在运行时动态创建对象、Field表示Class对象所表示的类的成员变量，通过它可以在运行时动态修改成员变量的属性值(包含private)、Method表示Class对象所表示的类的成员方法，通过它可以动态调用对象的方法(包含private)，下面将对这几个重要类进行分别说明。

#### 4.1 Constructor类及其用法
Constructor类存在于反射包(java.lang.reflect)中，反映的是Class 对象所表示的类的构造方法。获取Constructor对象是通过Class类中的方法获取的，Class类与Constructor相关的主要方法如下：

|方法返回值	|方法名称|方法说明|
|:--------:| :-------------|:-------------|
|static Class<?>	|forName(String className)|	返回与带有给定字符串名的类或接口相关联的 Class 对象。|
|Constructor<T>|	getConstructor(Class<?>... parameterTypes)	|返回指定参数类型、具有public访问权限的构造函数对象|
|Constructor<?>[]	|getConstructors()|	返回所有具有public访问权限的构造函数的Constructor对象数组|
|Constructor<T>	|getDeclaredConstructor(Class<?>... parameterTypes)|	返回指定参数类型、所有声明的（包括private）构造函数对象|
|Constructor<?>[]|	getDeclaredConstructor()|	返回所有声明的（包括private）构造函数对象|
|T|	newInstance()	|创建此 Class 对象所表示的类的一个新实例。|

下面看一个简单例子来了解Constructor对象的使用：
```java
public class ReflectDemo implements Serializable{
    public static void main(String[] args) throws Exception {

        Class<?> clazz = null;
        //获取Class对象的引用
        clazz = Class.forName("reflect.User");
        //第一种方法，实例化默认构造方法，User必须无参构造函数,否则将抛异常
        User user = (User) clazz.newInstance();
        user.setAge(20);
        user.setName("Rollen");
        System.out.println(user);
        System.out.println("--------------------------------------------");
        //获取带String参数的public构造函数
        Constructor cs1 =clazz.getConstructor(String.class);
        //创建User
        User user1= (User) cs1.newInstance("xiaolong");
        user1.setAge(22);
        System.out.println("user1:"+user1.toString());
        System.out.println("--------------------------------------------");
        //取得指定带int和String参数构造函数,该方法是私有构造private
        Constructor cs2=clazz.getDeclaredConstructor(int.class,String.class);
        //由于是private必须设置可访问
        cs2.setAccessible(true);
        //创建user对象
        User user2= (User) cs2.newInstance(25,"lidakang");
        System.out.println("user2:"+user2.toString());
        System.out.println("--------------------------------------------");
        //获取所有构造包含private
        Constructor<?> cons[] = clazz.getDeclaredConstructors();
        // 查看每个构造方法需要的参数
        for (int i = 0; i < cons.length; i++) {
            //获取构造函数参数类型
            Class<?> clazzs[] = cons[i].getParameterTypes();
            System.out.println("构造函数["+i+"]:"+cons[i].toString() );
            System.out.print("参数类型["+i+"]:(");
            for (int j = 0; j < clazzs.length; j++) {
                if (j == clazzs.length - 1)
                    System.out.print(clazzs[j].getName());
                else
                    System.out.print(clazzs[j].getName() + ",");
            }
            System.out.println(")");
        }
    }
}
class User {
    private int age;
    private String name;
    public User() {
        super();
    }
    public User(String name) {
        super();
        this.name = name;
    }
    /**
     * 私有构造
     */
    private User(int age, String name) {
        super();
        this.age = age;
        this.name = name;
    }
  //..........省略set 和 get方法
}
```
运行结果：
```shell
User [age=20, name=Rollen]
--------------------------------------------
user1:User [age=22, name=xiaolong]
--------------------------------------------
user2:User [age=25, name=lidakang]
--------------------------------------------
构造函数[0]:private reflect.User(int,java.lang.String)
参数类型[0]:(int,java.lang.String)
构造函数[1]:public reflect.User(java.lang.String)
参数类型[1]:(java.lang.String)
构造函数[2]:public reflect.User()
参数类型[2]:()
```
关于Constructor类本身一些常用方法如下(仅部分，其他可查API)

|方法返回值	|方法名称	|方法说明|
|:--------:| :-------------|:-------------|
|Class<T>|	getDeclaringClass()|	返回 Class 对象，该对象表示声明由此 Constructor 对象表示的构造方法的类,其实就是返回真实类型（不包含参数）|
|Type[]|	getGenericParameterTypes()|	按照声明顺序返回一组 Type 对象，返回的就是 Constructor对象构造函数的形参类型。|
|String|	getName()|	以字符串形式返回此构造方法的名称。|
|Class<?>[]	|getParameterTypes()	|按照声明顺序返回一组 Class 对象，即返回Constructor 对象所表示构造方法的形参类型|
|T	|newInstance(Object... initargs)|	使用此 Constructor对象表示的构造函数来创建新实例|
|String|	toGenericString()	|返回描述此 Constructor 的字符串，其中包括类型参数。|

代码演示如下：
```java
Constructor cs3=clazz.getDeclaredConstructor(int.class,String.class);
System.out.println("-----getDeclaringClass-----");
Class uclazz=cs3.getDeclaringClass();
//Constructor对象表示的构造方法的类
System.out.println("构造方法的类:"+uclazz.getName());

System.out.println("-----getGenericParameterTypes-----");
//对象表示此 Constructor 对象所表示的方法的形参类型
Type[] tps=cs3.getGenericParameterTypes();
for (Type tp:tps) {
    System.out.println("参数名称tp:"+tp);
}
System.out.println("-----getParameterTypes-----");
//获取构造函数参数类型
Class<?> clazzs[] = cs3.getParameterTypes();
for (Class claz:clazzs) {
    System.out.println("参数名称:"+claz.getName());
}
System.out.println("-----getName-----");
//以字符串形式返回此构造方法的名称
System.out.println("getName:"+cs3.getName());

System.out.println("-----getoGenericString-----");
//返回描述此 Constructor 的字符串，其中包括类型参数。
System.out.println("getoGenericString():"+cs3.toGenericString());
```
输出结果

```shell
 -----getDeclaringClass-----
 构造方法的类:reflect.User
 -----getGenericParameterTypes-----
 参数名称tp:int
 参数名称tp:class java.lang.String
 -----getParameterTypes-----
 参数名称:int
 参数名称:java.lang.String
 -----getName-----
 getName:reflect.User
 -----getoGenericString-----
 getoGenericString():private reflect.User(int,java.lang.String)
```
其中关于Type类型这里简单说明一下，Type 是 Java 编程语言中所有类型的公共高级接口。它们包括原始类型、参数化类型、数组类型、类型变量和基本类型。getGenericParameterTypes 与 getParameterTypes 都是获取构成函数的参数类型，前者返回的是Type类型，后者返回的是Class类型，由于Type顶级接口，Class也实现了该接口，因此Class类是Type的子类，Type 表示的全部类型而每个Class对象表示一个具体类型的实例，如String.class仅代表String类型。由此看来Type与 Class 表示类型几乎是相同的，只不过 Type表示的范围比Class要广得多而已。当然Type还有其他子类，如：
- TypeVariable：表示类型参数，可以有上界，比如：T extends Number
- ParameterizedType：表示参数化的类型，有原始类型和具体的类型参数，比如：List<String>
- WildcardType：表示通配符类型，比如：?, ? extends Number, ? super Integer

通过以上的分析，对于Constructor类已有比较清晰的理解，利用好Class类和Constructor类，我们可以在运行时动态创建任意对象，从而突破必须在编译期知道确切类型的障碍。

#### 4.2 Field类及其用法
Field 提供有关类或接口的单个字段的信息，以及对它的动态访问权限。反射的字段可能是一个类（静态）字段或实例字段。同样的道理，我们可以通过Class类的提供的方法来获取代表字段信息的Field对象，Class类与Field对象相关方法如下：

|方法返回值	|方法名称|	方法说明|
|:--------:| :-------------|:-------------|
|Field|	getDeclaredField(String name)|	获取指定name名称的(包含private修饰的)字段，不包括继承的字段|
|Field[]	|getDeclaredField()	|获取Class对象所表示的类或接口的所有(包含private修饰的)字段,不包括继承的字段|
|Field|	getField(String name)	|获取指定name名称、具有public修饰的字段，包含继承字段|
|Field[]|	getField()	|获取修饰符为public的字段，包含继承字段|
  
下面的代码演示了上述方法的使用过程
```java
public class ReflectField {
    public static void main(String[] args) throws ClassNotFoundException, NoSuchFieldException {
        Class<?> clazz = Class.forName("reflect.Student");
        //获取指定字段名称的Field类,注意字段修饰符必须为public而且存在该字段,
        // 否则抛NoSuchFieldException
        Field field = clazz.getField("age");
        System.out.println("field:"+field);

        //获取所有修饰符为public的字段,包含父类字段,注意修饰符为public才会获取
        Field fields[] = clazz.getFields();
        for (Field f:fields) {
            System.out.println("f:"+f.getDeclaringClass());
        }
        System.out.println("================getDeclaredFields====================");
        //获取当前类所字段(包含private字段),注意不包含父类的字段
        Field fields2[] = clazz.getDeclaredFields();
        for (Field f:fields2) {
            System.out.println("f2:"+f.getDeclaringClass());
        }
        //获取指定字段名称的Field类,可以是任意修饰符的自动,注意不包含父类的字段
        Field field2 = clazz.getDeclaredField("desc");
        System.out.println("field2:"+field2);
    }
}

class Person{
    public int age;
    public String name;
    //省略set和get方法
}

class Student extends Person{
    public String desc;
    private int score;
    //省略set和get方法
}
```
输出结果:
```shell
field:public int reflect.Person.age
f:public java.lang.String reflect.Student.desc
f:public int reflect.Person.age
f:public java.lang.String reflect.Person.name

================getDeclaredFields====================
f2:public java.lang.String reflect.Student.desc
f2:private int reflect.Student.score
field2:public java.lang.String reflect.Student.desc
```
上述方法需要注意的是，如果我们不期望获取其父类的字段，则需使用Class类的getDeclaredField/getDeclaredFields方法来获取字段即可，倘若需要连带获取到父类的字段，那么请使用Class类的getField/getFields，但是也只能获取到public修饰的的字段，无法获取父类的私有字段。下面将通过Field类本身的方法对指定类属性赋值，代码演示如下：
```java
//获取Class对象引用
Class<?> clazz = Class.forName("reflect.Student");

Student st= (Student) clazz.newInstance();
//获取父类public字段并赋值
Field ageField = clazz.getField("age");
ageField.set(st,18);
Field nameField = clazz.getField("name");
nameField.set(st,"Lily");

//只获取当前类的字段,不获取父类的字段
Field descField = clazz.getDeclaredField("desc");
descField.set(st,"I am student");
Field scoreField = clazz.getDeclaredField("score");
//设置可访问，score是private的
scoreField.setAccessible(true);
scoreField.set(st,88);
System.out.println(st.toString());

//输出结果：Student{age=18, name='Lily ,desc='I am student', score=88} 

//获取字段值
System.out.println(scoreField.get(st));
// 88
```
其中的set(Object obj, Object value)方法是Field类本身的方法，用于设置字段的值，而get(Object obj)则是获取字段的值，当然关于Field类还有其他常用的方法如下：

|方法返回值|	方法名称	|方法说明|
|:--------:| :-------------|:-------------|
|void|	set(Object obj, Object value)|	将指定对象变量上此 Field 对象表示的字段设置为指定的新值。|
|Object|	get(Object obj)|	返回指定对象上此 Field 表示的字段的值|
|Class<?>	|getType()|	返回一个 Class 对象，它标识了此Field 对象所表示字段的声明类型。|
|boolean	|isEnumConstant()|	如果此字段表示枚举类型的元素则返回 true；否则返回 false|
|String	|toGenericString()	|返回一个描述此 Field（包括其一般类型）的字符串|
|String	|getName()|	返回此 Field 对象表示的字段的名称|
|Class<?>	|getDeclaringClass()|	返回表示类或接口的 Class 对象，该类或接口声明由此 Field 对象表示的字段|
|void|	setAccessible(boolean flag)	|将此对象的 accessible 标志设置为指示的布尔值,即设置其可访问性|
  
上述方法可能是较为常用的，事实上在设置值的方法上，Field类还提供了专门针对基本数据类型的方法，如setInt()/getInt()、setBoolean()/getBoolean、setChar()/getChar()等等方法，这里就不全部列出了，需要时查API文档即可。需要特别注意的是被final关键字修饰的Field字段是安全的，在运行时可以接收任何修改，但最终其实际值是不会发生改变的。

#### 4.3 Method类及其用法
Method 提供关于类或接口上单独某个方法（以及如何访问该方法）的信息，所反映的方法可能是类方法或实例方法（包括抽象方法）。下面是Class类获取Method对象相关的方法：

|方法返回值|	方法名称|	方法说明|
|:--------:| :-------------:|:-------------|
|Method	|getDeclaredMethod(String name, Class<?>... parameterTypes)	|返回一个指定参数的Method对象，该对象反映此 Class 对象所表示的类或接口的指定已声明方法。|
|Method[]	|getDeclaredMethod()|	返回 Method 对象的一个数组，这些对象反映此 Class 对象表示的类或接口声明的所有方法，包括公共、保护、默认（包）访问和私有方法，但不包括继承的方法。|
|Method	|getMethod(String name, Class<?>... parameterTypes)	|返回一个 Method 对象，它反映此 Class 对象所表示的类或接口的指定公共成员方法。|
|Method[]	|getMethods()	|返回一个包含某些 Method 对象的数组，这些对象反映此 Class 对象所表示的类或接口（包括那些由该类或接口声明的以及从超类和超接口继承的那些的类或接口）的公共 member 方法。|

同样通过案例演示上述方法：
```java
public class ReflectMethod  {
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException {
        Class clazz = Class.forName("reflect.Circle");
        //根据参数获取public的Method,包含继承自父类的方法
        Method method = clazz.getMethod("draw",int.class,String.class);
        System.out.println("method:"+method);
        //获取所有public的方法:
        Method[] methods =clazz.getMethods();
        for (Method m:methods){
            System.out.println("m::"+m);
        }

        System.out.println("=========================================");

        //获取当前类的方法包含private,该方法无法获取继承自父类的method
        Method method1 = clazz.getDeclaredMethod("drawCircle");
        System.out.println("method1::"+method1);
        //获取当前类的所有方法包含private,该方法无法获取继承自父类的method
        Method[] methods1=clazz.getDeclaredMethods();
        for (Method m:methods1){
            System.out.println("m1::"+m);
        }
    }
}

class Shape {
    public void draw(){
        System.out.println("draw");
    }
    public void draw(int count , String name){
        System.out.println("draw "+ name +",count="+count);
    }

}
class Circle extends Shape{
    private void drawCircle(){
        System.out.println("drawCircle");
    }
    public int getAllCount(){
        return 100;
    }
}
```

输出结果:
```shell
method:public void reflect.Shape.draw(int,java.lang.String)
m::public int reflect.Circle.getAllCount()
m::public void reflect.Shape.draw()
m::public void reflect.Shape.draw(int,java.lang.String)
m::public final void java.lang.Object.wait(long,int) throws java.lang.InterruptedException
m::public final native void java.lang.Object.wait(long) throws java.lang.InterruptedException
m::public final void java.lang.Object.wait() throws java.lang.InterruptedException
m::public boolean java.lang.Object.equals(java.lang.Object)
m::public java.lang.String java.lang.Object.toString()
m::public native int java.lang.Object.hashCode()
m::public final native java.lang.Class java.lang.Object.getClass()
m::public final native void java.lang.Object.notify()
m::public final native void java.lang.Object.notifyAll()

=========================================
method1::private void reflect.Circle.drawCircle()

m1::public int reflect.Circle.getAllCount()
m1::private void reflect.Circle.drawCircle()
```
在通过getMethods方法获取Method对象时，会把父类的方法也获取到，如上的输出结果，把Object类的方法都打印出来了。而getDeclaredMethod/getDeclaredMethods方法都只能获取当前类的方法。我们在使用时根据情况选择即可。下面将演示通过Method对象调用指定类的方法：
```java
Class clazz = Class.forName("reflect.Circle");
//创建对象
Circle circle = (Circle) clazz.newInstance();

//获取指定参数的方法对象Method
Method method = clazz.getMethod("draw",int.class,String.class);

//通过Method对象的invoke(Object obj,Object... args)方法调用
method.invoke(circle,15,"圈圈");

//对私有无参方法的操作
Method method1 = clazz.getDeclaredMethod("drawCircle");
//修改私有方法的访问标识
method1.setAccessible(true);
method1.invoke(circle);

//对有返回值得方法操作
Method method2 =clazz.getDeclaredMethod("getAllCount");
Integer count = (Integer) method2.invoke(circle);
System.out.println("count:"+count);
```
输出结果：
```shell
draw 圈圈,count=15
drawCircle
count:100
```
在上述代码中调用方法，使用了Method类的invoke(Object obj,Object... args)第一个参数代表调用的对象，第二个参数传递的调用方法的参数。这样就完成了类方法的动态调用。

|方法返回值	|方法名称	|方法说明|
|:--------:| :-------------:|:-------------|
|Object	|invoke(Object obj, Object... args)|	对带有指定参数的指定对象调用由此 Method 对象表示的底层方法。|
|Class<?>	|getReturnType()	|返回一个 Class 对象，该对象描述了此 Method 对象所表示的方法的正式返回类型,即方法的返回类型|
|Type	|getGenericReturnType()|	返回表示由此 Method 对象所表示方法的正式返回类型的 Type 对象，也是方法的返回类型。|
|Class<?>[]	|getParameterTypes()|	按照声明顺序返回 Class 对象的数组，这些对象描述了此 Method 对象所表示的方法的形参类型。即返回方法的参数类型组成的数组|
|Type[]	|getGenericParameterTypes()	|按照声明顺序返回 Type 对象的数组，这些对象描述了此 Method 对象所表示的方法的形参类型的，也是返回方法的参数类型|
|String|	getName()|	以 String 形式返回此 Method 对象表示的方法名称，即返回方法的名称|
|boolean|	isVarArgs()	|判断方法是否带可变参数，如果将此方法声明为带有可变数量的参数，则返回 true；否则，返回 false。|
|String	|toGenericString()	|返回描述此 Method 的字符串，包括类型参数。|
  
getReturnType方法/getGenericReturnType方法都是获取Method对象表示的方法的返回类型，只不过前者返回的Class类型后者返回的Type(前面已分析过)，Type就是一个接口而已，在Java8中新增一个默认的方法实现，返回的就参数类型信息
```java
public interface Type {
    //1.8新增
    default String getTypeName() {
        return toString();
    }
}
```
而getParameterTypes/getGenericParameterTypes也是同样的道理，都是获取Method对象所表示的方法的参数类型，其他方法与前面的Field和Constructor是类似的。



### 5. 反射包中的Array类
在Java的java.lang.reflect包中存在着一个可以动态操作数组的类，Array，它提供了动态创建和访问 Java 数组的方法。Array 允许在执行 get 或 set 操作进行取值和赋值。在Class类中与数组关联的方法是：

|方法返回值	|方法名称|	方法说明|
|:--------:| :-------------:|:-------------|
|Class<?>	|getComponentType()|	返回表示数组元素类型的 Class，即数组的类型
|boolean|	isArray()	|判定此 Class 对象是否表示一个数组类。|

java.lang.reflect.Array中的常用静态方法如下：

|方法返回值|	方法名称|	方法说明|
|:--------:| :-------------:|:-------------|
|static Object	|set(Object array, int index)	|返回指定数组对象中索引组件的值。|
|static int	|getLength(Object array)|	以 int 形式返回指定数组对象的长度|
|static object	|newInstance(Class<?> componentType, int... dimensions)	|创建一个具有指定类型和维度的新数组。|
|static Object	|newInstance(Class<?> componentType, int length)|	创建一个具有指定的组件类型和长度的新数组。|
|static void	|set(Object array, int index, Object value)|	将指定数组对象中索引组件的值设置为指定的新值。|

下面通过一个简单例子来演示这些方法
```java
public class ReflectArray {

    public static void main(String[] args) throws ClassNotFoundException {
        int[] array = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        //获取数组类型的Class 即int.class
        Class<?> clazz = array.getClass().getComponentType();
        //创建一个具有指定的组件类型和长度的新数组。
        //第一个参数:数组的类型,第二个参数:数组的长度
        Object newArr = Array.newInstance(clazz, 15);
        //获取原数组的长度
        int co = Array.getLength(array);
        //赋值原数组到新数组
        System.arraycopy(array, 0, newArr, 0, co);
        for (int i:(int[]) newArr) {
            System.out.print(i+",");
        }

        //创建了一个长度为10 的字符串数组，
        //接着把索引位置为6 的元素设为"hello world!"，然后再读取索引位置为6 的元素的值
        Class clazz2 = Class.forName("java.lang.String");

        //创建一个长度为10的字符串数组，在Java中数组也可以作为Object对象
        Object array2 = Array.newInstance(clazz2, 10);

        //把字符串数组对象的索引位置为6的元素设置为"hello"
        Array.set(array2, 6, "hello world!");

        //获得字符串数组对象的索引位置为5的元素的值
        String str = (String)Array.get(array2, 6);
        System.out.println();
        System.out.println(str);//hello
    }
   
}
```
输出结果：
```shell
1,2,3,4,5,6,7,8,9,0,0,0,0,0,0,
hello world!
```
通过上述代码演示，确实可以利用Array类和反射相结合动态创建数组，也可以在运行时动态获取和设置数组中元素的值，其实除了上的set/get外Array还专门为8种基本数据类型提供特有的方法，如setInt/getInt、setBoolean/getBoolean，其他依次类推，需要使用是可以查看API文档即可。除了上述动态修改数组长度或者动态创建数组或动态获取值或设置值外，可以利用泛型动态创建泛型数组如下：
```java
/**
  * 接收一个泛型数组，然后创建一个长度与接收的数组长度一样的泛型数组，
  * 并把接收的数组的元素复制到新创建的数组中，
  * 最后找出新数组中的最小元素，并打印出来
  * @param a
  * @param <T>
  */
 public  <T extends Comparable<T>> void min(T[] a) {
     //通过反射创建相同类型的数组
     T[] b = (T[]) Array.newInstance(a.getClass().getComponentType(), a.length);
     for (int i = 0; i < a.length; i++) {
         b[i] = a[i];
     }
     T min = null;
     boolean flag = true;
     for (int i = 0; i < b.length; i++) {
         if (flag) {
             min = b[i];
             flag = false;
         }
         if (b[i].compareTo(min) < 0) {
             min = b[i];
         }
     }
     System.out.println(min);
 }
```
毕竟我们无法直接创建泛型数组，有了Array的动态创建数组的方式这个问题也就迎刃而解了。
```java
//无效语句，编译不通
T[] a = new T[];
```
ok~，到这反射中几个重要并且常用的类我们都基本介绍完了，但更重要是，我们应该认识到反射机制并没有什么神奇之处。当通过反射与一个未知类型的对象打交道时，JVM只会简单地检查这个对象，判断该对象属于那种类型，同时也应该知道，在使用反射机制创建对象前，必须确保已加载了这个类的Class对象，当然这点完全不必由我们操作，毕竟只能JVM加载，但必须确保该类的”.class”文件已存在并且JVM能够正确找到。关于Class类的方法在前面我们只是分析了主要的一些方法，其实Class类的API方法挺多的，建议查看一下API文档，浏览一遍，有个印象也是不错的选择，这里仅列出前面没有介绍过又可能用到的API：
```java
/** 
  *    修饰符、父类、实现的接口、注解相关 
  */

//获取修饰符，返回值可通过Modifier类进行解读
public native int getModifiers();
//获取父类，如果为Object，父类为null
public native Class<? super T> getSuperclass();
//对于类，为自己声明实现的所有接口，对于接口，为直接扩展的接口，不包括通过父类间接继承来的
public native Class<?>[] getInterfaces();
//自己声明的注解
public Annotation[] getDeclaredAnnotations();
//所有的注解，包括继承得到的
public Annotation[] getAnnotations();
//获取或检查指定类型的注解，包括继承得到的
public <A extends Annotation> A getAnnotation(Class<A> annotationClass);
public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass);

/** 
  *   内部类相关
  */
//获取所有的public的内部类和接口，包括从父类继承得到的
public Class<?>[] getClasses();
//获取自己声明的所有的内部类和接口
public Class<?>[] getDeclaredClasses();
//如果当前Class为内部类，获取声明该类的最外部的Class对象
public Class<?> getDeclaringClass();
//如果当前Class为内部类，获取直接包含该类的类
public Class<?> getEnclosingClass();
//如果当前Class为本地类或匿名内部类，返回包含它的方法
public Method getEnclosingMethod();

/** 
  *    Class对象类型判断相关
  */
//是否是数组
public native boolean isArray();  
//是否是基本类型
public native boolean isPrimitive();
//是否是接口
public native boolean isInterface();
//是否是枚举
public boolean isEnum();
//是否是注解
public boolean isAnnotation();
//是否是匿名内部类
public boolean isAnonymousClass();
//是否是成员类
public boolean isMemberClass();
//是否是本地类
public boolean isLocalClass(); 
```
完结。 
