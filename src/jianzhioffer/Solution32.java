package jianzhioffer;


import java.util.ArrayList;


/**
 * 题目描述
 * 输入一个正整数数组，把数组里所有数字拼接起来排成一个数，打印能拼接出的所有数字中最小的一个。例如输入数组{3，32，321}，则打印出这三个数字能排成的最小数字为321323。
 */
public class Solution32 {

    public String PrintMinNumber(int[] numbers) {


        if (null == numbers || numbers.length == 0)
            return "";

        StringBuilder s = new StringBuilder();
        ArrayList<Integer> list = new ArrayList<>();

        for (int number : numbers) {
            list.add(number);

        }
        list.sort((str1, str2) -> {
            String s1 = String.valueOf(str1) + String.valueOf(str2);
            String s2 = String.valueOf(str2) + String.valueOf(str1);
            return s1.compareTo(s2);
        });

        for (int j : list) {
            s.append(j);
        }
        return s.toString();

    }


    public static void main(String[] args) {
        int arr[] = {0, 0, 14, 61, 2, 13, 32, 99, 81};
        BeanUtil.print(new Solution32().PrintMinNumber(arr));

    }

}
