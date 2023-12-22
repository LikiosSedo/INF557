package LAB_05to07;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

public class ConnectedHandler_Stop_and_Wait extends Handler {

    public static int getUniqueID() {
        return (int) (Math.random() * Integer.MAX_VALUE);
    }

    private static final String HELLO = "--HELLO--";
    private static final String ACK = "--ACK--";

    private static final int DELAY = 500;
    private static final int MAX_REPEAT = 100;

    private static final Timer TIMER = new Timer("ConnectedHandler's Timer", true);

    private final int localId;
    private final String destination;
    private Handler aboveHandler;
    private volatile boolean hasReceivedAck = false;


    // Variables to store remote connectionId and current packet number
    private int remoteId = -1;
    private volatile int currentPacketNumber = 0;

    public ConnectedHandler_Stop_and_Wait(final Handler _under, int _localId, String _destination) {
        super(_under, _localId, true);
        this.localId = _localId;
        this.destination = _destination;

        sendHelloRepeatedly();
    }


    @Override
    public void bind(Handler above) {
        if (!this.upsideHandlers.isEmpty())
            throw new IllegalArgumentException(
                    "cannot bind a second handler onto this " + this.getClass().getName());
        this.aboveHandler = above;
        super.bind(above);
    }

    @Override
    public void handle(Message message) {
        String[] parts = message.payload.split(";");
        if (parts.length != 4) return;

        int senderId = Integer.parseInt(parts[0]);
        int destinationId = Integer.parseInt(parts[1]);
        int packetNumber = Integer.parseInt(parts[2]);
        String flag = parts[3];
        System.out.println("packetNumber"+packetNumber);
        System.out.println("currentPacketNumber"+(currentPacketNumber));
        if (flag.equals(HELLO) ) {
            if(remoteId == -1){
                remoteId = senderId;
            }
            System.out.println("remoteId:"+remoteId);
            System.out.println("Sent Hello!!!!");
            sendAckForHello();
//            if(currentPacketNumber == 0) {
//                currentPacketNumber++;
//                System.out.println("Changed ACK!!!");
//            }
        } else if (flag.equals(ACK)) {
            System.out.println("flag:"+flag+senderId);
            System.out.println(remoteId);
            if(senderId == remoteId){
                System.out.println("ABOUT TO RECEIVE ACK");
                //System.out.println(currentPacketNumber+packetNumber);
                synchronized (this) {
                    if (packetNumber <= currentPacketNumber  || currentPacketNumber == 0) {
                    hasReceivedAck = true;
                    this.notify();
                    System.out.println("Received ACK!!!");
                }
            }
            }
        } else {
            if (packetNumber == currentPacketNumber) {
                System.out.println("PAC" + currentPacketNumber);
                System.out.println("Received msg:" + flag +" " +packetNumber);
                currentPacketNumber++;
                //aboveHandler.receive(message);
                sendAckForMessage(Integer.toString(packetNumber));
            } else if (packetNumber < currentPacketNumber) {
                sendAckForMessage(Integer.toString(packetNumber));
            }
        }

    }

    @Override
    public void send(final String payload) {
        String message = localId + ";" + remoteId + ";" + currentPacketNumber + ";" + payload;
        sendMessageRepeatedly(message);
        System.out.println("SENT MSG:"+payload +" pac:"+currentPacketNumber);
        currentPacketNumber++;
    }

    @Override
    public void send(String payload, String destinationAddress) {
        no_send();
    }

    @Override
    public void close() {
        // Clean-up any resources if needed
        super.close();
    }

    private void sendHelloRepeatedly() {
        final TimerTask helloTask = new TimerTask() {
            private int repeats = 0;

            @Override
            public void run() {
                if (repeats++ >= MAX_REPEAT) {
                    this.cancel();
                    return;
                }
                _send(localId + ";-1;0;" + HELLO);
            }
        };
        TIMER.schedule(helloTask, 0, DELAY);

        synchronized (this) {
            try {
                this.wait(MAX_REPEAT * DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(remoteId == -1) {
            //helloTask.cancel();
            sendHelloRepeatedly();
            throw new RuntimeException("Connection failed: Timeout waiting for ACK");
        }
        helloTask.cancel();
    }


    private void sendMessageRepeatedly(String message) {
        hasReceivedAck = false;
        final TimerTask messageTask = new TimerTask() {
            private int repeats = 0;

            @Override
            public void run() {
                if (repeats++ >= MAX_REPEAT) {
                    this.cancel();
                    return;
                }
                System.out.println(message);
                _send(message);
            }
        };
        TIMER.schedule(messageTask, 0, DELAY);

        synchronized (this) {
            try {
                this.wait(MAX_REPEAT * DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        if(!hasReceivedAck) {

//            messageTask.cancel();
//            throw new RuntimeException("Message delivery failed: Timeout waiting for ACK");
            sendMessageRepeatedly(message);
        }
        messageTask.cancel();
    }


    private void _send(String message) {
        try {
            String[] parts = destination.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            SocketAddress destinationAddress = new InetSocketAddress(InetAddress.getByName(host), port);
            System.out.println("Port:"+port);
            System.out.println(message);
            GroundLayer.send(message, destinationAddress);
        } catch (UnknownHostException e) {
            System.err.println("Error resolving destination address: " + e.getMessage());
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Error parsing destination: " + e.getMessage());
        }
    }



    private void sendAckForHello() {
        System.out.println("SENT ACK!!!"+remoteId);
        _send(localId + ";" + remoteId + ";0;" + ACK);
    }

    private void sendAckForMessage(String packetNumber) {
        _send(localId + ";" + remoteId + ";" + packetNumber + ";" + ACK);
        System.out.println("SENT ACK FOR msg "+" pac:" +packetNumber);
    }
}