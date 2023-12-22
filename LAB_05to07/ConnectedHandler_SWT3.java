package LAB_05to07;

import java.util.Timer;
import java.util.TimerTask;

public class ConnectedHandler_SWT3 extends Handler {

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
    private static final int MAX_REPEAT = 20000;

    /** A single Timer for all usages. Don't cancel it. **/
    private static final Timer TIMER = new Timer("ConnectedHandler's Timer",
            true);

    private final int localId;
    private final String destination;
    private Handler aboveHandler;
    // to be completed
    private volatile int remoteId = -1;
    private volatile int packetNumber = 0;
    private int packetNumberSender = 0;
    private volatile int lastReceivedPacketNumber = -1; // to track the last received packet number for deduplication
    private volatile boolean  isWaitingForAck = false;

    private  Object mutex = new Object(); // used to block for ACK
    private  Object hello_mutex = new Object(); // used to block for initial HELLO
    private  TimerTask task;

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
    public ConnectedHandler_SWT3(final Handler _under, int _localId,
                                 String _destination) {
        super(_under, _localId, true);
        this.localId = _localId;
        this.destination = _destination;
        // to be completed
       // System.err.println(localId+destination);
        if(remoteId == -1)
        {
        send(HELLO);
        }

        synchronized (hello_mutex) {
            try {
                while (remoteId == -1) { // 只有在收到正确的ACK时才会设置remoteId
                    hello_mutex.wait(); // 等待HELLO的ACK
                }
            } catch (InterruptedException e) {
                System.out.println("Interrupted while waiting for HELLO ACK");
             //   Thread.currentThread().interrupt(); // 重新设置中断状态
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
        try {
            if(split_message.length < 4)
            {
                aboveHandler.receive(new Message("",destination)); // Deliver payload
                return;
            }
            int rcvd_localId = Integer.parseInt(split_message[0]);
            int rcvd_remoteId = Integer.parseInt(split_message[1]);
            int rcvd_packetNb = Integer.parseInt(split_message[2]);
            String answer = split_message[3];

            // If the payload is HELLO, do not increment packetNumber after sending

            if (answer.equals(ACK)) {
//                if (rcvd_remoteId == -1 && rcvd_packetNb == 0 && rcvd_localId >= 0) {
//                    // HELLO
//                    synchronized (hello_mutex) {
//                        if (remoteId == -1) { // 防止重复处理ACK
//                            remoteId = rcvd_localId;
//                            hello_mutex.notifyAll(); // 正确收到HELLO消息的ACK
//                        }
//                    }
//                }
           //     else {
                    if (rcvd_localId == remoteId && rcvd_remoteId == localId && rcvd_packetNb == packetNumber) {
                        synchronized (mutex) {
                            isWaitingForAck = false;
                            mutex.notifyAll();
                        }
                        if (task != null) {
                            task.cancel();
                        }
                    }
              // }
            }

            else if (answer.equals(HELLO)) {
                // Message is HELLO
                if (rcvd_remoteId == -1 && rcvd_packetNb == 0 && rcvd_localId >= 0) {
                    if (this.remoteId == -1) {
                    remoteId = rcvd_localId;
                    packetNumberSender = rcvd_packetNb;
                    synchronized (hello_mutex) {
                        hello_mutex.notifyAll();
                    }
                    // Not using send because an ACK isn't answered for an ACK
                }
                    String payload = localId + ";" + remoteId + ";" + 0 + ";" + ACK;
                    downside.send(payload,destination);
                    System.out.println(payload);
                }
            }
            // If the message is a normal payload
            else {
                // Only process packets if we have an established connection (remoteId is not -1)
                if(remoteId != -1){
                    if (rcvd_packetNb > lastReceivedPacketNumber && rcvd_remoteId == localId && rcvd_localId == remoteId) {
                        lastReceivedPacketNumber = rcvd_packetNb;
                        if (aboveHandler != null) {
                            // Deliver the payload to the application layer
                            String actualPayload = (answer.length() > 0) ? answer : ""; // Check for non-empty payload
                            aboveHandler.receive(new Message(actualPayload,destination)); // Deliver payload
                        }
                        synchronized (mutex) {
                            mutex.notifyAll();
                        }
                        // Send ACK for the received packet
                        String ackMessage = localId + ";" + remoteId + ";" + lastReceivedPacketNumber + ";" + ACK;
                        downside.send(ackMessage,destination);
                        System.err.println(ackMessage);
                    }
                    else if (rcvd_packetNb == lastReceivedPacketNumber && rcvd_remoteId == localId && rcvd_localId == remoteId) {
                        synchronized (mutex) {
                            mutex.notifyAll();
                        }
                        // Resend ACK if it's an old packet
                        String ackMessage = localId + ";" + remoteId + ";" + lastReceivedPacketNumber + ";" + ACK;
                        downside.send(ackMessage,destination);
                        System.err.println(ackMessage);
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
        // 需要一个本地变量来在TimerTask内部引用消息，以避免因为payload变化导致发送错误的消息
        final String message = payload.equals(HELLO) ?
                localId + ";" + -1 + ";" + 0 + ";" + HELLO :
                localId + ";" + remoteId + ";" + packetNumber + ";" + payload;

        if (payload.equals(HELLO)) {
            // Handle HELLO message
            synchronized (hello_mutex) {
                if (!isWaitingForAck) {
                    isWaitingForAck = true;
                    TimerTask helloTask = new TimerTask() {
                        @Override
                        public void run() {
                            downside.send(message, destination);
                            // 不需要在这里调用 hello_mutex.notify()
                        }
                    };
                    TIMER.schedule(helloTask, 0);
                    try {
                        hello_mutex.wait(); // 在hello_mutex上等待HELLO的ACK
                    } catch (InterruptedException e) {
                        System.out.println("Interrupted while waiting for HELLO ACK");
                        Thread.currentThread().interrupt(); // 重新设置中断状态
                    }
                }
            }
        } else {
            // Handle other messages
            synchronized (mutex) {
                if (task != null) {
                    task.cancel(); // 取消之前的任务
                }
                task = new TimerTask() {
                    private int count = 0; // 尝试发送的次数计数器
                    @Override
                    public void run() {
                        synchronized (mutex) {
                            if (count < MAX_REPEAT && isWaitingForAck) {
                                downside.send(message, destination);
                                count++; // 尝试次数加1
                            } else {
                                isWaitingForAck = false;
                                mutex.notifyAll(); // 通知所有等待的线程
                                this.cancel(); // 取消TimerTask
                            }
                        }
                    }
                };
                TIMER.schedule(task, 0, DELAY); // 立即开始任务并设置重试间隔
                isWaitingForAck = true;
                while (isWaitingForAck) {
                    try {
                        mutex.wait(); // 等待ACK的到来或任务被取消
                    } catch (InterruptedException e) {
                        System.out.println("Send interrupted");
                        Thread.currentThread().interrupt(); // 重新设置中断状态
                    }
                }
                packetNumber++; // 成功发送消息后递增消息编号
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
