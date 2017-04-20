package ia158;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ReceiveJob extends Job {

    public ReceiveJob(int priority) {
        super(priority);
    }

    private byte[] buffer = new byte[1];
    private DatagramPacket packet = new DatagramPacket(buffer, 1);

    @Override
    public void run() {
        DatagramSocket socket = getResource("socket", DatagramSocket.class);
        FixedPriorityScheduler scheduler = getResource("scheduler", FixedPriorityScheduler.class);
        try {
            socket.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ControlJob.Action action = ControlJob.Action.fromByte(buffer[0]);
        scheduler.planJob(new ControlJob(0, action));
    }


}
