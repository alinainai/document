## 1、获取 Class 对象引用的三种方式

### 1.1 通过继承自Object类的getClass方法获取，会触发类的初始化阶段
```java
new Candy().getClass();
```

### 1.2 通过`Class.forName()` 静态方法获取，无需通过持有该类的实例对象引用而去获取`Class`对象，会触发类的初始化阶段

```java
try{
  Class clazz=Class.forName("com.suger.Candy");
}catch()
```
### 1.3 通过字面常量的方式获取 Class 对象的引用不会自动初始化该类。

```java
Class clazz = Gum.class;
```
该方式不仅可以应用于普通的类，也可以应用用接口，数组以及基本数据类型。

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
### 1.4 看一个《Java编程思想上》的例子

```java
class Initable {
  
  static final int staticFinal = 47; //编译期静态常量
 
  static final int staticFinal2 = ClassInitialization.rand.nextInt(1000); //非编期静态常量
    
  static {
    System.out.println("Initializing Initable");
  }
}

class Initable2 {
  static int staticNonFinal = 147; //静态成员变量
  
  static {
    System.out.println("Initializing Initable2");
  }
}

class Initable3 {
  static int staticNonFinal = 74;//静态成员变量
  
  static {
    System.out.println("Initializing Initable3");
  }
}

public static void main(String[] args) throws Exception {

  Class initable = Initable.class;
  System.out.println("After creating Initable ref");
  
  System.out.println(Initable.staticFinal); //不触发类初始化
  
  System.out.println(Initable.staticFinal2); //会触发类初始化
  
  System.out.println(Initable2.staticNonFinal); //会触发类初始化
  
  Class initable3 = Class.forName("Initable3"); //forName方法获取Class对象
  System.out.println("After creating Initable3 ref");
  System.out.println(Initable3.staticNonFinal); //会触发类初始化
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
- 1.通过`字面常量`获取方式获取 `Initable` 类的 `Class` 对象并没有触发 Initable 类的初始化
- 2.同时发现调用 `Initable.staticFinal` 变量时也没有触发初始化，这是因为 `staticFinal` 属于编译期静态常量，在编译阶段通过常量传播优化的方式将 `Initable` 类的常量 `staticFinal` 存储到了一个称为 `NotInitialization` 类的常量池中，在以后对 `Initable` 类常量 `staticFinal` 的引用实际都转化为对 `NotInitialization` 类对自身常量池的引用，所以在编译期后，对编译期常量的引用都将在`NotInitialization` 类的常量池获取，这也就是引用编译期静态常量不会触发` Initable` 类初始化的重要原因。但在之后调用了 `Initable.staticFinal2` 变量后就触发了 `Initable` 类的初始化，注意 `staticFinal2` 虽然被 `static` 和 `final` 修饰，但其值在编译期并不能确定，因此 `staticFinal2` 并**不是编译期常量**，使用该变量必须先初始化 `Initable` 类。
- 3.`Initable2` 和 `Initable3` 类中都是静态成员变量并非编译期常量，引用都会触发初始化。


## 2、理解反射技术

>反射机制是在运行状态中，对于任意一个类，都能够知道这个类的所有属性和方法；

Class 类与 java.lang.reflect 类库一起对反射技术进行了全力的支持。

在反射包中，我们常用的类主要有

- `Constructor` 类表示的是 `Class` 对象所表示的类的构造方法，利用它可以在运行时动态创建对象;
- `Field` 表示 `Class` 对象所表示的类的成员变量，通过它可以在运行时动态修改成员变量的属性值(包含private);
- `Method` 表示 `Class` 对象所表示的类的成员方法，通过它可以动态调用对象的方法(包含private).

### 3.1 Constructor类及其用法

Constructor 位于 java.lang.reflect 中，反映的是 Class 对象所表示的类的构造方法。

|方法返回值	|方法名称|方法说明|
|:--------:| :-------------|:-------------|
|static Class<?>	|forName(String className)|	返回与带有给定字符串名的类或接口相关联的 Class 对象。|
|Constructor<T>   |getConstructor(Class<?>... parameterTypes)	|返回指定参数类型、具有public访问权限的构造函数对象|
|Constructor<?>[]	|getConstructors()|	返回所有具有public访问权限的构造函数的Constructor对象数组|
|Constructor<T>	  |getDeclaredConstructor(Class<?>... parameterTypes)|	返回指定参数类型、所有声明的（包括private）构造函数对象|
|Constructor<?>[] |getDeclaredConstructor()|	返回所有声明的（包括private）构造函数对象|
|T|	newInstance()	|创建此 Class 对象所表示的类的一个新实例。|

下面看一个简单例子来了解Constructor对象的使用：

```java
Class<?> clazz = null;
clazz = Class.forName("reflect.User");
User user = (User) clazz.newInstance(); //第一种方法，实例化默认构造方法，User类必须有无参构造函数,否则将抛异常
user.setAge(20);
user.setName("Rollen");// User [age=20, name=Rollen]

