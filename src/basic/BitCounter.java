package basic;

public class BitCounter {


    public static void main(String[] args) {


        int a=-5;
        System.out.println(a/2);
        System.out.println(Integer.toBinaryString(-1));
        System.out.println(Integer.toBinaryString(a));
        System.out.println(Integer.toBinaryString(a>>1));



//        int a = -1, b = -2;
//
//        //交换a、b的值
//        a = a ^ b;
//        b = a ^ b;
//        a = a ^ b;
//
//        System.out.println(a);
//        System.out.println(b);
//        //获取1的个数
//        System.out.println(new BitCounter().getNum1BinaryBit(-1));
//
//        //获取相反数
//        System.out.println(~a + 1);
//
//        //判断是否是奇数
//        System.out.println(new BitCounter().isOdd(4));
//
//        //判断是否是2的幂
//        System.out.println(new BitCounter().powerOf2(128));
//
//        //判断是否是4的幂
//        System.out.println(new BitCounter().powerOf4(16));
    }

    /**
     * 获取二进制位有多少个1
     *
     * @param num
     * @return
     */

    int getNum1BinaryBit(int num) {
        int count = 0;
        while (num != 0) {
            num = num & (num - 1);
            ++count;
        }
        return count;
    }

    /**
     * 判断是否是奇数
     *
     * @param n
     * @return
     */
    boolean isOdd(int n) {
        return ((n & 1) == 1);
    }

    /**
     * 是不是2的幂
     *
     * @param n
     * @return
     */

    boolean powerOf2(int n) {
        return (n & (n - 1)) == 0;
    }

    /**
     * 是不是4的幂
     * @param n
     * @return
     */
    boolean powerOf4(int n) {
        if ((n & (n - 1)) == 0) {
            return (n & 0x55555555)!=0;
        }
        return false;
    }

}
