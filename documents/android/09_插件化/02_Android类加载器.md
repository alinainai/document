在 `Android` 虚拟机里是无法直接运行 `.class` 文件的，在编译的时候通过 transform 将所有的 `.class` 文件转换成一个 `.dex` 文件，运行时通过 `BaseDexClassLoader` 加载 `.dex` 文件。

`BaseDexClassLoader` 有两个子类：`PathClassLoader` 和 `DexClassLoader`，就是我们这篇文档要讲的 `Android` 中的类加载器。
 
<img width="521" alt="image" src="https://user-images.githubusercontent.com/17560388/202835200-6a046d63-b71a-4148-bad9-6aff3c070b3b.png">

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

PathClassLoader 和 DexClassLoader 只是简单做了下构造的处理，所有的加载逻辑都在 BaseDexClassLoader 中。

```java
public class BaseDexClassLoader extends ClassLoader {

    @UnsupportedAppUsage
    private final DexPathList pathList;

    protected final ClassLoader[] sharedLibraryLoaders;

    protected final ClassLoader[] sharedLibraryLoadersAfter;

    public BaseDexClassLoader(String dexPath,
            String librarySearchPath, ClassLoader parent, ClassLoader[] sharedLibraryLoaders,
            ClassLoader[] sharedLibraryLoadersAfter,
            boolean isTrusted) {
        super(parent);
        // Setup shared libraries before creating the path list. ART relies on the class loader
        // hierarchy being finalized before loading dex files.
        this.sharedLibraryLoaders = sharedLibraryLoaders == null
                ? null
                : Arrays.copyOf(sharedLibraryLoaders, sharedLibraryLoaders.length);
        this.pathList = new DexPathList(this, dexPath, librarySearchPath, null, isTrusted);
        this.sharedLibraryLoadersAfter = sharedLibraryLoadersAfter == null
                ? null
                : Arrays.copyOf(sharedLibraryLoadersAfter, sharedLibraryLoadersAfter.length);
        // Run background verification after having set 'pathList'.
        this.pathList.maybeRunBackgroundVerification(this);
        reportClassLoaderChain();
    }


    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // First, check whether the class is present in our shared libraries.
        if (sharedLibraryLoaders != null) {
            for (ClassLoader loader : sharedLibraryLoaders) {
                try {
                    return loader.loadClass(name);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        // Check whether the class in question is present in the dexPath that
        // this classloader operates on.
        List<Throwable> suppressedExceptions = new ArrayList<Throwable>();
        Class c = pathList.findClass(name, suppressedExceptions);
        if (c != null) {
            return c;
        }
        // Now, check whether the class is present in the "after" shared libraries.
        if (sharedLibraryLoadersAfter != null) {
            for (ClassLoader loader : sharedLibraryLoadersAfter) {
                try {
                    return loader.loadClass(name);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        if (c == null) {
            ClassNotFoundException cnfe = new ClassNotFoundException(
                    "Didn't find class \"" + name + "\" on path: " + pathList);
            for (Throwable t : suppressedExceptions) {
                cnfe.addSuppressed(t);
            }
            throw cnfe;
        }
        return c;
    }
   

    @Override
    protected URL findResource(String name) {
        if (sharedLibraryLoaders != null) {
            for (ClassLoader loader : sharedLibraryLoaders) {
                URL url = loader.getResource(name);
                if (url != null) {
                    return url;
                }
            }
        }
        URL url = pathList.findResource(name);
        if (url != null) {
            return url;
        }
        if (sharedLibraryLoadersAfter != null) {
            for (ClassLoader loader : sharedLibraryLoadersAfter) {
                URL url2 = loader.getResource(name);
                if (url2 != null) {
                    return url2;
                }
            }
        }
        return null;
    }
    @Override
    protected Enumeration<URL> findResources(String name) {
        Enumeration<URL> myResources = pathList.findResources(name);
        if (sharedLibraryLoaders == null && sharedLibraryLoadersAfter == null) {
          return myResources;
        }
        int sharedLibraryLoadersCount =
                (sharedLibraryLoaders != null) ? sharedLibraryLoaders.length : 0;
        int sharedLibraryLoadersAfterCount =
                (sharedLibraryLoadersAfter != null) ? sharedLibraryLoadersAfter.length : 0;
        Enumeration<URL>[] tmp =
                (Enumeration<URL>[]) new Enumeration<?>[sharedLibraryLoadersCount +
                        sharedLibraryLoadersAfterCount
                        + 1];
        // First add sharedLibrary resources.
        // This will add duplicate resources if a shared library is loaded twice, but that's ok
        // as we don't guarantee uniqueness.
        int i = 0;
        for (; i < sharedLibraryLoadersCount; i++) {
            try {
                tmp[i] = sharedLibraryLoaders[i].getResources(name);
            } catch (IOException e) {
                // Ignore.
            }
        }
        // Then add resource from this dex path.
        tmp[i++] = myResources;
        // Finally add resources from shared libraries that are to be loaded after.
        for (int j = 0; j < sharedLibraryLoadersAfterCount; i++, j++) {
            try {
                tmp[i] = sharedLibraryLoadersAfter[j].getResources(name);
            } catch (IOException e) {
                // Ignore.
            }
        }
        return new CompoundEnumeration<>(tmp);
    }
    @Override
    public String findLibrary(String name) {
        return pathList.findLibrary(name);
    }

    @Override public String toString() {
        return getClass().getName() + "[" + pathList + "]";
    }

}
```

## 参考
- [深入理解Android ClassLoader](https://zhuanlan.zhihu.com/p/136083521)

