### 1.ViewModle

1.ViewModel 以生命周期的方式存储和管理界面相关的数据。

2.Activity 通过更改配置（如旋转）重建后，ViewModel 的生命周期不会影响。可以直接复用 ViewModel 以保存的数据。

3.不用再像原先一样在 Activit 的 Destroy 方法中做 ViewModel 的 clear 回调。

4.多个 Fragment 共享数据。

### 2.LiveData 和 MultableLiveData 的区别

MultableLiveData 是 LiveData 子类。并对外公开了 postValue 和 setValue 的方法。


