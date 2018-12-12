package jianzhioffer;


import java.util.ArrayList;

/**
 * 题目描述
 * 把只包含质因子2、3和5的数称作丑数（Ugly Number）。
 * 例如6、8都是丑数，但14不是，因为它包含质因子7。 习惯上我们把1当做是第一个丑数。
 * 求按从小到大的顺序的第N个丑数。
 */
public class Solution33 {

    public int GetUglyNumber_Solution(int index) {


        if (index <=0) return 0;
        //p2，p3，p5分别为三个队列的指针，newNum为从队列头选出来的最小数
        int p2 = 0, p3 = 0, p5 = 0, newNum = 1;
        ArrayList<Integer> arr = new ArrayList<>();
        arr.add(newNum);
        while (arr.size() < index) {
            //选出三个队列头最小的数
            newNum = Math.min(arr.get(p2) * 2, Math.min(arr.get(p3) * 3, arr.get(p5) * 5));
            //这三个if有可能进入一个或者多个，进入多个是三个队列头最小的数有多个的情况
            if (arr.get(p2) * 2 == newNum) p2++;
            if (arr.get(p3) * 3 == newNum) p3++;
            if (arr.get(p5) * 5 == newNum) p5++;
            arr.add(newNum);
        }
        return newNum;


    }


    public static void main(String[] args) {

        BeanUtil.print(new Solution33().GetUglyNumber_Solution(5));

    }

}
