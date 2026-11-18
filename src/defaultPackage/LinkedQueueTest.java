
package defaultPackage;

import static org.junit.Assert.*;

import java.util.NoSuchElementException;
import org.junit.Test;

/**
 * JUnit 4 tests for the generic, singly linked FIFO queue used by 4WayChaos.
 * Covers every public operation, invalid input, empty/nonempty transitions, and
 * element ordering after interleaved enqueues and dequeues.
 */
public class LinkedQueueTest
{

    /**
     * Verifies dequeue() rejects an empty queue with the documented exception.
     *
     * @param queue
     *            queue expected to have no elements
     */
    private <T> void assertEmptyDequeueFails(LinkedQueue<T> queue)
    {
        try
        {
            queue.dequeue();
            fail(
                "Dequeue on an empty queue must throw NoSuchElementException.");
        }
        catch (NoSuchElementException expected)
        {
            // Correct: an empty queue has no element to remove.
        }
    }


    /**
     * Verifies peek() rejects an empty queue with the documented exception.
     *
     * @param queue
     *            queue expected to have no elements
     */
    private <T> void assertEmptyPeekFails(LinkedQueue<T> queue)
    {
        try
        {
            queue.peek();
            fail("Peek on an empty queue must throw NoSuchElementException.");
        }
        catch (NoSuchElementException expected)
        {
            // Correct: an empty queue has no front element.
        }
    }


    /**
     * Verifies enqueue(null) rejects invalid input without changing the queue.
     *
     * @param queue
     *            queue into which null must not be accepted
     */
    private <T> void assertNullEnqueueFails(LinkedQueue<T> queue)
    {
        try
        {
            queue.enqueue(null);
            fail("Enqueuing null must throw IllegalArgumentException.");
        }
        catch (IllegalArgumentException expected)
        {
            // Correct: null elements are not permitted.
        }
    }


