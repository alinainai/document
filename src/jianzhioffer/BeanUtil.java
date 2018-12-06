package jianzhioffer;

public class BeanUtil {


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

    static  TreeNode createTree(int[] arr){

        if(arr.length<=0)
            return null;

        TreeNode root=null,node=null;

        if(arr.length==1){
            root=new TreeNode(arr[0]);
            return root;
        }



        int len=0;
        while (len<=(arr.length-1)/2){

            if(root==null){
                root=new TreeNode(arr[len]);
                if(2*len+1<arr.length){
                    root.left=new TreeNode(arr[2*len+1]);
                }
                if(2*len+2<arr.length){
                    root.right=new TreeNode(arr[2*len+1]);
                }

            }else{

            }


            len++;
        }

        return root;
    }



}
