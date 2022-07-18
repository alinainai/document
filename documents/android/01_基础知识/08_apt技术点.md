## 1、什么是 APT

`APT（Annotation Processing Tool）`，注解处理器。是一种处理注解的工具，确切的说它是`javac`的一个工具，它用来在编译时扫描和处理注解。
注解处理器以`Java`代码(或者编译过的字节码)作为输入，生成`.java`文件作为输出。

简单来说就是在编译期，通过注解生成`.java`文件。

## 2、实现

下面我们通过实现一个简单的例子（类似于`ButterKnife`中的`@BindView`）来看下 APT 是怎么使用的。

### 2.1 创建项目

首先我们创建一个 `android` 项目，结构如下

```java
创建项目 APTDemo (app) 
创建Java library Module命名为 apt-annotation
创建Java library Module命名为 apt-processor 依赖 apt-annotation
创建Android library Module 命名为apt-library依赖 apt-annotation、auto-service
```
包说明：

- `apt-annotation`：自定义注解，存放`@BindView`
- `apt-processor`：注解处理器，根据`apt-annotation`中的注解，在编译期生成`xxxActivity_ViewBinding.java`代码
- `apt-library`：工具类，调用`xxxActivity_ViewBinding.java`中的方法，实现`View`的绑定。

### 2.2 实现
1、apt-annotation（自定义注解）

创建注解类`BindView`
```java
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface BindView {
    int value(); //对应View的id
}
```
2、apt-processor（注解处理器）

在`Module`中添加依赖

dependencies {
    implementation 'com.google.auto.service:auto-service:1.0-rc2' 
    // Gradle 5.0后需要再加下面这行
    // annotationProcessor  'com.google.auto.service:auto-service:1.0-rc2' 
    implementation project(':apt-annotation')
}

创建BindViewProcessor
```java
@AutoService(Processor.class)
public class BindViewProcessor extends AbstractProcessor {

    private Messager mMessager;
    private Elements mElementUtils;
    private Map<String, ClassCreatorProxy> mProxyMap = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mMessager = processingEnv.getMessager();
        mElementUtils = processingEnv.getElementUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> supportTypes = new LinkedHashSet<>();
        supportTypes.add(BindView.class.getCanonicalName());
        return supportTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnv) {
        //根据注解生成Java文件
        return false;
    }
}
```
init：初始化。可以得到ProcessingEnviroment，ProcessingEnviroment提供很多有用的工具类Elements, Types 和 Filer
getSupportedAnnotationTypes：指定这个注解处理器是注册给哪个注解的，这里说明是注解BindView
getSupportedSourceVersion：指定使用的Java版本，通常这里返回 SourceVersion.latestSupported()
process：可以在这里写扫描、评估和处理注解的代码，生成Java文件（process中的代码下面详细说明）
```java
@AutoService(Processor.class)
public class BindViewProcessor extends AbstractProcessor {

    private Messager mMessager;
    private Elements mElementUtils;
    private Map<String, ClassCreatorProxy> mProxyMap = new HashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        mMessager.printMessage(Diagnostic.Kind.NOTE, "processing...");
        mProxyMap.clear();
        //得到所有的注解
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(BindView.class);
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
通过roundEnvironment.getElementsAnnotatedWith(BindView.class)得到所有注解elements，然后将elements的信息保存到mProxyMap中，最后通过mProxyMap创建对应的Java文件，其中mProxyMap是ClassCreatorProxy的Map集合。

ClassCreatorProxy是创建Java代码的代理类，如下：
```java
public class ClassCreatorProxy {
    private String mBindingClassName;
    private String mPackageName;
    private TypeElement mTypeElement;
    private Map<Integer, VariableElement> mVariableElementMap = new HashMap<>();

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

    /**
     * 创建Java代码
     * @return
     */
    public String generateJavaCode() {
        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(mPackageName).append(";\n\n");
        builder.append("import com.example.gavin.apt_library.*;\n");
        builder.append('\n');
        builder.append("public class ").append(mBindingClassName);
        builder.append(" {\n");

        generateMethods(builder);
        builder.append('\n');
        builder.append("}\n");
        return builder.toString();
    }

    /**
     * 加入Method
     * @param builder
     */
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

    public String getProxyClassFullName()
    {
        return mPackageName + "." + mBindingClassName;
    }

