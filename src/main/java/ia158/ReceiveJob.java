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
            //TODO
            e.printStackTrace();
        }
        if (buffer[0] == 0) {
            scheduler.planJob(new ControlJob(0, ControlJob.Action.NONE));
        } else if (buffer[0] == 1) {
            scheduler.planJob(new ControlJob(0, ControlJob.Action.LEFT));
        } else if (buffer[0] == 2) {
            scheduler.planJob(new ControlJob(0, ControlJob.Action.RIGHT));
        } else {
            throw new IllegalStateException("wtf are you sending me? "+buffer[0]);
        }
    }


}
