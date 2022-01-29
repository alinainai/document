### 1. 简单介绍

Moshi 底层 IO 操作采用 Okio，序列化的时候性能优于Gson及KS以及其它框架。
在反序列化的过程中，我们看到 Moshi 的解析效率跟 Kotlin 的官方序列化工具基本持平，稍快于Gson。



|          | 解析方式    |  支持语言  |自定义解析|
| -------- | :-----:| :----: |  :----: |
| Gson     | 反射     |   Java/Kotlin |TypeAdapter|
| Moshi    | 反射/注解 |   Java/Kotlin |JsonAdapter|
| Moshi    | 编译插件  |   Kotlin  |KSerializer|


### 2. 优点

支持 反射和注解 两种方式。





















