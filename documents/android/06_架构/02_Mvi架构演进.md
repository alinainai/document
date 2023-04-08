
## 一、架构知识

[官方的架构指南](https://developer.android.com/jetpack/guide?hl=zh-cn) 

升级版架构指南 2021、12，有点 `MVI` 的味道

<img width="573" alt="升级版架构指南" src="https://user-images.githubusercontent.com/17560388/154452988-d81d8ab8-e7a0-4713-8289-239d1e274956.png">

## 二、新版架在旧版的基础上的调整和建议

### 1、将 LiveData 组件改成了 StateFlow

对协程的使用更友好。并且更能体现面向数据流开发的思想。

实际上，依然使用LiveData也没毛病。

### 2、ViewModel传递给View的数据限制为View的UIState

ViewModel 从 Model层获取数据后，转换为 UIState 数据，通过 StateFlow 流向 View 层。

UIState 的数据面向界面组件而定义的，是能直接控制 View 组件如何显示的数据。

所以我们也可以称 UIState 为界面的状态或者 View 的状态。

如下：

```kotlin
data class NewsUiState(
    val isSignedIn: Boolean = false,
    val isPremium: Boolean = false,
    val newsItems: List<NewsItemUiState> = listOf()
)
```

### 3、单数据流or多数据流

官方指南并没有强制我们使用单流。

同一个界面应该使用单个 StateFlow 还是多个StateFlow，需要我们自己判断。

我们应该根据UIStates数据们之间关联程度来决定多流还是单流。

单流优缺点都十分明显：
- 优点：数据集中管控，会提高代码的可读性和修改的便利性。
- 缺点：当数据非常多且复杂时，会影响效率。因为我们没有diff功能，View层不能只更新有变化的数据，只会根据 UIState 刷新当前界面。

https://developer.android.com/jetpack/guide/ui-layer#additional-considerations

我们再看下官方新版架构图：

<img width="573" alt="官方新版架构图" src="https://user-images.githubusercontent.com/17560388/154453572-e8e847d8-71c1-4c45-8078-6e525f87bffc.png">

## 三、Jetpack 包

### 1、ViewModle
- 1.ViewModel 以生命周期的方式存储和管理界面相关的数据。
- 2.Activity 通过更改配置（如旋转）重建后，ViewModel 的生命周期不会影响。可以直接复用 ViewModel 以保存的数据。
- 3.不用再像原先一样在 Activit 的 Destroy 方法中做 ViewModel 的 clear 回调。
- 4.多个 Fragment 共享数据。

### 2、LiveData 和 MultableLiveData 的区别

MultableLiveData 是 LiveData 子类。并对外公开了 postValue 和 setValue 的方法。

## 参考

- [刚刚开源的 Android 官方 App，了解一下？](https://mp.weixin.qq.com/s/GDfC1J9rm3TtmP5gc_eXfA)
