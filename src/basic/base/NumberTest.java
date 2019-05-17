package basic.base;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class NumberTest {

    public static void main(String[] args) {

        String str = "---------------------------------------------------------";
        System.out.println("Integer.MAX_VALUE：内存溢出");
        System.out.println("Integer.MAX_VALUE-(-Integer.MAX_VALUE)" + "：" + (Integer.MAX_VALUE - (-Integer.MAX_VALUE))); //内存溢出
        System.out.println(str);

        System.out.println("Integer的范围：");
        System.out.println("Integer.MAX_VALUE" + "：" + Integer.MAX_VALUE); //2的31次方-1,10个数位，正的20亿左右,用在钱上面不一定够
        System.out.println("Integer.MIN_VALUE" + "：" + Integer.MIN_VALUE); //负的2的31次方

        System.out.println(str);

        System.out.println("Long的范围：");
        System.out.println("Long.MAX_VALUE" + "：" + Long.MAX_VALUE); //2的64次方-1,19个数位，很大了,可放心用在钱上面
        System.out.println("Long.MIN_VALUE" + "：" + Long.MIN_VALUE); //负的2的64次方

        System.out.println(str);

        System.out.println("Float的范围：");
        System.out.println("Float.MAX_VALUE" + "：" + Float.MAX_VALUE); //2的128次方-1,38个数位，比long多了一倍,这个主要用来做简单数学精确运算使用
        System.out.println("Float.MIN_VALUE" + "：" + Float.MIN_VALUE); //2的-149次方

        System.out.println(str);

        System.out.println("Double的范围：");
        System.out.println("Double.MAX_VALUE" + "：" + Double.MAX_VALUE); //2的1024次方-1,308个数位，是float数位的10倍，主要用来做复杂运算和天文运算
        System.out.println("Double.MIN_VALUE" + "：" + Double.MIN_VALUE); //2的-1074次方

        System.out.println(str);

        double num1 = 12345.67890d;

        System.out.println("formatNumber显示：");
        System.out.println("formatNumber 输出Double.MAX_VALUE" + "：" + formatNumber(Double.MAX_VALUE)); //2的1024次方-1,308个数位，是float数位的10倍，主要用来做复杂运算和天文运算
        System.out.println("formatNumber 输出Double.MIN_VALUE" + "：" + formatNumber(Double.MIN_VALUE)); //2的-1074次方
        System.out.println("formatNumber 输出 12345.67890" + "：" + formatNumber(num1)); //2的-1074次方

        System.out.println(str);

        System.out.println("formatNumber2显示：");
        System.out.println("formatNumber2 输出Double.MAX_VALUE" + "：" + formatNumber2(Double.MAX_VALUE)); //2的1024次方-1,308个数位，是float数位的10倍，主要用来做复杂运算和天文运算
        System.out.println("formatNumber2 输出Double.MIN_VALUE" + "：" + formatNumber2(Double.MIN_VALUE)); //2的-1074次方
        System.out.println("formatNumber2 输出 12345.67890" + "：" + formatNumber2(num1));



    }


    /**
     * 格式化double类型，不保留小数
     *
     * @param number
     * @return
     */
    public static String formatNumber(double number) {
        DecimalFormat df = new DecimalFormat("#,##0");
        return df.format(number);
    }


    /**
     * 格式化double类型，保留小数
     *
     * @param number
     * @return
     */
    public static String formatNumber2(double number) {
        DecimalFormat df = new DecimalFormat("#,###.##");
        return df.format(number);
    }


}
