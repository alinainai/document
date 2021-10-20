package leetcode

import java.util.*
import kotlin.math.max

/*
编写一个函数来查找字符串数组中的最长公共前缀。

如果不存在公共前缀，返回空字符串""。

示例 1：

输入：strs = ["flower","flow","flight"]
输出："fl"
示例 2：

输入：strs = ["dog","racecar","car"]
输出：""
解释：输入不存在公共前缀。


提示：

1 <= strs.length <= 200
0 <= strs[i].length <= 200
strs[i] 仅由小写英文字母组成
 */

class _0014Solution {

    fun longestCommonPrefix(strs: Array<String>): String {
        if(strs.isEmpty()) return ""
        Arrays.sort(strs)
        val first = strs.first()
        val end = strs.last()
        var index = 0
        for(i in first.indices){
            if(first[i]==end[i]){
                index++
            }else{
                break
            }
        }
        return first.substring(0,index)

    }


}