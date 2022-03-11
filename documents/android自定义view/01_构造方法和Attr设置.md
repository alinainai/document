### 1.初始化普通 `View`
```kotlin
class CloudRecordCircleProgress @JvmOverloads constructor(
    context: Context, 
    attrs: AttributeSet? = null, 
    defStyleAttr: Int = 0): View(context, attrs, defStyleAttr) {
}
```

### 2.拓展系统 `View`

拓展系统 `View`时，要注意 `defStyleAttr : Int = R.attr.editTextStyle` 的默认值

```kotlin
public AppCompatEditText(Context context, AttributeSet attrs) {
    this(context, attrs, R.attr.editTextStyle);
}

// 注意 defStyleAttr:Int = R.attr.editTextStyle 默认值
class CustomCompatEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null, 
    defStyleAttr: Int = R.attr.editTextStyle): AppCompatEditText(context, attrs, defStyleAttr) {
}
```
### 3.处理 attrs 

```html
<declare-styleable name="CloudRecordCircleProgress">
    <attr name="sweepAngle" format="integer" />
    ...
</declare-styleable>
```

```kotlin
private fun initAttrs(attrs: AttributeSet) {
    val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CloudRecordCircleProgress)
    //...
    typedArray.recycle()
}
//在init中调用
init{
    attrs?.let {
        initAttrs(it)
    }
}
```
