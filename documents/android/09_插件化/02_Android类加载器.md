在 `Android` 虚拟机里是无法直接运行 `.class` 文件的，在编译的时候通过 transform 将所有的 `.class` 文件转换成一个 `.dex` 文件，运行时通过 `BaseDexClassLoader` 加载 `.dex` 文件。`BaseDexClassLoader` 有两个子类：`PathClassLoader` 和 `DexClassLoader`，就是我们这篇文档要讲的 `Android` 中的类加载器。

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
A class loader that loads classes from .jar and .apk filescontaining a classes.dex entry. 
This can be used to execute code notinstalled as part of an application.
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
- `optimizedDirectory`：用来缓存优化的 dex 文件的路径，即从 apk 或 jar 文件中提取出来的 dex 文件。该路径不可以为空，且应该是应用私有的，有读写权限的路径。

