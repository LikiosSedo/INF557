package LAB_02and03;

import java.util.HashSet;

public class Wget_iterativeDownload {

    public static void iterativeDownload(String initialURL) {
        final URLQueue queue = new ListQueue();
        final HashSet<String> seen = new HashSet<String>();
        // defines a new URLhandler
        DocumentProcessing.handler = new DocumentProcessing.URLhandler() {
            @Override
            public void takeUrl(String url) {
                if (!seen.contains(url)) {  // Check if the URL has been seen before
                    seen.add(url);          // Mark the URL as seen
                    queue.enqueue(url);     // Add the URL to the queue for downloading
                    System.out.println("Processing URL: " + url);
                }
            }
        };

        // to start, we push the initial url into the queue
        DocumentProcessing.handler.takeUrl(initialURL);
        int maxIterations = 10;  // Set this to a suitable limit.
        int currentIteration = 0;
        while (!queue.isEmpty() && currentIteration < maxIterations) {
            String url = queue.dequeue();
            Xurl.download(url); // don't change this line
            currentIteration++;
        }
    }

    @SuppressWarnings("unused")
    public static void multiThreadedDownload(String initialURL) {
        // to be completed later
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
        //iterativeDownload("http://www.columbia.edu/~fdc/sample.html");
        iterativeDownload(args[0]);
    }
}
