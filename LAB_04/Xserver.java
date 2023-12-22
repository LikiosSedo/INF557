package LAB_04;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The SingleThread class is responsible for handling individual client HTTP requests.
 * Each instance of this class operates in a separate thread and serves one client request.
 * This class is capable of parsing HTTP requests, processing them, and sending appropriate responses back to the client.
 * The primary functionality includes reading HTTP headers, determining the type of request,
 * serving the appropriate files or resources from the server's root directory, and handling potential errors.
 */
class SingleThread implements Runnable{
    /** The socket connection with the client. */
    private Socket socket;

    /** The root directory from which files are to be served. */
    private String rootDir;

    /** Input stream to read data from the client. */
    private InputStreamReader is = null;

    /** Output stream to send data to the client. */
    private OutputStream os = null;

    /** Convenient print stream for writing textual data to the client. */
    PrintStream printStream = null;

    /** Buffered reader to efficiently read textual data from the client. */
    private BufferedReader buffer;

    /** Path to the file requested by the client. */
    private String path_to_file;

    /** Flag to track if the client request has the GET method. */
    private boolean flagGET;

    /** Flag to track if the client request has a valid Host header. */
    private boolean flagHOST;

    /** Flag to indicate if the client requested a specific path/file or just the root. */
    private boolean flagPath;

    /** Flag to keep track if the reading from the client is still valid or not. */
    private boolean readFlag=true;

    SingleThread(Socket socket,String rootDir) {
        this.socket=socket;
        this.rootDir=rootDir;
    }

    /**
     * Sends a "400 Bad Request" response to the client.
     * This method is typically called when the server encounters a malformed request.
     */
    public void BadRequest(){
        // Constructing the error message to send in the response.
        String errorMessage = "<html><body><h1>Bad Request</h1></body></html>";

        // Computing the content length for the header.
        long len = errorMessage.length();
        len = len + 1;  // adjusting the length

        // Sending the response headers and body to the client.
        String stateCode = "HTTP/1.1 400 Bad Request";
        printStream.println(stateCode);
        printStream.println("Content-Length: "+len);
        printStream.println("");
        printStream.println(errorMessage);

        // Flushing to ensure the data is sent immediately.
        printStream.flush();

        // Logging the bad request for debugging purposes.
        System.out.println("bad request");
    }

    /**
     * Sends a "404 Not Found" response to the client.
     * This method is typically called when the requested file/resource is not found on the server.
     */
    public void NotFound() {
        // Constructing the error message to send in the response.
        String errorMessage = "<html><body><h1>File Not Found</h1></body></html>";

        // Computing the content length for the header.
        long len = errorMessage.length();
        len = len + 1;  // adjusting the length

        // Sending the response headers and body to the client.
        String stateCode = "HTTP/1.1 404 Not Found";
        printStream.println(stateCode);
        printStream.println("Content-Length: "+len);
        printStream.println("");
        printStream.println(errorMessage);

        // Flushing to ensure the data is sent immediately.
        printStream.flush();

        // Logging the not found error for debugging purposes.
        System.out.println("Not Found");
    }

    /**
     * Sends a "200 OK" response to the client.
     * This method is typically called when the request is successfully processed by the server.
     */
    public void SuccessfulRequest(){
        // Constructing the successful message to send in the response.
        String context = "<html><body><h1>Welcome to Xserver!</h1></body></html>";

        // Computing the content length for the header.
        long len = context.length();
        len = len + 1;  // adjusting the length

        // Sending the response headers and body to the client.
        String stateCode = "HTTP/1.1 200 ok";
        printStream.println(stateCode);
        printStream.println("Content-Length: "+len);
        printStream.println("");
        printStream.println(context);

        // Flushing to ensure the data is sent immediately.
        printStream.flush();
    }


    /**
     * Checks the Host header from the client's HTTP request.
     * @param host_line The line from the HTTP request that contains the Host header.
     */
    public void CheckHost(String host_line) {
        // Define the regex pattern to capture the Host header value.
        String regex = "Host:\\s+(?<host>[^\\s]*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(host_line);

        if (matcher.find()) {
            System.out.println("Host correct");
            flagHOST = true;
        } else {
              BadRequest(); // Send Bad Request response if Host header is incorrect.
            readFlag = false;
        }
    }

    /**
     * Checks the GET line from the client's HTTP request to extract the path to the requested file.
     * @param get_line The line from the HTTP request that contains the GET verb.
     */
    public void CheckGET(String get_line) {
        // Define the regex pattern to capture the GET request details.
        String regex = "GET\\s+([^\\s]+)\\s+HTTP\\/1\\.1";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(get_line);

        if (matcher.find()) {
            // If the path is just '/', it means the root directory is being accessed.
            if(matcher.group(1).equals("/")) {
                flagPath = false;
            } else {
                path_to_file = matcher.group(1); // Extract the requested file path.
                flagPath = true;
            }
            System.out.println("GET correct");
            flagGET = true;
        } else {
            //  BadRequest(); // Send Bad Request response if GET line is incorrect.
            readFlag = false;
        }
    }

