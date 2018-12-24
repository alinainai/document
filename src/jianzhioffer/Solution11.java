package jianzhioffer;

/**
 * 题目描述
 * 输入一个整数，输出该数二进制表示中1的个数。其中负数用补码表示。
 */
public class Solution11 {

//    public int NumberOf1(int n) {
//
//        int count = 0;
//        int inValid = 1;
//        while (inValid != 0) {
//            if ((n & inValid) != 0) {
//                count++;
//            }
//            inValid = inValid << 1;
//        }
//        return count;
//
//    }

    public int NumberOf1(int n) {
        int count = 0;
        while(n!= 0){
            count++;
            n = n & (n - 1);
        }
        return count;
    }

    public static void main(String[] args) {

        Solution11 solu=new Solution11();
        System.out.printf("%d",solu.NumberOf1(-32));

    }


}
