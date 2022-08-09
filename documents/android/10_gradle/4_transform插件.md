## 1、Transform 的定义

### 1.1 什么是 Transform？

Transform API 是 Android Gradle Plugin 1.5 就引入的特性，主要用于在 Android 构建过程中，在 Class→Dex 这个节点修改 Class 字节码。

使用 Transform 的常见的应用场景有：

- 埋点统计： 在页面展现和退出等生命周期中插入埋点统计代码，以统计页面展现数据；
- 耗时监控： 在指定方法的前后插入耗时计算，以观察方法执行时间；
- 方法替换： 将方法调用替换为调用另一个方法。

### 1.2 Transform 的基本原理

- 1、工作时机：Transform 工作在 Android 构建中由 Class → Dex 的节点；
- 2、处理对象：处理对象包括 Javac 编译后的 Class 文件、Java 标准 resource 资源、本地依赖和远程依赖的 JAR/AAR。Android 资源文件不属于 Transform 的操作范围，因为它们不是字节码；
- 3、Transform Task： 每个 Transform 都对应一个 Task，Transform 的输入和输出可以理解为对应 Transform Task 的输入输出。每个 TransformTask 的输出都分别存储在 app/build/intermediates/transform/[Transform Name]/[Variant] 文件夹中；
- 4、Transform 链：TaskManager 会将每个 TransformTask 串联起来，前一个 Transform 的输出会作为下一个 Transform 的输入。

### 1.3 Transform API

这里仅列举出 Transform 抽象类中最核心的方法

```java
//com.android.build.api.transform.java
public abstract class Transform {

    // 该名称用于组成 Task 的名称，格式为 transform[InputTypes]With[name]For[Configuration]
    public abstract String getName();

    // （孵化中）用于过滤 Variant，返回 false 表示该 Variant 不执行 Transform
    public boolean applyToVariant(VariantInfo variant) {
        return true;
    }
    
    // 指定需要处理的数据类型
    public abstract Set<ContentType> getInputTypes();

    // 指定输出内容类型，默认是 getInputTypes() 的值
    public Set<ContentType> getOutputTypes() {
        return getInputTypes();
    }

    // 指定消费型输入内容范畴
    public abstract Set<? super Scope> getScopes();

    // 指定引用型输入内容范畴
    public Set<? super Scope> getReferencedScopes() {
        return ImmutableSet.of();
    }

    // 指定是否支持增量编译
    public abstract boolean isIncremental();

    public void transform(TransformInvocation transformInvocation)
            throws TransformException, InterruptedException, IOException {
        // 分发到过时 API，以兼容旧版本的 Transform
        //noinspection deprecation
        transform(transformInvocation.getContext(), transformInvocation.getInputs(),
                transformInvocation.getReferencedInputs(),
                transformInvocation.getOutputProvider(),
                transformInvocation.isIncremental());
    }

    // 指定是否支持缓存
    public boolean isCacheable() {
        return false;
    }
}
```
### 1.4 ContentType 内容类型

ContentType 是一个枚举类接口，表示输入或输出内容的类型，在 AGP 中定义了 DefaultContentType 和 ExtendedContentType 两个枚举类。但是，我们在自定义 Transform 时只能使用 DefaultContentType 中定义的枚举，即 CLASSES 和 RESOURCES 两种类型，其它类型仅供 AGP 内置的 Transform 使用。

自定义 Transform 需要在两个位置定义内容类型：

- 1、Set<ContentType> getInputTypes()： 指定输入内容类型，允许通过 Set 集合设置输入多种类型；
- 2、Set<ContentType> getOutputTypes()： 指定输出内容类型，默认取 getInputTypes() 的值，允许通过 Set 集合设置输出多种类型。
  
```java
// 加强类型，自定义 Transform 无法使用
public enum ExtendedContentType implements ContentType {
    DEX(0x1000),  // DEX 文件
    NATIVE_LIBS(0x2000), // Native 库
    CLASSES_ENHANCED(0x4000), // Instant Run 加强类    
    DATA_BINDING(0x10000), // Data Binding 中间产物 
    DEX_ARCHIVE(0x40000), // Dex Archive
    ;
}
// QualifiedContent.java
enum DefaultContentType implements ContentType {
    CLASSES(0x01), // Java 字节码，包括 Jar 文件和由源码编译产生的
    RESOURCES(0x02); // Java 资源
}
```

在 TransformManager 中，预定义了一部分内容类型集合，常用的是 CONTENT_CLASS 操作 Class。
    
```java
public static final Set<ContentType> CONTENT_CLASS = ImmutableSet.of(CLASSES);
public static final Set<ContentType> CONTENT_JARS = ImmutableSet.of(CLASSES, RESOURCES);
public static final Set<ContentType> CONTENT_RESOURCES = ImmutableSet.of(RESOURCES);
```
### 1.5 ScopeType 作用域
ScopeType 也是一个枚举类接口，表示输入内容的范畴。

