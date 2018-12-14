package jianzhioffer;

/**
 * 题目描述
 * 给定一个double类型的浮点数base和int类型的整数exponent。求base的exponent次方。
 */

public class Solution12 {

    /**
     * 二分幂
     * @param a
     * @param n
     * @return
     */
    long pow(int a,int n)//求a的n次幂
    {
        if (n==0)
            return 1;
        if (n==1)
            return a;
        long  ans=pow(a,n/2);//从函数的功能区理解递归
        ans*=ans;
        if (n%2==1)
            ans*=a;
        return ans;
    }

    /**
     * 快速幂
     * @param base
     * @param exponent
     * @return
     */

    public double Power(double base, int exponent) {
        double res = 1, curr = base;
        int n;
        if (exponent > 0) {
            n = exponent;
        } else if (exponent < 0) {
            if (base == 0)
                throw new RuntimeException("分母不能为0");
            n = -exponent;
        } else {// exponent==0
            return 1;// 0的0次方
        }
        while (n != 0) {//快速幂算法
            if ((n & 1) == 1){
                res *= curr;
            }
            curr *= curr;// 翻倍
            n >>= 1;// 右移一位
        }
        return exponent >= 0 ? res : (1 / res);
    }

//    double fun( double a, int b )
//    {
//        double r = 1;
//        double base = a;
//        while( b != 0 )
//        {
//            if((b&1)==1)//判断奇偶性,为1时才进行运算
//            {
//                r *= base;
//            }
//            base *= base;
//            b=b>>1;
//        }
//        return r;
//    }

    public static void main(String[] args) {

        Solution12 solu = new Solution12();
        System.out.println(String.valueOf(solu.pow(3, 4)));

    }

}
