package LAB_05to07;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectedHandler_SWT2 extends Handler {

    /**
     * @return an integer identifier, supposed to be unique.
     */
    public static int getUniqueID() {
        return (int) (Math.random() * Integer.MAX_VALUE);
    }

    // don't change the two following definitions

    private static final String HELLO = "--HELLO--";
    private static final String ACK = "--ACK--";
    private volatile boolean receivedACK; // Volatile to avoid the CPU optimization and always get an up to date value in the while loop
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
    private int remoteId;
    private int packetNumber;
    private int packetNumberSender;
    private Lock lock;                    // Lock to manage concurrency
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
    public ConnectedHandler_SWT2(final Handler _under, int _localId, String _destination) {
        super(_under, _localId, true);
        this.localId = _localId;
        this.destination = _destination;
        this.remoteId = -1;
        this.packetNumber = 0;
        this.packetNumberSender = -1;
        this.lock         = new ReentrantLock();
        this.receivingACK = lock.newCondition();
        this.receivedACK =false;
        // Send the initial HELLO message and wait for the connection to be established
        this.send(HELLO);
        lock.lock();
        try {
            while (remoteId == -1) {
                try {
                    receivingACK.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }  finally {
            lock.unlock();
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
        if(parts.length < 4)
        {
            aboveHandler.receive(new Message("","")); // Deliver payload
            return;
        }

        try {
            int senderId = Integer.parseInt(parts[0]);
            int receiverId = Integer.parseInt(parts[1]);
            int msgPacketNumber = Integer.parseInt(parts[2]);
            String content = parts[3];

            if (content.equals(ACK)) {
                handleACK(senderId,msgPacketNumber);
            }
            else if (content.equals(HELLO)) {
                handleHello(senderId,msgPacketNumber);
            }
            else {
                handleData(senderId, receiverId, msgPacketNumber, message);
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            System.out.println("Not correct");
        }
    }

    private void handleACK(int senderId, int msgPacketNumber) {
            if (msgPacketNumber == packetNumber && senderId == localId) {
                lock.lock();
                try {
                    receivedACK = true;
                    receivingACK.signalAll();
                } finally {
                    lock.unlock();
                }
        }
    }



    private void handleHello(int senderId, int MsgpacketNumber) {
        // Handle incoming HELLO messages
        if (MsgpacketNumber == 0) { // Initiate the connection
           this.remoteId =senderId;
            packetNumberSender = 0;
        }
        if (this.remoteId == senderId ) {
            // If the connection is already set, forward message to the GroundLayerd
            final String payload = localId + ";" + remoteId + ";" + 0 + ";" + ACK;
            downside.send(payload,destination);
        }
    }

    private void handleData(int senderId, int receiverId, int msgPacketNumber, Message msg) {
        // Handle regular data messages
        final String ackMessage = localId + ";" + remoteId + ";" + msgPacketNumber + ";" + ACK;
        downside.send(ackMessage, destination);
        if (aboveHandler != null && msgPacketNumber == packetNumberSender+1) {
            aboveHandler.receive(msg);
            packetNumberSender = msgPacketNumber;
        }
    }

    @Override
    public void send(final String payload) {
        receivedACK = false;
        final String message = localId + ";" + remoteId + ";" + packetNumber + ";" + payload;
        task = new TimerTask() {
            private int c = 0;
            @Override
            public void run() {
                if (c < MAX_REPEAT) {
                    downside.send(message, destination);
                    c++;
                } else {
                          return;
                }
            }
        };
        TIMER.schedule(task, 0, DELAY);

        lock.lock();
            try {
                while (!receivedACK) receivingACK.await();
                packetNumber++;
                task.cancel(); // Interupt the task when the ACK is received
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
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
