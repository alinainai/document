package leetcode

import kotlin.math.max

class _0034Solution {

    //https://leetcode-cn.com/problems/find-first-and-last-position-of-element-in-sorted-array/
    fun searchRange(nums: IntArray, target: Int): IntArray {
        val rang = intArrayOf(-1,-1)
        var left = 0
        var right = nums.size-1
        var lidx = -1
        var ridx = -1
        while (left<=right){
            val mid = left + (right  - left)/2
            if(nums[mid]>target){
                right = mid -1
            }else if(nums[mid]< target) {
                left = mid+1
            }else{
                if(mid>0 && nums[mid-1]==nums[mid]){
                    right = mid-1
                   if(ridx==-1) ridx = mid //记录一下第一次找到找到的index，后续找右边界的时候做减枝操作
                }else{
                    //找到最左边的
                    lidx = mid
                    break
                }
            }
        }
        //如果没有找到左边界，直接返回
        if(lidx==-1) return rang
        left = ridx.coerceAtLeast(lidx) //第一次找到的 target 的位置 和 lidx 的最大值
        right = nums.size-1
        while (left<=right){
            val mid = left + (right  - left)/2
            if(nums[mid]>target){
                right = mid -1
            }else if(nums[mid]< target) {
                left = mid+1
            }else{
                if(mid<nums.size-1 && nums[mid]==nums[mid+1]){
                    left = mid+1
                }else{
                    ridx = mid
                    break
                }
            }
        }
        rang[0] = lidx
        rang[1] =ridx
        return rang
    }

//    fun searchRange(nums: IntArray, target: Int): IntArray {
//        val rang = IntArray(2)
//        var left = 0
//        var right = nums.size-1
//        var index  = -1
//        while (left<=right){
//            val mid = left + (right  - left)/2
//            if(nums[mid]>target){
//                right = mid -1
//            }else if(nums[mid]<target){
//                left = mid +1
//            }else{
//                index = mid
//                break
//            }
//        }
//        println("index=$index")
//        if(index==-1){
//            rang[0] = -1
//            rang[1] = -1
//        }else{
//            var i = index
//            var j = index
//            while (i>=1){
//                if(nums[i]==nums[i-1]){
//                    i--
//                    println("i=$i")
//                }
//
//                else{
//                    println("i break i=$i")
//                    break
//                }
//            }
//            while (j<=nums.size-2){
//                if(nums[j]==nums[j+1]){
//                    j++
//                    println("j=$j")
//                }
//                else {
//                    println("j break j= $j")
//                    break
//                }
//            }
//            rang[0] = i
//            rang[1] = j
//        }
//        return rang
//    }

}