package LAB_04;
import java.io.*;
import java.net.*;

/**
 * A simple single-threaded web server.
 */
class Xserver_02 {


    /**
     * Sends a welcome message to the client.
     *
     * @param out The output stream to write the message to.
     */
    static void sendWelcomeMessage(PrintWriter out) {
            String welcomeMessage = "Welcome to Xserver!";
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Length: " + (welcomeMessage.length()+1));
         //   out.println("Connection: keep-alive"); stop it
            out.println();
            out.println(welcomeMessage);
            out.flush();
    }

    /**
     * Sends a requested file to the client.
     *
     * @param out The output stream to write the file to.
     * @param rootDir The root directory containing the files.
     * @param pathToFile The path of the requested file.
     */
    static void sendRequestedFile(PrintWriter out, String rootDir, String pathToFile) {
        File file = new File(rootDir + pathToFile);
        if (file.exists() && !file.isDirectory()) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(file.toPath()), "UTF-8");
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Length: " + content.length());
                //out.println("Connection: keep-alive"); //delete this
                out.println();
                out.println(content);
            }
            catch (FileNotFoundException e) {
                System.out.println(rootDir+pathToFile+" not found");
                //sendNotFound(out);
                e.printStackTrace();
            }
            catch (IOException e) {
                System.out.println("Error reading requested file: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            sendNotFound(out);
        }
    }


    /**
     * Sends a "400 Bad Request" response to the client.
     *
     * @param out The output stream to write the error message to.
     */
    static void sendBadRequest(PrintWriter out) {
            String errorMessage = "<html><body><h1>Bad Request</h1></body></html>";
            out.println("HTTP/1.1 400 Bad Request");
            out.println("Content-Length: " + (errorMessage.length()+1));//+1
            out.println();
            out.println(errorMessage);
            out.flush(); //fix with flush
    }

    /**
     * Sends a "404 Not Found" response to the client.
     *
     * @param out The output stream to write the error message to.
     */
    static void sendNotFound(PrintWriter out) {
        String errorMessage = "<html><body><h1>File Not Found</h1></body></html>";
        out.println("HTTP/1.1 404 Not Found");
        out.println("Content-Length: " + (errorMessage.length()+1));//+1
        out.println();
        out.println(errorMessage);
        out.flush(); //fix with flush
    }
    /**
     * The main entry point for the server.
     *
     * @param args Command line arguments containing the server port and root directory.
     * @throws IOException If an I/O error occurs when opening the socket.
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: java Xserver <serverPort> <rootDir>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        String rootDir = args[1];

        try (ServerSocket serverSocket = new ServerSocket(port, 50)) {
            System.out.println("Server started on port " + port);

            while (true)
            {
                try (Socket clientSocket = serverSocket.accept()) {
                    handleConnection(clientSocket, rootDir);
                }
                catch (IOException e) {
                    System.out.println("Error handling client connection: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        catch (IOException Se) {
            System.out.println("Error starting the server: " + Se.getMessage());
            Se.printStackTrace();
        }

    }
    /**
     * Handles an incoming client connection.
     *
     * @param socket The client socket.
     * @param rootDir The root directory from which to serve files.
     */
    static void handleConnection(Socket socket, String rootDir)  {
        try {
            socket.setSoTimeout(5000);  // 5 seconds timeout

            // Using try-with-resources to automatically close the resources
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                while (true) {
                    String line = in.readLine();

                    if (line == null || !line.startsWith("GET")) {
                        sendBadRequest(out);
                        return;
                    }

                    String[] parts = line.split(" ");
                    if (parts.length != 3 || !parts[2].equals("HTTP/1.1")) {
                        sendBadRequest(out);
                        return;
                    }

                    String pathToFile = parts[1];

                    line = in.readLine();
                    if (line == null || !line.startsWith("Host: ")) {
                        sendBadRequest(out);
                        return;
                    }

                    boolean closeConnection = false;
                    while (!(line = in.readLine()).isEmpty()) {
                        if (line.equalsIgnoreCase("Connection: close")) {
                            closeConnection = true;
                        }
                    }

                    if (pathToFile.equals("/")) {
                        sendWelcomeMessage(out);
                    } else {
                        sendRequestedFile(out, rootDir, pathToFile);
                    }

                    if (closeConnection) {
                        break;
                    }
                }
            }
            catch (SocketTimeoutException ste) {
                ste.printStackTrace();
                System.out.println("Connection timed out. Closing connection.");
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException ioException) {
                    System.out.println("Error while closing the socket.");
                    ioException.printStackTrace();
                }
            }
        }
        catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
