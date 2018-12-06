package leetcode;

/**
 * 汉诺塔问题
 *
 *  n个的移动次数=(n-1)的移动次数+1+(n-1)的移动次数
 *
 *  1个的时候是1次
 *  2个的时候是 2*1+1
 *  3个的时候是 2*(2*1+1)+1
 *  ...
 *  n个的时候是 2^n-1
 *
 */
public class Hanoi {

    /**
     *
     * @param n 盘子的数目
     * @param origin 源座
     * @param assist 辅助座
     * @param destination 目的座
     */
    public void hanoi(int n, char origin, char assist, char destination) {
        if (n == 1) {//n=1 只需要挪动一次
            move(origin, destination);
        } else {
            //把n-1个盘子按顺序挪到辅助杆
            hanoi(n - 1, origin, destination, assist);
            //把最底层的挪动到目标杆
            move(origin, destination);
            //把n-1个盘子按顺序从辅助杆挪到目标杆
            hanoi(n - 1, assist, origin, destination);
        }
    }

    // Print the route of the movement
    private void move(char origin, char destination) {
        System.out.println("Direction:" + origin + "--->" + destination);
    }

    public static void main(String[] args) {
        Hanoi hanoi = new Hanoi();
        hanoi.hanoi(4, 'A', 'B', 'C');
    }


}
