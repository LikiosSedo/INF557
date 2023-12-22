package LAB_05to07;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * The GroundLayer class provides a mechanism for sending and receiving messages
 * over the network using the UDP protocol. The messages are exchanged in the form
 * of String payloads and are transported via Datagram packets.
 * <p>
 * The layer can be started with a specified local port and can be associated with
 * a message handler that will be invoked upon the receipt of messages.
 * The layer also supports a reliability parameter that can be used to simulate
 * the potential loss of packets over the network.
 * </p>
 */
public class GroundLayer {

    // Charset converter to ensure consistent encoding of string messages across different systems.
    private static final Charset CONVERTER = StandardCharsets.UTF_8;

    // Probability that a message will be sent. This can be adjusted to simulate network unreliability.
    public static double RELIABILITY = 0.9;

    private static DatagramSocket localSocket = null; // Socket for sending and receiving datagram packets.
    private static Thread receiver = null;            // Thread for listening and processing incoming datagram packets.
    private static Handler handler = null;            // Message handler that will be invoked upon message receipt.

    /**
     * Initializes the GroundLayer with the specified local port and message handler.
     * Starts a background thread to listen for incoming datagram packets.
     *
     * @param _localPort The local UDP port to bind to.
     * @param _handler   The message handler that will process received messages.
     * @throws SocketException If there's an error during the socket creation.
     */
    public static void start(int _localPort, Handler _handler) throws SocketException {
        if (handler != null) {
            throw new IllegalStateException("GroundLayer is already started");
        }

        handler = _handler;
        localSocket = new DatagramSocket(_localPort);

        receiver = new Thread(() -> {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    localSocket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength(), CONVERTER);
                    Message message = new Message(received, packet.getSocketAddress().toString());
                    handler.receive(message);
                } catch (IOException e) {
                    System.err.println("Error in receiving: " + e.getMessage());
                    break;
                }
            }
        });

        receiver.setDaemon(true);
        receiver.start();
    }

    /**
     * Sends a message to a specified destination if it meets the reliability criteria.
     *
     * @param payload            The message string to send.
     * @param destinationAddress The target address to send the message to.
     */
    public static void send(String payload, SocketAddress destinationAddress) {
        double random = Math.random();
       // System.out.println("Random ="+random);
        if (random <= RELIABILITY) {
            byte[] data = payload.getBytes(CONVERTER);
            DatagramPacket packet = new DatagramPacket(data, data.length, destinationAddress);

            try {
                localSocket.send(packet);
            } catch (IOException e) {
                System.err.println("Error in sending: " + e.getMessage());
            }
        }
    }

    /**
     * Shuts down the GroundLayer by interrupting the receiver thread and closing the socket.
     * Once closed, the layer can be restarted on a new port.
     */
    public static void close() {
        if (receiver != null && !receiver.isInterrupted()) {
            receiver.interrupt();
        }
        if (localSocket != null && !localSocket.isClosed()) {
            localSocket.close();
        }
        handler = null; // Reset the handler
        System.err.println("GroundLayer closed");
    }

}
