package jianzhioffer;

/**
 * 题目描述
 * 在一个二维数组中（每个一维数组的长度相同），
 * 每一行都按照从左到右递增的顺序排序，
 * 每一列都按照从上到下递增的顺序排序。
 * 请完成一个函数，输入这样的一个二维数组和一个整数，判断数组中是否含有该整数。
 */

public class Solution01 {

    public boolean find(int target, int[][] array) {

        //共有多少行
        int rowCount = array.length;
        //共有多少列
        int colCount = array[0].length;
        int row = 0;
        int col = colCount - 1;
        //先从第一行最大值比较，
        // 如果比目标值小就增行，
        // 如果比目标值大就减列；
        while (row < rowCount && col >= 0) {
            if (array[row][col] == target)
                return true;
            else if (array[row][col] < target)
                row++;
            else
                col--;
        }
        return false;

    }

    public static void main(String[] args) {

        int[][] arr = {{1, 2, 8, 9}, {2, 4, 9, 12}, {4, 7, 10, 13}, {6, 8, 11, 15}};

        System.out.print(new Solution01().find(4, arr));

    }


}
