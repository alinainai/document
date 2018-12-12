package jianzhioffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Stack;

/**
 *题目描述
 *输入一个字符串,按字典序打印出该字符串中字符的所有排列。
 *例如输入字符串abc,则打印出由字符a,b,c所能排列出来的所有字符串abc,acb,bac,bca,cab和cba。
 */
public class Solution27 {





    public ArrayList<String> Permutation(String str) {

        ArrayList<String> re = new ArrayList<>();
        if (str == null || str.length() == 0) {
            return re;
        }
        HashSet<String> set = new HashSet<>();
        fun(set, str.toCharArray(), 0);
        re.addAll(set);
        Collections.sort(re);
        return re;

    }



    void fun(HashSet<String> re, char[] str, int k) {
        if (k == str.length) {
            re.add(new String(str));
            return;
        }
        for (int i = k; i < str.length; i++) {
            //依次交换数组位置
            swap(str, i, k);
            fun(re, str, k + 1);
            //复原数组
            swap(str, i, k);
        }
    }
    void swap(char[] str, int i, int j) {
        if (i != j) {
            char t = str[i];
            str[i] = str[j];
            str[j] = t;
        }
    }


    public static void main(String[] args) {

        ArrayList<String> arr=new Solution27().Permutation("abcd");
        for (String anArr : arr) System.out.println(anArr);


    }


}
