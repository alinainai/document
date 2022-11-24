package jianzhioffer;

/*
public class ListNode {
    int val;
    ListNode next = null;

    ListNode(int val) {
        this.val = val;
    }
}*/

/**
 * 题目描述
 * 输入一个链表，输出该链表中倒数第k个结点。
 */
public class Solution14 {
    public ListNode FindKthToTail(ListNode head,int k) {

        if(head==null||k<=0){
            return head;
        }
        ListNode p=head,q;

        for(int i=1;i<k;i++){
            if(p.next!=null){
                p=p.next;
            }else{
                return null;
            }
        }
        q=head;
        while(p.next!=null){
            p = p.next;
            q=q.next;
        }
        return q;
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

        System.out.println( new Solution14().FindKthToTail(node1,4).val);

    }


}
