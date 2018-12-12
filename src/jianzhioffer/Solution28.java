package jianzhioffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

/**
 * 题目描述
 * 数组中有一个数字出现的次数超过数组长度的一半，请找出这个数字。例如输入一个长度为9的数组{1,2,3,2,2,2,5,4,2}。
 * 由于数字2在数组中出现了5次，超过数组长度的一半，因此输出2。如果不存在则输出0。
 */
public class Solution28 {


    public int MoreThanHalfNum_Solution(int[] array) {


        int n = array.length;
        if (n == 0) return 0;

        int num = array[0], count = 1;
        for (int i = 1; i < n; i++) {
            if (count == 0) {
                num = array[i];
                count = 1;
            }else if (array[i] == num)
                count++;
            else
                count--;

        }
        // Verifying
        count = 0;
        for (int anArray : array) {
            if (anArray == num) count++;
        }
        if (count * 2 > n)
            return num;
        return 0;


    }

    public static void main(String[] args) {

        int arr[]={1,2,3,2,2,2,5,4,2};
        System.out.print(new Solution28().MoreThanHalfNum_Solution(arr));


    }


}