- 1、Set<ScopeType> getScopes() 消费型输入内容范畴： 此范围的内容会被消费，因此当前 Transform 必须将修改后的内容复制到 Transform 的中间目录中，否则无法将内容传递到下一个 Transform 处理；
- 2、Set<ScopeType> getReferencedScopes() 指定引用型输入内容范畴： 默认是空集合，此范围的内容不会被消费，因此不需要复制传递到下一个 Transform，也不允许修改。

QualifiedContent.java
    
```java
PROJECT(0x01), // 当前模块    
SUB_PROJECTS(0x04), // 子模块
EXTERNAL_LIBRARIES(0x10), // 外部依赖，包括当前模块和子模块本地依赖和远程依赖的 JAR/AAR
TESTED_CODE(0x20), // 当前变体所测试的代码（包括依赖项）
PROVIDED_ONLY(0x40), // 本地依赖和远程依赖的 JAR/AAR（provided-only）
```
在 TransformManager 中，预定义了一部分作用域集合，常用的是 SCOPE_FULL_PROJECT 所有模块。需要注意，Library 模块注册的 Transform 只能使用 Scope.PROJECT。

```java
public static final Set<ScopeType> PROJECT_ONLY = ImmutableSet.of(Scope.PROJECT);
public static final Set<ScopeType> SCOPE_FULL_PROJECT = ImmutableSet.of(Scope.PROJECT, Scope.SUB_PROJECTS, Scope.EXTERNAL_LIBRARIES);
``` 
### 1.6 transform 方法
  
transform() 是实现 Transform 的核心方法，方法的参数是 TransformInvocation，它提供了所有与输入输出相关的信息：

```java
public interface TransformInvocation {
    Context getContext();
    Collection<TransformInput> getInputs(); // 获取 TransformInput 对象，它是消费型输入内容，对应于 Transform#getScopes() 定义的范围；
    Collection<TransformInput> getReferencedInputs();  // 获取 TransformInput 对象，它是引用型输入内容，对应于 Transform#getReferenceScope() 定义的内容范围；
    Collection<SecondaryInput> getSecondaryInputs(); // 额外输入内容
    TransformOutputProvider getOutputProvider(); // 获取输出信息，TransformOutputProvider 是对输出文件的抽象。
    boolean isIncremental(); // 当前 Transform 任务是否增量构建；
}
```
输入内容 TransformInput 由两部分组成：

- DirectoryInput 集合： 以源码方式参与构建的输入文件，包括完整的源码目录结构及其中的源码文件；
- JarInput 集合： 以 Jar 和 aar 依赖方式参与构建的输入文件，包含本地依赖和远程依赖。
  
输入内容信息 TransformOutputProvider 有两个功能：

- deleteAll()： 当 Transform 运行在非增量构建模式时，需要删除上一次构建产生的所有中间文件，可以直接调用 deleteAll() 完成；
- getContentLocation()： 获得指定范围+类型的输出目标路径。
```java
public interface TransformOutputProvider {
    void deleteAll() // 删除所有中间文件
    // 获取指定范围+类型的目标路径
    File getContentLocation(String name, Set<QualifiedContent.ContentType> types, Set<? super QualifiedContent.Scope> scopes, Format format);
}
```
获取输入内容对应的输出路径：
```kotlin
for (input in transformInvocation.inputs) {
    for (jarInput in input.jarInputs) {
        // 输出路径
        val outputJar = outputProvider.getContentLocation(
            jarInput.name,
            jarInput.contentTypes,
            jarInput.scopes,
            Format.JAR
        )
    }
}
```
### 1.7 Transform 增量模式

任何构建系统都会尽量避免重复执行相同工作，Transform 也不例外。虽然增量构建并不是必须的，但作为一个合格的 Transform 实现应该具备增量能力。

1、增量模式标记位： Transform API 有两个增量标志位，不要混淆：

- Transform#isIncremental()： Transform 增量构建的使能开关，返回 true 才有可能触发增量构建；
- TransformInvocation#isIncremental()： 当次 TransformTask 是否增量执行，返回 true 表示正在增量模式。

增量模式下的所有输入都是带状态的，Transform 定义了四个输入文件状态：
```kotlin
public enum Status {
    NOTCHANGED, // 未修改，不需要处理，也不需要复制操作
    ADDED, // 新增，正常处理并复制给下一个任务
    CHANGED, // 已修改，正常处理并复制给下一个任务
    REMOVED; // 已删除，需同步移除 OutputProvider 指定的目标文件
}
```
## 2、抽象出一个通用 Transform 模板

整个 Transform 的核心过程是有固定套路，模板流程图如下：

