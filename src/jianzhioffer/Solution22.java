package jianzhioffer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;


/**
 * 题目描述
 * 从上往下打印出二叉树的每个节点，同层节点从左至右打印。
 */
/**
 public class TreeNode {
 int val = 0;
 TreeNode left = null;
 TreeNode right = null;

 public TreeNode(int val) {
 this.val = val;

 }

 }
 */
public class Solution22 {
    public ArrayList<Integer> PrintFromTopToBottom(TreeNode root) {


        ArrayList<Integer> arr=new ArrayList<>();
        if(root==null)
            return arr;

        Queue<TreeNode> queue = new LinkedList<>();
        TreeNode currentNode ;
        queue.offer(root);
        while (!queue.isEmpty()) {
            currentNode = queue.poll();
            if (currentNode.left != null)
                queue.offer(currentNode.left);
            if (currentNode.right != null)
                queue.offer(currentNode.right);
            arr.add(currentNode.val);
        }

        return arr;


    }


    public static void main(String[] args) {

        int[] a={1,5,7,11,43,12,45,32,23,9,21};
        TreeNode tree= BeanUtil.createCompleteBinaryTree(a);
        BeanUtil.print(tree,tree.val,0);
        BeanUtil.printArr(new Solution22().PrintFromTopToBottom(tree));

    }


}
