package jianzhioffer;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 题目描述
 * 从上到下按层打印二叉树，同一层结点从左至右输出。每一层输出一行
 */
public class Solution60 {


    ArrayList<ArrayList<Integer>> Print(TreeNode pRoot) {

        ArrayList<ArrayList<Integer>> result = new ArrayList<>();

        if (pRoot==null)
            return result;

        Queue<TreeNode> queue = new LinkedList<>();

        queue.add(pRoot);

        while (!queue.isEmpty()) {

            int start = 0, end = queue.size();

            ArrayList<Integer> layer = new ArrayList<>();

            while (start++ < end) {
                TreeNode cur = queue.poll();
                layer.add(cur.val);
                if (cur.left != null) {
                    queue.add(cur.left);
                }
                if (cur.right != null) {
                    queue.add(cur.right);
                }
            }

            result.add(layer);


        }
        return result;

    }

    public static void main(String[] args) {

        int[] a = {1, 5, 7, 11, 43, 12, 45, 32, 23, 9, 21};
        TreeNode tree = BeanUtil.createCompleteBinaryTree(a);
        BeanUtil.printTreeStructure(tree, tree.val, 0);


        ArrayList<ArrayList<Integer>> res = new Solution60().Print(null);

        for (ArrayList<Integer> arr:res){

            BeanUtil.print(arr);
            BeanUtil.printReturn();


        }

    }


}
