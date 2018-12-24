package jianzhioffer;


import java.util.ArrayList;

import java.util.Stack;

/**
 * 题目描述
 * 请实现一个函数按照之字形打印二叉树，即第一行按照从左到右的顺序打印，
 * 第二层按照从右至左的顺序打印，第三行按照从左到右的顺序打印，其他行以此类推。
 */
public class Solution59 {


    public ArrayList<ArrayList<Integer>> Print(TreeNode pRoot) {

        ArrayList<ArrayList<Integer>> result = new ArrayList<>();
        if (pRoot == null)
            return result;
        Stack<TreeNode> s1 = new Stack<>();//s1存奇数层节点
        s1.push(pRoot);
        Stack<TreeNode> s2 = new Stack<>();//s2存偶数层节点
        boolean oddLayer = true;//是否是奇数层
        while (!s1.empty() || !s2.empty()) {
            ArrayList<Integer> temp = new ArrayList<>();
            if (oddLayer) {
                while (!s1.empty()) {
                    TreeNode node = s1.pop();
                    temp.add(node.val);
                    if (node.left != null) {
                        s2.push(node.left);
                    }
                    if (node.right != null) {
                        s2.push(node.right);
                    }
                }

            } else {
                while (!s2.empty()) {
                    TreeNode node = s2.pop();
                    temp.add(node.val);
                    if (node.right != null) {
                        s1.push(node.right);
                    }
                    if (node.left != null) {
                        s1.push(node.left);
                    }
                }
            }
            result.add(temp);
            oddLayer = !oddLayer;
        }
        return result;

    }

    public static void main(String[] args) {

        int[] a = {1, 5, 7, 11, 43, 12, 45, 3, 23, 9, 21};
        TreeNode tree = BeanUtil.createCompleteBinaryTree(a);
        BeanUtil.printTreeStructure(tree, tree.val, 0);


        ArrayList<ArrayList<Integer>> res = new Solution59().Print(tree);

        for (ArrayList<Integer> arr : res) {

            BeanUtil.print(arr);
            BeanUtil.printReturn();


        }

    }


}
