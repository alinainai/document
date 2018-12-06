package jianzhioffer;

import java.util.Stack;

/**
 * 题目描述
 * 定义栈的数据结构，请在该类型中实现一个能够得到栈中所含最小元素的min函数（时间复杂度应为O（1））。
 */
public class Solution20 {

    private Stack<Integer> stack1 = new Stack<>();
    private Stack<Integer> stack2 = new Stack<>();

    void push(int value) {
        stack1.push(value);
        if (stack2.empty())
            stack2.push(value);
        else if (value <= stack2.peek()) {
            stack2.push(value);
        }
    }

    void pop() {
        if (stack1.peek() == stack2.peek())
            stack2.pop();
        stack1.pop();

    }

    int top() {
        return stack1.peek();
    }

    int min() {
        return stack2.peek();
    }


}
