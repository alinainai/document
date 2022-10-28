
理论知识都是为实践作基础，接下来我们就使用 DexClassLoader 来模拟热修复功能的实现。

创建 Android 项目 DexClassLoaderHotFix

项目结构如下：



ISay.java 是一个接口，内部只定义了一个方法 saySomething。







SayException.java 实现了 ISay 接口，但是在 saySomething 方法中，打印“something wrong here”来模拟一个线上的 bug。







最后在 MainActivity.java 中，当点击 Button 的时候，将 saySomething 返回的内容通过 Toast 显示在屏幕上。







最后运行效果如下：





创建 HotFix patch 包

新建 Java 项目，并分别创建两个文件 ISay.java 和 SayHotFix.java。











ISay 接口的包名和类名必须和 Android 项目中保持一致。SayHotFix 实现 ISay 接口，并在 saySomething 中返回了新的结果，用来模拟 bug 修复后的结果。



将 ISay.java 和 SayHotFix.java 打包成 say_something.jar，然后通过 dx 工具将生成的 say_something.jar 包中的 class 文件优化为 dex 文件。



dx --dex --output=say_something_hotfix.jar say_something.jar



上述 say_something_hotfix.jar 就是我们最终需要用作 hotfix 的 jar 包。

将 HotFix patch 包拷贝到 SD 卡主目录，并使用 DexClassLoader 加载 SD 卡中的 ISay 接口

首先将 HotFix patch 保存到本地目录下。一般在真实项目中，我们可以通过向后端发送请求的方式，将最新的 HotFix patch 下载到本地中。这里为了演示，我直接使用 adb 命令将 say_somethig_hotfix.jar 包 push 到 SD 卡的主目录下：



adb push say_something_hotfix.jar /storage/self/primary/ 



接下来，修改 MainActivity 中的逻辑，使用 DexClassLoader 加载 HotFix patch 中的 SayHotFix 类，如下：







注意：因为需要访问 SD 卡中的文件，所以需要在 AndroidManifest.xml 中申请权限。



最后运行效果如下：
