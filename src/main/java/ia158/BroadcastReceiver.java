package ia158;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * this class is for internal testing and it serves as an example of a working UDP broadcast receiver
 */
public class BroadcastReceiver {

    private static String eduroam = "147.251.45.255";
    private static String robot = "10.0.1.255";

    public static void main(String[] args) throws IOException {

        InetAddress group = InetAddress.getByName(eduroam);
        DatagramSocket socket = new DatagramSocket(9999, group);


        while (true) {
            byte[] buffer = new byte[1];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            ControlJob.Action action = ControlJob.Action.fromByte(buffer[0]);

            System.out.println("Action: "+action);
        }

    }
}
