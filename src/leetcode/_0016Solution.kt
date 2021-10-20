package leetcode

import java.util.*

class _0016Solution {

    fun threeSumClosest(nums: IntArray, target: Int): Int {

        Arrays.sort(nums)
        var diff = Int.MAX_VALUE
        for(i in 0..nums.size-3){
            if(i>0 && nums[i]*3>target && nums[i]*3-target >Math.abs(diff)) break
            if(i>0 && nums[i-1]==nums[i]) continue
            var l = i+1
            var r = nums.size-1
            while (l<r){
                val sum = nums[i]+nums[l]+nums[r]
                diff =if(Math.abs(diff) > Math.abs(sum-target)) sum-target else diff
                if(sum>target){
                    r--
                    while(r>l && nums[r+1] == nums[r]) r--
                }else if(sum<target){
                    l++
                    while(l<r && nums[l-1]==nums[l]) l++
                }else{
                    return target
                }
            }

        }
        return target+diff
    }


}