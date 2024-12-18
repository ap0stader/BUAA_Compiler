package util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class DoublyLinkedList<T> implements Iterable<DoublyLinkedList.Node<T>> {
    // 头节点
    private Node<T> head;
    // 尾节点
    private Node<T> tail;
    // 该链表上节点的个数
    private int nodeNumber;

    public DoublyLinkedList() {
        this.head = null;
        this.tail = null;
        this.nodeNumber = 0;
    }

    public Node<T> head() {
        return head;
    }

    public Node<T> tail() {
        return tail;
    }

    public int size() {
        return this.nodeNumber;
    }

    public boolean isEmpty() {
        return this.nodeNumber == 0;
    }

    public void insertBeforeHead(Node<T> node) {
        if (this.isEmpty()) {
            this.head = node;
            this.tail = node;
            this.nodeNumber++;
            node.parent = this;
        } else {
            node.insertBefore(this.head);
        }
    }

    public void insertAfterTail(Node<T> node) {
        if (this.isEmpty()) {
            this.head = node;
            this.tail = node;
            this.nodeNumber++;
            node.parent = this;
        } else {
            node.insertAfter(this.tail);
        }
    }

    @Override
    public Iterator<Node<T>> iterator() {
        return new DoublyLinkedListIterator(head);
    }

    private class DoublyLinkedListIterator implements Iterator<Node<T>> {
        private Node<T> current;
        private Node<T> next;

        private DoublyLinkedListIterator(Node<T> head) {
            this.current = null;
            this.next = head;
        }

        @Override
        public boolean hasNext() {
            return this.next != null;
        }

        @Override
        public Node<T> next() {
            if (this.hasNext()) {
                this.current = this.next;
                this.next = this.next.next;
                return this.current;
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            if (this.current != null) {
                this.current.eliminate();
                this.current = null;
            } else {
                throw new IllegalStateException();
            }
        }
    }

    public static class Node<T> {
        private final T value;
        private Node<T> pred;
        private Node<T> next;
        private DoublyLinkedList<T> parent;

        public Node(T value) {
            this.value = value;
            this.pred = null;
            this.next = null;
            this.parent = null;
        }

        public T value() {
            return value;
        }

        public Node<T> pred() {
            return pred;
        }

        public Node<T> next() {
            return next;
        }

        public void insertBefore(Node<T> next) {
            if (this.parent != null) {
                throw new RuntimeException("When insertBefore(), the node is already inserted.");
            }
            // 更新本节点信息
            this.parent = next.parent;
            this.pred = next.pred;
            this.next = next;
            // 更新临近节点信息
            next.pred = this;
            if (this.pred != null) {
                this.pred.next = this;
            }
            // 更新链表
            this.parent.nodeNumber++;
            // 更新链表头结点
            if (this.parent.head == next) {
                this.parent.head = this;
            }
        }

        public void insertAfter(Node<T> pred) {
            if (this.parent != null) {
                throw new RuntimeException("When insertAfter(), the node is already inserted.");
            }
            // 更新本节点信息
            this.parent = pred.parent;
            this.pred = pred;
            this.next = pred.next;
            // 更新临近节点信息
            pred.next = this;
            if (this.next != null) {
                this.next.pred = this;
            }
            // 更新链表
            this.parent.nodeNumber++;
            // 更新链表尾结点
            if (this.parent.tail == pred) {
                this.parent.tail = this;
            }
        }

        public void eliminate() {
            if (this.parent == null) {
                throw new RuntimeException("When eliminate(), the node is not inserted.");
            }
            // 处理上一节点
            if (this.pred != null) {
                this.pred.next = this.next;
            }
            // 处理下一节点
            if (this.next != null) {
                this.next.pred = this.pred;
            }
            // 更新链表
            this.parent.nodeNumber--;
            // 更新链表头节点
            if (this.parent.head == this) {
                this.parent.head = this.next;
            }
            // 更新链表尾节点
            if (this.parent.tail == this) {
                this.parent.tail = this.pred;
            }
        }
    }
}
