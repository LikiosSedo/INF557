package LAB_05to07;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Condition;

public class ConnectedHandler_SUCC2 extends Handler {

    /**
     * @return an integer identifier, supposed to be unique.
     */
    public static int getUniqueID() {
        return (int) (Math.random() * Integer.MAX_VALUE);
    }

    // don't change the two following definitions

    private static final String HELLO = "--HELLO--";
    private static final String ACK = "--ACK--";
    private volatile boolean ackReceived = false;
    private volatile boolean helloSent = false;

    private Condition receivingACK;       // Condition to wait for receiving ACK
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
    public ConnectedHandler_SUCC2(final Handler _under, int _localId, String _destination) {
        super(_under, _localId, true);
        this.localId = _localId;
        this.destination = _destination;

        // Send the initial HELLO message and wait for the connection to be established
        sendHELLO();
        waitForHelloACK();
    }

    private void sendHELLO() {
        // This method sends the HELLO message repeatedly until an ACK is received or until MAX_REPEAT is reached
        if(!helloSent) {
            TIMER.schedule(new TimerTask() {
                private int count = 0;

                @Override
                public void run() {
                    if (remoteId == -1 && count < MAX_REPEAT) {
                        String helloMessage = localId + ";-1;0;" + HELLO;
                        downside.send(helloMessage, destination);
                        count++;
                    } else {
                        this.cancel(); // Stop sending HELLO messages once an ACK is received or max repeats reached
                    }
                }
            }, 0, DELAY);
        }

        helloSent = true;
    }

    private void waitForHelloACK() {
        // This method blocks until the HELLO message has been acknowledged
        synchronized (hello_mutex) {
                try {
                    hello_mutex.wait(60000);
                } catch (InterruptedException e) {
                    System.out.println("Interrupted while waiting for HELLO ACK");
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
        String[] parts = message.payload.split(";");
        try {
            int senderId = Integer.parseInt(parts[0]);
            int receiverId = Integer.parseInt(parts[1]);
            int msgPacketNumber = Integer.parseInt(parts[2]);
            String content = parts[3];

            if (content.equals(ACK)) {
                if (receiverId == this.localId && msgPacketNumber == 0) {
                    if (senderId != this.localId) {
                        synchronized (hello_mutex) {
                            if (this.remoteId == -1) {
                                this.remoteId = senderId;
                                hello_mutex.notifyAll();
                            }
                        }
                    }
                } else {
                    handleACK(senderId, receiverId, msgPacketNumber);
                }
            } else if (content.equals(HELLO)) {
                if (receiverId == -1 && msgPacketNumber == 0 && senderId >= 0)
                {
                    handleHello(senderId);
                }
            } else {
                handleData(senderId, receiverId, msgPacketNumber, content);
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            System.out.println("Not correct");
        }
    }

    private void handleACK(int senderId, int receiverId, int msgPacketNumber) {
            if (senderId == this.remoteId && receiverId == this.localId) {
                if (msgPacketNumber == 0) {
                    synchronized (hello_mutex) {
                        if (this.remoteId == -1) {
                            this.remoteId = senderId;
                          //  this.ackReceived = true;
                            hello_mutex.notifyAll();
                        }
                    }
                } else if (msgPacketNumber == this.packetNumber) {
                    synchronized (mutex) {
                    this.ackReceived = true;
                    mutex.notifyAll();
                }
            }
        }
    }



    private void handleHello(int senderId) {
        // Handle incoming HELLO messages
        synchronized (hello_mutex) {
        if (remoteId == -1) {
            remoteId = senderId;
                hello_mutex.notifyAll();
            sendACK(0); // Send ACK with packet number 0 for HELLO
        }
        }
    }

    private void handleData(int senderId, int receiverId, int msgPacketNumber, String content) {
        // Handle regular data messages
        if (senderId == remoteId && receiverId == localId) {
            if (msgPacketNumber == packetNumberSender + 1) {
                // If it's the expected packet number
                packetNumberSender++;
                aboveHandler.send(content); // Deliver the payload to the application layer
                sendACK(msgPacketNumber); // Send an ACK back
            } else if (msgPacketNumber <= packetNumberSender) {
                // If it's a duplicate packet
                sendACK(msgPacketNumber); // Send an ACK back, even for duplicates
            }
        }
    }
    private void sendACK(int packetNb) {
        // Send an ACK for the received packet
        String ackPayload = localId + ";" + remoteId + ";" + packetNb + ";" + ACK;
        downside.send(ackPayload, destination);
    }

    @Override
    public void send(final String payload) {
        final String message = localId + ";" + remoteId + ";" + packetNumber + ";" + payload;
        boolean ACKRC = this.ackReceived;
        task = new TimerTask() {
            private int c = 0;
            @Override
            public void run() {
                if (ConnectedHandler_SUCC2.this.ackReceived) {
                    this.cancel();
                } else if (c < MAX_REPEAT) {
                    downside.send(message, destination);
                    c++;
                } else {
                    synchronized (mutex) {
                        if (!ConnectedHandler_SUCC2.this.ackReceived) {
                            mutex.notify();
                            this.cancel();
                        }
                    }
                }
            }
        };
        TIMER.schedule(task, 0, DELAY);

        synchronized (mutex) {
            try {
                mutex.wait();
                if (this.ackReceived) {
                    packetNumber++;
                } else {
                }
            } catch (InterruptedException e) {
                System.out.println("Send interrupted");

            }
        }
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
