package jianzhioffer;


import java.util.Arrays;

/**
 * 题目描述
 * 给定一个数组A[0,1,...,n-1],请构建一个数组B[0,1,...,n-1],
 * 其中B中的元素B[i]= A[0] x A[1] x ... x A[i-1] x A[i+1] x ... x A[n-1]。
 * 不能使用除法。
 */
public class Solution51 {


    public int[] multiply(int[] A) {

        if (A == null || A.length == 0)
            return null;

        int len = A.length;
        int[] result = new int[len];
        result[0] = 1;//默认初始化是1
        //计算左边的三角 也就是C[i]
        for (int i = 1; i < len; i++) {
            result[i] = result[i - 1] * A[i - 1];
        }
        //计算右边的三角 也就是D[i]
        int temp = 1;//默认初始化值
        for (int i = len - 2; i >= 0; i--) {
            temp *= A[i + 1];
            result[i] *= temp;
        }

        return result;

    }


    public static void main(String[] args) {

        int[] arr = {1, 3, 4, 1, 4, 3, 5, 1, 3, 5, 1};

        BeanUtil.print(Arrays.toString(new Solution51().multiply(arr)));

    }

}
