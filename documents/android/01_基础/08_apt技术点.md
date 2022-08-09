## 1、什么是 APT

`APT（Annotation Processing Tool，注解处理器）`直译过来就是处理注解的工具，属于 `javac` 的一个工具。用来在编译时扫描和处理注解，然后以`Java`代码(或者编译过的字节码)作为输入，然后生成`.java`文件作为输出。

简单来说就是在编译期通过 解析注解生成`java`文件。

## 2、实现

下面我们通过实现一个简单的例子（类似于`ButterKnife`中的`@BindView`）来看下 APT 是怎么使用的。

### 2.1 创建项目

首先我们创建一个 `android` 项目，结构如下

```java
创建 项目 AptDemo (app) 
创建 Java library Modul "apt-annotation" (实现自定义注解 @BindView)
创建 Java library Module "apt-processor" (注解处理器，根据`apt-annotation`中的注解，在编译期生成`xxxActivity_ViewBinding.java`代码) 并依赖 apt-annotation 
创建 Android library Module "apt-library" (工具类，调用`xxxActivity_ViewBinding.java`中的 bind 方法，实现`View`的绑定) 并依赖 apt-annotation 
app 添加 apt-annotation、apt-processor、apt-library 依赖
```


### 2.2 定义 @BindView 注解

在 apt-annotation 类库中定义注解类`@BindView`

```java
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface BindView {
    int value(); //对应View的id
}
```

### 2.2 实现 apt-processor 类库中的代码

1、首先添加添加依赖
```groove
dependencies {
    implementation 'com.google.auto.service:auto-service:1.0-rc2' 
    annotationProcessor  'com.google.auto.service:auto-service:1.0-rc2' 
    implementation project(':apt-annotation')
}
```
2、创建 BindViewProcessor
```java
@AutoService(Processor.class) // 通过 auto-service 中的 @AutoService 可以自动生成 META-INF/services/javax.annotation.processing.Processor (注解的注册文件)
public class BindViewProcessor extends AbstractProcessor {

    private Messager mMessager;
    private Elements mElementUtils;
    private final Map<String, ClassCreatorProxy> mProxyMap = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);// ProcessingEnviroment 提供了很多有用的工具类 Elements, Types 和 Filer
        mMessager = processingEnv.getMessager();
        mElementUtils = processingEnv.getElementUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> supportTypes = new LinkedHashSet<>();
        //指定这个注解处理器是注册给哪个注解的，这里说明是注解BindView
        supportTypes.add(BindView.class.getCanonicalName());
        return supportTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported(); // 指定使用的Java版本，通常这里返回 SourceVersion.latestSupported()
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {//扫描、评估和处理注解的代码，生成Java文件
        mMessager.printMessage(Diagnostic.Kind.NOTE, "processing...");
        mProxyMap.clear();
        //得到所有的注解
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(BindView.class);
        for (Element element : elements) {
            VariableElement variableElement = (VariableElement) element;
            TypeElement classElement = (TypeElement) variableElement.getEnclosingElement();
            String fullClassName = classElement.getQualifiedName().toString();
            ClassCreatorProxy proxy = mProxyMap.get(fullClassName);
            if (proxy == null) {
                proxy = new ClassCreatorProxy(mElementUtils, classElement);
                mProxyMap.put(fullClassName, proxy);
            }
            BindView bindAnnotation = variableElement.getAnnotation(BindView.class);
            int id = bindAnnotation.value();
            proxy.putElement(id, variableElement);
        }
        //通过遍历mProxyMap，创建java文件
        for (String key : mProxyMap.keySet()) {
            ClassCreatorProxy proxyInfo = mProxyMap.get(key);
            try {
                mMessager.printMessage(Diagnostic.Kind.NOTE, " --> create " + proxyInfo.getProxyClassFullName());
                JavaFileObject jfo = processingEnv.getFiler().createSourceFile(proxyInfo.getProxyClassFullName(), proxyInfo.getTypeElement());
                Writer writer = jfo.openWriter();
                writer.write(proxyInfo.generateJavaCode());
                writer.flush();
                writer.close();
            } catch (IOException e) {
                mMessager.printMessage(Diagnostic.Kind.NOTE, " --> create " + proxyInfo.getProxyClassFullName() + "error");
            }
        }
        mMessager.printMessage(Diagnostic.Kind.NOTE, "process finish ...");
        return true;
    }
}
```
通过`roundEnvironment.getElementsAnnotatedWith(BindView.class`)得到所有注解`elements`，然后将`elements`的信息保存到`mProxyMap`中，最后通过`mProxyMap`创建对应的`Java`文件，其中`mProxyMap`是`ClassCreatorProxy`的`Map`集合。

