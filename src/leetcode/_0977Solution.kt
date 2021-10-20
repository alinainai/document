package leetcode

import java.util.*
import kotlin.math.abs
import kotlin.math.max
//977
class _0977Solution {

    fun sortedSquares(nums: IntArray): IntArray {
        val result = IntArray(nums.size)
        var i =0
        var j = nums.size-1
        var index = nums.size-1
        while (i<=j){
            if(abs(nums[i]) >= abs(nums[j])){
                result[index--]=nums[i]*nums[i]
                i++
            }else{
                result[index--]=nums[j]*nums[j]
                j--
            }
        }
        return result
    }

}