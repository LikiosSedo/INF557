package LAB_05to07;

import java.util.Timer;
import java.util.TimerTask;

public class ConnectedHandler_for2 extends Handler {

    /**
     * @return an integer identifier, supposed to be unique.
     */
    public static int getUniqueID() {
        return (int) (Math.random() * Integer.MAX_VALUE);
    }

    // don't change the two following definitions

    private static final String HELLO = "--HELLO--";
    private static final String ACK = "--ACK--";

    /**
     * the two following parameters are suitable for manual experimentation and
     * automatic validation
     */

    /** delay before retransmitting a non acked message */
    private static final int DELAY = 100;

    /** number of times a non acked message is sent before timeout */
    private static final int MAX_REPEAT = 20;

    /** A single Timer for all usages. Don't cancel it. **/
    private static final Timer TIMER = new Timer("ConnectedHandler's Timer",
            true);

    private final int localId;
    private final String destination;
    private Handler aboveHandler;
    // to be completed
    private int remoteId = -1;
    private int packetNumber = 0;
    private int packetNumberSender = 0;
    private Object mutex = new Object(); // used to block for ACK
    private Object hello_mutex = new Object(); // used to block for initial HELLO
    private TimerTask task;
    private int lastReceivedPacketNumber = -1; // to track the last received packet number for deduplication
    // Class scope variable to control the waiting for ACK
    private boolean isWaitingForAck = false;

    /**
     * Initializes a new connected handler with the specified parameters
     *
     * @param _under
     *                     the {@link Handler} on which the new handler will be
     *                     stacked
     * @param _localId
     *                     the connection Id used to identify this connected handler
     * @param _destination
     *                     a {@code String} identifying the destination
     */
    public ConnectedHandler_for2(final Handler _under, int _localId, String _destination) {
        super(_under, _localId, true); // Call the superclass constructor
        this.localId = _localId; // Set the local connection ID
        this.destination = _destination; // Set the destination address

        // Start the connection setup by sending a HELLO message
        send(HELLO);
        // Wait for a proper HELLO and ACK response
        if (this.remoteId == -1) { // If Hello has not been received yet
            synchronized (hello_mutex) {
                try {
                    hello_mutex.wait();
                } catch (InterruptedException e) {
                    System.out.println("Interrupted while waiting for HELLO");
                }
            }
//        synchronized (hello_mutex) {
//            try {
//                // Loop to handle spurious wakeups.
//                while (this.remoteId == -1) {
//                    // If remoteId is still -1, it means we haven't received the HELLO and ACK yet, so wait
//                    hello_mutex.wait();
//                }
//            } catch (InterruptedException e) {
//                // This thread was interrupted while waiting
//                System.out.println("Interrupted while waiting for HELLO");
//                Thread.currentThread().interrupt(); // Preserve interrupt status
//                // It's good practice to re-interrupt the thread so that higher-level interrupt handlers can notice the interrupt
//            }
        }

        // By this point, we should have received a HELLO and ACK, so remoteId should be set
        // The constructor will complete, and the object is ready for use
    }


    @Override
    public void bind(Handler above) {
        if (!this.upsideHandlers.isEmpty())
            throw new IllegalArgumentException(
                    "cannot bind a second handler onto this "
                            + this.getClass().getName());
        this.aboveHandler = above;
        super.bind(above);
    }

    @Override
    public void handle(Message message) {
        // Split the received message into parts
        String[] split_message = message.payload.split(";");
        try {
            if(split_message.length < 4)
            {
                aboveHandler.receive(new Message("","")); // Deliver payload
                return;
            }
            int rcvd_localId = Integer.parseInt(split_message[0]);
            int rcvd_remoteId = Integer.parseInt(split_message[1]);
            int rcvd_packetNb = Integer.parseInt(split_message[2]);
            String answer = split_message[3];
            if(!answer.equals(HELLO)) {
            if (rcvd_remoteId != localId || rcvd_localId != remoteId) {
                return; // Ignore irrelevant packets
            }
            }

            // If the message is an ACK
            if (answer.equals(ACK)) {
                if (rcvd_localId == remoteId && rcvd_remoteId == localId && rcvd_packetNb == packetNumber) {
                    synchronized (mutex) {
                        // Stop waiting for an ACK since it's been received
                        isWaitingForAck = false;
                        mutex.notifyAll();
                    }
                    task.cancel(); // Cancel the retransmission task
                }
            }
            // If the message is a HELLO
            else if (answer.equals(HELLO)) {
                // Check if this HELLO message is for initiating a new connection
                if (rcvd_remoteId == -1 && rcvd_packetNb == 0) {
                    // Only process the HELLO message if this handler is not yet connected to a remoteId
                    if (this.remoteId == -1) {
                        synchronized (hello_mutex) {
                            hello_mutex.notifyAll(); // Notify that HELLO has been received
                        }
                        remoteId = rcvd_localId; // Set the remoteId because this HELLO is for this handler
                        // Send ACK for the received HELLO
                        String payload = localId + ";" + remoteId + ";" + rcvd_packetNb + ";" + ACK;
                        downside.send(payload, destination);
                    }
                }
            }

            // If the message is a normal payload
            else {
                if (rcvd_packetNb > lastReceivedPacketNumber && rcvd_remoteId == localId && rcvd_localId == remoteId) {
                    lastReceivedPacketNumber = rcvd_packetNb; // Update the last received packet number
                    if (aboveHandler != null) {
                        // Deliver the payload to the application layer
                        String actualPayload = (answer.length() > 0) ? answer : ""; // Check for non-empty payload
                        aboveHandler.receive(new Message(actualPayload,"")); // Deliver payload
                        // Send ACK for the received packet
                        String ackMessage = localId + ";" + rcvd_localId + ";" + rcvd_packetNb + ";" + ACK;
                        downside.send(ackMessage, destination);
                    }
                }
                else if (rcvd_packetNb == lastReceivedPacketNumber) {
                    // Resend ACK if it's an old packet
                    String ackMessage = localId + ";" + rcvd_localId + ";" + rcvd_packetNb + ";" + ACK;
                    downside.send(ackMessage, destination);
                }
            }


        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            System.out.println("Incorrect message received");
            return;
        }
    }


    @Override
    public void send(final String payload) {
        // Construct the message with the correct format including the packet number
        final String message = localId + ";" + remoteId + ";" + packetNumber + ";" + payload;
        task = new TimerTask() {
            private int c = 0; // Counter for the number of attempts

            @Override
            public void run() {
                if (payload.equals(HELLO) || c < MAX_REPEAT) {
                    // Send the message using the underlying handler
                    downside.send(message, destination);
                    c++; // Increment the attempt counter
                } else {
                    // If the maximum number of attempts is reached, notify the waiting thread
                    synchronized (mutex) {
                        isWaitingForAck = false; // Indicate that we should stop waiting
                        mutex.notify();
                        return;
                    }
                }
            }
        };

        // Start the timer task for sending the message, with retries
        TIMER.schedule(task, 0, DELAY);
        isWaitingForAck = true; // Assume we are waiting for an ACK
        synchronized (mutex) {
            while (isWaitingForAck) {
                try {
                    // Wait until an ACK is received or the task gives up after MAX_REPEAT attempts
                    mutex.wait();
                } catch (InterruptedException e) {
                    System.out.println("Send interrupted");
                    Thread.currentThread().interrupt(); // Preserve interrupt status
                }
            }
        }

        // Increment the packet number only if the message was acknowledged
        packetNumber++;
    }


    @Override
    public void send(String payload, String destinationAddress) {
        no_send();
    }

    @Override
    public void close() {
        // to be completed
        super.close();
    }

}