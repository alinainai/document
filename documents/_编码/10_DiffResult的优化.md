## 一、介绍

我们要实现的功能：在子线程中计算 RecyclerView 数据 DiffResult 并回调

RecyclerView 是我们日常开发中最常用的组件之一。当我们滑动列表，我们要去更新视图，更新数据。我们会从服务器获取新的数据，需要处理旧的数据。

通常，随着每个item越来越复杂，这个处理过程所需的时间也就越多。在列表滑动过程中的处理延迟的长短，决定着对用户体验的影响的多少。所以，我们会希望需要进行的计算越少越好。

现在，我们的列表已经显示在屏幕上，获取的新的数据后需要更新，我们会调用 notifyDataSetChanged() 方法。然而这个方法实际上非常消耗计算能力。因为它涉及很多迭代操作。

介于这些问题，Android 提供了一个优化类 DiffUtil 用来处理 RecyclerView 的数据更新问题。

### 1、DiffUtil

从 `24.2.0` 开始， `RecyclerView` 的支持库在 `v7` 提供了非常方便的优化类 `DiffUtil`。这个类帮助我们计算新数据集合和旧数据集合的区别，Adapter 在应用这些区别来刷新数据。

### 2、如何使用

DiffUtil.Callback 作为 callback 类，DiffUtil 计算出差别后会回掉这个类的方法。 DiffUtil.Callback 是拥有 4 个抽象方法和 1 个非抽象方法的抽象类。我们需要继承并实现它的所有方法：

- getOldListSize() 返回原始列表的 size。
- getNewListSize() 返回新列表的 size。
- areItemsTheSame(int oldItemPosition, int newItemPosition) 两个位置的对象是否是同一个item。
- areContentsTheSame(int oldItemPosition, int newItemPosition) 决定是否两个 item 的数据是相同的。只有当 areItemsTheSame() 返回true时会调用。
- getChangePayload(int oldItemPosition, int newItemPosition) 当 areItemsTheSame() 返回 true ，并且 areContentsTheSame() 返回 false 时调用，返回这个 item 更新相关的信息。

下面是一个简单的 Employee 类，使用了 EmployeeRecyclerViewAdapter 和 EmployeeDiffCallback 来完成这个列表的展示更新逻辑。
```java
public class Employee {  
    public int id;
    public String name;
    public String role;
}
```
这是 DiffUtil.Callback 类的实现。注意 getChangePayload() 不是抽象方法。
```java
public class EmployeeDiffCallback extends DiffUtil.Callback {

    private final List<Employee> mOldEmployeeList;
    private final List<Employee> mNewEmployeeList;

    public EmployeeDiffCallback(List<Employee> oldEmployeeList, List<Employee> newEmployeeList) {
        this.mOldEmployeeList = oldEmployeeList;
        this.mNewEmployeeList = newEmployeeList;
    }

    @Override
    public int getOldListSize() {
        return mOldEmployeeList.size();
    }

    @Override
    public int getNewListSize() {
        return mNewEmployeeList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return mOldEmployeeList.get(oldItemPosition).getId() == mNewEmployeeList.get(
                newItemPosition).getId();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        final Employee oldEmployee = mOldEmployeeList.get(oldItemPosition);
        final Employee newEmployee = mNewEmployeeList.get(newItemPosition);

        return oldEmployee.getName().equals(newEmployee.getName());
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        // Implement method if you're going to use ItemAnimator
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}
```
实现了 DiffUtil.Callback，我们就可以用下面的方式更新列表了。
```java
public class CustomRecyclerViewAdapter extends RecyclerView.Adapter<CustomRecyclerViewAdapter.ViewHolder> {

  ...
       public void updateEmployeeListItems(List<Employee> employees) {
        final EmployeeDiffCallback diffCallback = new EmployeeDiffCallback(this.mEmployees, employees);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.mEmployees.clear();
        this.mEmployees.addAll(employees);
        diffResult.dispatchUpdatesTo(this);
    }
}
```
调用 dispatchUpdatesTo(RecyclerView.Adapter) 方法分发更新后的列表。 DiffUtil 计算出差异得到 DiffResult ，DiffResult 再把差异分发给 Adapter，adapter 最后根据接收到的差异数据做更新。

