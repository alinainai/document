package basic;

public class StringTest {

    public static void main(String[] args) {

        final int NMAX = 10;

        // allocate triangular array
        int[][] odds = new int[NMAX + 1][];
        for (int n = 0; n <= NMAX; n++)
            odds[n] = new int[n + 1];

        // fill triangular array
        for (int n = 0; n < odds.length; n++)
            for (int k = 0; k < odds[n].length; k++) {
                /*
                 * compute binomial coefficient n*(n-1)*(n-2)*...*(n-k+1)/(1*2*3*...*k)
                 */
                int lotteryOdds = 1;
                for (int i = 1; i <= k; i++)
                    lotteryOdds = lotteryOdds * (n - i + 1) / i;

                odds[n][k] = lotteryOdds;
            }

        // print triangular array
        for (int[] row : odds) {
            for (int odd : row)
                System.out.printf("%4d", odd);
            System.out.println();
        }




        int int1 = 12;
        int int2 = 12;
        Integer Integer1 = new Integer(12);
        Integer Integer2 = new Integer(12);
        Integer Integer3 = new Integer(127);

        Integer a1 = 127;
        Integer b1 = 127;

        Integer a = 128;
        Integer b = 128;

        String s1 = "str";
        String s2 = "str";
        String str1 = new String("str");
        String str2 = new String("str");

        System.out.println("int1==int2:" + (int1 == int2));
        System.out.println("int1==Integer1:" + (int1 == Integer1));
        System.out.println("Integer1==Integer2:" + (Integer1 == Integer2));
        System.out.println("Integer3==b1:" + (Integer3 == b1));
        System.out.println("a1==b1:" + (a1 == b1));
        System.out.println("a==b:" + (a == b));


        System.out.println("s1==s2:" + (s1 == s2));
        System.out.println("s1==str1:" + (s1 == str1));
        System.out.println("str1==str2:" + (str1 == str2));


    }


}
