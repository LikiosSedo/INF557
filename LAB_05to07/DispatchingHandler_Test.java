package LAB_05to07;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.HashSet;
import java.util.Set;


public class DispatchingHandler_Test extends Handler {

    private static final String HELLO = "--HELLO--";
    private static final String ACK = "--ACK--";
    private static int counter = 35000;
    private final ArrayBlockingQueue<ConnectionParameters> queue;
    private volatile Set<String> receivedHellos = new HashSet<>();
    private static int times = 0;
    private static String MSG = "";
    private static final boolean DEBUG = false;
    private static int last_pacnumber = -1;
    private static int last_data_pacnumber = -1;
    private static Integer lastID = -1;
    private static Set<String> Handled = new HashSet<>();

    public DispatchingHandler_Test(final Handler _under, int _queueCapacity) {
        super(_under, ++counter, false);
        this.queue = new ArrayBlockingQueue<ConnectionParameters>(_queueCapacity);
        this.receivedHellos = new HashSet<>();
    }

    public ConnectionParameters accept() throws InterruptedException {
        return this.queue.take();
    }

    @Override
    public void send(String payload) {
        no_send();
    }

    @Override
    protected void send(String payload, String destinationAddress) {
        this.downside.send(payload, destinationAddress);
    }

    @Override
    public void handle(Message message) {
        DispatchingHandler_Test.times++;
        Integer theOnlyKey = -1;
        Handler handler;
        String[] split_message = message.payload.split(";");
        if (split_message.length < 4) return;

        int rcvd_localId = Integer.parseInt(split_message[0]);
        String rcvd_remoteId = split_message[1];
        int rcvd_pacnumber = Integer.parseInt(split_message[2]);
        String answer = split_message[3];
        String helloIdentifier = String.valueOf(rcvd_localId);
        ConnectionParameters newConnection = new ConnectionParameters(rcvd_localId, message.sourceAddress);
        if(!upsideHandlers.isEmpty() ) {
            if(!answer.equals(HELLO) && !answer.startsWith(ACK)) {
                handler = upsideHandlers.get(Integer.parseInt(rcvd_remoteId));
            }
            else handler = upsideHandlers.values().iterator().next();
            theOnlyKey = upsideHandlers.keySet().stream().findFirst().get();

            DispatchingHandler_Test.MSG += "Find" + counter + upsideHandlers +"is"+ handler.toString() + "KEY:" + theOnlyKey + "times:" +times + message;
            if(answer.equals(ACK))
            {
              //  String HD = theOnlyKey +", " + String.valueOf(rcvd_pacnumber) +" "+ rcvd_remoteId + " " + rcvd_localId;
                String HD = String.valueOf(rcvd_localId);
                DispatchingHandler_Test.Handled.add(HD);
            }
        }

        else {
            handler = null;
            DispatchingHandler_Test.MSG+= "UNfind" + counter + "times:" +times + message;
        }

      // if(times >= 84 )  System.err.println( "Find" + upsideHandlers +"is" +counter + "KEY:" + theOnlyKey + "TABLE:"+ DispatchingHandler.Handled +"times:" +times + message + MSG);

        if (HELLO.equals(answer)) {

            if( handler != null && Handled.contains(String.valueOf(rcvd_localId))) handler.receive(message);

                if (receivedHellos.contains(helloIdentifier)) {
                    //System.err.println("Error: Duplicate HELLO message detected: " + helloIdentifier);
                    return;
                }
                else {
                    boolean offered = this.queue.offer(newConnection);
                    if (offered) {
                         receivedHellos.add(helloIdentifier);
                    }
                    else {
                       // System.err.println("Error: Queue is full. Cannot add new connection: " + newConnection);
                    }
                    //times++;
                    //return;
            }
        }
        else {

            if(answer.startsWith(ACK)) {
                //    System.err.println( "Find" + upsideHandlers +"is" +counter + "KEY:" + theOnlyKey + "TABLE:"+ DispatchingHandler.Handled +"times:" +times + message + MSG);
                if (handler != null && Integer.parseInt(rcvd_remoteId) == theOnlyKey ) {
                    handler.receive(message);
                   // System.err.println( "Find" + upsideHandlers +"is" +counter + "HAND:"  +"KEY:" + theOnlyKey + "TABLE:"+ DispatchingHandler.Handled +"times:" +times + message + MSG);
                }
            }

            else {

            // For other messages, dispatch to the appropriate ConnectedHandler
                if (handler != null) {
                    handler.receive(message);
                }
                return;
        }

        }
    }

  //  @Override
//    public void close()
//    {
//       super.close();
//    }

}