getChangePayload() 返回的差异数据，会从 DiffResult 分发给 notifyItemRangeChanged(position, count, payload) 方法，最终交给 Adapter 的 onBindViewHolder(… List< Object > payloads) 处理。
```java
@Override
public void onBindViewHolder(ProductViewHolder holder, int position, List<Object> payloads) {  
// Handle the payload
}
```
DiffUtil 一般通过这四个方法通知 Adapter 来更新数据。
```java
notifyItemMoved()
notifyItemRangeChanged()
notifyItemRangeInserted()
notifyItemRangeRemoved()
```
## 二、DiffUtil的优化
如果列表很大，DiffUtil 的计算操作会花费很多时间。所以官方建议在后台线程计算差异，在主线程应用计算结果 DiffResult。于是官方推出了 ListAdapter 类。

但是 ListAdapter 没有数据更新后的回调方法，参考RecyclerView的ListAdapter代码做了部分更改，添加了数据更新后的后调方法。

#### CallBackAsyncListDiffer 代码
```kotlin
class CallBackAsyncListDiffer<T>(private val mUpdateCallback: ListUpdateCallback, private val mConfig: CallbackAsyncDifferConfig<T>) {
                                 
    private var mMainThreadExecutor: Executor? = null
    private class MainThreadExecutor internal constructor() : Executor {
        val mHandler = Handler(Looper.getMainLooper())
        override fun execute(command: Runnable) {
            mHandler.post(command)
        }
    }

    private var mList: List<T>? = null
    var currentList: List<T> = emptyList()
        private set
    private var mMaxScheduledGeneration = 0

    /**
     * 通过子线程更新数据然后在主线程的 mMainThreadExecutor 中返回计算结果
     */
    fun submitList(newList: List<T>?, runnable: Runnable) {
        // incrementing generation means any currently-running diffs are discarded when they finish
        val runGeneration = ++mMaxScheduledGeneration
        if (newList === mList) {
        	//同样一个集合，不做改变，直接返回即可。
            return
        }

        // fast simple remove all
        if (newList == null) {
            val countRemoved = mList!!.size
            mList = null
            currentList = emptyList()
            // notify last, after list is updated
            mUpdateCallback.onRemoved(0, countRemoved)
            runnable.run()
            return
        }

        // 如果是新传入数据，直接插入数据
        if (mList == null) {
            mList = newList
            currentList = Collections.unmodifiableList(newList)
            // notify last, after list is updated
            mUpdateCallback.onInserted(0, newList.size)
            runnable.run()
            return
        }
        val oldList: List<T> = mList!!
        //后台操作，使用Executor类实现数据的差异计算
        mConfig.backgroundThreadExecutor?.execute {
            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return oldList.size
                }

                override fun getNewListSize(): Int {
                    return newList.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem: T? = oldList[oldItemPosition]
                    val newItem: T? = newList[newItemPosition]
                    return if (oldItem != null && newItem != null) {
                        mConfig.diffCallback.areItemsTheSame(oldItem, newItem)
                    } else oldItem == null && newItem == null
                    // If both items are null we consider them the same.
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem: T? = oldList[oldItemPosition]
                    val newItem: T? = newList[newItemPosition]
                    if (oldItem != null && newItem != null) {
                        return mConfig.diffCallback.areContentsTheSame(oldItem, newItem)
                    }
                    if (oldItem == null && newItem == null) {
                        return true
                    }
                    throw AssertionError()
                }

                override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                    val oldItem: T? = oldList[oldItemPosition]
                    val newItem: T? = newList[newItemPosition]
                    if (oldItem != null && newItem != null) {
                        return mConfig.diffCallback.getChangePayload(oldItem, newItem)
                    }
                    throw AssertionError()
                }
            })
            //主线程中将差异应用到Adapter中
            mMainThreadExecutor!!.execute {
                if (mMaxScheduledGeneration == runGeneration) {
                    latchList(newList, result)
                    runnable.run()
                }
            }
        }
    }

    private fun latchList(newList: List<T>, diffResult: DiffUtil.DiffResult) {
        mList = newList
        currentList = Collections.unmodifiableList(newList)
        diffResult.dispatchUpdatesTo(mUpdateCallback)
    }

    class CallbackAsyncDifferConfig<T> internal constructor(
            val mainThreadExecutor: Executor?,
            val backgroundThreadExecutor: Executor?,
            val diffCallback: DiffUtil.ItemCallback<T>) {


        class Builder<T>(private val mDiffCallback: DiffUtil.ItemCallback<T>) {
            private var mMainThreadExecutor: Executor? = null
            private var mBackgroundThreadExecutor: Executor? = null

            fun build(): CallbackAsyncDifferConfig<T> {
                if (mBackgroundThreadExecutor == null) {
                    synchronized(sExecutorLock) {
                        if (sDiffExecutor == null) {
                            sDiffExecutor = Executors.newFixedThreadPool(2)
                        }
                    }
                    mBackgroundThreadExecutor = sDiffExecutor
                }
                return CallbackAsyncDifferConfig(
                        mMainThreadExecutor,
                        mBackgroundThreadExecutor,
                        mDiffCallback)
            }

            companion object {
                private val sExecutorLock = Any()
                private var sDiffExecutor: Executor? = null
            }

        }

    }

    companion object {
        private val sMainThreadExecutor: Executor = MainThreadExecutor()
    }

    init {
        mMainThreadExecutor = mConfig.mainThreadExecutor ?: sMainThreadExecutor
    }
}
```
## 三、BaseListViewAdapter主要代码
```kotlin
abstract class BaseListViewAdapter<VH : RecyclerView.ViewHolder, M> protected constructor(diffCallback: DiffUtil.ItemCallback<M>) : RecyclerView.Adapter<VH>() {

	//用于计算提交的两个lists的不同，在后台进行
    private val mHelper by lazy { CallBackAsyncListDiffer(AdapterListUpdateCallback(this), CallBackAsyncListDiffer.CallbackAsyncDifferConfig.Builder(diffCallback).build()) }

    val data: List<M>
        get() = mHelper.currentList

    fun getItem(position: Int): M? {
        if (position in data.indices)
            return data[position]
        return null
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun submitList(data: List<M>?, runnable: Runnable) {
        val newList: List<M> = ArrayList(data ?: emptyList())
        mHelper.submitList(newList, runnable)
    }

    fun getItemPos(item: M): Int {
        return data.indexOf(item)
    }

}
```
### 使用
```kotlin
class AsyncUpdateDataAdapter : BaseListViewAdapter<RecyclerView.ViewHolder, BaseTimestamp>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_EVENT_ITEM -> {
                ListViewHolder(parent)
            }
            else -> ListViewHolder(parent)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ListViewHolder -> {
                holder.bind(data[position] as DataItemBean)
            }
        }
    }

    companion object {
        val DIFF_CALLBACK: DiffUtil.ItemCallback<BaseTimestamp> = object : DiffUtil.ItemCallback<BaseTimestamp>() {
            override fun areItemsTheSame(oldItem: BaseTimestamp, newItem: BaseTimestamp): Boolean {
                //用于区分的实体类的唯一表示符
                return oldItem.uniqueId() == newItem.uniqueId()
            }

            override fun areContentsTheSame(oldItem: BaseTimestamp, newItem: BaseTimestamp): Boolean {
                //可变内容
                return oldItem.variableParam() == newItem.variableParam()
            }
        }
    }

    class ListViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_async_test_list, parent, false)) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        fun bind(info: DataItemBean) {
            tvTitle.text ="序号：${info.title} ----- 时间：${TimeUtils.getHourAndMinute(info.timeStamp)}"
        }
    }

}
```
