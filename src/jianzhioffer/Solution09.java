package jianzhioffer;

/**
 * 题目描述
 * 一只青蛙一次可以跳上1级台阶，也可以跳上2级……它也可以跳上n级。
 * 求该青蛙跳上一个n级的台阶总共有多少种跳法。
 */
public class Solution09 {

    public int JumpFloorII(int target) {

        int a=1;
        return a<<(target-1);

    }

//    public int JumpFloorII(int target) {
//
//        if (target <= 0) {
//            return 0;
//        } else if (target == 1) {
//            return 1;
//        } else {
//            return 2 * JumpFloorII(target - 1);
//        }
//
//    }

    public static void main(String[] args) {

        Solution09 solu=new Solution09();
        System.out.printf("%d",solu.JumpFloorII(23));

    }



}
