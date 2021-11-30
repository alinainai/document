核心是通过带接收器的拓展函数实现 DSL 代码

```kotlin
import androidx.viewpager.widget.ViewPager

class _OnPageChangeListener : ViewPager.OnPageChangeListener {

    private var _onPageScrollStateChanged: ((state: Int) -> Unit)? = null
    private var _onPageScrolled: ((position: Int, positionOffset: Float, positionOffsetPixels: Int) -> Unit)? = null
    private var _onPageSelected: ((position: Int) -> Unit)? = null

    override fun onPageScrollStateChanged(state: Int) {
        _onPageScrollStateChanged?.invoke(state)
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        _onPageScrolled?.invoke(position, positionOffset, positionOffsetPixels)
    }

    override fun onPageSelected(position: Int) {
        _onPageSelected?.invoke(position)
    }

    fun onPageScrollStateChanged(func: (state: Int) -> Unit) {
        _onPageScrollStateChanged = func
    }

    fun onPageScrolled(func: (position: Int, positionOffset: Float, positionOffsetPixels: Int) -> Unit) {
        _onPageScrolled = func
    }

    fun onPageSelected(func: (position: Int) -> Unit) {
        _onPageSelected = func
    }

}

inline fun ViewPager.addOnPageChangeListener(func:_OnPageChangeListener.()-> Unit) {
    val listener = _OnPageChangeListener()
    listener.func()
    addOnPageChangeListener(listener)
}
```
