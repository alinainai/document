package jianzhioffer;


import java.util.LinkedList;
import java.util.Queue;

/**
 * 题目描述
 * 输入一棵二叉树，求该树的深度。从根结点到叶结点依次经过的结点（含根、叶结点）形成树的一条路径，最长路径的长度为树的深度。
 */
public class Solution38 {


    public int TreeDepth(TreeNode root) {

        if (root == null)
            return 0;

        int left = TreeDepth(root.left);
        int right = TreeDepth(root.right);
        return left > right ? left + 1 : right + 1;


//        Queue<TreeNode> queue = new LinkedList<>();
//        queue.offer(root);
//        int count = 0, depth = 0, nextCount = 1;
//        while (!queue.isEmpty()) {
//            TreeNode node = queue.poll();
//            count ++;
//            if (node.left != null) {
//                queue.offer(node.left);
//            }
//            if (node.right != null) {
//                queue.offer(node.right);
//            }
//            if (count == nextCount) {
//                nextCount = queue.size();
//                count = 0;
//                depth++;
//            }
//        }
//        return depth;


    }


    public static void main(String[] args) {

        TreeNode tree = BeanUtil.createCompleteBinaryTree(new int[]{1, 3, 2, 5, 6, 34, 13, 54, 23, 31, 45, 23, 91, 22, 33, 44});
        BeanUtil.printTreeStructure(tree, tree.val, 0);
        BeanUtil.print(new Solution38().TreeDepth(tree));

    }

}
