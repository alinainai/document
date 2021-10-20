package leetcode

import java.lang.StringBuilder
import java.util.*
import kotlin.math.max

class _0021Solution {

    fun mergeTwoLists(l1: ListNode?, l2: ListNode?): ListNode? {
        val root = ListNode(Int.MIN_VALUE)
        var next = root
        var node1 = l1
        var node2 = l2
        while (node1 != null && node2 != null) {
            if(node1.`val`<node2.`val`){
                next.next=node1
                next=node1
                node1=node1.next
            }else{
                next.next=node2
                next=node2
                node2=node2.next
            }
        }
        if (node1 == null) {
            next.next=node2
        }
        if (node2 == null) {
            next.next=node1
        }
        return root.next
    }

}