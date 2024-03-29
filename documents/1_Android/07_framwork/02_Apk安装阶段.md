## 1、PMS安装过程概览

当我们点击某一个 App 安装包进行安装时，首先会弹出一个系统界面指示我们进行安装操作。这个界面是 Android Framework 中预置的一个 Activity—PackageInstallerActivity.java。

当点击安装后，PackageInstallerActivity 最终会将所安装的 apk 信息通过 PackageInstallerSession 传给 PMS，具体方法在 commitLocked 方法中，如下所示：

<img src="https://user-images.githubusercontent.com/17560388/132199632-b1cb233d-fc75-4bec-830d-619818f585ec.png" alt="图片替换文本" width="600"  align="bottom" />

图中的 mPm 就是系统服务 PackageManagerService。installStage 方法就是正式开始 apk 的安装过程。

整个 apk 的安装过程可以分为两大步：

- 拷贝安装包；
- 装载代码。

## 2、拷贝安装包

从 installStage 方法开始看起，代码如下：

<img src="https://user-images.githubusercontent.com/17560388/132199741-23cf292f-f31a-4f8e-a6e0-6274c5262820.png" alt="图片替换文本" width="600"  align="bottom" />

解释说明：

- 图中 1 处创建了类型为 INIT_COPY 的 Message。
- 图中 2 处创建 InstallParams，并传入安装包的相关数据。

Message 发送出去之后，由 PMS 的内部类 PackageHandler 接收并处理，如下：

<img src="https://user-images.githubusercontent.com/17560388/132199867-0330f181-45fd-4931-a29e-4cfba202a0f0.png" alt="图片替换文本" width="600"  align="bottom" />

解释说明：

- 图中 1 处从 Message 中取出 HandlerParams 对象，实际类型是 InstallParams 类型。
- 图中 2 处调用 connectToService 方法连接安装 apk 的 Service 服务。

**PackageHandler 的 connectToService 方法**

<img src="https://user-images.githubusercontent.com/17560388/132199976-f2e550ab-897f-4620-96fe-9eb451069c59.png" alt="图片替换文本" width="600"  align="bottom" />

通过隐式 Intent 绑定 Service，实际绑定的 Service 类型是 DefaultContainerService 类型。当绑定 Service 成功之后，会在 onServiceConnection 方法中发送一个绑定操作的 Message，如下所示：

<img src="https://user-images.githubusercontent.com/17560388/132208548-2f13043d-33c1-427a-9322-0a8db31d5fe5.png" alt="图片替换文本" width="600"  align="bottom" />

MCS_BOUND 的 Message 接收者还是 PackageHandler，具体如下：

<img src="https://user-images.githubusercontent.com/17560388/132208582-59b23f4f-6968-473d-abab-5d499318fdb0.png" alt="图片替换文本" width="600"  align="bottom" />

mPendingInstalls 是一个等待队列，里面保存所有需要安装的 apk 解析出来的 HandlerParams 参数，从 mPendingInstalls 中取出第一个需要安装的 HandlerParams 对象，并调用其 startCopy 方法，在 startCopy 方法中会继续调用一个抽象方法 handleStartCopy 处理安装请求。通过之前的分析，我们知道 HandlerParams 实际类型是 InstallParams 类型，因此最终调用的是 InstallParams 的 handlerStartCopy 方法，

**InstallParams 的 handlerStartCopy 方法**

这个方法是整个安装包拷贝的核心方法，具体如下：

<img src="https://user-images.githubusercontent.com/17560388/132208908-d7070c68-8c59-4161-8aa7-2f0f058059b1.png" alt="图片替换文本" width="600"  align="bottom" />

解释说明：

- 图中 1 处设置安装标志位，决定是安装在手机内部存储空间还是 sdcard 中。
- 图中 2 处判断 apk 安装位置。

如果安装位置合法，则执行图中 3 处逻辑，创建一个 InstallArgs，实际上是其子类 FileInstallArgs 类型，然后调用其 copyApk 方法进行安装包的拷贝操作。

**FileInstallArgs 的 copyApk 方法**

<img src="https://user-images.githubusercontent.com/17560388/132209052-cea16e5d-5d45-4dac-8712-7de30d476857.png" alt="图片替换文本" width="600"  align="bottom" />

