package jianzhioffer;

import java.util.Stack;

/**
 * 题目描述
 * 用两个栈来实现一个队列，完成队列的Push和Pop操作。 队列中的元素为int类型。
 */
public class Solution05 {

    Stack<Integer> stack1 = new Stack<Integer>();
    Stack<Integer> stack2 = new Stack<Integer>();

    public void push(int node) {
        stack1.push(node);
    }

    public int pop() {
        if(stack1.empty()&&stack2.empty()){
            throw new RuntimeException("Queue is empty!");
        }
        if(stack2.empty()){
            while(!stack1.empty()){
                stack2.push(stack1.pop());
            }
        }
        return stack2.pop();
    }


    public static void main(String[] args) {

        Solution05 solution05=new Solution05();

        solution05.push(11);
        solution05.push(15);
        solution05.push(8);
        solution05.push(7);

        System.out.printf("pop=%d\r\n",solution05.pop());
        System.out.printf("pop=%d\r\n",solution05.pop());

        solution05.push(6);
        solution05.push(5);

        System.out.printf("pop=%d\r\n",solution05.pop());
        System.out.printf("pop=%d\r\n",solution05.pop());


    }


}
