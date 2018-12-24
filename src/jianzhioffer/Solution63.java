package jianzhioffer;


import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * 题目描述
 * 如何得到一个数据流中的中位数？如果从数据流中读出奇数个数值，那么中位数就是所有数值排序之后位于中间的数值。
 * 如果从数据流中读出偶数个数值，那么中位数就是所有数值排序之后中间两个数的平均值。
 * 我们使用Insert()方法读取数据流，使用GetMedian()方法获取当前读取数据的中位数。
 */
public class Solution63 {

    int count;
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();//小根堆
    PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());//大根堆

    public void Insert(Integer num) {

        if ((count & 1) == 0) {
            if (!maxHeap.isEmpty() && num < maxHeap.peek()) {
                maxHeap.offer(num);
                num = maxHeap.poll();
            }
            minHeap.offer(num);
        } else {
            if (!minHeap.isEmpty() && num > minHeap.peek()) {
                minHeap.offer(num);
                num = minHeap.poll();
            }
            maxHeap.offer(num);
        }
        count++;
    }

    public Double GetMedian() {
        if(count==0)
            throw new RuntimeException("no available number!");
        double result;
        //总数为奇数时，大顶堆堆顶就是中位数
        if((count&1)==1)
            result=minHeap.peek();
        else
            result=(minHeap.peek()+maxHeap.peek())/2.0;
        return result;
    }

}
