package leetcode

import java.lang.StringBuilder
import java.util.*
import kotlin.math.max

class _0022Solution {

    private val rst = mutableListOf<String>()
    fun generateParenthesis(n: Int): List<String> {
        dfs(n, n, "")
        return rst
    }

    //left:左括号剩余插入的次数，right:右括号剩余可插入的个数
    private fun dfs(left: Int, right: Int, str: String) {
        if (left == 0 && right == 0) {
            rst += str
            return
        }
        if (left > 0) dfs(left - 1, right, "$str(") // 如果左括号还剩余的话，可以拼接左括号
        if (right > left) dfs(left, right - 1, "$str)") // 如果右括号剩余多于左括号剩余的话，可以拼接右括号
    }

}