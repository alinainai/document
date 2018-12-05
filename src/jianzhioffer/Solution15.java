package jianzhioffer;


/**
 * 题目描述
 * 输入一个链表，反转链表后，输出新链表的表头。
 */
public class Solution15 {


    public ListNode ReverseList(ListNode head) {

        if(head==null)
            return null;

        ListNode pre = null;
        ListNode next;

        while(head!=null){
            //先用next保存head的下一个节点的信息，保证单链表不会因为失去head节点的原next节点而就此断裂
            next = head.next;
            //让head从指向next变成指向pre
            head.next = pre;
            //更新pre
            pre = head;
            //把next当成新head，继续循环
            head = next;
        }
        return pre;
    }
    public static void main(String[] args) {

        ListNode node1=new ListNode(3);
        ListNode node2=new ListNode(13);
        ListNode node3=new ListNode(21);
        ListNode node4=new ListNode(33);
        ListNode node5=new ListNode(23);
        ListNode node6=new ListNode(42);
        ListNode node7=new ListNode(28);
        ListNode node8=new ListNode(19);

        node1.next=node2;
        node2.next=node3;
        node3.next=node4;
        node4.next=node5;
        node5.next=node6;
        node6.next=node7;
        node7.next=node8;

        ListNode node=new Solution15().ReverseList(node1);
        while (node!=null){
            System.out.println( node.val);
            node=node.next;
        }


    }
}


