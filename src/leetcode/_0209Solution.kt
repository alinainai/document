package leetcode


import kotlin.math.max
//977
class _0209Solution {

    fun minSubArrayLen(target: Int, nums: IntArray): Int {
        //采用滑动窗口解决
        var i = 0 //窗口的起始位置
        var sum = 0 //记录一下窗口的和
        var result = Int.MAX_VALUE //返回结果
        for(j in nums.indices){ //窗口的结束位置
            sum+=nums[j] //
            while (sum>=target){
                result = Math.min(result,j-i+1)
                sum -=nums[i++]
            }
        }
        return  if(result== Int.MAX_VALUE) 0 else result
    }

}