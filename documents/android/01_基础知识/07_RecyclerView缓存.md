## 1、ListView与RecyclerView缓存机制原理

`ListView` 与 `RecyclerView` 缓存机制原理大致相似，如下图所示：

<img width="673" alt="listview_cache" src="https://user-images.githubusercontent.com/17560388/125718870-718eb197-b7f4-41a2-9640-168817395ce1.jpeg">

滑动过程中，离屏的ItemView即被回收至缓存，入屏的ItemView则会优先从缓存中获取，只是ListView与RecyclerView的实现细节有差异.（这只是缓存使用的其中一个场景，还有如刷新等）

## 2、缓存机制对比

### 2.1 层级不同：

`RecyclerView` 比 `ListView` 多两级缓存，支持多个离`ItemView` 缓存，支持开发者自定义缓存处理逻辑，支持所有 `RecyclerView` 共用同一个 `RecyclerViewPool(缓存池)`。

具体来说：

`ListView`(两级缓存)：

<img width="673" alt="listview_cache" src="https://user-images.githubusercontent.com/17560388/125719530-df9560f0-948d-49ca-80d0-11b7aea63225.png">

`RecyclerView`(四级缓存)：

<img width="673" alt="listview_cache" src="https://user-images.githubusercontent.com/17560388/125719559-2847a0ed-5125-402b-a9eb-836168d27335.png">

`ListView` 和 `RecyclerView` 缓存机制基本一致：

1).mActiveViews和mAttachedScrap功能相似，意义在于快速重用屏幕上可见的列表项ItemView，而不需要重新createView和bindView；

2).mScrapView和mCachedViews + mReyclerViewPool功能相似，意义在于缓存离开屏幕的ItemView，目的是让即将进入屏幕的ItemView重用.

3).RecyclerView的优势在于:
 - a.mCacheViews的使用，可以做到屏幕外的列表项ItemView进入屏幕内时也无须bindView快速重用；
 - b.mRecyclerPool可以供多个RecyclerView共同使用，在特定场景下，如viewpaper+多个列表页下有优势。

客观来说，RecyclerView在特定场景下对ListView的缓存机制做了补强和完善。

### 2.2 缓存不同

1).`RecyclerView` 缓存 `RecyclerView.ViewHolder`，抽象可理解为：

`View` + `ViewHolder`(避免每次`createView`时调用findViewById) + flag(标识状态)；

2).`ListView` 缓存 `View`。

二者在缓存的使用上也略有差别，具体来说：

### ListView获取缓存的流程：

<img width="400" alt="listview_cache" src="https://user-images.githubusercontent.com/17560388/125720347-e031d494-ce0a-4952-be7b-523291c5d3b6.png">

### RecyclerView获取缓存的流程：

<img width="400" alt="listview_cache" src="https://user-images.githubusercontent.com/17560388/125720402-bac1e8c8-8c4e-418b-ace9-0d047c777dc5.png">

1).`RecyclerView` 中 `mCacheViews` (屏幕外)获取缓存时，是通过匹配 `pos` 获取目标位置的缓存，这样做的好处是，当数据源数据不变的情况下，无须重新 `bindView`：

![image](https://user-images.githubusercontent.com/17560388/125720446-0fc30756-fa8c-4e1f-9e62-3ff9b03a7bf4.png)

而同样是离屏缓存，`ListView` 从 `mScrapViews` 根据 `pos` 获取相应的缓存，但是并没有直接使用，而是重新 `getView（即必定会重新bindView）`，相关代码如下：

<img width="400" alt="listview_cache" src="https://user-images.githubusercontent.com/17560388/125720467-2410efab-8a90-4f42-9599-551b4ac44fb1.png">

2).`ListView`中通过`pos`获取的是`view`，即`pos-->view`；

`RecyclerView`中通过`pos`获取的是`viewholder`，即`pos --> (view，viewHolder，flag)`；

从流程图中可以看出，标志`flag`的作用是判断`view`是否需要重新`bindView`，这也是R`ecyclerView`实现局部刷新的一个核心.

## 3、局部刷新

由上文可知，`RecyclerView` 的缓存机制确实更加完善，但还不算质的变化，`RecyclerView` 更大的亮点在于提供了局部刷新的接口，通过局部刷新，就能避免调用许多无用的 `bindView`.

![image](https://user-images.githubusercontent.com/17560388/125720714-f6e49881-8e80-4890-8a97-8ecb9d525040.png)

(`RecyclerView` 和 `ListView` 添加，移除`Item`效果对比)

结合`RecyclerView`的缓存机制，看看局部刷新是如何实现的：

以`RecyclerView`中`notifyItemRemoved(1)`为例，最终会调用`requestLayout()`，使整个`RecyclerView`重新绘制，过程为：

`onMeasure()-->onLayout()-->onDraw()`

其中，onLayout()为重点，分为三步：

`dispathLayoutStep1()`：记录`RecyclerView`刷新前列表项`ItemView`的各种信息，如`Top,Left,Bottom,Right`，用于动画的相关计算；

`dispathLayoutStep2()`：真正测量布局大小，位置，核心函数为`layoutChildren()`；

`dispathLayoutStep3()`：计算布局前后各个`ItemView`的状态，如`Remove，Add，Move，Update`等，如有必要执行相应的动画.

其中，`layoutChildren()`流程图：

<img width="400" alt="listview_cache" src="https://user-images.githubusercontent.com/17560388/125720742-70404adf-2ae4-4748-8ea9-c2919ad362fa.png">

<img width="400" alt="listview_cache" src="https://user-images.githubusercontent.com/17560388/125720793-e1a1f26d-642f-4a31-9c69-e352493d9209.png">

当调用 `notifyItemRemoved` 时，会对屏幕内`ItemView`做预处理，修改`ItemView`相应的`pos`以及`flag(流程图中红色部分)`：

<img width="600" alt="listview_cache" src="https://user-images.githubusercontent.com/17560388/125720814-2e4b2a34-88ab-41d9-a10c-2b81a5ef165b.png">


当调用`fill()`中`RecyclerView.getViewForPosition(pos)`时，`RecyclerView`通过对`pos`和`flag`的预处理，使得`bindview`只调用一次.

需要指出，`ListView`和`RecyclerView`最大的区别在于数据源改变时的缓存的处理逻辑，`ListView`是"一锅端"，将所有的`mActiveViews`都移入了二级缓存`mScrapViews`，而`RecyclerView`则是更加灵活地对每个`View`修改标志位，区分是否重新`bindView`。
