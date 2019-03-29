package jianzhioffer;


/**
 * 题目描述
 * 请实现一个函数用来匹配包括'.'和'*'的正则表达式。模式中的字符'.'表示任意一个字符，
 * 而'*'表示它前面的字符可以出现任意次（包含0次）。 在本题中，匹配是指字符串的所有字符匹配整个模式。
 * 例如，字符串"aaa"与模式"a.a"和"ab*ac*a"匹配，但是与"aa.a"和"ab*a"均不匹配
 */
public class Solution52 {


    public boolean match(char[] str, char[] pattern) {

        if (null == str || null == pattern)
            return false;
        return matchCore(str, 0, pattern, 0);

    }

    private boolean matchCore(char[] str, int indexOfStr, char[] pattern, int indexOfPattern) {

        if (indexOfPattern == pattern.length)//pattern遍历完了
            return indexOfStr == str.length;//如果str也完了，返回true，不然false

        //注意数组越界问题
        if (indexOfPattern < pattern.length - 1 && pattern[indexOfPattern + 1] == '*') {//下一个是*
            if (str.length != indexOfStr && (str[indexOfStr] == pattern[indexOfPattern] || pattern[indexOfPattern] == '.')) //匹配
                return matchCore(str, indexOfStr, pattern, indexOfPattern + 2)
                        || matchCore(str, indexOfStr + 1, pattern, indexOfPattern)
                        || matchCore(str, indexOfStr + 1, pattern, indexOfPattern+2);
            else//当前不匹配
                return matchCore(str, indexOfStr, pattern, indexOfPattern + 2);
        }
        //下一个不是“*”，当前匹配
        if (str.length != indexOfStr && (str[indexOfStr] == pattern[indexOfPattern] || pattern[indexOfPattern] == '.'))
            return matchCore(str, indexOfStr + 1, pattern, indexOfPattern + 1);

        return false;

    }


    public static void main(String[] args) {

        BeanUtil.print(new Solution52().match("aaa".toCharArray(), "ab*ac*a".toCharArray()));

    }

}
