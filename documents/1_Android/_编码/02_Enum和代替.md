Java 版本
```java
public class Constants {
    public static final int STATE_ONE = 1;
    public static final int STATE_TWO = 2;
    public static final int STATE_THREE = 3;

    // 自定义一个注解MyState
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATE_ONE, STATE_TWO, STATE_THREE})
    public @interface  MyState {}
}
```

修饰变量：
```java
@Constants.MyState
private int state;
```
修饰方法入参
```java
private void setState(@Constants.MyState int state) {
    //some code
}
```
Kotlin 版本

```kotlin
enum class ColorEnum(val typeId: Int) {
   NULL(0),
   GREEN(1),
   RED(2);
    companion object {
        fun parseFromInt(id: Int) = values().find { it.typeId == id} ?: NULL
    }
}
```

Kotlin 版本替代

```kotlin
sealed class NetworkState {

    object Loading : NetworkState()
    object Success : NetworkState()

    data class Error(
            val code: Int? = null,
            val heading: Int? = null,
            val message: Int? = null
    ) : NetworkState()

    var optionCallback: ((pos: Int) -> Unit)? = null
    var optionCallbackNoParam: (() -> Unit)? = null

}
```
