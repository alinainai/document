package jianzhioffer;


/**
 * 题目描述
 * 输入一棵二叉树，判断该二叉树是否是平衡二叉树。
 */
public class Solution39 {


    public boolean IsBalanced_Solution(TreeNode root) {
        return getDepth(root) != -1;
    }

    private int getDepth(TreeNode root) {
        if (root == null) return 0;
        int left = getDepth(root.left);
        if (left == -1) return -1;
        int right = getDepth(root.right);
        if (right == -1) return -1;
        return Math.abs(left - right) > 1 ? -1 : 1 + Math.max(left, right);
    }



    public static void main(String[] args) {

        TreeNode tree = BeanUtil.createCompleteBinaryTree(new int[]{1, 3, 2, 5, 6, 34, 13, 54, 23, 31, 45, 23, 91, 22, 33, 44});
        BeanUtil.print(new Solution39().IsBalanced_Solution(tree));
        BeanUtil.print(Integer.MAX_VALUE);

    }

}
