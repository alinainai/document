package leetcode

import java.lang.StringBuilder
import kotlin.math.max

class _0017Solution {

    fun letterCombinations(digits: String): List<String> {
        if(digits.isBlank()) return emptyList()
        val maps = mapOf(2 to "abc",3 to "def",4 to "ghi",5 to "gkl",6 to "mno",7 to "pqrs",8 to "tuv",9 to "wxyz")
        val rst = mutableListOf<String>()
        val str = CharArray(digits.length)
        backTracking(digits,maps,rst,str,0)
        return rst

    }

    private fun backTracking(digits: String,maps:Map<Int,String>,rst:MutableList<String>,str:CharArray,idx:Int){
        if(idx>digits.length-1){
            rst.add(String(str) )
            return
        }
        maps[digits[idx].toString().toInt()]!!.toCharArray().forEach { c->
            str[idx]=c
            backTracking(digits,maps,rst,str,idx+1)
        }


    }

}