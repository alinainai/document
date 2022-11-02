## 一、注册平台和准备工作

### 1、注册Sonatype账号

Sonatype 公司负责维护 Maven Central，所以我们先注册一个 Sonatype 账号。

官方地址：[https://issues.sonatype.org/secure/Dashboard.jspa](https://issues.sonatype.org/secure/Dashboard.jspa)

账号注册之后，我们创建一个项目的 issue

<img width="791" alt="image" src="https://user-images.githubusercontent.com/17560388/199385106-dc401de2-93ea-4109-84d5-a734b02ac4db.png">

- 项目选择: Open Source
- 项目类型选择: New Project（我们需要新建 Group ID）
- 概要:填写一下项目的基本信息
- group id:发布项目的group id，一般使用 io.github.yourname 即可，可以自己定制
- project url: 项目的介绍地址，如：https://github.com/sonatype/nexus-public
- SCM url: 项目的仓库地址，如：https://github.com/sonatype/nexus-public.git

一般我们填写之后会有审核人员给我们审核，等待一会即可。如果不通过，可以按照审核人员的说明去修改我们的 issue 即可。

如：可能需要咱们在 github 的仓库中创建一个确认 group id 的 OSSRH-85852 项目。

审核通过后关闭 issue 即可。

### 2、创建GPG签名

GPG签名主要是为了给需要发布到maven central的包进行签名，每个发布上去的包都需要进行这个操作，为了接下来我们可以直接使用，我们所以我们先创建一个自己的GPG签名。

下载地址：[http://www.gnupg.org/download/](http://www.gnupg.org/download/)

## 参考

- [发布Android Lib库（Jar、AAR、SO）到Maven Central](https://blog.csdn.net/xiaozeiqwe/article/details/117379335)
