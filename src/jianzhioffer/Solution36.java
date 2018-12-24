package jianzhioffer;


/**
 * 题目描述
 * 输入两个链表，找出它们的第一个公共结点。
 */
public class Solution36 {




//    public ListNode FindFirstCommonNode(ListNode pHead1, ListNode pHead2) {
//
//
//        ListNode p1 = pHead1;
//        ListNode p2 = pHead2;
//        while(p1!=p2){
//            p1 = (p1==null ? pHead2 : p1.next);
//            p2 = (p2==null ? pHead1 : p2.next);
//        }
//        return p1;
//
//    }

    public ListNode FindFirstCommonNode(ListNode pHead1, ListNode pHead2) {
        if(pHead1==null||pHead2==null)
            return null;
        int len1=getlen(pHead1);
        int len2=getlen(pHead2);
        if(len1>len2){
            int len=len1-len2;
            while(len-->0)
                pHead1=pHead1.next;

        }else{
            int len=len2-len1;
            while(len-->0)
                pHead2=pHead2.next;
        }

        while(pHead1!=null&&pHead2!=null){
            if(pHead1.val==pHead2.val)
                return pHead1;
            else{
                pHead1=pHead1.next;
                pHead2=pHead2.next;
            }
        }
        return null;

    }
    public int getlen(ListNode head){
        int count=0;
        while(head!=null){
            count++;
            head=head.next;
        }
        return count;
    }


    @SuppressWarnings("Duplicates")
    public static void main(String[] args) {

        ListNode node1=new ListNode(1);
        ListNode node2=new ListNode(2);
        ListNode node3=new ListNode(3);
        ListNode node4=new ListNode(4);
        ListNode node5=new ListNode(5);
        ListNode node6=new ListNode(6);
        ListNode node7=new ListNode(7);

        node1.next=node2;
        node2.next=node3;
        node3.next=node6;

        node4.next=node5;
        node5.next=node6;

        node6.next=node7;


        ListNode node=new Solution36().FindFirstCommonNode(node1,node4);
        if(node==null){
            BeanUtil.print("null");
        }else{
            BeanUtil.print(node.val);
        }




    }

}
