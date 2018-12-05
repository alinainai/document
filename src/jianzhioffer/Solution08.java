package jianzhioffer;

/**
 * 题目描述
 * 一只青蛙一次可以跳上1级台阶，也可以跳上2级。
 * 求该青蛙跳上一个n级的台阶总共有多少种跳法（先后次序不同算不同的结果）。
 */
public class Solution08 {


    public int JumpFloor(int target) {


        if (target <= 0) {
            return 0;
        }
        if (target == 1) {
            return 1;
        }
        if (target == 2) {
            return 2;
        }
        int one = 1, two = 2, result = 0;

        for (int i = 3; i <= target; i++) {
            result = one + two;
            one = two;
            two = result;
        }
        return result;

    }

    public static void main(String[] args) {

        Solution08 solu=new Solution08();
        System.out.printf("%d",solu.JumpFloor(4));

    }


}
