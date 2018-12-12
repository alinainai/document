package jianzhioffer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;


/**
 * 题目描述
 * 输入一个整数数组，判断该数组是不是某二叉搜索树的后序遍历的结果。如果是则输出Yes,否则输出No。假设输入的数组的任意两个数字都互不相同。
 */
public class Solution23 {


    public boolean VerifySquenceOfBST(int[] sequence) {
        if (sequence.length == 0) return false;
        return judge(sequence, 0, sequence.length - 1);
    }

    public boolean judge(int[] sequence, int left, int right) {


        if (left >= right)
            return true;

        int i = left;

        for (; i < right; i++) {
            if (sequence[i] > sequence[right]) break;
        }

        for (int j = i; j < right; j++) {
            if (sequence[j] < sequence[right]) return false;
        }
        return judge(sequence, left, i - 1) && judge(sequence, i, right - 1);
    }


    public static void main(String[] args) {

        int[] arr = {3, 5, 4, 8, 12, 13, 7};
        System.out.print(new Solution23().VerifySquenceOfBST(arr));


    }


}
