package defaultPackage;

import java.util.NoSuchElementException;

public class SinglyLinkedQueue<T> {

    private Node<T> head;
    private Node<T> tail;
    private int size;

    private static class Node<T> {
        private T data;
        private Node<T> next;

        public Node(T data) {
            this.data = data;
            this.next = null;
        }
    }

    public SinglyLinkedQueue() {
        // TODO: Initialize fields
    }

    public void enqueue(T item) {
        // TODO: Validate item
        // TODO: Create a node
        // TODO: Update tail
        // TODO: Update size
    }

    public T dequeue() {
        // TODO: Handle empty queue
        // TODO: Remove head
        // TODO: Update tail if needed
        // TODO: Update size
        return null;
    }

    public T peek() {
        // TODO: Return front item
        return null;
    }

    public boolean isEmpty() {
        // TODO: Check empty state
        return false;
    }

    public int size() {
        // TODO: Return size
        return 0;
    }
}

