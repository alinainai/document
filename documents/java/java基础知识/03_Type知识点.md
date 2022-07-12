## 1、Type 类型

`Type` 是 `Java` 语言中所有类型的公共父接口，其从 `JDK5` 开始引入，引入的目的主要是为了支持泛型。

没有泛型的之前，`Java` 只有所谓的原始类型(`raw types`)。此时，所有的原始类型都通过字节码类 `Class` 进行抽象。`Class` 类的一个具体对象(例如 `String.class`)就代表一个指定的原始类型。

泛型的出现扩充了数据类型的概念，从只有原始类型(`raw types`)扩充了`参数化类型、类型变量类型、泛型数组类型和通配符类型`。他们都是 `Type` 的子接口。

```java
// Type is the common superinterface for all types in the Java programming language.
// These include raw types, parameterized types, array types, type variables and primitive types.
// @since 1.5
public interface Type {
    // Returns a string describing this type, including information about any type parameters.
    default String getTypeName() {
        return toString();
    }
}
```
`Class` 也是 `Type` 的一个实现类
```java
class Class<T> implements Serializable, GenericDeclaration, Type, AnnotatedElement
```

- 所有已知子接口：`GenericArrayType, ParameterizedType, TypeVariable<D>, WildcardType`
- 所有已知实现类：`Class`
  
## 2、Java 中的所有类型
  
### 2.1 原始类型   

`raw type`：原始类型，对应 `Class`

- 即我们通常说的引用类型，包括普通的类，例如 `String.class、List.class`
- 也包括`数组(Array.class)`、`接口(Cloneable.class)`、`注解(Annotation.class)`、`枚举(Enum.class)`等
 
### 2.2 基本类型 

`primitive types`：基本类型，对应 `Class`
  
- 包括 `Built-in` 内置类型，例如 `int.class、char.class、void.class`
- 也包括 `Wrappers` 内置类型包装类型，例如 `Integer.class、Boolean.class、Void.class`

### 2.3 参数化类型 
  
`parameterized types`：参数化类型，对应 `ParameterizedType`
  
- 带有类型参数的类型，即常说的泛型，例如 `List<T>、Map<Integer, String>、List<? extends Number>`
- 实现类 `sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl`

### 2.4 类型变量类型

`type variables`：类型变量类型，对应 `TypeVariable<D>`

- 即参数化类型 `ParameterizedType` 中的 `E、Kv 等类型变量，表示泛指任何类
- 实现类 `sun.reflect.generics.reflectiveObjects.TypeVariableImpl`

### 2.5 泛型数组类型

`array types`：泛型数组类型，对应 `GenericArrayType`

- 元素类型是`参数化类型`或者`类型变量的泛型数组类型`，例如 `T[]`
- 实现类 `sun.reflect.generics.reflectiveObjects.GenericArrayTypeImpl`

>`Type` 接口的另一个子接口 `WildcardType` 代表通配符表达式类型，或泛型表达式类型，比如`?`、`? super T`、`? extends Tv，他并不是 `Java` 类型中的一种。

## 3、综合测试代码

```java
public class Test {
    public static void main(String[] args) throws NoSuchMethodException {
        new Test().showType();
    }
    
    private void showType() throws NoSuchMethodException {
        // 注意 int.class 和 Integer.class 是不一样的(没有所谓的自动装箱、自动拆箱机制)，不能互用
        Class<?> clazz = List.class;
        Method method = Test.class.getMethod("testType", int.class, Boolean.class, clazz, clazz, clazz, clazz, clazz, Map.class);
        Type[] genericParameterTypes = method.getGenericParameterTypes(); //按照方法参数声明顺序返回参数的 Type 数组
        for (Type type : genericParameterTypes) {
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type[] types = parameterizedType.getActualTypeArguments(); //返回表示此类型【实际类型参数】的 Type 数组
                for (int i = 0; i < types.length; i++) {
                    System.out.println(i + getTypeInfo(types[i]));
                }
            } else {
                System.out.println(" " + getTypeInfo(type));
            }
        }
    }
    
    private String getTypeInfo(Type type) {
        String typeName = type.getTypeName();
        Class<?> clazz = type.getClass();
        Class<?>[] interfaces = clazz.getInterfaces();
        StringBuilder typeInterface = new StringBuilder();
        for (Class<?> clazzType : interfaces) {
            typeInterface.append(clazzType.getSimpleName()).append(",");
        }
        return "[" + typeName + "]    [" + clazz.getSimpleName() + "]    [" + typeInterface + "]";
    }
    
    public <T> void testType(int i, Boolean b, List<String> a1, List<ArrayList<String>> a2, List<T> a3, //
                             List<? extends Number> a4, List<ArrayList<String>[]> a5, Map<Boolean, Integer> a6) {
    }
}
```

运行结果

