## 1、MVVM 简单介绍

先看一个简单的 MVVM 的架构图

<img width="331" alt="MVVM" src="https://user-images.githubusercontent.com/17560388/189058728-aeccda4c-5fe6-4dc8-9f41-ea226a71fea9.png">

我们简单的分析一下上图中的三个元素：

- View层: 数据的展示层，将数据展示给用户，并接收用户交互的信号。
- ViewModel: 视图模型，进行数据处理、封装交互逻辑。
- Modle: 为 ViewModel 提供数据源，封装数据获取的逻辑。在开发中，我们可以直接使用数据仓库或者 Domain 替代。

MVVM 比 MVP 好的地方就是，通过`监听者模式`实现UI和数据的同步，不用再使用`回调的方式`实现 `View层` 和 `Presenter层` 的交互。

- 当数据层改变时，ViewModel 中的 `State` 改变，监听 State 的 UI 做对应的处理。
- 当用户和UI交互时，产生 `Event` 事件，ViewModel 处理 Event 事件，并做响应的逻辑处理。如更新数据、跳转界面、提示等操作。


大体了解MVVM之后我们看下怎么结合代码去实现这个架构。


## 2、代码实现

先说一下我们更新MVVM架构的优势

- Google 官方提供了强力支持，推出了和 Mvvm 配套的 Jetpack 相关的组件。
- 和 Activity/Fragment 充分解耦，二者不用再去实现 View 层的接口。
- ViewModel 和 Activity/Fragment 脱离，可以更好的支持单元测试。

再看一个和代码结合之后的 MVVM 的架构图。

<img width="800" alt="MVVM结合代码" src="https://user-images.githubusercontent.com/17560388/189060550-abce019e-a861-4928-ba4a-63d067086ab9.png">

架构图的链接：[https://www.processon.com/view/link/63198e6c7d9c0833ec8302b9](https://www.processon.com/view/link/63198e6c7d9c0833ec8302b9)

Google 建议应用架构分层是 `UI层 -> Domian层 -> Data层`。当然，Domain 层不是必须，所以我们在上图中使用虚线框表示 Domain 层。

结合图例我们大体说下 MVVM 的代码实现。

- UI层

不管是之前的 MVP 架构，现在流行的 MVVM 架构，还是 Google 最近推出的 MVI 架构，其实都是针对 UI 层的封装。在 Android 中，我们主要通过 Activity/Fragment 来充当架构中的 View角色。而 MVVM 中的 VM（ViewModel）由 Ggoogle 提供的 Jetpack 的 ViewModel 组件来担任。
ViewModel 通过`数据仓库或者 Domain` 来获取数据源，并对外提供数据源变化的被监听者（LiveData）。我们在 Activity/Fragment 中监听 LiveData 中的数据变化，当数据源改变(也就是数据的State改变)时，触发 Activity/Fragment 的变化。

- Domain层和Data层

Domain层和数据仓库都可以理解为 Model 层。有些业务逻辑不太复杂的，我们直接通过 `数据仓库` 获取数据源。如果业务比较复杂，涉及到多个业务的数据交互，我们通过封装 Domain层中的用例（UseCase）来处理数据间的复杂逻辑，每个用例都应仅负责单个功能。

UseCase中可以持有多个 DataRepository，也可以持有其他业务的 UseCase，如图。

DataRepository 通过不同的数据源获取相关的数据，我们图中给出了两个用例。用于获取 WebServer 数据的 RemoteDataSource 和用于获取本地数据的 LocalDataSource。


## 参考

- [应用架构指南](https://developer.android.com/jetpack/guide?hl=zh-cn)
- [Google mvvm 的官方架构](https://github.com/android/architecture-samples/tree/mvvm+)
