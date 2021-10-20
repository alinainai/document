package leetcode

import java.util.*
import kotlin.math.abs
import kotlin.math.max
//904
class _0904Solution {

    fun totalFruit(fruits: IntArray): Int {
        if(fruits.isEmpty()) return 0
        var rst = 0
        var left = 0
        val set = mutableMapOf<Int,Int>()
        fruits.indices.forEach { right->
            set[fruits[right]] = set.getOrDefault(fruits[right],0)+1
            while(set.size>2){
                set[fruits[left]]=set[fruits[left]]!!-1
                if(set[fruits[left]]==0) set.remove(fruits[left])
                left++
            }
            val newRst = right-left+1
            rst =if(rst>newRst) rst else newRst
        }
        return rst
    }

}