```shell
  [int]    [Class]    [Serializable,GenericDeclaration,Type,AnnotatedElement,OfField,Constable,]
  [java.lang.Boolean]    [Class]    [Serializable,GenericDeclaration,Type,AnnotatedElement,OfField,Constable,]
0 [java.lang.String]    [Class]    [Serializable,GenericDeclaration,Type,AnnotatedElement,OfField,Constable,]
0 [java.util.ArrayList]    [ParameterizedTypeImpl]    [ParameterizedType,]
0 [T]    [TypeVariableImpl]    [TypeVariable,]
0 [? extends java.lang.Number]    [WildcardTypeImpl]    [WildcardType,]
0 [java.util.ArrayList[]]    [GenericArrayTypeImpl]    [GenericArrayType,]
0 [java.lang.Boolean]    [Class]    [Serializable,GenericDeclaration,Type,AnnotatedElement,OfField,Constable,]
1 [java.lang.Integer]    [Class]    [Serializable,GenericDeclaration,Type,AnnotatedElement,OfField,Constable,]

```

## 4、Type 的子接口

### 4.1 `ParameterizedType` 参数化类型

`ParameterizedType` 表示参数化类型，带有类型参数的类型，即常说的泛型，如：`List<T>、Map<Integer, String>、List<? extends Number>`。

```java
// ParameterizedType represents a parameterized type such as Collection<String>.
public interface ParameterizedType extends Type
```
方法

- `Type[] getActualTypeArguments()`

Returns an array of Type objects representing the actual type arguments to this type.
Note that in some cases, the returned array be empty. This can occur if this type represents a non-parameterized type nested within a parameterized type.

简单来说就是获得<>里的类型参数的类型，可能有多个类型参数，例如 Map<K, V>，也可能没有类型参数

- `Type getOwnerType()`

Returns a Type object representing the type that this type is a member of.
For example, if this type is O<T>.I<S>, return a representation of O<T>.
If this type is a top-level type, null is returned.

- `Type getRawType()v

Returns the Type object representing the class or interface that declared this type
返回声明此 `Type` 的类或接口，简单来说就是返回`<>`前面那个类型，例如`Map<K ,V>`返回的是`Map`

测试代码

```java
private void testParameterizedType() throws NoSuchMethodException {
    Method method = Test.class.getMethod("testType", Map.Entry.class);
    ParameterizedType parameterizedType = (ParameterizedType) method.getGenericParameterTypes()[0];
    
    System.out.println("getOwnerType  " + getTypeInfo(parameterizedType.getOwnerType()));
    System.out.println("getRawType  " + getTypeInfo(parameterizedType.getRawType()));
    
    Type[] types = parameterizedType.getActualTypeArguments();
    for (Type type : types) {
        System.out.println(getTypeInfo(type));
    }
}

public <T> void testType(Map.Entry<String, T> mapEntry) {
}
```
```shell
getOwnerType  【java.util.Map】    【Class】    【Serializable,GenericDeclaration,Type,AnnotatedElement,】
getRawType  【java.util.Map$Entry】    【Class】    【Serializable,GenericDeclaration,Type,AnnotatedElement,】
【java.lang.String】    【Class】    【Serializable,GenericDeclaration,Type,AnnotatedElement,】
【T】    【TypeVariableImpl】    【TypeVariable,】
```
### 4.2 TypeVariable 类型变量类型

`TypeVariable` 表示类型变量类型，如参数化类型中的 `E、K` 等类型变量，表示泛指任何类。

```java
// TypeVariable is the common superinterface for type variables of kinds 各种类型变量
public interface TypeVariable<D extends GenericDeclaration> extends Type

// Type Parameters D: the type of generic declaration 泛型声明的类型 that declared the underlying type variable 基础类型变量.
```
方法

- `Type[] getBounds()`
Returns an array of Type objects representing the upper bound(s) of this type variable.
Note that if no upper bound is explicitly declared, the upper bound is Object.

- `D getGenericDeclaration()` 。
Returns the GenericDeclaration object representing the generic declaration declared this type variable 声明此类型变量的泛型声明

- `String getName()`
Returns the name of this type variable, as it occurs in the source code.

测试代码

```java
private void testTypeVariable() throws NoSuchMethodException {
    Method method = Test.class.getMethod("testType");
    TypeVariable<?>[] typeVariables = method.getTypeParameters(); //返回泛型声明的 TypeVariable 数组
    
    for (int i = 0; i < typeVariables.length; i++) {
        TypeVariable<?> typeVariable = typeVariables[i];
        Type[] bounds = typeVariable.getBounds();
        GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration(); //【public void Test.test()】
        boolean isSameObj = genericDeclaration.getTypeParameters()[i] == typeVariable; // true，是同一个对象
        
        System.out.println(getTypeInfo(typeVariable));
        for (Type type : bounds) {
            System.out.println("    " + getTypeInfo(type));
        }
    }
}

