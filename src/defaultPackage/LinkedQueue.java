package defaultPackage;

import java.util.NoSuchElementException;

/**
 * A generic FIFO queue implemented using a singly linked list. Elements are
 * added at the tail and removed from the head. Null elements are not permitted.
 *
 * @param <T>
 *            type of elements stored in the queue
 */
public class LinkedQueue<T>
{

    /**
     * Internal node used to store a queue element and a reference to the next
     * node.
     */
    private static class Node<E>
    {
        private E data;
        private Node<E> next;

        public Node(E data)
        {
            this.data = data;
            this.next = null;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    /**
     * Creates an empty queue.
     */
    public LinkedQueue()
    {
        head = null;
        tail = null;
        size = 0;
    }


    /**
     * Adds an element to the end of the queue.
     *
     * @param item
     *            element to add
     * @throws IllegalArgumentException
     *             if item is null
     */
    public void enqueue(T item)
    {
        if (item == null)
        {
            throw new IllegalArgumentException("Cannot enqueue a null item.");
        }

        Node<T> newNode = new Node<>(item);

        if (isEmpty())
        {
            head = newNode;
        }
        else
        {
            tail.next = newNode;
        }

        tail = newNode;
        size++;
    }


    /**
     * Removes and returns the first element.
     *
     * @return the first element in the queue
     * @throws NoSuchElementException
     *             if empty
     */
    public T dequeue()
    {
        if (isEmpty())
        {
            throw new NoSuchElementException("Cannot dequeue an empty queue.");
        }

        T item = head.data;
        head = head.next;
        size--;

        // Reset tail if the final element was removed.
        if (head == null)
        {
            tail = null;
        }

        return item;
    }


    /**
     * Returns the first element without removing it.
     *
     * @return the first element
     * @throws NoSuchElementException
     *             if empty
     */
    public T peek()
    {
        if (isEmpty())
        {
            throw new NoSuchElementException("Cannot peek at an empty queue.");
        }

        return head.data;
    }


    /**
     * Checks whether the queue contains any elements.
     *
     * @return true if empty, false otherwise
     */
    public boolean isEmpty()
    {
        return size == 0;
    }


    /**
     * Returns the number of stored elements.
     *
     * @return number of elements in the queue
     */
    public int size()
    {
        return size;
    }
}
