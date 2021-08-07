package jianzhioffer;
import java.util.Stack;

/**
 * 题目描述
 * 输入一棵二叉搜索树，将该二叉搜索树转换成一个排序的双向链表。要求不能创建任何新的结点，只能调整树中结点指针的指向。
 */
public class Solution26 {

    public TreeNode ConvertBSTToBiList(TreeNode pRootOfTree) {

        if(pRootOfTree == null) return pRootOfTree;

        TreeNode pre = null;
        Stack<TreeNode> stack = new Stack<>();
        while(pRootOfTree != null || !stack.isEmpty()){
            //如果不为空
            if(pRootOfTree != null) {
                //入栈
                stack.push(pRootOfTree);
                //
                pRootOfTree = pRootOfTree.right;
            } else {
                pRootOfTree = stack.pop();
                if(pre == null)
                    pre = pRootOfTree;
                else {
                    pre.left = pRootOfTree;
                    pRootOfTree.right = pre;
                    pre = pRootOfTree;
                }
                pRootOfTree = pRootOfTree.left;
            }
        }

        return pre;
    }


    public static void main(String[] args) {


    }


}
