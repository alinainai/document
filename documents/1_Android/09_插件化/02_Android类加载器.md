在 `Android` 虚拟机里是无法直接运行 `.class` 文件的，在编译的时候通过 transform 将所有的 `.class` 文件转换成一个 `.dex` 文件，运行时通过 `BaseDexClassLoader` 加载 `.dex` 文件。

`BaseDexClassLoader` 有两个子类：`PathClassLoader` 和 `DexClassLoader`，就是我们这篇文档要讲的 `Android` 中的类加载器。
 
<img width="614" alt="image" src="https://user-images.githubusercontent.com/17560388/202845286-dfed484c-bed5-453d-b71a-bb11632bd421.png">


## 一、PathClassLoader

`PathClassLoader` 是用来加载系统 `apk` 和被安装到手机中的 `apk` 的 `.dex` 文件的。它的 2 个构造函数如下：

```java
public class PathClassLoader extends BaseDexClassLoader {
    public PathClassLoader(String dexPath, ClassLoader parent) {
        super((String)null, (File)null, (String)null, (ClassLoader)null);
        throw new RuntimeException("Stub!");
    }

    public PathClassLoader(String dexPath, String librarySearchPath, ClassLoader parent) {
        super((String)null, (File)null, (String)null, (ClassLoader)null);
        throw new RuntimeException("Stub!");
    }
}
```

参数说明：

- `dexPath`：dex 文件路径，或者包含 dex 文件的 jar 包路径；
- `librarySearchPath`：C/C++ native 库的路径。

PathClassLoader 里面除了这 2 个构造方法以外就没有其他的代码了，具体的实现都是在 BaseDexClassLoader 里面。其 dexPath 比较受限制，一般是已经安装应用的 apk 文件路径。

当一个 `App` 被安装到手机后，`apk` 里面的 `class.dex` 中的 `class` 都是通过 `PathClassLoader` 来加载的，可以通过如下代码验证：

```java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        ClassLoader loader = MainActivity.class.getClassLoader();
        Log.e("loader",loader.toString());
    }
}
```
打印结果如下：
```shell
E/loader: dalvik.system.PathClassLoader[DexPathList[[dex file "/data/data/com.egas.demo/code_cache/.overlay/base.apk/classes4.dex"
```

## 二、DexClassLoader

先来看官方对 `DexClassLoader` 的描述：
```shell
A class loader that loads classes from .jar and .apk filescontaining a classes.dex entry. This can be used to execute code notinstalled as part of an application.
```
很明显，对比 `PathClassLoader` 只能加载已经安装应用的 `dex` 或 `apk` 文件，`DexClassLoader` 则没有此限制，可以从 `SD` 卡上加载包含 `class.dex` 的 `.jar` 和 `.apk` 文件，这也是插件化和热修复的基础，在不需要安装应用的情况下，完成需要使用的 `dex` 的加载。

`DexClassLoader` 的源码里面只有一个构造方法，如下：
```java
public class DexClassLoader extends BaseDexClassLoader {
    public DexClassLoader(String dexPath, String optimizedDirectory,
            String libraryPath, ClassLoader parent) {
        super(dexPath, new File(optimizedDirectory), libraryPath, parent);
    }
}
```
参数说明：

- `dexPath`：包含 `class.dex` 的 `apk、jar` 文件路径 ，多个路径用文件分隔符（默认是“:”）分隔。
- `optimizedDirectory`：用来缓存优化的 dex 文件的路径，即从 apk 或 jar 文件中提取出来的 dex 文件。该路径不可以为空，且应该是应用私有的，有读写权限的路径。8.0 以后已经废弃了，设置不设置没有什么区别。

## 三、Demo中的ClassLoader打印

我们新建一个 android 项目，在 MainAcitity 的 onCreate 方法中打印如下信息
```kotlin
println("MainActivity.class=${this.classLoader}")
println("Context.class=${Context::class.java.classLoader}")
println("View.class=${View::class.java.classLoader}")
println("SystemClassLoader.class=${ClassLoader.getSystemClassLoader()}")
```
日志如下
```shell
MainActivity.class = dalvik.system.PathClassLoader[DexPathList[[zip file "/data/app/com.egas.demo-dMZVdS4aKHW_VYMfCfE6PQ==/base.apk"],nativeLibraryDirectories=[/data/app/com.egas.demo-dMZVdS4aKHW_VYMfCfE6PQ==/lib/arm64, /system/lib64, /system/product/lib64]]]

Context.class = java.lang.BootClassLoader@e9f5a5f
View.class = java.lang.BootClassLoader@e9f5a5f

SystemClassLoader.class = dalvik.system.PathClassLoader[DexPathList[[directory "."],nativeLibraryDirectories=[/system/lib64, /system/product/lib64, /system/lib64, /system/product/lib64]]]
```

## 四、BaseDexClassLoader 源码

