package LAB_02and03;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe implementation of the {@link URLQueue} interface.
 * <p>
 * This class uses a {@link LinkedList} as its underlying data structure
 * and a {@link ReentrantLock} to ensure synchronized access.
 * </p>
 */
public class SynchronizedListQueue implements URLQueue {

    /** The underlying linked list to hold the URLs. */
    private final LinkedList<String> list = new LinkedList<>();

    /** The lock to ensure synchronized access to the linked list. */
    private final Lock lock = new ReentrantLock();

    /**
     * Checks if the queue is empty.
     *
     * @return true if the queue is empty, false otherwise.
     */
    @Override
    public boolean isEmpty() {
        lock.lock();  // Lock to ensure thread-safe access
        try {
            return list.isEmpty();
        } finally {
            lock.unlock();  // Always unlock in a finally block to ensure the lock is released
        }
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
    public boolean isFull() {
        return false;  // LinkedList has no fixed size, so it's never full
    }

    /**
     * Adds a URL to the end of the queue.
     * <p>
     * If the provided URL is null, an {@link IllegalArgumentException} is thrown.
     * </p>
     *
     * @param url The URL to be added.
     * @throws IllegalArgumentException if the URL is null.
     */
    @Override
    public void enqueue(String url) {
        if (url == null) {
            throw new IllegalArgumentException("URL cannot be null");
        }

        lock.lock();  // Lock to ensure thread-safe access
        try {
            list.add(url);
        } finally {
            lock.unlock();  // Always unlock in a finally block to ensure the lock is released
        }
    }

    /**
     * Removes and returns the URL from the front of the queue.
     * <p>
     * If the queue is empty, a {@link NoSuchElementException} is thrown.
     * </p>
     *
     * @return the URL from the front of the queue.
     * @throws NoSuchElementException if the queue is empty.
     */
    @Override
    public String dequeue() {
        lock.lock();  // Lock to ensure thread-safe access
        try {
            if (list.isEmpty()) {
                throw new NoSuchElementException("Queue is empty");
            }
            return list.removeFirst();
        } finally {
            lock.unlock();  // Always unlock in a finally block to ensure the lock is released
        }
    }
}
