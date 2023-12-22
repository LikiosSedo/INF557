package LAB_05to07;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Represents a handler for managing connections using a handshake mechanism.
 * It facilitates sending hello messages to initiate a connection and awaits acknowledgments.
 * Additionally, it manages sending, receiving, and processing messages with a reliable mechanism.
 */
public class ConnectedHandler_3 extends Handler {

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
    private static final int MAX_REPEAT = 100;

    // Timer for handling periodic message sends.
    private static final Timer TIMER = new Timer("ConnectedHandler's Timer", true);

    // Instance variables for tracking connection state.
    private final int localId;             // ID for the local connection.
    private final String destination;      // Destination address for communication.
    private Handler aboveHandler;          // The upper layer handler.
    private volatile boolean hasReceivedAck = false; // Flag to track acknowledgment receipt.

    // State variables for connection management.
    private int remoteId = -1;             // ID of the remote connection.
    private int currentPacketNumber = 0;   // Tracks the sequence number of packets.

    /**
     * Constructor to set up the connected handler.
     *
     * @param _under       the underlying handler layer.
     * @param _localId     the local connection ID.
     * @param _destination the destination address for communication.
     */
    public ConnectedHandler_3(final Handler _under, int _localId, String _destination) {
        super(_under, _localId, true);
        this.localId = _localId;
        this.destination = _destination;

        sendHelloRepeatedly();
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

        // Extract key parts of the message.
        int senderId = Integer.parseInt(parts[0]);
        int destinationId = Integer.parseInt(parts[1]);
        String flag = parts[3];

        // Check if this is an initial HELLO message and if we haven't set the remoteId yet.
        if (flag.equals(HELLO) && remoteId == -1) {
            remoteId = senderId;
            System.out.println("Sent Hello!!!!");
            sendAckForHello();  // Acknowledge the HELLO message.
        } else if (flag.equals(ACK) && senderId == remoteId) {
            // If it's an acknowledgment from the expected sender, update the status and notify any waiting threads.
            synchronized (this) {
                hasReceivedAck = true;
                this.notify();  // Notify threads waiting for this ACK.
                System.out.println("Received ACK!!!");
            }
        } else {
            // For other types of messages, process them and send an acknowledgment.
            System.out.println("Received Msg:"+flag);
            sendAckForMessage(parts[2]);  // Reply with an ACK using the packet number.
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
        String message = localId + ";" + remoteId + ";" + currentPacketNumber + ";" + payload;

        // Use a helper method to handle the sending logic, including retries.
        sendMessageRepeatedly(message);
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

    /**
     * Repeatedly sends a HELLO message until an acknowledgment (ACK) is received or a max retry limit is reached.
     */
    private void sendHelloRepeatedly() {
        final TimerTask helloTask = new TimerTask() {
            private int repeats = 0;

            @Override
            public void run() {
                // If the max number of retries is reached, cancel the task.
                if (repeats++ >= MAX_REPEAT) {
                    this.cancel();
                    return;
                }
                _send(localId + ";-1;0;" + HELLO);
            }
        };

        // Schedule the task to run immediately and repeat at fixed delay intervals.
        TIMER.schedule(helloTask, 0, DELAY);

        synchronized (this) {
            try {
                // Block until an ACK is received or a timeout is reached.
                this.wait(MAX_REPEAT * DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Check if the ACK was received.
        if(remoteId == -1) {
            // If the ACK was not received, cancel the task and throw an exception.
            helloTask.cancel();
            throw new RuntimeException("Connection failed: Timeout waiting for ACK");
        }
        helloTask.cancel();
    }

    /**
     * Repeatedly sends a given message until an acknowledgment (ACK) is received or a max retry limit is reached.
     *
     * @param message The message to be sent repeatedly.
     */
    private void sendMessageRepeatedly(String message) {
        hasReceivedAck = false;
        final TimerTask messageTask = new TimerTask() {
            private int repeats = 0;

            @Override
            public void run() {
                // If the max number of retries is reached, cancel the task.
                if (repeats++ >= MAX_REPEAT) {
                    this.cancel();
                    return;
                }
                System.out.println(message);
                _send(message);
            }
        };

        // Schedule the task to run immediately and repeat at fixed delay intervals.
        TIMER.schedule(messageTask, 0, DELAY);

        synchronized (this) {
            try {
                // Block until an ACK is received or a timeout is reached.
                this.wait(MAX_REPEAT * DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Check if the ACK was received.
        if(!hasReceivedAck) {
            // If the ACK was not received, cancel the task and throw an exception.
            messageTask.cancel();
            throw new RuntimeException("Message delivery failed: Timeout waiting for ACK");
        }
        messageTask.cancel();
    }

    /**
     * Sends a message to a specified destination address.
     *
     * @param message The message to be sent.
     */
   private void _send(String message) {
        try {
            // Parse the destination into its constituent parts (host and port).
            String[] parts = destination.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            SocketAddress destinationAddress = new InetSocketAddress(InetAddress.getByName(host), port);
            System.out.println("Port:"+port);
            System.out.println(message);
            downside.send(message, String.valueOf(destinationAddress));
        } catch (UnknownHostException e) {
            System.err.println("Error resolving destination address: " + e.getMessage());
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Error parsing destination: " + e.getMessage());
        }
    }

    /**
     * Sends an acknowledgment (ACK) in response to a received HELLO message.
     */
    private void sendAckForHello() {
        // Construct the acknowledgment message for the HELLO message.
        _send(localId + ";" + remoteId + ";0;" + ACK);
    }

    /**
     * Sends an acknowledgment (ACK) for a specific received message, identified by its packet number.
     *
     * @param packetNumber The number of the packet/message for which the acknowledgment is being sent.
     */
    private void sendAckForMessage(String packetNumber) {
        // Construct the acknowledgment message using the provided packet number.
        _send(localId + ";" + remoteId + ";" + packetNumber + ";" + ACK);
    }

}