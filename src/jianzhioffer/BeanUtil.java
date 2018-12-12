package jianzhioffer;

import java.util.ArrayList;
import java.util.List;

public class BeanUtil {


    /**
     * 按数组顺序生成链表
     * @param arr
     * @return
     */
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

    /**
     * 按数组顺序生成完全二叉树
     * @param arr
     * @return
     */
    static  TreeNode createCompleteBinaryTree(int[] arr){

        if(arr.length==1)
            return new TreeNode(arr[0]);
        List<TreeNode> nodeList = new ArrayList<>();
        for(int i=0;i<arr.length;i++)
            nodeList.add(new TreeNode(arr[i]));

        for(int i=1;i<=arr.length/2;i++) {
            if(2*i-1<=arr.length-1)
                nodeList.get(i-1).left = nodeList.get(2*i-1);
            if(2*i<=arr.length-1)
                nodeList.get(i-1).right = nodeList.get(2*i);
        }
        return nodeList.get(0);

    }

    /**
     * 按数组顺序升成二叉排序树
     * @param arr
     * @return
     */
    static  TreeNode createBinarySearchTree(int[] arr){


        return null;

    }

    /**
     * 打印树结构
     * @param tree
     * @param key
     * @param direction
     */
    static void printTreeStructure(TreeNode tree, int key, int direction) {
        if(tree != null) {
            if(direction==0)    // tree是根节点
                System.out.printf("%2d is root\n", tree.val, key);
            else                // tree是分支节点
                System.out.printf("%2d is %2d's %6s child\n", tree.val, key, direction==1?"right" : "left");

            printTreeStructure(tree.left, tree.val, -1);
            printTreeStructure(tree.right,tree.val,  1);
        }
    }

    /**
     * 打印arrlist
     * @param arr
     */
    static void print(ArrayList<Integer> arr) {

        if(arr!=null&&arr.size()>0){
            for (Integer anArr : arr) System.out.printf("%d ", anArr);
        }

    }

    /**
     * 打印int
     * @param a
     */
    static void print(int a) {

        System.out.println("数字="+a);

    }

    /**
     * 打印String
     * @param a
     */
    static void print(String a) {

        System.out.println(a);

    }

    /**
     * 打印long int
     * @param a
     */
    static void print(long a) {

        System.out.println(a);

    }



}