PathClassLoader 和 DexClassLoader 只是简单做了下构造的处理，所有的加载逻辑都在 [BaseDexClassLoader](https://android.googlesource.com/platform/libcore/+/master/dalvik/src/main/java/dalvik/system/BaseDexClassLoader.java) 中。
在 BaseDexClassLoader 的构造方法中会生成一个 DexPathList 对象，在 BaseDexClassLoader#findClass 内部继续调用 DexPathList 的 findClass 方法查找类。

```java
public class BaseDexClassLoader extends ClassLoader {

    @UnsupportedAppUsage
    private final DexPathList pathList;
    protected final ClassLoader[] sharedLibraryLoaders;

    public BaseDexClassLoader(String dexPath, String librarySearchPath, ClassLoader parent, ClassLoader[] sharedLibraryLoaders,
            ClassLoader[] sharedLibraryLoadersAfter, boolean isTrusted) {
        super(parent);
        this.sharedLibraryLoaders = sharedLibraryLoaders == null ? null: Arrays.copyOf(sharedLibraryLoaders, sharedLibraryLoaders.length);
        // 核心代码：生成一个 DexPathList     
        this.pathList = new DexPathList(this, dexPath, librarySearchPath, null, isTrusted);
        ...
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // First, check whether the class is present in our shared libraries. 首先检查 sharedLibraryLoaders 中是否能加载 name 对应的类
        if (sharedLibraryLoaders != null) {
            for (ClassLoader loader : sharedLibraryLoaders) {
                try {
                    return loader.loadClass(name);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        ...
        // 核心代码：findClass 方法内部继续调用 DexPathList.findClass 方法
        Class c = pathList.findClass(name, suppressedExceptions);
        if (c != null) {
            return c;
        }
        ...
    }

    @Override public String toString() {
        return getClass().getName() + "[" + pathList + "]";
    }
}
```
我们继续看下 [DexPathList](https://android.googlesource.com/platform/libcore-snapshot/+/refs/heads/ics-mr1/dalvik/src/main/java/dalvik/system/DexPathList.java)源码。  
DexPathList 内部有一个 Element 数组，Element 是 DexPathList 的一个内部类，Element 类中有一个 DexFile 的全局变量，DexFile 就是我们的 .dex 文件。  

DexPathList 类的构造中通过 DexPathList#makeDexElements 方法生成 Element 数组， DexPathList#findClass 方法内部遍历 Element 数组，通过 DexFile#loadClassBinaryName 去加载类。
loadClassBinaryName 封装了一个 Native 方法，在上篇文档中我们分析过 Dex 文件的结构，该方法通过解析 Dex 文件去加载对应的类。

```java
final class DexPathList {
    private static final String DEX_SUFFIX = ".dex";

    private final ClassLoader definingContext;
    /** list of dex/resource (class path) elements */
    private final Element[] dexElements;
    /** list of native library directory elements */
    private final File[] nativeLibraryDirectories;
  
    public DexPathList(ClassLoader definingContext, String dexPath,
            String libraryPath, File optimizedDirectory) {
        ...
        if (optimizedDirectory != null) {
            ...
        }
        this.definingContext = definingContext;
        this.dexElements =  makeDexElements(splitDexPath(dexPath), optimizedDirectory);
        this.nativeLibraryDirectories = splitLibraryPath(libraryPath);
    }
  
    /**
     * Makes an array of dex/resource path elements, one per element of the given array.
     */
    private static Element[] makeDexElements(ArrayList<File> files, File optimizedDirectory) {
        ArrayList<Element> elements = new ArrayList<Element>();
        for (File file : files) {
            ZipFile zip = null;
            DexFile dex = null;
            String name = file.getName();
            if (name.endsWith(DEX_SUFFIX)) { // 是否是 .dex 文件
                try {
                    dex = loadDexFile(file, optimizedDirectory); //生成一个 DexFile
                } catch (IOException ex) {
                    System.logE("Unable to load dex file: " + file, ex);
                }
            } else if (name.endsWith(APK_SUFFIX) || name.endsWith(JAR_SUFFIX)  || name.endsWith(ZIP_SUFFIX)) { // apk/jar/zip 格式的文件
                try {
                    zip = new ZipFile(file);
                } catch (IOException ex) {
                    System.logE("Unable to open zip file: " + file, ex);
                }
                try {
                    dex = loadDexFile(file, optimizedDirectory);
                } catch (IOException ignored) {
                }
            } else {
                System.logW("Unknown file type for: " + file);
            }
            if ((zip != null) || (dex != null)) {
                elements.add(new Element(file, zip, dex));
            }
        }
        return elements.toArray(new Element[elements.size()]);
    }

    private static DexFile loadDexFile(File file, File optimizedDirectory)
            throws IOException {
        if (optimizedDirectory == null) { // 
            return new DexFile(file);
        } else {
            String optimizedPath = optimizedPathFor(file, optimizedDirectory);
            return DexFile.loadDex(file.getPath(), optimizedPath, 0);
        }
    }

    public Class findClass(String name) {
        for (Element element : dexElements) {
            DexFile dex = element.dexFile;
            if (dex != null) {
                Class clazz = dex.loadClassBinaryName(name, definingContext); // loadClassBinaryName 是一个 native 方法
                if (clazz != null) {
                    return clazz;
                }
            }
        }
        return null;
    }
  
    /*package*/ static class Element {
        public final File file;
        public final ZipFile zipFile;
        public final DexFile dexFile;
        ...
    }
}
```

## 五、总结

通过上面的分析我们大概了解 Android ClassLoader 的大概流程。

1、PathClassLoader/DexClassLoader 加载的工作都在 BaseDexClassLoader 中进行。  
2、BaseDexClassLoader 的 findClass 方法继续调用 DexPathList 的 findClass 方法，DexPathList 内部有一个 Element 数组，Element 中有 DexFile 文件。  
3、DexPathList 的 findClass 内部遍历 Element 数组，通过 DexFile#loadClassBinaryName 去查找类。

```java
PathClassLoader/DexClassLoader -> BaseDexClassLoader#findClass(name) -> DexPathList#findClass(name) -> DexFile#loadClassBinaryName 
```

## 参考
- [深入理解Android ClassLoader](https://zhuanlan.zhihu.com/p/136083521)

