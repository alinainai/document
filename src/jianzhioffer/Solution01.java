package jianzhioffer;

/**
 * 题目描述
 * 在一个二维数组中（每个一维数组的长度相同），
 * 每一行都按照从左到右递增的顺序排序，
 * 每一列都按照从上到下递增的顺序排序。
 * 请完成一个函数，输入这样的一个二维数组和一个整数，判断数组中是否含有该整数。
 *
 */

public class Solution01 {

    public boolean find(int target, int[][] array) {

        //共有多少行
        int rowCount = array.length;
        //共有多少列
        int colCount = array[0].length;
        int row = 0;
        int line = colCount - 1;
        //先从第一行最大值比较，
        // 如果比目标值小就增行，
        // 如果比目标值大就减列；
        while (row < rowCount && line >= 0) {
            if (array[row][line] == target)
                return true;
            else if (array[row][line] < target)
                row++;
            else
                line--;
        }
        return false;

    }

    public static void main(String[] args) {

        int[][] arr={{1,2,3,4},{5,6,7,8},{9,10,11,12},{13,14,15,16}};

        System.out.print( new Solution01().find(5,arr));

    }


}
