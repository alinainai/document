package leetcode

import java.lang.StringBuilder
import kotlin.math.max

class _0019Solution {

    fun removeNthFromEnd(head: ListNode?, n: Int): ListNode? {
        if(head==null) return head
        val trick = ListNode(-1) //加一个 trick 来处理一个节点的情况
        trick.next = head
        var fast = trick
        var slow = trick
        var k = n
        while (k>0){
            fast=fast.next!!
            k--
        }
        while (fast.next!=null){
            fast = fast.next!!
            slow = slow.next!!
        }
        slow.next=slow.next?.next
        return trick.next
    }

}