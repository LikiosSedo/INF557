package LAB_01;

import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Xurl_2a {

    // Constant representing the User-Agent string to impersonate browser requests
    private static final String USER_AGENT = "Mozilla/5.0";

    /**
     * For 2a
     * Main entry point of the program.
     * The program expects a single URL as a command-line argument.
     * It fetches content from this URL, handles redirects, and saves the content to a local file.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        // Ensure that the program receives exactly one argument (the URL)
        if (args.length != 1) {
            System.err.println("Usage: java Xurl <URL>");
            System.exit(1);
        }

        String urlString = args[0];

        try {
            // Initialize our custom URL object with the given string
            MyURL myURL = new MyURL(urlString);
            // Fetch content from the provided URL and save it to a file
            fetchAndSaveContent(myURL);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Fetches content from the specified URL. It follows redirects (up to a maximum of 5)
     * and saves the final content to a file.
     *
     * @param myURL The custom URL object containing the target URL details.
     * @throws IOException If an I/O error occurs during the fetch operation.
     */
    private static void fetchAndSaveContent(MyURL myURL) throws IOException {
        int redirectCount = 0;  // Counter to track the number of redirects encountered

        // Continuously fetch the content until either the content is fetched successfully or
        // the maximum redirect limit (5) is reached
        while (redirectCount < 5) {
            try (
                    // Open a connection to the specified host and port
                    Socket socket = new Socket(myURL.getHost(), myURL.getPort() == -1 ? 80 : myURL.getPort());
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    InputStream in = socket.getInputStream()
            ) {
                // Sequence of bytes indicating the end of HTTP headers
                byte[] endOfHeaders = "\r\n\r\n".getBytes();
                byte[] bufferLast4Bytes = new byte[4];

                // Compose and send the HTTP GET request
                String request = buildRequest(myURL);
                out.print(request);
                out.flush();

                // Collect the HTTP headers from the response
                ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
                int byteRead;
                while (true) {
                    byteRead = in.read();
                    if (byteRead == -1) {
                        break;
                    }
                    headerBuffer.write(byteRead);
                    // Check if the end of headers is reached
                    if (headerBuffer.size() >= 4) {
                        byte[] last4Bytes = headerBuffer.toByteArray();
                        System.arraycopy(last4Bytes, last4Bytes.length - 4, bufferLast4Bytes, 0, 4);
                        if (Arrays.equals(bufferLast4Bytes, endOfHeaders)) {
                            break;
                        }
                    }
                }

                // Process headers to decide on subsequent actions (e.g., follow redirects, read body)
                String headers = headerBuffer.toString();
                String[] statusLine = headers.split("\r\n", 2);
                int statusCode = Integer.parseInt(statusLine[0].split(" ")[1]);

                if (statusCode == 301 || statusCode == 302) {
                    // Handle redirects by extracting the new URL and looping back to fetch the content
                    String locationHeader = Arrays.stream(headers.split("\r\n"))
                            .filter(h -> h.toLowerCase().startsWith("location:"))
                            .findFirst()
                            .orElse(null);

                    if (locationHeader != null) {
                        String newUrl = locationHeader.split(": ", 2)[1];
                        myURL = new MyURL(newUrl);
                        redirectCount++;
                        continue;
                    }
                } else if (statusCode >= 400 && statusCode < 500) {
                    // Handle client errors by throwing an exception
                    throw new IOException(statusLine[0]);
                }

                // Read the body of the HTTP response
                ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
                String contentLengthHeader = Arrays.stream(headers.split("\r\n"))
                        .filter(h -> h.toLowerCase().startsWith("content-length:"))
                        .findFirst()
                        .orElse(null);

                if (contentLengthHeader != null) {
                    // If content length is specified, read the exact number of bytes
                    int contentLength = Integer.parseInt(contentLengthHeader.split(":")[1].trim());
                    for (int i = 0; i < contentLength; i++) {
                        byteRead = in.read();
                        if (byteRead == -1) {
                            break;
                        }
                        bodyBuffer.write(byteRead);
                    }
                } else {
                    // If content length isn't specified, read until the end of the stream
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        bodyBuffer.write(buffer, 0, bytesRead);
                    }
                }

                // Save the fetched content to a local file
                saveResponseToFile(myURL, bodyBuffer.toString());
                break;
            }
        }
    }

    /**
     * Constructs an HTTP GET request for the specified URL.
     *
     * @param myURL The custom URL object containing the target URL details.
     * @return The full HTTP GET request as a string.
     */
    private static String buildRequest(MyURL myURL) {
        StringBuilder request = new StringBuilder();
        request.append("GET ").append(myURL.getPath()).append(" HTTP/1.1\r\n");
        request.append("Host: ").append(myURL.getHost());
        if (myURL.getPort() != -1) {
            request.append(":").append(myURL.getPort());
        }
        request.append("\r\n");
        request.append("User-Agent: ").append(USER_AGENT).append("\r\n");
        request.append("\r\n");  // Indicates the end of the HTTP headers
        return request.toString();
    }

    /**
     * Saves the fetched content to a local file. The filename is derived from the URL.
     * If no specific filename is provided in the URL, "index" is used as a default.
     *
     * @param myURL    The custom URL object.
     * @param response The content fetched from the URL.
     * @throws IOException If there's an issue writing to the file.
     */
    private static void saveResponseToFile(MyURL myURL, String response) throws IOException {
        String path = myURL.getPath();
        String filename = path.substring(path.lastIndexOf('/') + 1);

        if (filename.isEmpty()) {
            filename = "index";
        }

        try (FileWriter fileWriter = new FileWriter(filename)) {
            fileWriter.write(response);
        }
    }
}
