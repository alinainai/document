ListView与RecyclerView缓存机制原理大致相似，如下图所示：

<img width="673" alt="listview_cache" src="https://user-images.githubusercontent.com/17560388/125718870-718eb197-b7f4-41a2-9640-168817395ce1.jpeg">

滑动过程中，离屏的ItemView即被回收至缓存，入屏的ItemView则会优先从缓存中获取，只是ListView与RecyclerView的实现细节有差异.（这只是缓存使用的其中一个场景，还有如刷新等）


## 缓存机制对比

#### 1. 层级不同：

RecyclerView比ListView多两级缓存，支持多个离ItemView缓存，支持开发者自定义缓存处理逻辑，支持所有RecyclerView共用同一个RecyclerViewPool(缓存池)。

具体来说：

ListView(两级缓存)：

<img width="673" alt="listview_cache" src="https://user-images.githubusercontent.com/17560388/125719530-df9560f0-948d-49ca-80d0-11b7aea63225.png">

RecyclerView(四级缓存)：

<img width="673" alt="listview_cache" src="https://user-images.githubusercontent.com/17560388/125719559-2847a0ed-5125-402b-a9eb-836168d27335.png">

ListView和RecyclerView缓存机制基本一致：

1). mActiveViews和mAttachedScrap功能相似，意义在于快速重用屏幕上可见的列表项ItemView，而不需要重新createView和bindView；

2). mScrapView和mCachedViews + mReyclerViewPool功能相似，意义在于缓存离开屏幕的ItemView，目的是让即将进入屏幕的ItemView重用.

3). RecyclerView的优势在于:
 - a.mCacheViews的使用，可以做到屏幕外的列表项ItemView进入屏幕内时也无须bindView快速重用；
 - b.mRecyclerPool可以供多个RecyclerView共同使用，在特定场景下，如viewpaper+多个列表页下有优势。

客观来说，RecyclerView在特定场景下对ListView的缓存机制做了补强和完善。


#### 2. 缓存不同：

1). RecyclerView缓存RecyclerView.ViewHolder，抽象可理解为：

View + ViewHolder(避免每次createView时调用findViewById) + flag(标识状态)；

2). ListView缓存View。

缓存不同，二者在缓存的使用上也略有差别，具体来说：

#### ListView获取缓存的流程：

<img width="400" alt="listview_cache" src="https://user-images.githubusercontent.com/17560388/125720347-e031d494-ce0a-4952-be7b-523291c5d3b6.png">

#### RecyclerView获取缓存的流程：

<img width="400" alt="listview_cache" src="https://user-images.githubusercontent.com/17560388/125720402-bac1e8c8-8c4e-418b-ace9-0d047c777dc5.png">

1). RecyclerView中mCacheViews(屏幕外)获取缓存时，是通过匹配pos获取目标位置的缓存，这样做的好处是，当数据源数据不变的情况下，无须重新bindView：

![image](https://user-images.githubusercontent.com/17560388/125720446-0fc30756-fa8c-4e1f-9e62-3ff9b03a7bf4.png)

而同样是离屏缓存，ListView从mScrapViews根据pos获取相应的缓存，但是并没有直接使用，而是重新getView（即必定会重新bindView），相关代码如下：

<img width="400" alt="listview_cache" src="https://user-images.githubusercontent.com/17560388/125720467-2410efab-8a90-4f42-9599-551b4ac44fb1.png">

2). ListView中通过pos获取的是view，即pos-->view；

RecyclerView中通过pos获取的是viewholder，即pos --> (view，viewHolder，flag)；

从流程图中可以看出，标志flag的作用是判断view是否需要重新bindView，这也是RecyclerView实现局部刷新的一个核心.

## 局部刷新

由上文可知，RecyclerView的缓存机制确实更加完善，但还不算质的变化，RecyclerView更大的亮点在于提供了局部刷新的接口，通过局部刷新，就能避免调用许多无用的bindView.

![image](https://user-images.githubusercontent.com/17560388/125720714-f6e49881-8e80-4890-8a97-8ecb9d525040.png)

(RecyclerView和ListView添加，移除Item效果对比)

结合RecyclerView的缓存机制，看看局部刷新是如何实现的：

以RecyclerView中notifyItemRemoved(1)为例，最终会调用requestLayout()，使整个RecyclerView重新绘制，过程为：

onMeasure()-->onLayout()-->onDraw()

其中，onLayout()为重点，分为三步：

dispathLayoutStep1()：记录RecyclerView刷新前列表项ItemView的各种信息，如Top,Left,Bottom,Right，用于动画的相关计算；

dispathLayoutStep2()：真正测量布局大小，位置，核心函数为layoutChildren()；

dispathLayoutStep3()：计算布局前后各个ItemView的状态，如Remove，Add，Move，Update等，如有必要执行相应的动画.

其中，layoutChildren()流程图：

<img width="400" alt="listview_cache" src="https://user-images.githubusercontent.com/17560388/125720742-70404adf-2ae4-4748-8ea9-c2919ad362fa.png">

<img width="400" alt="listview_cache" src="https://user-images.githubusercontent.com/17560388/125720793-e1a1f26d-642f-4a31-9c69-e352493d9209.png">

当调用notifyItemRemoved时，会对屏幕内ItemView做预处理，修改ItemView相应的pos以及flag(流程图中红色部分)：

<img width="600" alt="listview_cache" src="https://user-images.githubusercontent.com/17560388/125720814-2e4b2a34-88ab-41d9-a10c-2b81a5ef165b.png">


当调用fill()中RecyclerView.getViewForPosition(pos)时，RecyclerView通过对pos和flag的预处理，使得bindview只调用一次.

需要指出，ListView和RecyclerView最大的区别在于数据源改变时的缓存的处理逻辑，ListView是"一锅端"，将所有的mActiveViews都移入了二级缓存mScrapViews，而RecyclerView则是更加灵活地对每个View修改标志位，区分是否重新bindView。
