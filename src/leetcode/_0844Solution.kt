package leetcode

import java.util.*
import kotlin.math.max
//977
class _0844Solution {

    fun backspaceCompare(s: String, t: String): Boolean {
        val stack1: Deque<Char> = LinkedList()
        val stack2: Deque<Char> = LinkedList()
        s.toCharArray().forEach {char ->
            if(char=='#' )
                if(stack1.isNotEmpty()) stack1.pop()
            else stack1.push(char)
        }
        t.toCharArray().forEach {char ->
            if(char=='#')
                stack2.pop()
            else stack2.push(char)
        }
       return stack1==stack2
    }

}