package jianzhioffer;


/**
 * 题目描述
 * 一个整型数组里除了两个数字之外，其他的数字都出现了偶数次。请写程序找出这两个只出现一次的数字。
 */
public class Solution40 {


    public void FindNumsAppearOnce(int[] array, int[] num1, int[] num2) {

        if (array.length < 2) return;

        int myxor = 0;

        for (int i : array)
            myxor ^= i;
        //提取出整数n最后一位为1的数
        myxor = myxor & (~myxor + 1);

        for (int i : array) {

            if ((myxor & i) == 0)
                num2[0] ^= i;
            else
                num1[0] ^= i;
        }

    }


    public static void main(String[] args) {

        int[] num1 = {0}, num2 = {0};
        int[] array = {2, 4, 3, 6, 3, 2, 5, 5};
        new Solution40().FindNumsAppearOnce(array, num1, num2);

        BeanUtil.print(num1[0]);
        BeanUtil.print(num2[0]);
    }

}
