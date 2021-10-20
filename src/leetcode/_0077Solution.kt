package leetcode

import kotlin.math.max

class _0077Solution {

    fun combine(n: Int, k: Int): List<List<Int>> {
        val rst =  ArrayList<List<Int>>()
        val path = mutableListOf<Int>()
        backTracking(n,k,rst,path,1)
        return rst
    }

    private fun backTracking(n:Int,k:Int,rst:ArrayList<List<Int>>,path:MutableList<Int>,startIdx:Int){
        if(path.size==k){
            rst.add(path.toList())
            return
        }
        for(i in 1..(n-(k-path.size)+1)){
            path.add(i)
            backTracking(n,k,rst,path,i+1)
            path.remove(i)
        }
    }

}