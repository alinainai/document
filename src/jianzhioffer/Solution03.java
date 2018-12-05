package jianzhioffer;

import java.util.Stack;
import java.util.ArrayList;

/**
 * 题目描述
 * 输入一个链表，按链表值从尾到头的顺序返回一个ArrayList。
 */
public class Solution03 {
    public ArrayList<Integer> printListFromTailToHead(ListNode listNode) {
        Stack<Integer> stack = new Stack<>();
        while (listNode != null) {
            stack.push(listNode.val);
            listNode = listNode.next;
        }

        ArrayList<Integer> list = new ArrayList<>();
        while (!stack.isEmpty()) {
            list.add(stack.pop());
        }
        return list;
    }




    public static void main(String[] args) {

        ListNode node1=new ListNode(3);
        ListNode node2=new ListNode(13);
        ListNode node3=new ListNode(21);

        node1.next=node2;
        node2.next=node3;

        System.out.print( new Solution03().printListFromTailToHead(node1).toString());

    }


}