Constructor cs1 =clazz.getConstructor(String.class); // 获取带String参数的public构造函数
User user1= (User) cs1.newInstance("xiaolong");
user1.setAge(22); // user1:User [age=22, name=xiaolong]

Constructor cs2=clazz.getDeclaredConstructor(int.class,String.class);// 取得指定带int和String参数构造函数,该方法是私有构造private
cs2.setAccessible(true); //由于是private必须设置可访问
User user2= (User) cs2.newInstance(25,"lidakang");// user2:User [age=25, name=lidakang]

Constructor<?> cons[] = clazz.getDeclaredConstructors();//获取所有构造包含private
for (int i = 0; i < cons.length; i++) {
    Class<?> clazzs[] = cons[i].getParameterTypes();// 获取构造方法的参数类型
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
    private User(int age, String name) {
        super();
        this.age = age;
        this.name = name;
    }
}
```

关于Constructor类本身一些常用方法如下(仅部分，其他可查API)

|方法返回值	|方法名称	|方法说明|
|:--------:| :-------------|:-------------|
|Class<T>  |	getDeclaringClass()|	返回 Class 对象，该对象表示声明由此 Constructor 对象表示的构造方法的类,其实就是返回真实类型（不包含参数）|
|Type[]    |	getGenericParameterTypes()|	按照声明顺序返回一组 Type 对象，返回的就是 Constructor对象构造函数的形参类型。|
|String    |	getName()|	以字符串形式返回此构造方法的名称。|
|Class<?>[]|  getParameterTypes()	|按照声明顺序返回一组 Class 对象，即返回Constructor 对象所表示构造方法的形参类型|
|T	       |  newInstance(Object... initargs)|	使用此 Constructor对象表示的构造函数来创建新实例|
|String    |	toGenericString()	|返回描述此 Constructor 的字符串，其中包括类型参数。|

代码演示如下：
```java
Constructor cs3=clazz.getDeclaredConstructor(int.class,String.class);
Class uclazz=cs3.getDeclaringClass();// reflect.User
Type[] tps=cs3.getGenericParameterTypes();// int、java.lang.String
Class<?> clazzs[] = cs3.getParameterTypes(); // 
cs3.getName() // reflect.User
cs3.toGenericString() //private reflect.User(int,java.lang.String)
```

#### 4.2 Field类及其用法

Field 提供有关类或接口的单个字段的信息，以及对它的动态访问权限。

Class 类与 Field 对象相关方法如下：

|方法返回值	|方法名称|	方法说明|
|:--------:| :-------------|:-------------|
|Field     |	getDeclaredField(String name)|	获取指定name名称的(包含private修饰的)字段，不包括继承的字段|
|Field[]	 |  getDeclaredField()	|获取Class对象所表示的类或接口的所有(包含private修饰的)字段,不包括继承的字段|
|Field     |	getField(String name)	|获取指定name名称、具有 `public` 修饰的字段，包含继承字段|
|Field[]   |	getField()	|获取修饰符为 `public` 的字段，包含继承字段|
  
下面的代码演示了上述方法的使用过程
```java
Class<?> clazz = Class.forName("reflect.Student");

