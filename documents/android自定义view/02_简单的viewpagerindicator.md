### 具体代码

![image](https://user-images.githubusercontent.com/17560388/158371862-54a2e0b7-b8e1-4a25-b0d6-cacebd2878c0.png)


属性值

```html
    <declare-styleable name="RectShapeIndicator">
        <attr name="rsi_selected_color" format="color|reference" />
        <attr name="rsi_default_color" format="color|reference" />
        <attr name="rsi_radius" format="dimension|reference" />
        <attr name="rsi_radius_selected" format="dimension|reference" />
        <attr name="rsi_length" format="dimension|reference" />
        <attr name="rsi_gap" format="dimension|reference" /> 
        <attr name="rsi_scale" format="float" />
        <attr name="rsi_animation" format="boolean" />
    </declare-styleable>
```

注意 ViewPager 滑动时，posi 和 offset 的变化

```kotlin
class RectShapeIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private lateinit var paintFill: Paint
    private var mNum = 0 //个数
    private var mRadius = 0F//半径
    private var mOffset = 0f//偏移量
    private var mSelected_color = 0 //选中颜色
    private var mGap = 0f//间隔距离
    private var mPosition = 0//第几张
    private val mIsLeft = false
    private var mAnimation = false
    private var mScale = 0f
    private var unselectUnitLength = 0f
    private var drawTop = 0F
    private var drawStart = 0F
    private var drawBottom = 0F
    private var centerY = 0F

    init {
        attrs?.let {
            initAttrs(it)
        }
        init()
    }

    private fun initAttrs(attrs: AttributeSet) {
        val array = context.obtainStyledAttributes(attrs, R.styleable.RectShapeIndicator)
        mSelected_color = array.getColor(R.styleable.RectShapeIndicator_rsi_selected_color, -0x1)
        mRadius = array.getDimension(R.styleable.RectShapeIndicator_rsi_radius, 20F) //px
        mGap = array.getDimension(R.styleable.RectShapeIndicator_rsi_gap, 20F) //px
        mScale = array.getFloat(R.styleable.RectShapeIndicator_rsi_scale, 2.0F) //px
        mAnimation = array.getBoolean(R.styleable.RectShapeIndicator_rsi_animation, true)
        array.recycle()
    }

    private fun init() {
        paintFill = Paint()
        paintFill.style = Paint.Style.FILL_AND_STROKE
        paintFill.isAntiAlias = true
        paintFill.strokeWidth = 0f
        paintFill.color = mSelected_color

        unselectUnitLength = mGap + 2 * mRadius
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightSize =
            measureView(heightMeasureSpec, paddingTop + paddingBottom + 2 * mRadius.toInt())
        setMeasuredDimension(widthMeasureSpec, heightSize)
    }
    
    //onSizeChanged 方法获取的 h 和 w 是准确的
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerY = (h / 2).toFloat()
        drawTop = centerY - mRadius
        drawBottom = drawTop + 2 * mRadius
        drawStart = paddingStart.toFloat()
    }

    //根据 MeasureMode 去设置最终的 w 和 h
    private fun measureView(measureSpec: Int, defaultSize: Int): Int {
        var result = defaultSize
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            result = result.coerceAtLeast(specSize)
        } else if (specMode == MeasureSpec.AT_MOST) {
            result = result.coerceAtMost(specSize)
        }
        return result
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(-drawStart, 0F)
        if (mNum <= 0) {
            return
        }
        if (mNum == 1) { //就一张图就话一个点，简单处理
            canvas.drawCircle(mRadius, centerY, mRadius, paintFill)
            return
        }
        val rW = (mNum - 1) * unselectUnitLength + mScale * 2 * mRadius
        if (mPosition == mNum - 1) { //最后一个 右滑
            //第一个 线 选中 消失
            val leftClose = 0f
            val rightClose = leftClose + 2 * mRadius + mOffset
            canvas.drawRoundRect(
                leftClose,
                drawTop,
                rightClose,
                drawBottom,
                mRadius,
                mRadius,
                paintFill
            )
            //最后一个 线  显示
            val leftOpen = rW - mScale * 2 * mRadius + mOffset
            canvas.drawRoundRect(leftOpen, drawTop, rW, drawBottom, mRadius, mRadius, paintFill)
            //圆
            for (i in 1 until mNum - 1) {
                canvas.drawCircle(
                    rightClose + mGap + mRadius + (i - 1) * unselectUnitLength,
                    centerY,
                    mRadius,
                    paintFill
                )
            }
        } else {
            //第一个 线 选中 消失
            val leftClose = mPosition * unselectUnitLength
            val rightClose = leftClose + mScale * 2 * mRadius - mOffset
            canvas.drawRoundRect(
                leftClose,
                drawTop,
                rightClose,
                drawBottom,
                mRadius,
                mRadius,
                paintFill
            )
            //第二个 线  显示
            if (mPosition < mNum - 1) {
                val rightOpen = (mPosition + 1) * unselectUnitLength + mScale * 2 * mRadius
                val leftOpen = rightOpen - 2 * mRadius - mOffset
                canvas.drawRoundRect(
                    leftOpen,
                    drawTop,
                    rightOpen,
                    drawBottom,
                    mRadius,
                    mRadius,
                    paintFill
                )
                //圆
                for (i in mPosition + 2 until mNum) {
                    canvas.drawCircle(
                        rightOpen + mGap + mRadius + (i - mPosition - 2) * unselectUnitLength,
                        centerY,
                        mRadius,
                        paintFill
                    )
                }
            }
            for (i in mPosition - 1 downTo 0) {
                canvas.drawCircle(mRadius + i * unselectUnitLength, centerY, mRadius, paintFill)
            }
        }
    }

    private fun move(percent: Float, position: Int) {
        mPosition = position
        mOffset = percent * (mScale - 1) * 2 * mRadius
        invalidate()
    }

    fun setViewPager(viewPager: ViewPager) {
        viewPager.let {
            if (viewPager.adapter?.count ?: -1 >= 0) {
                setViewPager(viewPager, viewPager.adapter!!.count)
            }
        }
    }

    /**
     * @param viewpager   适配的viewpager
     * @param cycleNum 循环的个数
     */
    fun setViewPager(viewpager: ViewPager, cycleNum: Int): RectShapeIndicator {
        mNum = cycleNum
        viewpager.addOnPageChangeListener(object : OnPageChangeListener {
            //记录上一次滑动的positionOffsetPixels值
            private var lastValue = -1
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (!mAnimation) {
                    //不需要动画
                    return
                }
                var isLeft = mIsLeft
                if (lastValue / 10 > positionOffsetPixels / 10) {
                    //右滑
                    isLeft = false
                } else if (lastValue / 10 < positionOffsetPixels / 10) {
                    //左滑
                    isLeft = true
                }
                move(positionOffset, position % mNum)
                lastValue = positionOffsetPixels
            }

            override fun onPageSelected(position: Int) {
                if (mAnimation) {
                    //需要动画
                    return
                }
                move(0f, position % mNum)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        return this
    }

}
```

### 参考：

https://github.com/zhpanvip/viewpagerindicator
