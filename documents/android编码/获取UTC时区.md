 ```kotlin   
 private fun getFormatTimezone(): String {
     //取到秒级别减少运算
     val zoneOffset = TimeZone.getDefault().rawOffset / 1000
     val hour = zoneOffset / 3600
     return StringBuilder("UTC").apply {
         if (hour >= 0) append("+")
         append(hour)
         val remain = zoneOffset % 3600
         if (remain != 0) {
             (remain.toFloat() / 3600F * 10).toInt().absoluteValue.also {
                 if (it > 0)
                     append(".").append(it)
             }
         }
     }.toString()
 }
```
