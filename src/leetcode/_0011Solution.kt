package leetcode

import kotlin.math.max

class _0011Solution {

    fun maxArea(height: IntArray): Int {
        var rst = 0
        var i = 0
        var j = height.size - 1
        while (i < j) {
            if (height[i] > height[j]) {
                rst = if (rst > (j - i) * height[j]) rst else (j - i) * height[j]
                j--
            } else {
                rst = if (rst > (j - i) * height[i]) rst else (j - i) * height[i]
                i++
            }
        }
        return rst
    }

}