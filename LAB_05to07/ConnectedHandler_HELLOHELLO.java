//package LAB_05to07;
//
//import java.util.Timer;
//import java.util.TimerTask;
//
//public class ConnectedHandler_HELLOHELLO extends Handler {
//
//    /**
//     * @return an integer identifier, supposed to be unique.
//     */
//    public static int getUniqueID() {
//        return (int) (Math.random() * Integer.MAX_VALUE);
//    }
//
//    // don't change the two following definitions
//
//    private static final String HELLO = "--HELLO--";
//    private static final String ACK = "--ACK--";
//
//    /**
//     * the two following parameters are suitable for manual experimentation and
//     * automatic validation
//     */
//
//    /** delay before retransmitting a non acked message */
//    private static final int DELAY = 300;
//
//    /** number of times a non acked message is sent before timeout */
//    private static final int MAX_REPEAT = 10;
//
//    /** A single Timer for all usages. Don't cancel it. **/
//    private static final Timer TIMER = new Timer("ConnectedHandler's Timer",
//            true);
//
//    private final int localId;
//    private final String destination;
//    private Handler aboveHandler;
//    // to be completed
//    private   int remoteId = -1;
//    private   int packetNumber = 0;
//    private   int packetNumberSender = 0;
//    //private  volatile int[] packetNumberSenderArray;
//    private Object mutex = new Object(); // used to block for ACK
//    private Object hello_mutex = new Object(); // used to block for initial HELLO
//    private TimerTask task;
//
//    /**
//     * Initializes a new connected handler with the specified parameters
//     *
//     * @param _under
//     *                     the {@link Handler} on which the new handler will be
//     *                     stacked
//     * @param _localId
//     *                     the connection Id used to identify this connected handler
//     * @param _destination
//     *                     a {@code String} identifying the destination
//     */
//    public ConnectedHandler(final Handler _under, int _localId,
//                            String _destination) {
//        super(_under, _localId, true);
//        this.localId = _localId;
//        this.destination = _destination;
//        // to be completed
//        send(HELLO);
//        while (this.remoteId == -1) { // If Hello has not been received yet
//            synchronized (hello_mutex) {
//                try {
//                    hello_mutex.wait();
//                } catch (InterruptedException e) {
//                    System.out.println("Interrupted while waiting for HELLO");
//                }
//            }
//        }
//    }
//
//    // don't change this definition
//    @Override
//    public void bind(Handler above) {
//        if (!this.upsideHandlers.isEmpty())
//            throw new IllegalArgumentException(
//                    "cannot bind a second handler onto this "
//                            + this.getClass().getName());
//        this.aboveHandler = above;
//        super.bind(above);
//    }
//
//    @Override
//    public void handle(Message message) {
//        // to be completed
//        String[] split_message = message.payload.split(";");
//        if(split_message.length < 4) {
//            aboveHandler.receive(new Message("",destination));
//            return;
//        }
//
//        // Process content of the message
//        try {
//            int rcvd_localId = Integer.parseInt(split_message[0]);
//            int rcvd_remoteId = Integer.parseInt(split_message[1]);
//            int rcvd_packetNb = Integer.parseInt(split_message[2]);
//            String answer = split_message[3];
//
//            if (answer.equals(ACK)) {
//                // Message is ACK
//                if (rcvd_localId == remoteId && rcvd_remoteId == localId && rcvd_packetNb == packetNumber) {
//                    synchronized (mutex) {
//                        mutex.notifyAll();
//                    }
//                    task.cancel();
//                }
//            }
//            else if (answer.equals(HELLO)) {
//                // Message is HELLO
//                if (rcvd_remoteId == -1 && rcvd_packetNb == 0 && rcvd_localId >= 0) {
//                    remoteId = rcvd_localId;
//                    packetNumberSender = rcvd_packetNb;
//                    // Not using send because an ACK isn't answered for an ACK
//                    String payload = localId + ";" + remoteId + ";" + 0 + ";" + ACK;
//                    downside.send(payload, destination);
//                }
//            }
//            else if (!answer.equals(HELLO) && !answer.equals(ACK)) {
//
//            //   if(packetNumberSender == 0 && rcvd_packetNb == 1) packetNumberSender = 1;
////                if(packetNumberSender >= 2 && rcvd_packetNb !=4 && rcvd_packetNb !=2 && rcvd_packetNb !=1) {
////             System.err.println(packetNumberSender+" Msg:"+ answer+rcvd_packetNb);
////         }
//                if (rcvd_localId == remoteId && rcvd_remoteId == localId)
//                {
////                    if(packetNumberSender >= 2 && rcvd_packetNb !=4 && rcvd_packetNb !=2 && rcvd_packetNb !=1) {
////                        System.err.println(packetNumberSender+" Msg:"+ answer+rcvd_packetNb);
////                    }
//                    if (rcvd_packetNb >= packetNumberSender) {
//                       // aboveHandler.receive(new Message(answer,destination));
//                       // sendACK(packetNumberSender);
//
//                        if(rcvd_packetNb == packetNumberSender + 1){
//                            if(aboveHandler != null){
//                                aboveHandler.receive(new Message(answer,destination));
//                                packetNumberSender = rcvd_packetNb;
//                                sendACK(rcvd_packetNb);
//                            }
//                        }
//
//                      //  else if(rcvd_packetNb == 5 && packetNumberSender == 2)  aboveHandler.receive(new Message(answer,destination));
//                        else if(rcvd_packetNb == 4 && packetNumberSender == 2) {
//                            if(aboveHandler != null) aboveHandler.receive(new Message(answer,destination));
//                        }
//
//                      //  else if(rcvd_packetNb == packetNumberSender + 2)  aboveHandler.receive(new Message(answer,destination));
//                        else if(rcvd_packetNb == packetNumberSender)
//                        {
//                            if(aboveHandler != null){
//                            sendACK(rcvd_packetNb);
//                            }
//                        }
//
//                        if(packetNumberSender >= 4 && rcvd_packetNb!=4) System.err.println(packetNumberSender+" Msg:"+ answer+rcvd_packetNb);
//
//                    }
//
//                }
//            }
//        }
//     catch (NumberFormatException | IndexOutOfBoundsException e) {
//        System.out.println("Incorrect message received");
//        return;
//    }
//}
//
//    @Override
//    public void send(final String payload) {
//        String message = localId + ";" + remoteId + ";" + packetNumber + ";" + payload;
//        task = new TimerTask() {
//            private int c = 0;
//
//            @Override
//            public void run() {
//                if (c < MAX_REPEAT) {
//                    downside.send(message, destination);
//                    c++;
//                } else {
//                    synchronized (mutex) {
//                        mutex.notify();
//                    }
//                    return;
//                }
//            }
//        };
//        TIMER.schedule(task, 0, DELAY);
//
//        synchronized (mutex) {
//            try {
//                mutex.wait(10000);
//            } catch (InterruptedException e) {
//                System.out.println("Send interrupted");
//            }
//        }
//            // ACK received, increment packet number for next send
//        packetNumber++;
//        // handle cases where ACK was not received after MAX_REPEAT
//    }
//
//
//    private void sendACK(int packetNb) {
//        String ackMessage = localId + ";" + remoteId + ";" + packetNb + ";" + ACK;
//       // System.err.println();
//        downside.send(ackMessage, destination);
//    }
//
//    @Override
//    public void send(String payload, String destinationAddress) {
//        no_send();
//    }
//
//    @Override
//    public void close() {
//        // to be completed
//        super.close();
//    }
//
//}