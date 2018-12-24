package jianzhioffer;


/**
 * 题目描述
 * 将一个字符串转换成一个整数(实现Integer.valueOf(string)的功能，
 * 但是string不符合数字要求时返回0)，要求不能使用字符串转换整数的库函数。
 * 数值为0或者字符串不是一个合法的数值则返回0。
 * 输入描述:
 * 输入一个字符串,包括数字字母符号,可以为空
 * 输出描述:
 * 如果是合法的数值表达则返回该数字，否则返回0
 */
public class Solution49 {

    enum Status {
        kValid, kInvalid
    }

    public Status status;

    public int StrToInt(String str) {
        status = Status.kInvalid;
        long num = 0;
        if (str != null && str.length() > 0) {

            int minus = 1;//符号
            int start = 0;
            char[] chars = str.toCharArray();
            if (chars[0] == '+') {
                start = 1;
            } else if (chars[0] == '-') {
                start = 1;
                minus = -1;
            }
            for (int i = start; i < chars.length; i++) {
                if (chars[i] > '0' && chars[i] < '9') {
                    num = (num << 3) + (num << 1) + (chars[i] & 0xf);
                    if ((minus > 0 && num > 0x7FFFFFFF) || (minus < 0 && num > 0x80000000L)) {
                        num = 0;
                        break;
                    }
                    if(i==chars.length-1)
                        status = Status.kValid;
                } else {
                    num = 0;
                    break;
                }
            }
            if(status == Status.kValid)
                num = num * minus;

        }

        return (int) num;
    }


    public static void main(String[] args) {

        BeanUtil.print(new Solution49().StrToInt("123141"));
        BeanUtil.print(new Solution49().StrToInt("-123141"));
        BeanUtil.print(new Solution49().StrToInt("-2147483648"));
        BeanUtil.print(new Solution49().StrToInt("2147483648"));
        BeanUtil.print(new Solution49().StrToInt("-"));
        BeanUtil.print(new Solution49().StrToInt("123a41"));
        BeanUtil.print(new Solution49().StrToInt("-123b141"));

    }

}