<img width="600" alt="Transform的核心过程" src="https://user-images.githubusercontent.com/17560388/179890215-5cc7e596-c21d-47df-9d65-3ba1322f705a.png">

我们把整个流程图做成一个抽象模板类，子类需要重写 provideFunction() 方法，从输入流读取 Class 文件，修改完字节码后再写入到输出流。其他的一切方法都交给 BaseTransform 去完成

```kotlin
abstract class BaseTransform(private val debug: Boolean) : Transform() {

    abstract fun provideFunction(): ((InputStream, OutputStream) -> Unit)?

    open fun classFilter(className: String) = className.endsWith(SdkConstants.DOT_CLASS)

    override fun isIncremental() = true

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)

        log("Transform start, isIncremental = ${transformInvocation.isIncremental}.")

        val inputProvider = transformInvocation.inputs
        val referenceProvider = transformInvocation.referencedInputs
        val outputProvider = transformInvocation.outputProvider

        // 1. Transform logic implemented by subclasses.
        val function = provideFunction()

        // 2. Delete all transform tmp files when not in incremental build.
        if (!transformInvocation.isIncremental) {
            log("All File deleted.")
            outputProvider.deleteAll()
        }

        for (input in inputProvider) {
            // 3. Transform jar input.
            log("Transform jarInputs start.")
            for (jarInput in input.jarInputs) {
                val inputJar = jarInput.file
                val outputJar = outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                if (transformInvocation.isIncremental) {
                    // 3.1 Transform jar input in incremental build.
                    when (jarInput.status ?: Status.NOTCHANGED) {
                        Status.NOTCHANGED -> {
                            // Do nothing.
                        }
                        Status.ADDED, Status.CHANGED -> {
                            // Do transform.
                            transformJar(inputJar, outputJar, function)
                        }
                        Status.REMOVED -> {
                            // Delete.
                            FileUtils.delete(outputJar)
                        }
                    }
                } else {
                    // 3.2 Transform jar input in full build.
                    transformJar(inputJar, outputJar, function)
                }
            }
            // 4. Transform dir input.
            log("Transform dirInput start.")
            for (dirInput in input.directoryInputs) {
                val inputDir = dirInput.file
                val outputDir = outputProvider.getContentLocation(dirInput.name, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY)
                if (transformInvocation.isIncremental) {
                    // 4.1 Transform dir input in incremental build.
                    for ((inputFile, status) in dirInput.changedFiles) {
                        val outputFile = concatOutputFilePath(outputDir, inputFile)
                        when (status ?: Status.NOTCHANGED) {
                            Status.NOTCHANGED -> {
                                // Do nothing.
                            }
                            Status.ADDED, Status.CHANGED -> {
                                // Do transform.
                                doTransformFile(inputFile, outputFile, function)
                            }
                            Status.REMOVED -> {
                                // Delete
                                FileUtils.delete(outputFile)
                            }
                        }
                    }
                } else {
                    // 4.2 Transform dir input in full build.
                    for (inputFile in FileUtils.getAllFiles(inputDir)) {
                        // Traversal fileTree (depthFirstPreOrder).
                        if (classFilter(inputFile.name)) {
                            val outputFile = concatOutputFilePath(outputDir, inputFile)
                            doTransformFile(inputFile, outputFile, function)
                        }
                    }
                }
            }
        }
        log("Transform end.")
    }

    /**
     * Do transform Jar.
     */
    private fun transformJar(inputJar: File, outputJar: File, function: ((InputStream, OutputStream) -> Unit)?) {
        // Create parent directories to hold outputJar file.
        Files.createParentDirs(outputJar)
        // Unzip.
        FileInputStream(inputJar).use { fis ->
            ZipInputStream(fis).use { zis ->
                // Zip.
                FileOutputStream(outputJar).use { fos ->
                    ZipOutputStream(fos).use { zos ->
                        var entry = zis.nextEntry
                        while (entry != null && isValidZipEntryName(entry)) {
                            if (!entry.isDirectory && classFilter(entry.name)) {
                                zos.putNextEntry(ZipEntry(entry.name))
                                // Apply transform function.
                                applyFunction(zis, zos, function)
                            }
                            entry = zis.nextEntry
                        }
                    }
                }
            }
        }
    }

    /**
     * Do transform file.
     */
    private fun doTransformFile(inputFile: File, outputFile: File, function: ((InputStream, OutputStream) -> Unit)?) {
        // Create parent directories to hold outputFile file.
        Files.createParentDirs(outputFile)
        FileInputStream(inputFile).use { fis ->
            FileOutputStream(outputFile).use { fos ->
                // Apply transform function.
                applyFunction(fis, fos, function)
            }
        }
    }

    private fun concatOutputFilePath(outputDir: File, inputFile: File) = File(outputDir, inputFile.name)

    private fun applyFunction(input: InputStream, output: OutputStream, function: ((InputStream, OutputStream) -> Unit)?) {
        try {
            if (null != function) {
                function.invoke(input, output)
            } else {
                // Copy
                input.copyTo(output)
            }
        } catch (e: UncheckedIOException) {
            throw e.cause!!
        }
    }

    private fun log(logStr: String) {
        if (debug) {
            println("$name - $logStr")
        }
    }
}
```
## 3、具体应用

