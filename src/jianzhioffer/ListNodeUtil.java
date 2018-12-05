package jianzhioffer;

public class ListNodeUtil {


    static  ListNode createListNode(int[] arr){

        ListNode pre=null,head=null;
        int len=0;
        while (len<arr.length){
            ListNode node=new ListNode(arr[len]);

            if(head==null){
                head=node;
                pre=head;
            }else{
                pre.next=node;
                pre=node;
            }
            len++;
        }

        return head;

    }



}
