package leetcode

import kotlin.math.max

class _0076Solution {

    fun minWindow(s: String, t: String): String {

        var rst = ""
        val t_map = hashMapOf<Char,Int>() //
        val s_map = hashMapOf<Char,Int>()
        t.toCharArray().forEach { c->
            t_map[c]=1
        }
        val chars = s.toCharArray()
        var t_size = 0 //标记值
        var l =0 //滑动窗口左边界
        chars.indices.forEach {r -> //滑动窗口右边界
            s_map[chars[r]]=s_map.getOrDefault(chars[r],0)+1
            if(s_map[chars[r]]!! <= t_map.getOrDefault(chars[r],0)) t_size++
            while(t_size==t_map.size && l<r ){
                if(t_map[chars[l]]!=null && s_map[chars[l]]==t_map[chars[l]]){
                    t_size--
                }
                s_map[chars[l]] = s_map[chars[l]]!!-1
                if(r-l+1<rst.length) rst = s.substring(l,r+1)
                l++
            }
        }
        return rst
    }

}