### 3.1 初始化代码框架

我们通过自定义 Gradle 插件来承载 Transform 的逻辑，可维护性更好。

提示： 提醒一下，并不是说一定要由 Gradle 插件来承载，你直接在 .gradle 文件中实现也是 OK 的。

插件实现类如下：

ToastPlugin.kt

class ToastPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // 获取 Android 扩展
        val androidExtension = project.extensions.getByType(BaseExtension::class.java)
        // 注册 Transform，支持额外增加依赖
        androidExtension.registerTransform(ToastTransform(project)/* 支持增加依赖*/)
    }
}
### 4.2 实现一个具体的 BaseTransform 子类

将我们实现的 BaseTransform 模板类复制到工程下，再实现一个子类：

ToastTransform.kt

internal class ToastTransform(val project: Project) : BaseCustomTransform(true) {

    override fun getName() = "ToastTransform"

    override fun isIncremental() = true

    /**
     * 用于过滤 Variant，返回 false 表示该 Variant 不执行 Transform
     */
    @Incubating
    override fun applyToVariant(variant: VariantInfo?): Boolean {
        return "debug" == variant?.buildTypeName
    }

    // 指定输入内容类型
    override fun getInputTypes() = TransformManager.CONTENT_CLASS

    // 指定消费型输入内容范畴
    override fun getScopes() = TransformManager.SCOPE_FULL_PROJECT

    // 转换方法
    override fun provideFunction() = { ios: InputStream, zos: OutputStream ->
        input.copyTo(output)
    }
}
其中，provideFunction() 是模板代码，参数分别表示源 Class 文件的输入流和目标 Class 文件输出流。子类要做的事，就是从输入流读取 Class 信息，修改后写入到输出流。

4.3 步骤 3：使用 Javassist 修改字节码
使用 Javassist API 从输入流加载数据，在匹配到 onCreate() 方法后检查是否声明 @Hello 注解。是则在该方法末尾织入一句 Toast：Hello Transform。本文重点不是 Javassist，此处就不展开了。

override fun provideFunction() = { ios: InputStream, zos: OutputStream ->
    val classPool = ClassPool.getDefault()
    // 加入android.jar
    classPool.appendClassPath((project.extensions.getByName("android") as BaseExtension).bootClasspath[0].toString())
    classPool.importPackage("android.os.Bundle")
    // Input
    val ctClass = classPool.makeClass(ios)
    try {
        ctClass.getDeclaredMethod("onCreate").also {
            println("onCreate found in ${ctClass.simpleName}")
            val attribute = it.methodInfo.getAttribute(AnnotationsAttribute.invisibleTag) as? AnnotationsAttribute
            if (null != attribute?.getAnnotation("com.pengxr.hellotransform.Hello")) {
                println("Insert toast in ${ctClass.simpleName}")
                it.insertAfter(
                    """android.widget.Toast.makeText(this,"Hello Transform!",android.widget.Toast.LENGTH_SHORT).show();  
                                  """
                )
            }
        }
    } catch (e: NotFoundException) {
        // ignore
    }
    // Output
    zos.write(ctClass.toBytecode())
    ctClass.detach()
}
4.4 步骤 4：应用插件
sample 模块 build.gradle

apply plugin: 'com.pengxr.toastplugin'
4.5 步骤 5：声明 @Hello 注解
HelloActivity.kt

class HelloActivity : AppCompatActivity() {

    @Hello
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hello)
    }
}
4.6 步骤 6：运行
完成以上步骤后，编译运行程序。可以在 Build Output 看到以下输出，HelloActivity 启动时会弹出 Toast HelloTransform，说明织入成功。

...
Task :sample:mergeDebugJavaResource

> Task :sample:transformClassesWithToastTransformForDebug
...
onCreate found in HelloActivity
Insert toast in HelloActivity
ToastTransform - Transform end.

> Task :sample:dexBuilderDebug
> Task :sample:mergeExtDexDebug
> Task :sample:mergeDexDebug
> Task :sample:packageDebug
> Task :sample:createDebugApkListingFileRedirect
> Task :sample:assembleDebug

BUILD SUCCESSFUL in 3m 18s
33 actionable tasks: 33 executed

Build Analyzer results available



## 参考

- [Android Gradle Transform 详解](https://www.jianshu.com/p/cf90c557b866)
- [其实 Gradle Transform 就是个纸老虎 —— Gradle 系列(4)](https://www.jianshu.com/p/067675243777)











