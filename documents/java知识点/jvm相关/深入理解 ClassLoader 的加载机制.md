## 1. Java 中 ClassLoader

<img src="https://user-images.githubusercontent.com/17560388/119774316-91106300-bef4-11eb-926b-278e89a19d94.png" alt="图片替换文本" width="600"  align="bottom" />

`jvm` 中自带 3 个类加载器：

1. 启动类加载器 `BootstrapClassLoader`
2. 扩展类加载器 `ExtClassLoader` （JDK 1.9 之后，改名为 PlatformClassLoader）
3. 系统加载器 `AppClassLoader`

### 1.1 AppClassLoader 系统类加载器

部分源码如下：

<img src="https://user-images.githubusercontent.com/17560388/132939873-aaef4123-1b0a-4052-ac80-495d6cb12ce2.png" alt="AppClassLoader" width="600"  align="bottom" />

可以看出，`AppClassLoader` 主要加载系统属性 `java.class.path` 配置下类文件，也就是环境变量 `CLASS_PATH` 配置的路径。因此 `AppClassLoader` 是面向用户的类加载器，我们自己编写的代码以及使用的第三方 `jar` 包通常都是由它来加载的。

### 1.2 `ExtClassLoader` 扩展类加载器

部分源码如下：

<img src="https://user-images.githubusercontent.com/17560388/132940028-d161c97f-fda2-40a5-a176-23006b98eeef.png" alt="图片替换文本" width="600"  align="bottom" />

可以看出，`ExtClassLoader` 加载系统属性 `java.ext.dirs` 配置下类文件，可以打印出这个属性来查看具体有哪些文件：

<img src="https://user-images.githubusercontent.com/17560388/132940033-052e497d-330d-4e73-95bb-f41ac11e7d8b.png" alt="图片替换文本" width="600"  align="bottom" />

结果如下：

<img src="https://user-images.githubusercontent.com/17560388/132940038-27d39293-e376-42b2-b3a7-eb07f66fbbd0.png" alt="图片替换文本" width="600"  align="bottom" />

### 1.3 BootstrapClassLoader 启动类加载器

`BootstrapClassLoader` 同上面的两种 `ClassLoader` 不太一样。

<img src="https://user-images.githubusercontent.com/17560388/132940057-ecf49c59-366f-431f-ad7e-f66231db8695.png" alt="图片替换文本" width="600"  align="bottom" />

首先，它并不是使用 `java` 代码实现的，而是由 `C/C++` 语言编写的，它本身属于虚拟机的一部分。因此我们无法在 `java` 代码中直接获取它的引用。

如果尝试在 `java` 层获取 `BootstrapClassLoader` 的引用，系统会返回 `null`。

`BootstrapClassLoader` 加载系统属性 `sun.boot.class.path` 配置下类文件，可以打印出这个属性来查看具体有哪些文件：

结果如下：

<img src="https://user-images.githubusercontent.com/17560388/132940060-fae1e153-f720-479c-9adf-1d815cb1a8ea.png" alt="图片替换文本" width="600"  align="bottom" />

可以看到，这些全是 `JRE` 目录下的 `jar` 包或者 `class` 文件。

### 1.4 双亲委托模式

ClassLoader 的 loadClass 方法

```java
protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException{
    // First, check if the class has already been loaded
    Class<?> c = findLoadedClass(name);
    if (c == null) {
        try {
            if (parent != null) {
                c = parent.loadClass(name, false);
            } else {
                c = findBootstrapClassOrNull(name);
            }
        } catch (ClassNotFoundException e) {
            // ClassNotFoundException thrown if class not found
            // from the non-null parent class loader
        }
        if (c == null) {
            // If still not found, then invoke findClass in order
            // to find the class.
            c = findClass(name);
        }
    }
    return c;
}
```
### 1.5 流程总结
1. 判断该class是否加载，如果加载过直接返回。
2. 如果没有加载，判断parent是否为空，不为空委托给parent加载。
3. parent为空，调用BootstrapClassLoader加载。
4. 都没加载成功，调用当前ClassLoader的findClass尝试加载。

## Java 中的类何时被加载器加载

