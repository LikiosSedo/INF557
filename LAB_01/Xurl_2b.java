package LAB_01;

import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Xurl_2b {

    // Define the user agent string that will be used in the HTTP request header.
    private static final String USER_AGENT = "Mozilla/5.0";

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

    private static void fetchAndSaveContent(MyURL myURL, String proxyName, int proxyPort) throws IOException {
        // Determine which host and port to connect to (either the target server or the proxy server).
        String hostToConnect = proxyName == null ? myURL.getHost() : proxyName;
        int portToConnect = proxyPort == -1 ? (myURL.getPort() == -1 ? 80 : myURL.getPort()) : proxyPort;

        try (Socket socket = new Socket(hostToConnect, portToConnect);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             InputStream in = socket.getInputStream()) {

            // Build the HTTP request.
            String request = buildRequest(myURL, proxyName);
            // Send the request to the server.
            out.print(request);
            out.flush();

            // Prepare to parse HTTP headers.
            byte[] endOfHeaders = "\r\n\r\n".getBytes();
            byte[] bufferLast4Bytes = new byte[4];
            ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
            int byteRead;
            // Read headers until the double CRLF sequence is found.
            while (true) {
                byteRead = in.read();
                if (byteRead == -1) {
                    break;
                }
                headerBuffer.write(byteRead);
                if (headerBuffer.size() >= 4) {
                    byte[] last4Bytes = headerBuffer.toByteArray();
                    System.arraycopy(last4Bytes, last4Bytes.length - 4, bufferLast4Bytes, 0, 4);
                    if (Arrays.equals(bufferLast4Bytes, endOfHeaders)) {
                        break;
                    }
                }
            }

            // Extract and handle HTTP status code.
            String headers = headerBuffer.toString();
            String[] statusLine = headers.split("\r\n", 2);
            int statusCode = Integer.parseInt(statusLine[0].split(" ")[1]);
            if (statusCode >= 400 && statusCode < 500) {
                throw new IOException(statusLine[0]);
            }

            // Check if response is chunked or has a Content-Length
            String transferEncodingHeader = Arrays.stream(headers.split("\r\n"))
                    .filter(h -> h.toLowerCase().startsWith("transfer-encoding:"))
                    .findFirst()
                    .orElse(null);
            String contentLengthHeader = Arrays.stream(headers.split("\r\n"))
                    .filter(h -> h.toLowerCase().startsWith("content-length:"))
                    .findFirst()
                    .orElse(null);

            ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
            if (transferEncodingHeader != null && "chunked".equals(transferEncodingHeader.split(":")[1].trim().toLowerCase())) {
                // Handling chunked transfer encoding
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while (true) {
                    line = reader.readLine();
                    if (line.equals("0")) {
                        reader.readLine(); // Read the trailing CRLF after the last chunk size
                        break;
                    }
                    System.out.println("Reading chunk size from line: " + line);
                    int chunkSize;
                    try {
                        chunkSize = Integer.parseInt(line, 16);
                    } catch (NumberFormatException e) {
                        throw new IOException("Invalid chunk size: " + line);
                    }
                    byte[] chunkData = new byte[chunkSize];
                    int bytesRead;
                    int offset = 0;
                    while (offset < chunkSize && (bytesRead = in.read(chunkData, offset, chunkSize - offset)) != -1) {
                        offset += bytesRead;
                    }
                    bodyBuffer.write(chunkData);
                    reader.readLine(); // Reading and discarding the trailing CRLF after each chunk
                }
                reader.readLine(); // Reading and discarding the last CRLF after the last chunk
            }
            else if (contentLengthHeader != null) {
                int contentLength = Integer.parseInt(contentLengthHeader.split(":")[1].trim());
                for (int i = 0; i < contentLength; i++) {
                    byteRead = in.read();
                    if (byteRead == -1) {
                        break; // Unexpected EOF
                    }
                    bodyBuffer.write(byteRead);
                }
            } else {
                // If neither chunked nor Content-Length, read until EOF
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    bodyBuffer.write(buffer, 0, bytesRead);
                }
            }

            System.out.println(headers);

            // Save the response to a file.
            String charset = "ISO-8859-1"; // default charset for HTTP
            String contentTypeHeader = Arrays.stream(headers.split("\r\n"))
                    .filter(h -> h.toLowerCase().startsWith("content-type:"))
                    .findFirst()
                    .orElse(null);
            if (contentTypeHeader != null && contentTypeHeader.contains("charset=")) {
                charset = contentTypeHeader.split("charset=")[1].trim();
            }
            saveResponseToFile(myURL, bodyBuffer.toString(charset));
        }
    }




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

        return request.toString();
    }

    private static void saveResponseToFile(MyURL myURL, String response) throws IOException {
        // Extract the filename from the URL.
        String path = myURL.getPath();
        String filename = path.substring(path.lastIndexOf('/') + 1);

        // Default to "index" if no filename is found.
        if (filename.isEmpty()) {
            filename = "index";
        }

        // Write the response to the file.
        try (FileWriter fileWriter = new FileWriter(filename)) {
            fileWriter.write(response);
        }

        System.out.println("Response saved to: " + filename);
    }
}