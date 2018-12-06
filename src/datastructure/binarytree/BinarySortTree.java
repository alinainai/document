package datastructure.binarytree;

import java.util.LinkedList;
import java.util.Queue;

class BinarySortTree<E extends Comparable<E>> {

    private Node<E> root;

    BinarySortTree() {
        root = null;
    }

    public void insertNode(E value) {
        if (root == null) {
            root = new Node<E>(value);
            return;
        }
        Node<E> currentNode = root;
        while (true) {
            if (value.compareTo(currentNode.value) > 0) {
                if (currentNode.right == null) {
                    currentNode.right = new Node<E>(value);
                    break;
                }
                currentNode = currentNode.right;
            } else {
                if (currentNode.left == null) {
                    currentNode.left = new Node<E>(value);
                    break;
                }
                currentNode = currentNode.left;
            }
        }
    }

    public Node<E> getRoot(){
        return root;
    }

    /**
     * 先序遍历二叉树（递归）
     * @param node
     */
    public void preOrderTraverse(Node<E> node) {
        System.out.print(node.value + " ");
        if (node.left != null)
            preOrderTraverse(node.left);
        if (node.right != null)
            preOrderTraverse(node.right);
    }

    /**
     * 中序遍历二叉树（递归）
     * @param node
     */
    public void inOrderTraverse(Node<E> node) {
        if (node.left != null)
            inOrderTraverse(node.left);
        System.out.print(node.value + " ");
        if (node.right != null)
            inOrderTraverse(node.right);
    }

    /**
     * 后序遍历二叉树（递归）
     * @param node
     */
    public void postOrderTraverse(Node<E> node) {
        if (node.left != null)
            postOrderTraverse(node.left);
        if (node.right != null)
            postOrderTraverse(node.right);
        System.out.print(node.value + " ");
    }

    /**
     * 先序遍历二叉树（非递归）
     * @param root
     */
    public void preOrderTraverseNoRecursion(Node<E> root) {
        LinkedList<Node<E>> stack = new LinkedList<Node<E>>();
        Node<E> currentNode = null;
        stack.push(root);
        while (!stack.isEmpty()) {
            currentNode = stack.pop();
            System.out.print(currentNode.value + " ");
            if (currentNode.right != null)
                stack.push(currentNode.right);
            if (currentNode.left != null)
                stack.push(currentNode.left);
        }
    }

    /**
     * 中序遍历二叉树（非递归）
     * @param root
     */
    public void inOrderTraverseNoRecursion(Node<E> root) {
        LinkedList<Node<E>> stack = new LinkedList<Node<E>>();
        Node<E> currentNode = root;
        while (currentNode != null || !stack.isEmpty()) {
            // 一直循环到二叉排序树最左端的叶子结点（currentNode是null）
            while (currentNode != null) {
                stack.push(currentNode);
                currentNode = currentNode.left;
            }
            currentNode = stack.pop();
            System.out.print(currentNode.value + " ");
            currentNode = currentNode.right;
        }
    }

    /**
     * 后序遍历二叉树（非递归）
     * @param root
     */
    public void postOrderTraverseNoRecursion(Node<E> root) {
        LinkedList<Node<E>> stack = new LinkedList<Node<E>>();
        Node<E> currentNode = root;
        Node<E> rightNode = null;
        while (currentNode != null || !stack.isEmpty()) {
            // 一直循环到二叉排序树最左端的叶子结点（currentNode是null）
            while (currentNode != null) {
                stack.push(currentNode);
                currentNode = currentNode.left;
            }
            currentNode = stack.pop();
            // 当前结点没有右结点或上一个结点（已经输出的结点）是当前结点的右结点，则输出当前结点
            while (currentNode.right == null || currentNode.right == rightNode) {
                System.out.print(currentNode.value + " ");
                rightNode = currentNode;
                if (stack.isEmpty()) {
                    return; //root以输出，则遍历结束
                }
                currentNode = stack.pop();
            }
            stack.push(currentNode); //还有右结点没有遍历
            currentNode = currentNode.right;
        }
    }

    /**
     * 广度优先遍历二叉树，又称层次遍历二叉树
     * @param root
     */
    public void breadthFirstTraverse(Node<E> root) {
        Queue<Node<E>> queue = new LinkedList<>();
        Node<E> currentNode ;
        queue.offer(root);
        while (!queue.isEmpty()) {
            currentNode = queue.poll();
            System.out.print(currentNode.value + " ");
            if (currentNode.left != null)
                queue.offer(currentNode.left);
            if (currentNode.right != null)
                queue.offer(currentNode.right);
        }
    }

}

