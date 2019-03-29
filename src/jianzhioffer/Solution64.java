package jianzhioffer;


import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;

/**
 * 题目描述
 * 给定一个数组和滑动窗口的大小，找出所有滑动窗口里数值的最大值。
 * 例如，如果输入数组{2,3,4,2,6,2,5,1}及滑动窗口的大小3，那么一共存在6个滑动窗口，
 * 他们的最大值分别为{4,4,6,6,6,5}； 针对数组{2,3,4,2,6,2,5,1}的滑动窗口有以下6个：
 * {[2,3,4],2,6,2,5,1}， {2,[3,4,2],6,2,5,1}， {2,3,[4,2,6],2,5,1}，
 * {2,3,4,[2,6,2],5,1}， {2,3,4,2,[6,2,5],1}， {2,3,4,2,6,[2,5,1]}。
 */
public class Solution64 {


    public ArrayList<Integer> maxInWindows(int[] num, int size) {
        ArrayList<Integer> ret = new ArrayList<>();
        if (num == null) {
            return ret;
        }
        if (num.length >= size && size >= 1) {
            Deque<Integer> deque = new LinkedList<>();
            for (int i = 0; i < size - 1; i++) {
                while (!deque.isEmpty() && num[i] > num[deque.getLast()]) {
                    deque.removeLast();
                }
                deque.addLast(i);
            }

            for (int i = size - 1; i < num.length; i++) {
                while (!deque.isEmpty() && num[i] > num[deque.getLast()]) {
                    deque.removeLast();
                }
                deque.addLast(i);
                if (i - deque.getFirst() + 1 > size) {
                    deque.removeFirst();
                }
                ret.add(num[deque.getFirst()]);
            }
        }
        return ret;
    }


    public static void main(String[] args) {

        int[] arr = {2, 3, 4, 2, 6, 2, 5, 1};

        BeanUtil.print(new Solution64().maxInWindows(arr, 3));

    }

}
