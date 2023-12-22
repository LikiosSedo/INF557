package LAB_02and03;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Wget_multiThreadedDownload {

    private static final Set<String> seen = Collections.synchronizedSet(new HashSet<>());
    private static final URLQueue queue = new SynchronizedListQueue();  // Use SynchronizedListQueue
    public static void iterativeDownload(String initialURL) {
        DocumentProcessing.handler = url -> {
            if (seen.add(url)) { // This will return false if the URL is already in the set
                queue.enqueue(url);
                System.out.println("Processing URL: " + url);
            }
        };

        DocumentProcessing.handler.takeUrl(initialURL);
        int maxIterations = 10;
        int currentIteration = 0;
        while (!queue.isEmpty() && currentIteration < maxIterations) {
            String url = queue.dequeue();
            Xurl.download(url);
            System.out.println("Processing URL: " + url);
            currentIteration++;
        }
    }
    public static  void print(){System.out.println("HHHH");}
    public static void multiThreadedDownload(String initialURL) {
        DocumentProcessing.handler = new DocumentProcessing.URLhandler() {
            @Override
            public synchronized void takeUrl(String url) {
                if (seen.add(url)) {
                    queue.enqueue(url);
                    System.out.println("Processing URL: " + url);
                }
            }
        };

        DocumentProcessing.handler.takeUrl(initialURL);

        int threadCount = 0;  // to give a unique name to each thread

        while (true) {
            if (!queue.isEmpty()) {
                String url = queue.dequeue();
                Thread downloadThread;  // setting custom thread name
                System.out.println("FIRST"+Thread.activeCount());
                downloadThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Xurl.download(url);
                    }
                }, "DownloadThread-" + threadCount++);
                downloadThread.start();
                System.out.println(downloadThread + " is downloading " + url);
            } else {
                while(Thread.activeCount()>2){
                try {
                    Thread.sleep(1000);  // Sleep for a while if the queue is empty
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                }
            }
            System.out.println(Thread.activeCount());
            System.out.println(queue.isEmpty());
            if (queue.isEmpty() && Thread.activeCount()<=2) {
                System.out.println(Thread.activeCount());
                System.out.println("Main is out");
                break;
            }
        }
    }

    @SuppressWarnings("unused")
    public static void threadPoolDownload(int poolSize, String initialURL) {
        // to be completed later
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Wget url");
            System.exit(-1);
        }
        multiThreadedDownload(args[0]);  // Call multiThreadedDownload for testing
    }
}
