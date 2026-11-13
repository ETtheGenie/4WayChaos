package defaultPackage;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.NoSuchElementException;
import org.junit.Test;

/**
 * JUnit 4 unit tests for LinkedQueue.
 */
public class LinkedQueueTest
{

    @Test
    public void testConstructor()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }


    @Test
    public void testEnqueue()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();

        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");

        assertEquals(3, queue.size());
        assertEquals("A", queue.peek());
        assertFalse(queue.isEmpty());
    }


    @Test
    public void testDequeueFIFO()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();

        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");

        assertEquals("A", queue.dequeue());
        assertEquals("B", queue.dequeue());
        assertEquals("C", queue.dequeue());

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }


    @Test
    public void testPeekDoesNotRemove()
    {
        LinkedQueue<Integer> queue = new LinkedQueue<>();

        queue.enqueue(10);
        queue.enqueue(20);

        assertEquals(Integer.valueOf(10), queue.peek());
        assertEquals(Integer.valueOf(10), queue.peek());
        assertEquals(2, queue.size());
    }


    @Test
    public void testNullEnqueue()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        queue.enqueue("A");

        try
        {
            queue.enqueue(null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected exception.
        }

        assertEquals(1, queue.size());
        assertEquals("A", queue.peek());
    }


    @Test
    public void testDequeueEmpty()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();

        try
        {
            queue.dequeue();
            fail("Expected NoSuchElementException");
        }
        catch (NoSuchElementException e)
        {
            // Expected exception.
        }

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }


    @Test
    public void testPeekEmpty()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();

        try
        {
            queue.peek();
            fail("Expected NoSuchElementException");
        }
        catch (NoSuchElementException e)
        {
            // Expected exception.
        }

        assertTrue(queue.isEmpty());
    }


    @Test
    public void testReuseAfterEmpty()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();

        queue.enqueue("A");
        assertEquals("A", queue.dequeue());

        assertTrue(queue.isEmpty());

        queue.enqueue("B");
        assertEquals("B", queue.peek());
        assertEquals(1, queue.size());

        assertEquals("B", queue.dequeue());
        assertTrue(queue.isEmpty());
    }


    @Test
    public void testInterleavedOperations()
    {
        LinkedQueue<Integer> queue = new LinkedQueue<>();

        queue.enqueue(1);
        queue.enqueue(2);

        assertEquals(Integer.valueOf(1), queue.dequeue());

        queue.enqueue(3);

        assertEquals(Integer.valueOf(2), queue.dequeue());
        assertEquals(Integer.valueOf(3), queue.dequeue());

        assertEquals(0, queue.size());
    }


    @Test
    public void testIndependentQueues()
    {
        LinkedQueue<String> first = new LinkedQueue<>();
        LinkedQueue<String> second = new LinkedQueue<>();

        first.enqueue("North");
        second.enqueue("South");

        assertEquals("North", first.dequeue());
        assertEquals("South", second.peek());

        assertTrue(first.isEmpty());
        assertFalse(second.isEmpty());
    }
}
