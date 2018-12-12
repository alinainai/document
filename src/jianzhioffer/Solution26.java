package jianzhioffer;
import java.util.Stack;

/**
 * 题目描述
 * 输入一个复杂链表（每个节点中有节点值，以及两个指针，一个指向下一个节点，
 * 另一个特殊指针指向任意一个节点），返回结果为复制后复杂链表的head。
 * （注意，输出结果中请不要返回参数中的节点引用，否则判题程序会直接返回空）
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
