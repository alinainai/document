package jianzhioffer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 *题目描述
 * 输入n个整数，找出其中最小的K个数。例如输入4,5,1,6,2,7,3,8这8个数字，则最小的4个数字是1,2,3,4,。
 */
public class Solution29 {

    public ArrayList<Integer> GetLeastNumbers_Solution(int [] input, int k) {

        ArrayList<Integer> result = new ArrayList<>();
        int length = input.length;
        if(k > length || k == 0){
            return result;
        }
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(k, Comparator.reverseOrder());
        for (int anInput : input) {
            if (maxHeap.size() != k) {
                maxHeap.offer(anInput);
            } else if (maxHeap.peek() > anInput) {
                maxHeap.poll();
                maxHeap.offer(anInput);
            }
        }
        result.addAll(maxHeap);
        return result;

    }

    public static void main(String[] args) {

        int arr[]={4,5,1,6,2,7,3,8};
        BeanUtil.print(new Solution29().GetLeastNumbers_Solution(arr,3));

    }


}