可以看出在 copyApk 方法中调用了 doCopyApk 方法，doCopyAPk 方法中主要做了 3 件事情：

- 图中 1 处创建存储安装包的目标路径，实际上是 /data/app/ 应用包名目录；
- 图中 2 处调用服务的 copyPackage 方法将安装包 apk 拷贝到目标路径中；
- 图中 3 处将 apk 中的动态库 .so 文件也拷贝到目标路径中。

上图中的 IMediaContainerService 实际上就是在开始阶段进行连接操作的 DefaultContainerService 对象，其内部 copyPackage 方法本质上就是执行 IO 流操作，具体如下：

<img src="https://user-images.githubusercontent.com/17560388/132209176-64c4aa6d-d874-40c0-aa65-1cdd72053591.png" alt="图片替换文本" width="600"  align="bottom" />

最终安装包在 data/app 目录下以 base.apk 的方式保存，至此安装包拷贝工作就已经完成。

### 3、装载代码

代码拷贝结束之后，就开始进入真正的安装步骤。

代码回到上述的 HandlerParams 中的 startCopy 方法：

<img src="https://user-images.githubusercontent.com/17560388/132209229-cd0b9994-410f-453d-8723-cc2cf5d9f3bf.png" alt="图片替换文本" width="600"  align="bottom" />

可以看出当安装包拷贝操作结束之后，继续调用 handleReturnCode 方法来处理返回结果，最终调用 processPendingInstall 方法处理安装过程，代码具体如下：

<img src="https://user-images.githubusercontent.com/17560388/132209639-c9c8d95c-2e7c-4628-99bd-2930522a4b8d.png" alt="图片替换文本" width="600"  align="bottom" />

解释说明：

- 图中 1 处执行预安装操作，主要是检查安装包的状态，确保安装环境正常，如果安装环境有问题会清理拷贝文件。
- 图中 2 处是真正的安装阶段，installPackageTraceLI 方法中添加跟踪 Trace，然后调用 installPackageLI 方法进行安装。
- 图中 3 处处理安装完成之后的操作。

installPackageLI 是 apk 安装阶段的核心代码，方法实现很长，部分核心代码如下：

<img src="https://user-images.githubusercontent.com/17560388/132209665-cc0ae5ca-4d95-4233-a5a9-3bff0d996929.png" alt="图片替换文本" width="600"  align="bottom" />

解释说明：

- 图中 1 处调用 PackageParser 的 parsePackage 方法解析 apk 文件，主要是解析 AndroidManifest.xml 文件，将结果记录在 PackageParser.Package 中。我们在清单文件中声明的 Activity、Service 等组件就是在这一步中被记录到系统 Framework 中，后续才可以通过 startActivity 或者 startService 启动相应的活动或者服务。
- 图中 2 处对 apk 中的签名信息进行验证操作。collectCertificates 做签名验证，collectManifestDigest 主要是做包的项目清单摘要的收集，主要适合用来比较两个包的是否一样。如果我们设备上已经安装了一个 debug 版本的 apk，再次使用一个 release 版本的 apk 进行覆盖安装时，会在这一步验证失败，最终导致安装失败。
- 图中 3 处时执行 dex 优化，实际为 dex2oat 操作，用来将 apk 中的 dex 文件转换为 oat 文件。
- 图中 4 处调用 installNewPackageLI 方法执行新 apk 的安装操作

installNewPackageLI 方法负责完成最后的 apk 安装过程，具体代码如下：

<img src="https://user-images.githubusercontent.com/17560388/132209895-740ad189-be04-40c5-b1ed-1c140759b4b1.png" alt="图片替换文本" width="600"  align="bottom" />

解释说明：

- scanPackageLI 继续扫描解析 apk 安装包文件，保存 apk 相关信息到 PMS 中，并创建 apk 的 data 目录，具体路径为 /data/data/应用包名。
- updateSettingsLI 如果安装成功，更新系统设置中的应用信息，比如应用的权限信息。
- deletePackageLI 如果安装失败，则将安装包以及各种缓存文件删除

>至此整个 apk 的安装过程结束，实际上安装成功之后，还会发送一个 App 安装成功的广播 ACTION_PACKAGE_ADDED。手机桌面应用注册了这个广播，当接收到应用安装成功之后，就将 apk 的启动 icon 显示在桌面上。
