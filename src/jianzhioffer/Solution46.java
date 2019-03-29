package jianzhioffer;


import java.util.LinkedList;
import java.util.List;

/**
 * 题目描述
 * 0，1，2，……，n-1，这n个数字排成一个圆圈，从数字0开始，每次从这个圆圈里删除第m个数字。
 * 求出这个圆圈里剩下的最后一个数字。
 */
public class Solution46 {

    public int LastRemaining_Solution(int n, int m) {

        if (n < 1 || m < 1) {
            return -1;
        }
        int last = 0;
        for (int i = 2; i <= n; ++i) {
            last = (last + m) % i;
        }
        // 因为实际编号为(1~n)
        return last;

    }


    public static void main(String[] args) {

        BeanUtil.print(new Solution46().LastRemaining_Solution(98, 6));

    }

}
