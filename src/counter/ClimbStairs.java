package counter;

/**
 * 已知一个楼梯有n个台阶，每次可以选择迈上一个或者两个台阶，求走完一共有多少种不同的走法
 */
public class ClimbStairs {

    public static int climbStairs(int n) {

        if(n<=0)
            return 0;
        if(n==1){
            return 1;
        }
        if(n==2){
            return 2;
        }
        else
            return climbStairs(n-1)+climbStairs(n-2);
    }

   


    public  int AddFrom1ToN_Recursive(int n) {
        return n <= 0 ? 0 :n + AddFrom1ToN_Recursive(n - 1);
    }

    public int AddFrom1ToN_Iternative(int n) {
        int result = 0;
        for (int i = 0; i <= n; ++i)
            result += i;

        return result;

    }

    public static void tower(int n,char a,char b,char c)//n个塔从s经过m最终全部移动到e
    {
        if(n==1)
            move(a,c);
        else
        {
            tower(n-1,a,c,b);
            move(a,c);
            tower(n-1,b,a,c);
        }
    }
    public static void move(char s,char e){
        System.out.println("move "+s+" to "+e);
    }




    public static void main(String []args){
//        int a =climbStairs(8);
//        System.out.println(a);

        tower(4,'A','B','C');

    }


}
