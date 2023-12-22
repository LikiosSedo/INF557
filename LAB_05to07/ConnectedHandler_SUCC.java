package LAB_05to07;


import java.util.Timer;
import java.util.TimerTask;

public class ConnectedHandler_SUCC extends Handler {

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
    private static final int DELAY = 300;

    /** number of times a non acked message is sent before timeout */
    private static final int MAX_REPEAT = 10;

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
    private volatile int lastReceivedPacketNumber = -1;
    private volatile boolean isWaitingForAck = true;

    private Object mutex = new Object(); // used to block for ACK
    private Object hello_mutex = new Object(); // used to block for initial HELLO
    private TimerTask task;

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
    public ConnectedHandler_SUCC(final Handler _under, int _localId,
                            String _destination) {
        super(_under, _localId, true);
        this.localId = _localId;
        this.destination = _destination;
        // to be completed
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

    // don't change this definition
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
        // to be completed
        String[] split_message = message.payload.split(";");
        // Process content of the message
        if(split_message.length < 4)
        {
            aboveHandler.receive(new Message("",destination)); // Deliver payload
           // System.err.println("empty msg");
            return;
        }
        try {
            int rcvd_localId = Integer.parseInt(split_message[0]);
            int rcvd_remoteId = Integer.parseInt(split_message[1]);
            int rcvd_packetNb = Integer.parseInt(split_message[2]);
            String answer = split_message[3];
            if (answer.equals(ACK)) {
                // Message is ACK
                if (rcvd_localId == remoteId && rcvd_remoteId == localId && rcvd_packetNb == packetNumber) {
                    synchronized (mutex) {
                        mutex.notifyAll();
                    }
                    task.cancel();
                }
            }
            else if (answer.equals(HELLO)) {
                // Message is HELLO
                if (rcvd_remoteId == -1 && rcvd_packetNb == 0 && rcvd_localId >= 0) {
                    remoteId = rcvd_localId;
                    packetNumberSender = rcvd_packetNb;
                    synchronized (hello_mutex) {
                        hello_mutex.notifyAll();
                    }
                    // Not using send because an ACK isn't answered for an ACK
                    String payload = localId + ";" + remoteId + ";" + packetNumberSender + ";" + ACK;
                    downside.send(payload, destination);
                }
            }
            else {
                // Only process packets if we have an established connection (remoteId is not -1)
                if(remoteId != -1){
                    if (rcvd_packetNb > lastReceivedPacketNumber && rcvd_remoteId == localId && rcvd_localId == remoteId) {
                        lastReceivedPacketNumber = rcvd_packetNb;
                        if (aboveHandler != null) {
                            // Deliver the payload to the application layer
                            String actualPayload = (answer.length() > 0) ? answer : ""; // Check for non-empty payload
                           // System.err.println("push forward eroor");
                            aboveHandler.receive(new Message(actualPayload,destination)); // Deliver payload
                        }
                        synchronized (mutex) {
                            mutex.notifyAll();
                        }
                //         Send ACK for the received packet
                        String ackMessage = localId + ";" + remoteId + ";" + lastReceivedPacketNumber + ";" + ACK;
                        downside.send(ackMessage,destination);
                       // System.out.println(ackMessage);
                    }
                     if (rcvd_packetNb == lastReceivedPacketNumber && rcvd_remoteId == localId && rcvd_localId == remoteId) {
                        synchronized (mutex) {
                          //  isWaitingForAck = true;
                            mutex.notifyAll();
                        }
//                        // Resend ACK if it's an old packet
                        String ackMessage = localId + ";" + remoteId + ";" + lastReceivedPacketNumber + ";" + ACK;
                        downside.send(ackMessage,destination);
//                        System.out.println(ackMessage);
                    }
                }
            }

        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            System.out.println("Incorrect message received");
            return;
        }
    }

    @Override
    public void send(final String payload) {
        // to be completed
        String message = localId + ";" + remoteId + ";" + packetNumber + ";" + payload;
        task = new TimerTask() {
            private int c = 0;

            @Override
            public void run() {
                if (payload.equals(HELLO) || c < MAX_REPEAT) {
                    downside.send(message, destination);
                    c++;
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
            }
        }
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