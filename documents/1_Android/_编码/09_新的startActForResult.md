注意: ActivityResultLauncher 必须在 onCreate 或者 onAttach 方法下初始化

## 1、使用

```kotlin
val startForResult = registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
    if (result.resultCode == Activity.RESULT_OK) {
        val intent = result.data
        // Handle the Intent
    }
}

override fun onCreate(savedInstanceState: Bundle) {
    // ...

    val startButton = findViewById(R.id.start_button)

    startButton.setOnClickListener {
        // Use the Kotlin extension in activity-ktx
        // passing it the Intent you want to start
        startForResult.launch(Intent(this, ResultProducingActivity::class.java))
    }
}
```

```java
ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
    @Override
    public void onActivityResult(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent intent = result.getData();
            // Handle the Intent
        }
    }
});
 
@Override
public void onCreate(@Nullable savedInstanceState: Bundle) {
    // ...

    Button startButton = findViewById(R.id.start_button);

    startButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View view) {
            // The launcher with the Intent you want to start
            mStartForResult.launch(new Intent(this, ResultProducingActivity.class));
        }
    });
}
```
## 2、简答讲解
registerForActivityResult 方法内部传入一个约定 (ActivityResultContract) 和一个回调 (ActivityResultCallback)，然后返回一个 ActivityResultLauncher 来执行lanch 方法
```java
public final <I, O> ActivityResultLauncher<I> registerForActivityResult(
            @NonNull final ActivityResultContract<I, O> contract,
            @NonNull final ActivityResultCallback<O> callback)
```
