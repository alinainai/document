package jianzhioffer;


import java.util.ArrayList;

/**
 * 题目描述
 * 输入一个递增排序的数组和一个数字S，在数组中查找两个数，使得他们的和正好是S，
 * 如果有多对数字的和等于S，输出两个数的乘积最小的。
 * <p>
 * 对应每个测试案例，输出两个数，小的先输出
 *
 *
 * 链接：https://www.nowcoder.com/questionTerminal/390da4f7a00f44bea7c2f3d19491311b
 * 来源：牛客网
 *
 * *
 *  */



public class Solution42 {


    public ArrayList<Integer> FindNumbersWithSum(int[] array, int sum) {


        ArrayList<Integer> res = new ArrayList<>();
        if (null == array || array.length < 2)
            return res;

        int low = 0, high = array.length - 1;

        while (low < high) {

            if (array[low] + array[high] > sum) {
                high--;
            } else if (array[low] + array[high] < sum) {
                low++;
            } else {
                res.add(array[low]);
                res.add(array[high]);
                break;
            }
        }

        return res;

    }


    public static void main(String[] args) {

        int[] arr = new int[]{1, 2, 4, 7, 11, 15};
        BeanUtil.print(new Solution42().FindNumbersWithSum(arr, 6));

    }

}