    /**
     * Attempts to find the requested file in the server's root directory.
     * If found, the file's content is written to the client's output stream; otherwise, a 404 Not Found response is sent.
     */
    public void findFile() {
        // Construct the full path to the requested file.
        String finalPath = rootDir + path_to_file;
        File file = new File(finalPath);

        FileInputStream in = null;
        long len = file.length();

        if (file.exists()) { // Check if the file exists in the server directory.
            try {
                in = new FileInputStream(file); // Open a stream to read the file.
                byte[] bytes = new byte[1024 * 1024]; // Buffer to hold file data.

                // Send the response headers.
                String stateCode = "HTTP/1.1 200 ok";
                printStream.println(stateCode);
                printStream.println("Content-Length: " + len);
                printStream.println("");

                // Read the file and write its content to the client's output stream.
                int readCount;
                while ((readCount = in.read(bytes)) != -1) {
                    os.write(bytes, 0, readCount);
                }
                os.flush(); // Ensure all data is sent to the client.
            } catch (FileNotFoundException e) {
                System.out.println(finalPath + " not found");
                e.printStackTrace();
                // Here is the point I struggled in, I should add a throw exception
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                // Close the FileInputStream, but leave the output stream open.
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        } else {
            // If the file doesn't exist, send a 404 Not Found response.
            NotFound();
        }
    }


    /**
     * Closes the active connection by shutting down all I/O streams and the socket.
     */
    public void closeConnection() {
        try {
            // Close the output stream.
            os.close();
            // Close the input stream reader.
            is.close();
            // Close the buffered reader.
            buffer.close();
            // Close the output stream .
            printStream.close();
            // Close the socket connection.
            socket.close();


            System.out.println("Connection closed!\n");
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the logic for parsing and responding to HTTP requests.
     * This method is expected to be executed in its own thread for every new connection.
     */
    public void run() {
        try {
            // Initialize the output stream associated with the socket.
            os = socket.getOutputStream();
            printStream = new PrintStream(os);

            // Initialize the input stream reader associated with the socket.
            is = new InputStreamReader(socket.getInputStream());
            buffer = new BufferedReader(is);

            String read_line;

            // Reset flags.
            flagGET = false;
            flagHOST = false;
            readFlag = true;
            flagPath = false;

            // Parse the incoming HTTP request.
            while ((read_line = buffer.readLine()) != null && !socket.isClosed()) {
                if (!flagGET) {
                    CheckGET(read_line);  // Parse the GET line.
                }
                else if (!flagHOST) {
                    CheckHost(read_line); // Parse the Host header.
                }

                // A blank line indicates the end of the HTTP headers.
                if (flagHOST && flagGET && read_line.isEmpty()) {
                    if (flagPath == false) {
                        System.out.println("successful");
                        SuccessfulRequest(); // Send the welcome message.
                    } else {
                        System.out.println("go to the rootDir");
                        findFile(); // Attempt to find and send the requested file.
                    }

                    // Reset flags for a potential new request on the same connection.
                    flagGET = false;
                    flagHOST = false;
                    readFlag = true;
                    flagPath = false;
                }

                // If there was an error, close the connection.
                if (readFlag == false) {
                    BadRequest(); // Send Bad Request response if Host header is incorrect.
                    //closeConnection();
                    break; // Terminate the while loop.
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

}


/**
 * The Xserver class is a multi-threaded server that listens for client connections
 * on a specified port and serves their requests using a thread pool.
 *
 * Usage: java Xserver serverPort rootDir [nThreads]
 *
 * Where:
 * - serverPort: The port on which the server should listen.
 * - rootDir: The root directory for the server.
 * - nThreads (optional): The number of threads in the thread pool. Default is 1.
 *
 * Each client connection is processed by an instance of the SingleThread class.
 */
public class Xserver {
    /** The port on which the server will listen. */
    public static int serverPort;

    /** The root directory for the server. */
    public static String rootDir;

    /** The ServerSocket object responsible for accepting client connections. */
    public static ServerSocket server;

    /**
     * Handles a new client connection by submitting a task to the thread pool.
     *
     * @param socket The client socket representing the connection.
     * @param pool The thread pool to which the task is submitted.
     */
    public static void handleConnection(Socket socket, ExecutorService pool) {
        // Submit a new task (client request) to the thread pool.
        pool.submit(new SingleThread(socket, rootDir));
    }

    /**
     * The main method responsible for starting the server, listening for client
     * connections, and processing them using a thread pool.
     *
     * @param args The command-line arguments.
     */
    public static void main(String[] args) {
        // Check for the correct number of command-line arguments.
        if (args.length < 2) {
            System.err.println("Usage: java Xserver serverPort rootDir [nThreads]");
            System.exit(-1);
        }

        // Parse the server port and root directory from the command-line arguments.
        serverPort = Integer.parseInt(args[0]);
        rootDir = args[1];

        // Fetch the number of threads from arguments or default to 1.
        int nThreads = (args.length > 2) ? Integer.parseInt(args[2]) : 1;

        // Create a thread pool with the specified number of threads.
        ExecutorService pool = Executors.newFixedThreadPool(nThreads);

        try {
            // Start the server on the specified port.
            server = new ServerSocket(serverPort);
            System.out.println("The server is listening....");

            // Continuously listen for client connections.
            while (true) {
                Socket client = server.accept();
                System.out.println("Correctly connected a client!");

                // Handle the client connection using the thread pool.
                handleConnection(client, pool);
            }
        } catch (IOException e) {
            // Print any IO exceptions.
            e.printStackTrace();
        } finally {
            // Ensure the thread pool is gracefully shut down when exiting.
            if (pool != null) {
                pool.shutdown();
            }
        }
    }
}


