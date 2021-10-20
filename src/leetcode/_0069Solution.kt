package leetcode

import kotlin.math.max

class _0069Solution {

    fun mySqrt(x: Int): Int {
        if(x<2) return x
        var l = 0
        var r = x
        while(l<=r){
           val mid = l+ (r-l)/2
            when {
                x/mid>mid -> {
                    l=mid+1
                }
                x/mid<mid -> {
                    r=mid-1
                }
                else -> {
                    return mid
                }
            }
        }
        return r
    }

}