public <T extends List<String>, U extends Integer, Int> void testType() {
}
```
```shell
【T】    【TypeVariableImpl】    【TypeVariable,】
    【java.util.List<java.lang.String>】    【ParameterizedTypeImpl】    【ParameterizedType,】
【U】    【TypeVariableImpl】    【TypeVariable,】
    【java.lang.Integer】    【Class】    【Serializable,GenericDeclaration,Type,AnnotatedElement,】
【Int】    【TypeVariableImpl】    【TypeVariable,】
    【java.lang.Object】    【Class】    【Serializable,GenericDeclaration,Type,AnnotatedElement,】
 ```  
 
### 4.3 GenericArrayType 泛型数组类型
 
`GenericArrayType` 表示泛型数组类型，比如 `T[]`。注意，这不是我们说的一般数组，而是表示一种【元素类型是参数化类型或者类型变量】的数组类型。

```java
// GenericArrayType represents an array type 数组类型 whose component type 组件(元素)类型 is either a parameterized type 参数化类型 or a type variable 类型变量.
public interface GenericArrayType extends Type{
    // Returns a Type object representing the **component type** of this array
    Type  getGenericComponentType(); // 获取泛型数组中元素的类型
}
```
测试代码

```java
private void testGenericArrayType() throws NoSuchMethodException {
    Method method = Test.class.getMethod("testType", Object[].class, String[].class, List.class);
    Type[] types = method.getGenericParameterTypes(); //按照方法参数声明顺序返回参数的 Type 数组
    for (Type type : types) {
        System.out.println(getTypeInfo(type));
        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            System.out.println("    " + getTypeInfo(genericArrayType.getGenericComponentType()));
        }
    }
}

// 只有第一个参数是【泛型数组】类型
public <T> void testType(T[] a1, String[] a2, List<T> a3) {
}
```
    
```shell
【T[]】    【GenericArrayTypeImpl】    【GenericArrayType,】
    【T】    【TypeVariableImpl】    【TypeVariable,】
【java.lang.String[]】    【Class】    【Serializable,GenericDeclaration,Type,AnnotatedElement,】
【java.util.List<T>】    【ParameterizedTypeImpl】    【ParameterizedType,】
```
    
### 4.4 WildcardType 通配符类型
    
`WildcardType` 代表通配符类型，或泛型表达式类型，比如`?`、`? super T`、`? extends T`，他并不是 `Java` 类型中的一种。

```java
// WildcardType represents a wildcard type expression 通配符类型表达式, such as ?, ? extends Number, or ? super Integer.
public interface WildcardType extends Type
```
方法

- `Type[] getUpperBounds()`
    
Returns an array of Type objects representing the upper bound(s) of this type variable.
Note that if no upper bound is explicitly declared 明确声明, the upper bound is Object.

- `Type[] getLowerBounds()`
Returns an array of Type objects representing the lower bound(s) of this type variable.
Note that if no lower bound is explicitly declared, the lower bound is the type of null. In this case, a zero length array is returned.

```java
private void testWildcardType() throws NoSuchMethodException {
    Method method = Test.class.getMethod("testType", List.class, List.class, List.class, List.class);
    Type[] types = method.getGenericParameterTypes(); //按照方法参数声明顺序返回参数的 Type 数组
    for (Type type : types) {
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments(); //返回表示此类型【实际类型参数】的 Type 数组
        if (actualTypeArguments[0] instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) actualTypeArguments[0];
            System.out.println("是通配符类型" + getTypeInfo(wildcardType));
            for (Type upperType : wildcardType.getUpperBounds()) {
                System.out.println("  upperType" + getTypeInfo(upperType));
            }
            for (Type lowerType : wildcardType.getLowerBounds()) {
                System.out.println("  lowerType" + getTypeInfo(lowerType));
            }
        } else {
            System.out.println("非通配符类型" + getTypeInfo(actualTypeArguments[0]));
        }
    }
}

public <T> void testType(List<T> a1, List<?> a2, List<? extends T> a3, List<? super Integer> a4) {
}
```

```java
非通配符类型【T】    【TypeVariableImpl】    【TypeVariable,】
是通配符类型【?】    【WildcardTypeImpl】    【WildcardType,】
  upperType【java.lang.Object】    【Class】    【Serializable,GenericDeclaration,Type,AnnotatedElement,】
是通配符类型【? extends T】    【WildcardTypeImpl】    【WildcardType,】
  upperType【T】    【TypeVariableImpl】    【TypeVariable,】
是通配符类型【? super java.lang.Integer】    【WildcardTypeImpl】    【WildcardType,】
  upperType【java.lang.Object】    【Class】    【Serializable,GenericDeclaration,Type,AnnotatedElement,】
  lowerType【java.lang.Integer】    【Class】    【Serializable,GenericDeclaration,Type,AnnotatedElement,】
```
