
在上一篇文档中我们简单的实现了一个 AIDL 的例子，在本篇中我们来进阶一下，看下 Aidl 的关键字和其他一些技巧

## 一、in/out/inout 关键字

还是用咱们上篇文档中已经实现的 AIDL 的 demo。我们注意到 addPerson() 方法有个 `in` 关键字

在 IUserAidlInterface.adil 添加两个新方法


