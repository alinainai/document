package jianzhioffer;


import java.util.Stack;

/**
 * 题目描述
 * 给定一棵二叉搜索树，请找出其中的第k小的结点。例如， （5，3，7，2，4，6，8）    中，按结点数值大小顺序第三小结点的值为4。
 */
public class Solution62 {

    TreeNode KthNode(TreeNode pRoot, int k) {

        if (pRoot == null || k <= 0)
            return null;
        int i = 1;
        Stack<TreeNode> stack = new Stack<>();
        TreeNode p = pRoot;
        while (p != null || !stack.isEmpty()) {
            while (p != null) {
                stack.push(p);
                p = p.left;
            }
            p = stack.pop();
            if (i++ == k)
                return p;
            p = p.right;
        }
        return null;
    }

}
