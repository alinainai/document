package leetcode

import java.lang.StringBuilder
import java.util.*
import kotlin.math.max

class _0020Solution {

    fun isValid(s: String): Boolean {

        val stack: Deque<Char> = LinkedList()
        s.toCharArray().forEach { c->
            when(c){
               '{','[','(' -> stack.push(c)
                '}'-> if(stack.peek()=='{') stack.pop() else return false
                ']'-> if(stack.peek()=='[') stack.pop() else return false
                ')'-> if(stack.peek()=='(') stack.pop() else return false
            }
        }
        return stack.isEmpty()

    }

}