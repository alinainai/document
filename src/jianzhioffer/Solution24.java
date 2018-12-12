package jianzhioffer;


import java.util.ArrayList;

/**
 * 题目描述
 * 输入一颗二叉树的根节点和一个整数，打印出二叉树中结点值的和为输入整数的所有路径。
 * 路径定义为从树的根结点开始往下一直到叶结点所经过的结点形成一条路径。
 * (注意: 在返回值的list中，数组长度大的数组靠前)
 */
public class Solution24 {


    public ArrayList<ArrayList<Integer>> FindPath(TreeNode root, int target) {

        ArrayList<ArrayList<Integer>> ret = new ArrayList<>();
        ArrayList<Integer> trace = new ArrayList<>();
        if (root == null)
            return ret;

        pa(root, target, ret, trace);
        return ret;
    }

    public void pa(TreeNode root, int target, ArrayList<ArrayList<Integer>> ret, ArrayList<Integer> trace) {


        trace.add(root.val);
        //若该结点是叶子结点则比较当前路径和是否等于期待和。
        if (root.left == null && root.right == null) {
            if (target == root.val)
                ret.add(new ArrayList<>(trace));
        }
        if (root.left != null)
            pa(root.left, target - root.val, ret, trace);
        if (root.right != null)
            pa(root.right, target - root.val, ret, trace);

        //弹出结点，每一轮递归返回到父结点时，当前路径也应该回退一个结点
        trace.remove(trace.size() - 1);

    }


    public static void main(String[] args) {



    }


}
