package leetcode

import kotlin.math.max

class _0026Solution {

    fun removeDuplicates(nums: IntArray): Int {
        var j = 0
        for(i in nums.indices){
            if(i==0 || i>0 && nums[i]==nums[i-1]){
                nums[j++]=nums[i]
            }
        }
        return j
    }

}