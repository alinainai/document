package jianzhioffer;


import java.util.ArrayList;
import java.util.Stack;

/**
 * 题目描述
 * 请实现两个函数，分别用来序列化和反序列化二叉树
 */
public class Solution61 {


    String Serialize(TreeNode root) {

        if(root==null)
            return "#,";
        String a=root.val+",";

            a=a+Serialize(root.left);

            a=a+Serialize(root.right);
        return a;

    }
    TreeNode Deserialize(String str) {

        TreeNode treeNode=null;

        if(str==null||str.length()==0)
        return null;


        String[] strs=str.split(",");


        if(strs[0].equals("#")){
            return null;
        }else{
            treeNode=new TreeNode(Integer.valueOf(strs[0]));
        }

        Stack<TreeNode> stack=new Stack<>();
        stack.push(treeNode);

//        for (int i=1;i<strs.length;i++){
//
//            treeNode=
//
//
//        }
        return null;

    }


    public static void main(String[] args) {

        int[] a = {1, 5, 7, 11, 43, 12, 45, 32, 23, 9, 21};
        TreeNode tree = BeanUtil.createCompleteBinaryTree(a);
        BeanUtil.printTreeStructure(tree, tree.val, 0);


        String str=new Solution61().Serialize(tree);

        BeanUtil.print(str);

        String[] strs=str.split(",");

        for (String s:strs) {
            BeanUtil.print(s);
        }

//        TreeNode tree1=new Solution61().Deserialize(str);
//
//        BeanUtil.printTreeStructure(tree1, tree1.val, 0);

    }

}
