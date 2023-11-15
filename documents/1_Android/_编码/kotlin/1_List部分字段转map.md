```kotlin
info.add(A("", ""))
info.add(A("", ""))
val map = info.associate { w ->
    (w.a ?: "") to (w.b ?: "")
}
map.forEach { (k, v) ->
    println( "k=$k ,v = $v")
}
```
