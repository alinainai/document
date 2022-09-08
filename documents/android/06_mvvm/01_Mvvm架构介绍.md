## 1. MVVM 简单介绍

先看一个简单的架构图

<img width="331" alt="image" src="https://user-images.githubusercontent.com/17560388/189058728-aeccda4c-5fe6-4dc8-9f41-ea226a71fea9.png">

View层: 数据的展示层，将数据展示给用户，并接收用户交互的信号。
ViewModel: 视图模型，进行数据处理、封装交互逻辑。
Modle: 为 ViewModel 提供数据源，封装数据获取的逻辑。在开发中，我们可以直接使用数据仓库或者 Domain 替代。

MVVM 比 MVP 好的地方就是，通过`监听者模式`实现UI和数据的同步，不用再使用`回调的方式`实现 View层 和 Presenter层 的交互。
- 当数据层改变时，ViewModel 中的 State 改变，监听 State 的 UI 做对应的处理。
- 当用户和UI交互时，产生 Event 事件。ViewModel 处理 Event 事件，并做响应的逻辑处理。如更新数据、跳转界面、提示等操作。




<img width="800" alt="类图" src="https://user-images.githubusercontent.com/17560388/189053570-e1e6dfc5-71de-4ad4-94f7-ccaad8ff5d51.svg">

架构图的链接：[https://www.processon.com/view/link/63198e6c7d9c0833ec8302b9](https://www.processon.com/view/link/63198e6c7d9c0833ec8302b9)



## 参考

- [应用架构指南](https://developer.android.com/jetpack/guide?hl=zh-cn)
- [Google mvvm 的官方架构](https://github.com/android/architecture-samples/tree/mvvm+)
