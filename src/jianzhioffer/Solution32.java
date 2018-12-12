package jianzhioffer;


import java.util.ArrayList;


/**
 * 题目描述
 * 求出1-13的整数中1出现的次数,并算出100-1300的整数中1出现的次数？
 * 为此他特别数了一下1~13中包含1的数字有1、10、11、12、13因此共出现6次,但是对于后面问题他就没辙了。
 * ACMer希望你们帮帮他,并把问题更加普遍化,可以很快的求出任意非负整数区间中1出现的次数（从1 到 n 中1出现的次数）。
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