    public TypeElement getTypeElement()
    {
        return mTypeElement;
    }
}
```
上面的代码主要就是从Elements、TypeElement得到想要的一些信息，如package name、Activity名、变量类型、id等，通过StringBuilder一点一点拼出Java代码，每个对象分别代表一个对应的.java文件。

看下生成的代码（不大整齐，被我格式化了）

public class MainActivity_ViewBinding {
    public void bind(com.example.gavin.apttest.MainActivity host) {
        host.mButton = (android.widget.Button) (((android.app.Activity) host).findViewById(2131165218));
        host.mTextView = (android.widget.TextView) (((android.app.Activity) host).findViewById(2131165321));
    }
}
缺陷
通过StringBuilder的方式一点一点来拼写Java代码，不但繁琐还容易写错~~

更好的方案
通过javapoet可以更加简单得生成这样的Java代码。(后面会说到）

介绍下依赖库auto-service
在使用注解处理器需要先声明，步骤：
1、需要在 processors 库的 main 目录下新建 resources 资源文件夹；
2、在 resources文件夹下建立 META-INF/services 目录文件夹；
3、在 META-INF/services 目录文件夹下创建 javax.annotation.processing.Processor 文件；
4、在 javax.annotation.processing.Processor 文件写入注解处理器的全称，包括包路径；）
这样声明下来也太麻烦了？这就是用引入auto-service的原因。
通过auto-service中的@AutoService可以自动生成AutoService注解处理器是Google开发的，用来生成 META-INF/services/javax.annotation.processing.Processor 文件的

3、apt-library 工具类
完成了Processor的部分，基本快大功告成了。

在BindViewProcessor中创建了对应的xxxActivity_ViewBinding.java，我们改怎么调用？当然是反射啦！！！

在Module的build.gradle中添加依赖

dependencies {
    implementation project(':apt-annotation')
}
创建注解工具类BindViewTools

public class BindViewTools {

    public static void bind(Activity activity) {

        Class clazz = activity.getClass();
        try {
            Class bindViewClass = Class.forName(clazz.getName() + "_ViewBinding");
            Method method = bindViewClass.getMethod("bind", activity.getClass());
            method.invoke(bindViewClass.newInstance(), activity);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
apt-library的部分就比较简单了，通过反射找到对应的ViewBinding类，然后调用其中的bind()方法完成View的绑定。

到目前为止，所有相关的代码都写完了，终于可以拿出来溜溜了



4、app
依赖
在Module的 build.gradle中（Gradle>=2.2）

dependencies {
    implementation project(':apt-annotation')
    implementation project(':apt-library')
    annotationProcessor project(':apt-processor')
}
Android Gradle 插件 2.2 版本的发布，Android Gradle 插件提供了名为 annotationProcessor的功能来完全代替 android-apt

（若Gradle<2.2）
在Project的 build.gradle中：

buildscript {
    dependencies {
        classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8'  
    }
}
在Module的buile.gradle中：

apply plugin: 'com.android.application'
apply plugin: 'com.neenbedankt.android-apt'
dependencies {
    apt project(':apt-processor')
}
使用
在MainActivity中，在View的前面加上BindView注解，把id传入即可

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv)
    TextView mTextView;
    @BindView(R.id.btn)
    Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BindViewTools.bind(this);
        mTextView.setText("bind TextView success");
        mButton.setText("bind Button success");
    }
}
运行的结果想必大家都知道了，不够为了证明这个BindView的功能完成了，我还是把图贴出来
结果
生成的代码
上面的功能一直在完成一件事情，那就是生成Java代码，那么生成的代码在哪？
在app/build/generated/source/apt中可以找到生成的Java文件
目录

对应的代码（之前已经贴过了）：
public class MainActivity_ViewBinding {
    public void bind(com.example.gavin.apttest.MainActivity host) {
        host.mButton = (android.widget.Button) (((android.app.Activity) host).findViewById(2131165218));
        host.mTextView = (android.widget.TextView) (((android.app.Activity) host).findViewById(2131165321));
    }
}
通过javapoet生成代码
上面在ClassCreatorProxy中，通过StringBuilder来生成对应的Java代码。这种做法是比较麻烦的，还有一种更优雅的方式，那就是javapoet。

先添加依赖

dependencies {
    implementation 'com.squareup:javapoet:1.10.0'
}
然后在ClassCreatorProxy中

public class ClassCreatorProxy {
    //省略部分代码...

    /**
     * 创建Java代码
     * @return
     */
    public TypeSpec generateJavaCode2() {
        TypeSpec bindingClass = TypeSpec.classBuilder(mBindingClassName)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(generateMethods2())
                .build();
        return bindingClass;

    }

    /**
     * 加入Method
     */
    private MethodSpec generateMethods2() {
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

最后在 BindViewProcessor中

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //省略部分代码...
        //通过javapoet生成
        for (String key : mProxyMap.keySet()) {
            ClassCreatorProxy proxyInfo = mProxyMap.get(key);
            JavaFile javaFile = JavaFile.builder(proxyInfo.getPackageName(), proxyInfo.generateJavaCode2()).build();
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
相比用StringBuilder拼Java代码，明显简洁和很多。最后生成的代码跟之前是一样的，就不贴出来了。

javapoet详细用法

Tips
1、如果是ElementType.METHOD类型的注解，解析Element时使用ExecutableElement，而不是Symbol.MethodSymbol，否则编译运行的时候没问题，打包的时候会报错。别问我时为什么知道的...

2、gradle升级到3.4.0以后，AutoService要这么用


implementation 'com.google.auto.service:auto-service:1.0-rc2'
annotationProcessor  'com.google.auto.service:auto-service:1.0-rc2' 

作者：带心情去旅行
链接：https://www.jianshu.com/p/7af58e8e3e18
来源：简书
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。


## 参考

[【Android】APT](https://www.jianshu.com/p/7af58e8e3e18)

