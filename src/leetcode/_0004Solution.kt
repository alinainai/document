package leetcode

import kotlin.math.max

class _0004Solution {

    fun findMedianSortedArrays(nums1: IntArray, nums2: IntArray): Double {
        //这里用一个小技巧，求k1和k2，这样可以忽略奇数偶数的区别
        val k1 = (nums1.size+nums2.size+1)/2 //从左边数，第k1个数
        val k2 = (nums1.size+nums2.size+2)/2 //从左边数，第k2个数
        return (getKthEle(nums1,0,nums2,0,k1) + getKthEle(nums1,0,nums2,0,k2))/2.0
    }

    //获取两个排序数组之后的第k个数，注意不是 index，这个方法的整体思想是对 k 这个长度做二分。
    //i:n1数组起始位置，j:n2数组起始位置
    private fun getKthEle(a:IntArray,sta:Int,b:IntArray,stb:Int,kth:Int):Int{
        if(sta>=a.size) return b[stb+kth-1]
        if(stb>=b.size) return a[sta+kth-1]
        if(kth==1) return Math.min(a[sta],b[stb])
        val h = kth/2
        //a.size-sta a数组还剩多少个
        val vala =if(a.size-sta >= h) a[sta+h-1] else a.last()
        val counta =if(a.size-sta >=h) h else a.size-sta

        val valb =if(b.size-stb >= h) b[stb+h-1] else b.last()
        val countb =if(b.size-stb >=h) h else b.size-stb

        if(vala<=valb){
            return getKthEle(a,sta+counta,b,stb,kth-counta)
        }
        return getKthEle(a,sta,b,stb+countb,kth)
    }

}