    /**
     * Tests the constructor, empty queue status, and initial size.
     */
    @Test
    public void testNewQueueStartsEmpty()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }


    /**
     * Tests enqueue() on an empty queue and checks the first element.
     */
    @Test
    public void testEnqueueFirstElement()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        queue.enqueue("A");
        assertFalse(queue.isEmpty());
        assertEquals(1, queue.size());
        assertEquals("A", queue.peek());
    }


    /**
     * Tests enqueue() on an already populated queue without altering its head.
     */
    @Test
    public void testEnqueueMultipleElements()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");
        assertEquals(3, queue.size());
        assertEquals("A", queue.peek());
        assertFalse(queue.isEmpty());
    }


    /**
     * Tests the invalid-input case enqueue(null) on a new queue.
     */
    @Test
    public void testEnqueueNullOnEmptyQueueRejected()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        assertNullEnqueueFails(queue);
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }


    /**
     * Tests enqueue(null) does not corrupt an existing queue or its ordering.
     */
    @Test
    public void testEnqueueNullOnNonemptyQueueRejectedWithoutMutation()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        queue.enqueue("A");
        queue.enqueue("B");
        assertNullEnqueueFails(queue);
        assertEquals(2, queue.size());
        assertEquals("A", queue.dequeue());
        assertEquals("B", queue.dequeue());
        assertTrue(queue.isEmpty());
    }


    /**
     * Tests dequeue() on an empty queue and verifies state remains empty.
     */
    @Test
    public void testDequeueEmptyQueueRejected()
    {
        LinkedQueue<Integer> queue = new LinkedQueue<>();
        assertEmptyDequeueFails(queue);
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
    }


    /**
     * Tests dequeue() of the only element, including the transition to empty.
     */
    @Test
    public void testDequeueOnlyElement()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        queue.enqueue("only");
        assertEquals("only", queue.dequeue());
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
        assertEmptyDequeueFails(queue);
        assertEmptyPeekFails(queue);
    }


    /**
     * Tests removal of multiple elements in their insertion order.
     */
    @Test
    public void testDequeueReturnsFifoOrder()
    {
        LinkedQueue<Integer> queue = new LinkedQueue<>();
        queue.enqueue(10);
        queue.enqueue(20);
        queue.enqueue(30);
        assertEquals(Integer.valueOf(10), queue.dequeue());
        assertEquals(Integer.valueOf(20), queue.dequeue());
        assertEquals(Integer.valueOf(30), queue.dequeue());
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }


    /**
     * Tests empty dequeue after draining a previously populated queue.
     */
    @Test
    public void testDequeueEmptyAfterDrainingRejected()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        queue.enqueue("A");
        queue.enqueue("B");
        queue.dequeue();
        queue.dequeue();
        assertEmptyDequeueFails(queue);
        assertEquals(0, queue.size());
    }


    /**
     * Tests peek() on an empty queue, including nonmutation after failure.
     */
    @Test
    public void testPeekEmptyQueueRejected()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        assertEmptyPeekFails(queue);
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }


    /**
     * Tests repeated peek() calls return the head without removing elements.
     */
    @Test
    public void testPeekDoesNotRemove()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        queue.enqueue("first");
        queue.enqueue("second");
        assertEquals("first", queue.peek());
        assertEquals("first", queue.peek());
        assertEquals(2, queue.size());
        assertEquals("first", queue.dequeue());
        assertEquals("second", queue.peek());
    }


    /**
     * Tests that peek() advances to the next element only after a dequeue.
     */
    @Test
    public void testPeekTracksFrontAfterDequeue()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");
        assertEquals("A", queue.peek());
        assertEquals("A", queue.dequeue());
        assertEquals("B", queue.peek());
        assertEquals("B", queue.dequeue());
        assertEquals("C", queue.peek());
        assertEquals(1, queue.size());
    }


    /**
     * Tests isEmpty() across empty, nonempty, and emptied-again states.
     */
    @Test
    public void testIsEmptyTransitions()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        assertTrue(queue.isEmpty());
        queue.enqueue("A");
        assertFalse(queue.isEmpty());
        queue.enqueue("B");
        assertFalse(queue.isEmpty());
        queue.dequeue();
        assertFalse(queue.isEmpty());
        queue.dequeue();
        assertTrue(queue.isEmpty());
    }


    /**
     * Tests size() increases on enqueue, decreases on dequeue, and reaches
     * zero.
     */
    @Test
    public void testSizeTracksEveryMutation()
    {
        LinkedQueue<Integer> queue = new LinkedQueue<>();
        assertEquals(0, queue.size());
        queue.enqueue(1);
        assertEquals(1, queue.size());
        queue.enqueue(2);
        assertEquals(2, queue.size());
        queue.enqueue(3);
        assertEquals(3, queue.size());
        queue.dequeue();
        assertEquals(2, queue.size());
        queue.dequeue();
        assertEquals(1, queue.size());
        queue.dequeue();
        assertEquals(0, queue.size());
    }


    /**
     * Tests rejected enqueue, dequeue, and peek operations never change size.
     */
    @Test
    public void testInvalidOperationsLeaveSizeUnchanged()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        assertNullEnqueueFails(queue);
        assertEmptyDequeueFails(queue);
        assertEmptyPeekFails(queue);
        assertEquals(0, queue.size());
        queue.enqueue("A");
        assertNullEnqueueFails(queue);
        assertEquals(1, queue.size());
        assertEquals("A", queue.peek());
    }


    /**
     * Directly checks the rubric's interleaved enqueue/dequeue FIFO criterion.
     * New elements must always join the tail, even after head removals.
     */
    @Test
    public void testInterleavedEnqueueDequeuePreservesFifo()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        queue.enqueue("A");
        queue.enqueue("B");
        assertEquals("A", queue.dequeue());
        queue.enqueue("C");
        assertEquals("B", queue.dequeue());
        queue.enqueue("D");
        assertEquals("C", queue.dequeue());
        assertEquals("D", queue.dequeue());
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }


    /**
     * Tests a longer sequence of mixed operations and verifies the full order.
     */
    @Test
    public void testMultipleInterleavedEnqueuesAndDequeues()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");
        assertEquals("A", queue.dequeue());
        queue.enqueue("D");
        queue.enqueue("E");
        assertEquals("B", queue.dequeue());
        assertEquals("C", queue.dequeue());
        queue.enqueue("F");
        assertEquals("D", queue.peek());
        assertEquals(3, queue.size());
        assertEquals("D", queue.dequeue());
        assertEquals("E", queue.dequeue());
        assertEquals("F", queue.dequeue());
        assertEmptyDequeueFails(queue);
    }


    /**
     * Tests that the queue can be reused after its last element is removed,
     * exercising the tail reset and subsequent fresh enqueue.
     */
    @Test
    public void testReuseAfterFullyDrainingQueue()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        queue.enqueue("old1");
        queue.enqueue("old2");
        assertEquals("old1", queue.dequeue());
        assertEquals("old2", queue.dequeue());
        assertTrue(queue.isEmpty());
        queue.enqueue("new1");
        queue.enqueue("new2");
        assertEquals(2, queue.size());
        assertEquals("new1", queue.dequeue());
        assertEquals("new2", queue.dequeue());
        assertTrue(queue.isEmpty());
    }


    /**
     * Tests alternating enqueue/dequeue operations when size is at most one.
     */
    @Test
    public void testAlternatingSingleElementEnqueueDequeue()
    {
        LinkedQueue<Integer> queue = new LinkedQueue<>();
        for (int i = 0; i < 250; i++)
        {
            queue.enqueue(i);
            assertFalse(queue.isEmpty());
            assertEquals(1, queue.size());
            assertEquals(Integer.valueOf(i), queue.peek());
            assertEquals(Integer.valueOf(i), queue.dequeue());
            assertTrue(queue.isEmpty());
        }
        assertEquals(0, queue.size());
    }


    /**
     * Tests two queue instances operate independently when operations overlap.
     */
    @Test
    public void testSeparateQueuesDoNotShareElementsOrState()
    {
        LinkedQueue<String> first = new LinkedQueue<>();
        LinkedQueue<String> second = new LinkedQueue<>();
        first.enqueue("A");
        second.enqueue("X");
        first.enqueue("B");
        assertEquals("X", second.dequeue());
        second.enqueue("Y");
        assertEquals("A", first.dequeue());
        assertEquals("Y", second.dequeue());
        assertEquals("B", first.dequeue());
        assertTrue(first.isEmpty());
        assertTrue(second.isEmpty());
    }


    /**
     * Tests the generic implementation with an unrelated numeric element type.
     */
    @Test
    public void testGenericIntegerElements()
    {
        LinkedQueue<Integer> queue = new LinkedQueue<>();
        queue.enqueue(-10);
        queue.enqueue(0);
        queue.enqueue(500);
        assertEquals(Integer.valueOf(-10), queue.dequeue());
        assertEquals(Integer.valueOf(0), queue.dequeue());
        assertEquals(Integer.valueOf(500), queue.dequeue());
    }


    /**
     * Tests that stored objects are returned by reference, not copied or cast.
     */
    @Test
    public void testObjectReferenceIdentityIsPreserved()
    {
        LinkedQueue<Object> queue = new LinkedQueue<>();
        Object first = new Object();
        Object second = new Object();
        queue.enqueue(first);
        queue.enqueue(second);
        assertSame(first, queue.peek());
        assertSame(first, queue.dequeue());
        assertSame(second, queue.dequeue());
    }


    /**
     * Tests that empty and whitespace-only strings are valid, non-null values.
     */
    @Test
    public void testEmptyAndWhitespaceStringsAreValidElements()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        queue.enqueue("");
        queue.enqueue(" ");
        assertEquals("", queue.dequeue());
        assertEquals(" ", queue.dequeue());
        assertTrue(queue.isEmpty());
    }


    /**
     * Stress-tests FIFO order and size over 10,000 queued integers.
     */
    @Test
    public void testLargeQueueRetainsFifoOrder()
    {
        LinkedQueue<Integer> queue = new LinkedQueue<>();
        for (int i = 0; i < 10000; i++)
        {
            queue.enqueue(i);
        }
        assertEquals(10000, queue.size());
        for (int i = 0; i < 10000; i++)
        {
            assertEquals(Integer.valueOf(i), queue.dequeue());
        }
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }


    /**
     * Stress-tests head/tail updates during a rolling sequence of interleaved
     * enqueues and dequeues that keeps three elements in the queue.
     */
    @Test
    public void testLargeInterleavedFifoSequence()
    {
        LinkedQueue<Integer> queue = new LinkedQueue<>();
        queue.enqueue(0);
        queue.enqueue(1);
        queue.enqueue(2);
        for (int i = 3; i < 10003; i++)
        {
            assertEquals(Integer.valueOf(i - 3), queue.dequeue());
            queue.enqueue(i);
            assertEquals(3, queue.size());
            assertEquals(Integer.valueOf(i - 2), queue.peek());
        }
        assertEquals(Integer.valueOf(10000), queue.dequeue());
        assertEquals(Integer.valueOf(10001), queue.dequeue());
        assertEquals(Integer.valueOf(10002), queue.dequeue());
        assertTrue(queue.isEmpty());
    }
}
