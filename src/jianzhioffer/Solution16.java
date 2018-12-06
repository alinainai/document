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
 *
 * 题目描述
 * 输入两个单调递增的链表，输出两个链表合成后的链表，当然我们需要合成后的链表满足单调不减规则。
 *
 */

public class Solution16 {
//    public ListNode Merge(ListNode list1,ListNode list2) {
//
//
//        if(list1==null){
//
//            if(list2!=null){
//                return list2;
//            }
//            return null;
//
//        }
//
//        ListNode pre=null,head=list1;
//
//        while (list1!=null){
//
//            if(list2!=null&&list1.val<=list2.val){
//                pre=list1;
//                list1=list1.next;
//            }else if(list2!=null){
//                ListNode l2next=list2.next;
//                pre.next=list2;
//                list2.next=list1;
//                pre=list2;
//                list2=l2next;
//            }else{
//                break;
//            }
//        }
//
//        if (list2!=null){
//            pre.next=list2;
//
//        }
//
//        return head;
//
//    }

    public ListNode Merge(ListNode list1,ListNode list2) {

        if (list1 == null) {
            return list2;
        }
        if (list2 == null) {
            return list1;
        }
        ListNode mergeHead = null;
        ListNode current = null;
        while (list1 != null && list2 != null) {
            if (list1.val <= list2.val) {
                if (mergeHead == null) {
                    mergeHead = current = list1;
                } else {
                    current.next = list1;
                    current = current.next;
                }
                list1 = list1.next;
            } else {
                if (mergeHead == null) {
                    mergeHead = current = list2;
                } else {
                    current.next = list2;
                    current = current.next;
                }
                list2 = list2.next;
            }
        }
        if (list1 == null) {
            current.next = list2;
        } else {
            current.next = list1;
        }
        return mergeHead;
    }



//    public ListNode Merge(ListNode list1,ListNode list2) {
//        if(list1 == null){
//            return list2;
//        }
//        if(list2 == null){
//            return list1;
//        }
//        if(list1.val <= list2.val){
//            list1.next = Merge(list1.next, list2);
//            return list1;
//        }else{
//            list2.next = Merge(list1, list2.next);
//            return list2;
//        }
//    }


    public static void main(String[] args) {

//        ListNode node1=new ListNode(3);
//        ListNode node2=new ListNode(13);
//        ListNode node3=new ListNode(21);
//        ListNode node4=new ListNode(33);
//        ListNode node5=new ListNode(19);
//        ListNode node6=new ListNode(23);
//        ListNode node7=new ListNode(28);

        ListNode node1= BeanUtil.createListNode(new int[]{3,13,21,33,19,23,28});


        ListNode node8=new ListNode(42);
        ListNode node9=new ListNode(45);
        ListNode node10=new ListNode(49);
        ListNode node11=new ListNode(52);

//        node1.next=node2;
//        node2.next=node3;
//        node3.next=node4;
//        node4.next=node5;
//        node5.next=node6;
//        node6.next=node7;
////        node7.next=node8;
        node8.next=node9;
        node9.next=node10;
        node10.next=node11;

        ListNode node=new Solution16().Merge(node1,node8);
        while (node!=null){
            System.out.println( node.val);
            node=node.next;
        }


    }
}
