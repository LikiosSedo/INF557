package LAB_02and03;

import java.util.LinkedList;

/**
 * A thread-safe blocking implementation of the {@link URLQueue} interface.
 * <p>
 * This class uses a {@link LinkedList} as its underlying data structure.
 * The dequeue operation blocks if the queue is empty until an item is enqueued.
 * </p>
 */
public class BlockingListQueue implements URLQueue {

    /** The underlying linked list to hold the URLs. */
    private final LinkedList<String> list = new LinkedList<>();

    /** A constant to represent the STOP signal. */
    private static final String STOP = "**STOP**";

    /**
     * Checks if the queue is empty.
     *
     * @return true if the queue is empty, false otherwise.
     */
    @Override
    public synchronized boolean isEmpty() {
        return list.isEmpty();
    }

    /**
     * Checks if the queue is full.
     * <p>
     * This implementation always returns false as the underlying LinkedList
     * does not have a preset size limit.
     * </p>
     *
     * @return always false.
     */
    @Override
    public synchronized boolean isFull() {
        return false;  // LinkedList has no fixed size, so it's never full
    }

    /**
     * Adds a URL to the end of the queue and notifies any waiting threads.
     * <p>
     * If the provided URL is null, an {@link IllegalArgumentException} is thrown.
     * </p>
     *
     * @param url The URL to be added.
     * @throws IllegalArgumentException if the URL is null.
     */
    @Override
    public synchronized void enqueue(String url) {
        if (url == null) {
            throw new IllegalArgumentException("URL cannot be null");
        }

        list.add(url);
        // Notify any waiting threads that an item has been added
        notifyAll();
    }

    /**
     * Removes and returns the URL from the front of the queue.
     * If the queue is empty, it waits until an item is enqueued.
     * <p>
     * If the STOP signal is dequeued, the thread's interrupt flag is set.
     * </p>
     *
     * @return the URL from the front of the queue or the STOP signal.
     */
    @Override
    public synchronized String dequeue() {
        while (list.isEmpty()) {
            try {
                // Wait until an item is added
                wait();
            } catch (InterruptedException e) {
              //  Thread.currentThread().interrupt();
                return STOP;
            }
        }

        String url = list.removeFirst();
        if (STOP.equals(url)) {
            Thread.currentThread().interrupt();
        }
        return url;
    }
}