3、`ClassCreatorProxy`工具类
`ClassCreatorProxy` 是创建`Java代码`的代理类，如下：
```java
public class ClassCreatorProxy {
    private final String mBindingClassName;
    private final String mPackageName;
    private final TypeElement mTypeElement;
    private final Map<Integer, VariableElement> mVariableElementMap = new HashMap<>();

    public ClassCreatorProxy(Elements elementUtils, TypeElement classElement) {
        this.mTypeElement = classElement;
        PackageElement packageElement = elementUtils.getPackageOf(mTypeElement);
        String packageName = packageElement.getQualifiedName().toString();
        String className = mTypeElement.getSimpleName().toString();
        this.mPackageName = packageName;
        this.mBindingClassName = className + "_ViewBinding";
    }

    public void putElement(int id, VariableElement element) {
        mVariableElementMap.put(id, element);
    }

    public String generateJavaCode() {
        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(mPackageName).append(";\n\n");
        builder.append("import com.gas.library.*;\n");
        builder.append('\n');
        builder.append("public class ").append(mBindingClassName);
        builder.append(" {\n");

        generateMethods(builder);
        builder.append('\n');
        builder.append("}\n");
        return builder.toString();
    }

    private void generateMethods(StringBuilder builder) {
        builder.append("public void bind(" + mTypeElement.getQualifiedName() + " host ) {\n");
        for (int id : mVariableElementMap.keySet()) {
            VariableElement element = mVariableElementMap.get(id);
            String name = element.getSimpleName().toString();
            String type = element.asType().toString();
            builder.append("host." + name).append(" = ");
            builder.append("(" + type + ")(((android.app.Activity)host).findViewById( " + id + "));\n");
        }
        builder.append("  }\n");
    }

    public String getProxyClassFullName() {
        return mPackageName + "." + mBindingClassName;
    }

    public TypeElement getTypeElement() {
        return mTypeElement;
    }
}
```
上面的代码主要就是从`Elements、TypeElement`得到想要的一些信息，如`package name`、`Activity名`、`变量类型`、`id`等，通过`StringBuilder`一点一点拼出`Java`代码，每个对象分别代表一个对应的`.java`文件。

4、生成的 java 代码
```java
public class MainActivity_ViewBinding {
public void bind(com.gas.aptdemo.MainActivity host ) {
host.mTextView = (android.widget.TextView)(((android.app.Activity)host).findViewById( 2131231106));
  }

}
```
### 2.3 apt-library 工具类

在Module的build.gradle中添加依赖
```groove
dependencies {
    implementation project(':apt-annotation')
}
```
创建注解工具类BindViewTools
```java
public class BindViewTools {
   public static void bind(Activity activity) {
      Class<? extends Activity> clazz = activity.getClass();
      try {
         Class<?> bindViewClass = Class.forName(clazz.getName() + "_ViewBinding");
         Method method = bindViewClass.getMethod("bind", activity.getClass());
         method.invoke(bindViewClass.newInstance(), activity);
      } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
         e.printStackTrace();
      }
   }
}
```
apt-library 的部分就比较简单了，通过反射找到对应的ViewBinding类，然后调用其中的 bind() 方法完成 View 的绑定。

### 2.4 app 中的代码
```groove
dependencies {
    implementation project(':apt-annotation')
    implementation project(':apt-library')
    kapt project(':apt-processor')
}
```
在MainActivity中，在View的前面加上 BindView 注解，把 id 传入即可
```kotlin
class MainActivity : AppCompatActivity() {

    @BindView(R.id.textView)
    lateinit var mTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        BindViewTools.bind(this)
        mTextView.text = "aptdemo"
    }
}
```

代码分支 [main](https://github.com/alinainai/AptDemo/tree/main)

## 3、通过javapoet生成代码

[javapoet](https://github.com/square/javapoet) 

上面在 ClassCreatorProxy 中，通过 StringBuilder 来生成对应的 Java 代码。这种做法是比较麻烦的，还有一种更优雅的方式，那就是 javapoet。

先添加依赖
```groove
dependencies {
    implementation 'com.squareup:javapoet:1.10.0'
}
```

然后在 ClassCreatorProxy 中替换代码的生成方式

```java
public class ClassCreatorProxy {
    //...
    public TypeSpec generateJavaCode() {
        TypeSpec bindingClass = TypeSpec.classBuilder(mBindingClassName)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(generateMethods())
                .build();
        return bindingClass;

    }

    private MethodSpec generateMethods() {
        ClassName host = ClassName.bestGuess(mTypeElement.getQualifiedName().toString());
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("bind")
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(host, "host");

        for (int id : mVariableElementMap.keySet()) {
            VariableElement element = mVariableElementMap.get(id);
            String name = element.getSimpleName().toString();
            String type = element.asType().toString();
            methodBuilder.addCode("host." + name + " = " + "(" + type + ")(((android.app.Activity)host).findViewById( " + id + "));");
        }
        return methodBuilder.build();
    }

    public String getPackageName() {
        return mPackageName;
    }
}
```
最后在 `BindViewProcessor` 中做如下改动
```java
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //省略部分代码...
        //通过javapoet生成
        for (String key : mProxyMap.keySet()) {
            ClassCreatorProxy proxyInfo = mProxyMap.get(key);
            JavaFile javaFile = JavaFile.builder(proxyInfo.getPackageName(), proxyInfo.generateJavaCode()).build();
            try {
                //　生成文件
                javaFile.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mMessager.printMessage(Diagnostic.Kind.NOTE, "process finish ...");
        return true;
    }
```

代码分支 [main_poet](https://github.com/alinainai/AndroidDemo/tree/feature_apt)

## 参考

[【Android】APT](https://www.jianshu.com/p/7af58e8e3e18)

[Android APT 系列 （三）：APT 技术探究](https://juejin.cn/post/6978500975770206239)

