## 一、Matrix Android ApkChecker

官方链接地址：[https://github.com/Tencent/matrix/wiki/Matrix-Android-ApkChecker](https://github.com/Tencent/matrix/wiki/Matrix-Android-ApkChecker)


Matrix是微信终端自研和正在使用的一套APM（Application Performance Management）系统。

Matrix-ApkChecker 作为Matrix系统的一部分，是针对android安装包的分析检测工具，根据一系列设定好的规则检测apk是否存在特定的问题，并输出较为详细的检测结果报告，用于分析排查问题以及版本追踪。Matrix-ApkChecker以一个jar包的形式提供使用，通过命令行执行 `java -jar ApkChecker.jar` 即可运行。

### Matrix-ApkChecker 的使用

#### 1、直接在命令行执行
```shell
java -jar ApkChecker.jar
```
即可以查看Matrix-ApkChecker的使用说明 （注意：下面所说的路径为完整路径，非相对路径）
```shell
Usages: 
    --config CONFIG-FILE-PATH
or
    [--input INPUT-DIR-PATH] [--apk APK-FILE-PATH] [--unzip APK-UNZIP-PATH] [--mappingTxt MAPPING-FILE-PATH] [--resMappingTxt RESGUARD-MAPPING-FILE-PATH] [--output OUTPUT-PATH] [--format OUTPUT-FORMAT] [--formatJar OUTPUT-FORMAT-JAR] [--formatConfig OUTPUT-FORMAT-CONFIG (json-array format)] [Options]
```

Options:
```shell
-manifest
     Read package info from the AndroidManifest.xml.
-fileSize [--min DOWN-LIMIT-SIZE (KB)] [--order ORDER-BY ('asc'|'desc')] [--suffix FILTER-SUFFIX-LIST (split by ',')]
     Show files whose size exceed limit size in order.
-countMethod [--group GROUP-BY ('class'|'package')]
     Count methods in dex file, output results group by class name or package name.
-checkResProguard
     Check if the resguard was applied.
-findNonAlphaPng [--min DOWN-LIMIT-SIZE (KB)]
     Find out the non-alpha png-format files whose size exceed limit size in desc order.
-checkMultiLibrary
     Check if there are more than one library dir in the 'lib'.
-uncompressedFile [--suffix FILTER-SUFFIX-LIST (split by ',')]
     Show uncompressed file types.
-countR
     Count the R class.
-duplicatedFile
     Find out the duplicated resource files in desc order.
-checkMultiSTL  --toolnm TOOL-NM-PATH
     Check if there are more than one shared library statically linked the STL.
-unusedResources --rTxt R-TXT-FILE-PATH [--ignoreResources IGNORE-RESOURCES-LIST (split by ',')]
     Find out the unused resources.
-unusedAssets [--ignoreAssets IGNORE-ASSETS-LIST (split by ',')]
     Find out the unused assets file.
-unstrippedSo  --toolnm TOOL-NM-PATH
     Find out the unstripped shared library file.
```

Matrix-ApkChecker的命令行参数比较多，主要包括global参数和option参数两类：

```shell
global
  --apk   输入apk文件路径（默认文件名以apk结尾即可）
  --mappingTxt   代码混淆mapping文件路径 （默认文件名是mapping.txt）
  --resMappingTxt   资源混淆mapping文件路径（默认文件名是resguard-mapping.txt）
  --input   包含了上述输入文件的目录（给定--input之后，则可以省略上述输入文件参数，但上述输入文件必须使用默认文件名）
  --unzip   解压apk的输出目录
  --output   输出结果文件路径（不含后缀，会根据format决定输出文件的后缀）
  --format   结果文件的输出格式（例如 html、json等）
  --formatJar   实现了自定义结果文件输出格式的jar包
  --formatConfig   对结果文件输出格式的一些配置项（json数组格式）
```
global参数之后紧跟若干个Option，这些Option是可选的，一个Option表示针对apk的一个检测选项。

#### option参数

- manifest: 从AndroidManifest.xml文件中读取apk的全局信息，如packageName、versionCode等。

- fileSize: 列出超过一定大小的文件，可按文件后缀过滤，并且按文件大小排序
```shell
--min: 文件大小最小阈值，单位是KB
--order: 按照文件大小升序（asc）或者降序（desc）排列
--suffix: 按照文件后缀过滤，使用","作为多个文件后缀的分隔符
```
- countMethod:统计方法数
```shell
group:输出结果按照类名(class)或者包名(package)来分组
```
- checkResProguard:检查是否经过了资源混淆(AndResGuard)

- findNonAlphaPng:发现不含alpha通道的png文件
```shell
min:png文件大小最小阈值，单位是KB
checkMultiLibrary 检查是否包含多个ABI版本的动态库
```
- uncompressedFile 发现未经压缩的文件类型（即该类型的所有文件都未经压缩）
```shell
suffix   按照文件后缀过滤，使用","作为多个文件后缀的分隔符
```
- countR 统计apk中包含的R类以及R类中的field count

- duplicatedFile 发现冗余的文件，按照文件大小降序排序

- checkMultiSTL 检查是否有多个动态库静态链接了STL
```shell
toolnm   nm工具的路径
```
- unusedResources 发现apk中包含的无用资源
```shell
rTxt   R.txt文件的路径（如果在全局参数中给定了--input，则可以省略）
ignoreResources   需要忽略的资源，使用","作为多个资源名称的分隔符
```
- unusedAssets 发现apk中包含的无用assets文件
```shell
ignoreAssets   需要忽略的assets文件，使用","作为多个文件的分隔符
```
- unstrippedSo 发现apk中未经裁剪的动态库文件
```shell
toolnm   nm工具的路径
```

#### 2、通过配置文件使用

除了直接在命令行中带上详细参数外，也可以将参数配置以 json 的格式写到一个配置文件中，然后在命令行中使用

```shell
config CONFIG-FILE_PATH
```
指定配置文件的路径。一个典型的配置文件格式如下：
```shell
{
  "--apk":"/Users/lijiaxing/workspace/kr-app-android-exchange/build/app/outputs/apk/korea/devDebug/app-korea-devDebug.apk",
  "--mappingTxt":"/Users/lijiaxing/workspace/kr-app-android-exchange/modules/app/mapping.txtuo",
  "--output":"/Users/lijiaxing/workspace/kr-app-android-exchange/build/app/outputs/apk/korea/devDebug",
  "--format":"mm.html,mm.json",
  "--formatConfig":
  [
    {
      "name":"-countMethod",
      "group":
      [
        {
          "name":"Android System",
          "package":"android"
        },
        {
          "name":"java system",
          "package":"java"
        },
        {
          "name":"com.tencent.test.$",
          "package":"com.tencent.test.$"
        }
      ]
    }
  ],
  "options": [
    {
      "name":"-manifest"
    },
    {
      "name":"-fileSize",
      "--min":"10",
      "--order":"desc",
      "--suffix":"png, jpg, jpeg, gif, arsc"
    },
    {
      "name":"-countMethod",
      "--group":"package" 
    },
    {
      "name":"-checkResProguard" 
    },
    {
      "name":"-findNonAlphaPng",
      "--min":"10"
    },
    {
      "name":"-checkMultiLibrary"
    },
    {
      "name":"-uncompressedFile",
      "--suffix":"png, jpg, jpeg, gif, arsc"
    },
    {
      "name":"-countR"
    },
    {
      "name":"-duplicatedFile"
    },
    {
      "name":"-checkMultiSTL",
      "--toolnm":"/Users/lijiaxing/Library/Android/sdk/ndk-bundle/toolchains/arm-linux-androideabi-4.9/prebuilt/darwin-x86_64/bin/arm-linux-androideabi-nm"
    },
    {
      "name":"-unusedResources",
      "--rTxt":"/Users/lijiaxing/workspace/kr-app-android-exchange/build/app/intermediates/runtime_symbol_list/koreaDevDebug/R.txt",
      "--ignoreResources"
      :["R.raw.*",
        "R.style.*",
        "R.attr.*",
        "R.id.*",
        "R.string.ignore_*"
      ]
    },
    {
      "name":"-unusedAssets",
      "--ignoreAssets":["*.so" ]
    },
    {
      "name":"-unstrippedSo",
      "--toolnm":"/Users/lijiaxing/Library/Android/sdk/ndk-bundle/toolchains/arm-linux-androideabi-4.9/prebuilt/darwin-x86_64/bin/arm-linux-androideabi-nm"
    }
  ]
}
```
其中，mm.html 和 mm.json 是微信使用的自定义输出格式，Matrix-ApkChecker默认提供 html 、json、mm.html 以及 mm.json 四种输出格式。

- 注意1：其中“--apk”，“--mappingTxt”，“--output”，“--toolnm”，“--rTxt”需要修改为自己的路径
- 注意2：配置文件中的任务，可以按照自己的需求修改。

## 二、功能

Matrix-ApkChecker 当前主要包含以下功能

### 读取manifest的信息
从AndroidManifest.xml文件中读取apk的全局信息，如packageName、versionCode等。

### 按文件大小排序列出apk中包含的文件
列出超过一定大小的文件，可按文件后缀过滤，并且按文件大小排序

### 统计方法数
统计dex包含的方法数，并支持将输出结果按照类名(class)或者包名(package)来分组

### 检查是否经过了资源混淆(AndResGuard)
检查apk是否经过了资源混淆，推荐使用资源混淆来进一步减小apk的大小

### 搜索不含alpha通道的png文件
对于不含alpha通道的png文件，可以转成jpg格式来减少文件的大小

### 检查是否包含多个ABI版本的动态库
so文件的大小可能会在apk文件大小中占很大的比例，可以考虑在apk中只包含一个ABI版本的动态库

### 搜索未经压缩的文件类型
某个文件类型的所有文件都没有经过压缩，可以考虑是否需要压缩

### 统计apk中包含的R类以及R类中的field count
编译之后，代码中对资源的引用都会优化成int常量，除了R.styleable之外，其他的R类其实都可以删除

### 搜索冗余的文件
对于两个内容完全相同的文件，应该去冗余

### 检查是否有多个动态库静态链接了STL
如果有多个动态库都依赖了STL，应该采用动态链接的方式而非多个动态库都去静态链接STL

### 搜索apk中包含的无用资源
apk中未经使用到的资源，应该予以删除

### 搜索apk中包含的无用assets文件
apk中未经使用的assets文件，应该予以删除

### 搜索apk中未经裁剪的动态库文件
动态库经过裁剪之后，文件大小通常会减小很多

## 三、示例分析

下面，我们对一个示例 apk 使用 Matrix-ApkChecker 进行检查，并根据检查的结果进行针对性的减包优化。

从 Matrix-ApkChecker 的输出结果中可以看到示例 apk 的相关全局信息如下图所示：

<img width="500" alt="globle" src="https://user-images.githubusercontent.com/17560388/197672119-ae1ff66b-8192-4227-84bc-cba55c476f52.png">

示例apk中包含的文件按类型统计如下图所示： 

<img width="500" alt="file-type" src="https://user-images.githubusercontent.com/17560388/197672331-95bcbebb-2f8c-4519-a5da-f2e4bb9dc68c.png">

对于示例apk，我们使用Matrix-ApkChecker进行了全面检查，主要发现以下几个问题：

- png文件（不包括**.9.png**）未经压缩，可以考虑一定程度的压缩 uncompress_file

- 存在一些冗余的文件，文件内容相同的文件应该只保留一份 duplicated_file

- 存在无用资源，包括未使用的系统support包中的资源、第三方资源包中的无用资源以及示例app定义的资源 unused_resources

- 存在无用的assets资源，应该删除 unused_assets

针对上述Matrix-ApkChecker检测出来的问题，做如下针对性的优化：

- 首先删除冗余文件
res/drawable-xxxhdpi 目下存在与 res/drawable 目录内容相同的文件，删除 res/drawable 目录下的 icon.png 以及 round.png。 删除之后，可以看到示例apk中png文件缩小了23.89 KB 。 ret-sub-duplicate

- 将png文件转换成webp格式
从示例输出中可以看到，示例apk的 minSdkVersion 是18，android对于API >= 18的版本已经支持透明的webp。使用Android Studio自带的webp转换功能，选择无损压缩，将部分png文件（不含 .9.png ）转成webp之后，示例apk的大小缩小了 7.03 KB ret-convert-file

- 删除无用的assets文件
将assets/music目录下的 .mp3 文件删除，示例apk的大小缩减了 69.39 KB ret-sub-unused-assets

- 删除无用资源
可以看到删除之后，apk中无用资源大大减少，同时示例apk中arsc文件大小缩减了 36.99 KB ret-sub-unused-resource


经过上述优化，示例apk的大小一共缩减了 **137.3** KB 。

## 四、实现原理

首先来看下Matrix-ApkChecker的整体工作流程 

<img width="1000" alt="total-work-flow" src="https://user-images.githubusercontent.com/17560388/197672721-7b9a53bf-099c-4b8e-ad55-66ea0569c2fe.png">

### 1.输入的Apk文件首先会经过UnzipTask处理，解压到指定目录，在这一步还会做一些全局的准备工作，包括反混淆类名（读取mapping.txt）、反混淆资源(读取resMapping.txt)、统计文件大小等。

### 2.接下来的若干Task即用来实现各种检查规则，这些Task可以并行执行，下面一一简单介绍各个Task的实现方法:
ManifestAnalyzeTask 用于读取AndroidManifest.xml中的信息，如：packageName、verisonCode、clientVersion等。

实现方法：利用ApkTool中的 AXmlResourceParser 来解析二进制的AndroidManifest.xml文件，并且可以反混淆出AndroidManifest.xml中引用的资源名称。

- ShowFileSizeTask 根据文件大小以及文件后缀名来过滤出超过指定大小的文件，并按照升序或降序排列结果。
实现方法：直接利用UnzipTask中统计的文件大小来过滤输出结果。

- MethodCountTask 可以统计出各个Dex中的方法数，并按照类名或者包名来分组输出结果。
实现方法：利用google开源的 com.android.dexdeps 类库来读取dex文件，统计方法数。

- ResProguardCheckTask 可以判断apk是否经过了资源混淆
实现方法：资源混淆之后的res文件夹会重命名成r，直接判断是否存在文件夹r即可判断是否经过了资源混淆。

- FindNonAlphaPngTask 可以检测出apk中非透明的png文件
实现方法：通过 java.awt.BufferedImage 类读取png文件并判断是否有alpha通道。

- MultiLibCheckTask 可以判断apk中是否有针对多个ABI的so
实现方法：直接判断lib文件夹下是否包含多个目录。

- CheckMultiSTLTask 可以检测apk中的so是否静态链接STL
实现方法：通过nm工具来读取so的符号表，如果出现 std:: 即表示so静态链接了STL。

- CountRTask 可以统计R类以及R类的中的field数目
实现方法：同样是利用 com.android.dexdeps 类库来读取dex文件，找出R类以及field数目。

- UncompressedFileTask 可以检测出未经压缩的文件类型
实现方法：直接利用UnzipTask中统计的各个文件的压缩前和压缩后的大小，判断压缩前和压缩后大小是否相等。

- DuplicatedFileTask 可以检测出冗余的文件
实现方法：通过比较文件的MD5是否相等来判断文件内容是否相同。

- UnusedResourceTask 可以检测出apk中未使用的资源，对于getIdentifier获取的资源可以加入白名单
实现方法： 
- （1）过读取R.txt获取apk中声明的所有资源得到declareResourceSet； 
- （2）通过读取smali文件中引用资源的指令（包括通过reference和直接通过资源id引用资源）得出class中引用的资源classRefResourceSet； 
- （3）通过ApkTool解析res目录下的xml文件、AndroidManifest.xml 以及 resource.arsc 得出资源之间的引用关系； 
- （4）根据上述几步得到的中间数据即可确定出apk中未使用到的资源。

- UnusedAssetsTask 可以检测出apk中未使用的assets文件
实现方法：搜索smali文件中引用字符串常量的指令，判断引用的字符串常量是否某个assets文件的名称

- UnStrippedSoCheckTask 可以检测出apk中未经裁剪的动态库文件
实现方法：使用nm工具读取动态库文件的符号表，若输出结果中包含no symbols字样则表示该动态库已经过裁剪


### 3.每个Task的输出结果保存在json对象中，然后通过 *OutputFormater* 来对输出结果进一步加工（可以转成html格式），也可以实现自己的OutputFormater自定义输出内容的格式。
