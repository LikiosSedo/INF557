package LAB_05to07;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class DispatchingHandler extends Handler {

    /** one will need it */
    private static final String HELLO = "--HELLO--";

    private static final String ACK = "--ACK--";

    /** An arbitrary base value for the numbering of handlers. **/
    private static int counter = 35000;

    /** the queue for pending connections */
    private final ArrayBlockingQueue<ConnectionParameters> queue;

    // to be completed
    private Set<Integer> HELLOReceived = new HashSet<>();

    private HashMap<Integer, Integer> ACKReceived = new HashMap<>();

    private static final boolean DEBUG = false;

    /**
     * Initializes a new dispatching handler with the specified parameters
     *
     * @param _under
     *                         the {@link Handler} on which the new handler will
     *                         be stacked
     * @param _queueCapacity
     *                         the capacity of the queue of pending connections
     */
    public DispatchingHandler(final Handler _under, int _queueCapacity) {
        super(_under, ++counter, false);
        this.queue = new ArrayBlockingQueue<ConnectionParameters>(_queueCapacity);
        // add other initializations if needed
    }

    /**
     * Retrieves and removes the head of the queue of pending connections, waiting
     * if no elements are present on this queue.
     *
     * @return the connection parameters record at the head of the queue
     * @throws InterruptedException
     *                                if the calling thread is interrupted while
     *                                waiting
     */
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
        // to be completed
        String[] parts = message.payload.split(";");

        int localId = Integer.parseInt(parts[0]);
        int remoteId = Integer.parseInt(parts[1]);
        int packetNumber = Integer.parseInt(parts[2]);
        String payload = parts[3];

        ConnectionParameters connectionParams = new ConnectionParameters(localId, message.sourceAddress);

        Handler connectedHandler = null;

        if (payload.equals(HELLO)) {
            if (!HELLOReceived.contains(localId)) {
                boolean isOffered = this.queue.offer(connectionParams);
                if (isOffered) {
                    HELLOReceived.add(localId);
                }
            }

            if (ACKReceived.containsKey(localId)) {
                connectedHandler = upsideHandlers.get(ACKReceived.get(localId));
                if (connectedHandler != null) {
                    connectedHandler.receive(message);
                }
            }
        }
        else {
            if (payload.equals(ACK)) {
                ACKReceived.put(localId, remoteId);
            }
            connectedHandler = upsideHandlers.get(remoteId);
            if (connectedHandler != null) {
                connectedHandler.receive(message);
            }
        }
    }
}
