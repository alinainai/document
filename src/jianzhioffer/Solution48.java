package jianzhioffer;


/**
 * 题目描述
 * 写一个函数，求两个整数之和，要求在函数体内不得使用+、-、*、/四则运算符号。
 */
public class Solution48 {


    public int Add(int num1, int num2) {


        while (num2 != 0) {
            int temp = num1;
            num1 = num1 ^ num2;
            num2 = (temp & num2) << 1;

        }


        return num1;
    }


    public static void main(String[] args) {

        BeanUtil.print(new Solution48().Add(2, 5));

    }

}
