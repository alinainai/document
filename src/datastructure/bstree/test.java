package datastructure.bstree;

import java.util.*;

public class test {

    public static void main(String[] args) {




        //乱序插入到二叉排序树中
        BinarySearchTree binarySearchTree = new BinarySearchTree(null);
        binarySearchTree.insert(8);
//        binarySearchTree.insert(3);

        binarySearchTree.insert(10);
        binarySearchTree.insert(14);
        binarySearchTree.insert(13);

//        binarySearchTree.insert(1);
//        binarySearchTree.insert(6);
//        binarySearchTree.insert(4);
//        binarySearchTree.insert(7);

        //中序遍历
        binarySearchTree.iterateMediumOrder(binarySearchTree.getRoot());
        System.out.println("");
        //查找某个数据
//        System.out.println(binarySearchTree.search(10).getData());
        //删除某个数据对应的元素
        binarySearchTree.delete(8);
        //中序遍历删除后的二叉排序树
        binarySearchTree.iterateMediumOrder(binarySearchTree.getRoot());



//        ArrayDeque stack=new ArrayDeque();
//        stack.push("jim");
//        stack.push("tom");
//        stack.push("lulu");
////栈先进后出，所以输出结果是反向的
//        System.out.println(stack);
//        System.out.println(stack.peek());
//        System.out.println(stack);
//        System.out.println(stack.pop());
//        System.out.println(stack);



    }

    private static void testInsertTime(){


        long runCount=20000000;
        Map<Integer,Integer> hashMap = new HashMap<Integer, Integer>();
        Date dateBegin = new Date();
        for (int i = 0; i < runCount; i++) {
            hashMap.put(i, i);
        }
        Date dateEnd = new Date();
        System.out.println("HashMap插入用时为：" + (dateEnd.getTime() - dateBegin.getTime()));

        Map<Integer,Integer> hashtable = new Hashtable<Integer, Integer>();
        Date dateBegin1 = new Date();
        for (int i = 0; i < runCount; i++) {
            hashtable.put(i, i);
        }
        Date dateEnd1 = new Date();
        System.out.println("Hashtable插入用时为：" + (dateEnd1.getTime() - dateBegin1.getTime()));

    }

    private static void testGetTime(){


        long runCount=20000000;
        Map<Integer,Integer> hashMap = new HashMap<Integer, Integer>();
        for (int i = 0; i < runCount; i++) {
            hashMap.put(i, i);
        }
        Date dateBegin = new Date();
        for (Integer key : hashMap.keySet()) {
            hashMap.get(key);
        }
        Date dateEnd = new Date();
        System.out.println("HashMap读取用时为：" + (dateEnd.getTime() - dateBegin.getTime()));

        Map<Integer,Integer> hashtable = new Hashtable<Integer, Integer>();
        for (int i = 0; i < runCount; i++) {
            hashtable.put(i, i);
        }
        Date dateBegin1 = new Date();
        for (Integer key : hashtable.keySet()) {
            hashtable.get(key);
        }
        Date dateEnd1 = new Date();
        System.out.println("Hashtable读取用时为：" + (dateEnd1.getTime() - dateBegin1.getTime()));


    }



}
