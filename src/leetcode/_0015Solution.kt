package leetcode

import java.util.*
import kotlin.math.max


class _0015Solution {
    fun threeSum(nums: IntArray): List<List<Int>> {
        val lists = mutableListOf<List<Int>>()
        //先排序，让负数在前并且相同的值会排到一起
        Arrays.sort(nums)
        //处理下边界问题，最后便利到第三个即可
        for (i in 0.. nums.size-3) {
            //如果当前遍历的值 >0，后面相加不会出现等于0的情况，直接返回 result 即可
            if (nums[i] > 0) return lists
            //如果当前数和前一个数字相同直接 continue，因为最后的结果要去重，这里做一下拦截。
            if (i > 0 && nums[i] == nums[i - 1]) continue
            var left = i + 1;
            var right = nums.size-1
            while (left < right) {
                when {
                    nums[i] + nums[left] + nums[right] > 0 -> {
                        right--
                        while (right > left && nums[right] == nums[right + 1]) right--
                    }
                    nums[i] + nums[left] + nums[right] < 0 -> {
                        left++
                        while (right > left && nums[left] == nums[left - 1]) left++
                    }
                    else -> {
                        lists.add(listOf(nums[i] , nums[left] ,nums[right]))
                        while (right > left && nums[right] == nums[right - 1]) right--
                        while (right > left && nums[left] == nums[left + 1]) left++
                        right--
                        left++
                    }
                }
            }
        }
        return lists
    }
}