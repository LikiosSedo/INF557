package LAB_01;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Xurl_2c {

    // Constant representing the User-Agent string to impersonate browser requests
    private static final String USER_AGENT = "Mozilla/5.0";
    private static List<String> LISTOFBUFFER = new ArrayList<>();

    /**
     * Main entry point of the program.
     * The program expects a single URL as a command-line argument.
     * It fetches content from this URL, handles redirects, and saves the content to a local file.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        // Check for the correct number of arguments.
        if (args.length < 1) {
            System.err.println("Usage: java Xurl <URL> [proxyName [proxyPort]]");
            System.exit(1);
        }

        // Extract the URL from the arguments.
        String urlString = args[0];
        // Initialize proxy details.
        String proxyName = null;
        int proxyPort = -1;

        // Extract the proxy name if provided.
        if (args.length >= 2) {
            proxyName = args[1];
        }

        // Extract the proxy port if provided.
        if (args.length == 3) {
            proxyPort = Integer.parseInt(args[2]);
        }
        try {
            // Create a custom URL object from the provided string.
            MyURL myURL = new MyURL(urlString);
            // Attempt to fetch the content and save it to a file.
            fetchAndSaveContent(myURL, proxyName, proxyPort);
        } catch (Exception e) {
            // Handle any exception that arises.
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static boolean isChunkedTransfer(String headers) {
        return Arrays.stream(headers.split("\r\n"))
                .anyMatch(h -> h.toLowerCase().equals("transfer-encoding: chunked"));
    }

    /**
     * Fetches content from the specified URL. It follows redirects (up to a maximum of 5)
     * and saves the final content to a file.
     *
     * @throws IOException If an I/O error occurs during the fetch operation.
     */
    private static void fetchAndSaveContent(MyURL myURL,String proxyName, int proxyPort) throws IOException {
        int redirectCount = 0;  // Counter to track the number of redirects encountered

        // Continuously fetch the content until either the content is fetched successfully or
        // the maximum redirect limit (5) is reached
        String hostToConnect = proxyName == null ? myURL.getHost() : proxyName;
        int portToConnect = proxyPort == -1 ? (myURL.getPort() == -1 ? 80 : myURL.getPort()) : proxyPort;
        while (redirectCount < 5) {
            try (
                    // Open a connection to the specified host and port
                    Socket socket = new Socket(hostToConnect, portToConnect);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    InputStream in = socket.getInputStream()

            ) {
                LISTOFBUFFER.clear();// if redirect happens, clear the buffer lines
                BufferedReader BufferIn = new BufferedReader(new InputStreamReader(in));
                socket.setSoTimeout(20000);

                // Sequence of bytes indicating the end of HTTP headers
                byte[] endOfHeaders = "\r\n\r\n".getBytes();
                byte[] bufferLast4Bytes = new byte[4];

                // Compose and send the HTTP GET request
                String request = buildRequest(myURL, proxyName);
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
                System.out.println(headers);
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

                String charset = "UTF-8"; // default value

                String contentTypeHeader = Arrays.stream(headers.split("\r\n"))
                        .filter(h -> h.toLowerCase().startsWith("content-type:"))
                        .findFirst()
                        .orElse(null);

                if (contentTypeHeader != null) {
                    String[] parts = contentTypeHeader.split(";");
                    for (String part : parts) {
                        part = part.trim();
                        if (part.toLowerCase().startsWith("charset=")) {
                            charset = part.split("=")[1].trim();
                            break;
                        }
                    }
                }

                if (contentLengthHeader != null) {
                    // If content length is specified, read the exact number of bytes
                    int contentLength = Integer.parseInt(contentLengthHeader.split(":")[1].trim());
                    String linein;
                    int byteread = 0;
                    while((linein = BufferIn.readLine()) != null)
                    {
                        LISTOFBUFFER.add(linein);
                        byteread += linein.getBytes().length +1;
                        if(contentLength<=byteread)
                        {
                            break;
                        }
                    }

                }
                else if (isChunkedTransfer(headers)) {
                    String linein;
                    while (true) {
                        linein = BufferIn.readLine();
                        if(linein == null) break;
                        int size;
                        try{
                            size = Integer.parseInt(linein.trim(),16);
                            System.out.println(size);
                        }
                        catch (NumberFormatException e)
                        {
                            throw new IOException("size is not valid");
                        }

                        if (size == 0) {
                            break;
                        }

                        char[] buffer = new char[size];
                        int bytesRead = 0;
                        while (bytesRead < size) {
                            int read = BufferIn.read(buffer, bytesRead, size - bytesRead);
                            bytesRead += read;
                        }
                        LISTOFBUFFER.add(new String(buffer,0,bytesRead));
                        //System.out.println(new String(buffer,0,bytesRead));

                        String trailingLine = BufferIn.readLine();  // read the trailing empty line
                    }
                }
                else {
                    // If content length isn't specified and not chunked transfer, read until the end of the stream
                    int bytesRead;
                    String linein;
                    while((linein = BufferIn.readLine()) != null) {
                        LISTOFBUFFER.add(linein);
                        if(linein.isEmpty()) break;
                    }
                }

                // Save the fetched content to a local file
                saveResponseToFile(myURL,LISTOFBUFFER);

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
    private static String buildRequest(MyURL myURL, String proxyName) {
        StringBuilder request = new StringBuilder();

        // If using a proxy, the full URL should be used in the request line.
        if (proxyName == null) {
            request.append("GET ").append(myURL.getPath()).append(" HTTP/1.1\r\n");
        } else {
            request.append("GET ").append(myURL.toString()).append(" HTTP/1.1\r\n");
        }

        // Add necessary headers.
        request.append("Host: ").append(myURL.getHost());
        if (myURL.getPort() != -1) {
            request.append(":").append(myURL.getPort());
        }
        request.append("\r\n");
        request.append("User-Agent: ").append(USER_AGENT).append("\r\n");
        request.append("\r\n");
        System.out.println(request.toString());
        return request.toString();
    }

    /**
     * Saves the fetched content to a local file. The filename is derived from the URL.
     * If no specific filename is provided in the URL, "index" is used as a default.
     *
     * @param myURL    The custom URL object.
     * @param LISTOFBUFFER The content fetched from the URL.
     * @throws IOException If there's an issue writing to the file.
     */
    private static void saveResponseToFile(MyURL myURL, List<String> LISTOFBUFFER) throws IOException {
        String path = myURL.getPath();
        String filename = path.substring(path.lastIndexOf('/') + 1);

        if (filename.isEmpty()) {
            filename = "index";
        }

        try (PrintStream File = new PrintStream(new File(filename))) {
            for(String line : LISTOFBUFFER)
            {
                File.println(line);
                System.out.println(line);
            }
        }
    }
}