//注意字段修饰符必须为public而且存在该字段,否则抛NoSuchFieldException
Field field = clazz.getField("age"); // public int reflect.Person.age

//获取所有修饰符为public的字段,包含父类字段,注意修饰符为public才会获取
Field fields[] = clazz.getFields();// public java.lang.String reflect.Student.desc、public int reflect.Person.age、public java.lang.String reflect.Person.name

//获取当前类所字段(包含private字段),注意不包含父类的字段
Field fields2[] = clazz.getDeclaredFields(); // public java.lang.String reflect.Student.desc、private int reflect.Student.score

//获取指定字段名称的Field类,可以是任意修饰符的自动,注意不包含父类的字段
Field field2 = clazz.getDeclaredField("desc"); // public java.lang.String reflect.Student.desc

Field ageField = clazz.getField("age");
ageField.set(st,18);

Field scoreField = clazz.getDeclaredField("score");
//设置可访问，score是private的
scoreField.setAccessible(true);
scoreField.set(st,88);

class Person{
    public int age;
    public String name;
}

class Student extends Person{
    public String desc;
    private int score;
    //省略set和get方法
}
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

### 4.3 Method类及其用法

Method 提供关于类或接口上单独某个方法（以及如何访问该方法）的信息，所反映的方法可能是类方法或实例方法（包括抽象方法）。下面是Class类获取Method对象相关的方法：

|方法返回值|	方法名称|	方法说明|
|:--------:| :-------------:|:-------------|
|Method	|getDeclaredMethod(String name, Class<?>... parameterTypes)	|返回一个指定参数的Method对象，该对象反映此 Class 对象所表示的类或接口的指定已声明方法。|
|Method[]	|getDeclaredMethod()|	返回 Method 对象的一个数组，这些对象反映此 Class 对象表示的类或接口声明的所有方法，包括公共、保护、默认（包）访问和私有方法，但不包括继承的方法。|
|Method	|getMethod(String name, Class<?>... parameterTypes)	|返回一个 Method 对象，它反映此 Class 对象所表示的类或接口的指定 public 成员方法。|
|Method[]	|getMethods()	|返回一个包含某些 Method 对象的数组，这些对象反映此 Class 对象所表示的类或接口（包括那些由该类或接口声明的以及从超类和超接口继承的那些的类或接口）的公共 member 方法。|

同样通过案例演示上述方法：
```java
Class clazz = Class.forName("reflect.Circle");

//根据参数获取 public 的Method，包含继承自父类的方法
Method method = clazz.getMethod("draw",int.class,String.class);

//获取所有 public 的方法，包含继承自父类的方法
Method[] methods =clazz.getMethods();
// m::public int reflect.Circle.getAllCount()
// m::public void reflect.Shape.draw()
// m::public void reflect.Shape.draw(int,java.lang.String)
// m::public final void java.lang.Object.wait(long,int) throws java.lang.InterruptedException
// m::public final native void java.lang.Object.wait(long) throws java.lang.InterruptedException
// m::public final void java.lang.Object.wait() throws java.lang.InterruptedException
// m::public boolean java.lang.Object.equals(java.lang.Object)
// m::public java.lang.String java.lang.Object.toString()
// m::public native int java.lang.Object.hashCode()
// m::public final native java.lang.Class java.lang.Object.getClass()
// m::public final native void java.lang.Object.notify()
// m::public final native void java.lang.Object.notifyAll()

//获取当前类的方法包含private,该方法无法获取继承自父类的method
Method method1 = clazz.getDeclaredMethod("drawCircle");

//获取当前类的所有方法包含private,该方法无法获取继承自父类的method
Method[] methods1=clazz.getDeclaredMethods();
// m1::public int reflect.Circle.getAllCount()
// m1::private void reflect.Circle.drawCircle()

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
