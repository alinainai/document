package jianzhioffer;


import java.util.LinkedList;
import java.util.List;

/**
 * 题目描述
 * 0，1，2，……，n-1，这n个数字排成一个圆圈，从数字0开始，每次从这个圆圈里删除第m个数字。
 * 求出这个圆圈里剩下的最后一个数字。
 */
public class Solution46 {




    public int LastRemaining_Solution(int n, int m) {

        List<Integer> res= new LinkedList<>();


        for (int i=0;i<n;n++){
            res.add(i);
        }
        int num=0;
        int size=res.size();
        while (res.size()>1){

            for (int i=0;i<m;i++){
                num++;
            }

            if(num>size){
               num=(num-size+1)%m;
               res.remove(num);
               size=res.size();
            }else{
                res.remove(num);
            }

        }
        return res.get(0);

    }


    public static void main(String[] args) {

        BeanUtil.print(new Solution46().LastRemaining_Solution(98,6));

    }

}
