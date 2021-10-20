package leetcode

class _0035Solution {

    //https://leetcode-cn.com/problems/search-insert-position/
    fun searchInsert(nums: IntArray, target: Int): Int {
        if(nums.isEmpty()) return -1
        var left = 0
        var right = nums.size-1
        if(nums[left]>target) return 0
        if(nums[right]<target) return nums.size
        while (left<=right){
            val mid = left+(right-left)/2
            if(nums[mid]>target){
                right  = mid-1
            }else if(nums[mid]<target){
                left = mid+1
            }else{
                return mid
            }
        }
        return left
    }

}