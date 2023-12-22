package LAB_02and03;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

//https://chat.openai.com/share/35515917-9101-4a38-8b59-df8df919efb3
//https://chat.openai.com/share/fe6ac3d5-44cf-44f5-8dee-3b4082bee252
//一些问题汇总
/**
 * Wget is a simple web crawler class.
 * It provides methods for downloading content from the web in both an iterative and a multi-threaded manner.
 */
public class Wget_threadPoolDownload_out_of_band_signal {

    // A synchronized set to keep track of URLs that have been processed or are in queue to avoid duplication.
    private static final Set<String> seen = Collections.synchronizedSet(new HashSet<>());
    // A thread-safe queue to manage URLs to be processed.
    private static final URLQueue queue = new BlockingListQueue();

    /**
     * Downloads web content iteratively.
     *
     * @param initialURL The initial URL to start the downloading process from.
     */
    public static void iterativeDownload(String initialURL) {
        DocumentProcessing.handler = new DocumentProcessing.URLhandler() {
            @Override
            public void takeUrl(String url) {
                if (seen.add(url)) {
                    queue.enqueue(url);
                    System.out.println("Processing URL: " + url);
                }
            }
        };

        DocumentProcessing.handler.takeUrl(initialURL);
        int maxIterations = 10;
        int currentIteration = 0;
        while (!queue.isEmpty() && currentIteration < maxIterations) {
            String url = queue.dequeue();
            Xurl.download(url);
            System.out.println("Processed URL: " + url);
            currentIteration++;
        }
    }

    /**
     * Downloads web content using multiple threads.
     *
     * @param initialURL The initial URL to start the downloading process from.
     */
    public static void multiThreadedDownload(String initialURL) {
        DocumentProcessing.handler = new DocumentProcessing.URLhandler() {
            @Override
            public void takeUrl(String url) {
                if (seen.add(url)) {
                    queue.enqueue(url);
                    System.out.println("Processing URL: " + url);
                }
            }
        };

        DocumentProcessing.handler.takeUrl(initialURL);

        int threadCount = 0; // to give a unique name to each thread

        while (true) {
            if (!queue.isEmpty()) {
                String url = queue.dequeue();
                Thread downloadThread = new Thread(() -> {
                    Xurl.download(url);
                }, "DownloadThread-" + threadCount++); // setting custom thread name
                System.out.println(downloadThread.getName() + " is downloading " + url);
                downloadThread.start();
            } else {
                try {
                    Thread.sleep(100); // Sleep for a while if the queue is empty
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Termination condition: Check if all the threads we've started have terminated
            // and the queue is empty.
            if (Thread.activeCount() <= 2 && queue.isEmpty()) { // 2 for the main thread and this monitoring thread
                break;
            }
        }
    }


    /**
     * Downloads web content using a thread pool design pattern.
     *
     * @param poolSize The number of threads in the thread pool.
     * @param initialURL The initial URL to start the downloading process from.
     */
    public static void threadPoolDownload(int poolSize, String initialURL) {
        // AtomicInteger to track the number of threads currently processing URLs.
        AtomicInteger activeWorkerCount = new AtomicInteger(0);

        // Initialize the URL handler.
        DocumentProcessing.handler = new DocumentProcessing.URLhandler() {
            @Override
            public synchronized void takeUrl(String url) {
                if (seen.add(url)) {
                    queue.enqueue(url);
                    System.out.println("Enqueued URL: " + url);
                }
            }
        };

        // Add the initial URL.
        DocumentProcessing.handler.takeUrl(initialURL);

        // Create worker threads.
        Thread[] workers = new Thread[poolSize];
        for (int i = 0; i < poolSize; i++) {
            workers[i] = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    String url = queue.dequeue();

                    // Skip the invalid URLs
                    if (url == null || url.equals("**STOP**")) continue;

                    activeWorkerCount.incrementAndGet();
                    Xurl.download(url);
                    activeWorkerCount.decrementAndGet();

                    System.out.println(Thread.currentThread().getName() + " processed URL: " + url);
                }
            }, "WorkerThread-" + i);

            workers[i].start();
        }

        // Monitoring thread.
        Thread monitorThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println(activeWorkerCount);
                for (Thread t : Thread.getAllStackTraces().keySet()) {
                    System.out.printf("%-15s \t %-15s \t %-15d \t %s\n", t.getName(), t.getState(), t.getPriority(),
                            t.isDaemon());
                }
                System.out.println("-------------------------------------------");
                // If the queue is empty and no threads are processing URLs.
                if (queue.isEmpty() && activeWorkerCount.get() == 0) {
                    // Interrupt all worker threads.
                    for (Thread worker : workers ) {
                        worker.interrupt();
                    }
                    for (Thread t : Thread.getAllStackTraces().keySet()) {
                        System.out.printf("%-15s \t %-15s \t %-15d \t %s\n", t.getName(), t.getState(), t.getPriority(),
                                t.isDaemon());
                    }
                    break;
                }

            }
        });
        monitorThread.start();

        // Wait for the monitor thread to finish.
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    /**
     * The main method to execute the Wget class functionalities.
     *
     * @param args The command-line arguments where the first argument should be the URL to start from.
     */

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Wget url");
            System.exit(-1);
        }
        threadPoolDownload(100,args[0]);
    }
}

