package LAB_05to07;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Represents a handler for managing connections using a handshake mechanism.
 * It facilitates sending hello messages to initiate a connection and awaits acknowledgments.
 * Additionally, it manages sending, receiving, and processing messages with a reliable mechanism.
 */
public class ConnectedHandler_Hello extends Handler {

    /**
     * Generates a unique identifier for connections or messages.
     *
     * @return an integer representing the unique ID.
     */
    public static int getUniqueID() {
        return (int) (Math.random() * Integer.MAX_VALUE);
    }

    // Constants representing types of messages.
    private static final String HELLO = "--HELLO--";
    private static final String ACK = "--ACK--";

    // Configuration for retry mechanism.
    private static final int DELAY = 500;
    private static final int MAX_REPEAT = 20;

    // Timer for handling periodic message sends.
    private static final Timer TIMER = new Timer("ConnectedHandler's Timer", true);

    // Instance variables for tracking connection state.
    private final int localId;             // ID for the local connection.
    private final String destination;      // Destination address for communication.
    private Handler aboveHandler;          // The upper layer handler.

    // State variables for connection management.
    private int remoteId = -1;             // ID of the remote connection.
    private int packetNumber = 0;
    private int packetNumberSender = 0;
    private Object mutex = new Object(); // used to block for ACK
    private Object hello_mutex = new Object(); // used to block for initial HELLO
    private TimerTask task;
    /**
     * Constructor to set up the connected handler.
     *
     * @param _under       the underlying handler layer.
     * @param _localId     the local connection ID.
     * @param _destination the destination address for communication.
     */
    public ConnectedHandler_Hello(final Handler _under, int _localId, String _destination) {
        super(_under, _localId, true);
        this.localId = _localId;
        this.destination = _destination;

        send(HELLO);
        if (this.remoteId == -1) { // If Hello has not been received yet
            synchronized (hello_mutex) {
                try {
                    hello_mutex.wait();
                } catch (InterruptedException e) {
                    System.out.println("Interrupted while waiting for HELLO");
                }
            }
        }
    }
    /**
     * Binds an upper layer handler to this handler.
     *
     * @param above the upper layer handler.
     */
    @Override
    public void bind(Handler above) {
        if (!this.upsideHandlers.isEmpty())
            throw new IllegalArgumentException(
                    "cannot bind a second handler onto this " + this.getClass().getName());
        this.aboveHandler = above;
        super.bind(above);
    }
    /**
     * Handles incoming messages by parsing their payload and taking appropriate actions.
     * This method can identify and act on HELLO messages (connection initializations),
     * ACK messages (acknowledgments of received messages), or regular messages.
     *
     * @param message The received message to be processed.
     */
    @Override
    public void handle(Message message) {
        // Split the payload into its constituent parts.
        String[] parts = message.payload.split(";");

        // Ensure the message has the expected format.
        if (parts.length != 4) return;
        try {
        // Extract key parts of the message.
        int rc_senderId = Integer.parseInt(parts[0]);
        int rc_destinationId = Integer.parseInt(parts[1]);
        int rc_packageID = Integer.parseInt(parts[2]);
        String flag = parts[3];

        // Check if this is an initial HELLO message and if we haven't set the remoteId yet.
        if (flag.equals(HELLO)) {
            if (rc_destinationId == -1 && rc_packageID == 0 && rc_senderId >= 0) {
                remoteId = rc_senderId;
                packetNumberSender = rc_packageID;
                synchronized (hello_mutex) {
                    hello_mutex.notifyAll();
                }
                // Not using send because an ACK isn't answered for an ACK
                String payload = localId + ";" + remoteId + ";" + packetNumberSender + ";" + ACK;
                downside.send(payload, destination);
            }
        }
        else if (flag.equals(ACK)) {
            if (rc_senderId == remoteId && rc_destinationId == localId && rc_packageID == packetNumber) {
                synchronized (mutex) {
                    mutex.notifyAll();
                }
                task.cancel();
            }
            // If it's an acknowledgment from the expected sender, update the status and notify any waiting threads.
        }
        }
       catch (NumberFormatException | IndexOutOfBoundsException e) {
            System.out.println("Incorrect message received");
            return;
        }
    }

    /**
     * Sends a message by constructing its payload and then repeatedly trying to send it until an acknowledgment is received.
     *
     * @param payload The core content of the message to be sent.
     */
    @Override
    public void send(final String payload) {
        // Construct the full message with headers and payload.
        String message = localId + ";" + remoteId + ";" + packetNumber + ";" + payload;
        task = new TimerTask() {
            private int repeat = 0;

            @Override
            public void run() {
                if (payload.equals(HELLO) || repeat < MAX_REPEAT) {
                    downside.send(message, destination);
                    repeat++;
                } else {
                    synchronized (mutex) {
                        mutex.notify();
                    }
                    return;
                }
            }
        };
        TIMER.schedule(task, 0, DELAY);
        synchronized (mutex) {
            try {
                mutex.wait();
            } catch (InterruptedException e) {
                System.out.println("Send interrupted");
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Error parsing destination: " + e.getMessage());
        }
        }
        packetNumber++;

    }
    @Override
    public void send(String payload, String destinationAddress) {
        no_send();
    }
    /**
     * Closes the connection and cleans up any necessary resources.
     */
    @Override
    public void close() {
        // Clean-up any resources if needed
        super.close();
    }
}