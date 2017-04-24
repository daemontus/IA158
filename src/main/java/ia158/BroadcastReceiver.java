package ia158;

import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * this class is for internal testing and it serves as an example of a working UDP broadcast receiver
 */
public class BroadcastReceiver {

    private static String eduroam = "147.251.45.255";
    private static String robot = "10.0.1.255";

    public static void main(String[] args) throws IOException {

        InetAddress group = InetAddress.getByName(robot);
        DatagramSocket socket = new DatagramSocket(9999, group);

        long start = System.currentTimeMillis();

        RegulatedMotor rightWheels = new EV3LargeRegulatedMotor(MotorPort.A);
        RegulatedMotor leftWheels = new EV3LargeRegulatedMotor(MotorPort.B);

        while (System.currentTimeMillis() < start + 60000) {
            byte[] buffer = new byte[1];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            ControlJob.Action action = ControlJob.Action.fromByte(buffer[0]);

            switch (action) {
                case RIGHT:
                    // stop rotating
                    rightWheels.stop(true);
                    leftWheels.stop(true);
                    // move left
                    rightWheels.rotate(-20, true);
                    leftWheels.rotate(20, true);
                    break;
                case LEFT:
                    // stop rotating
                    rightWheels.stop(true);
                    leftWheels.stop(true);
                    // move left
                    rightWheels.rotate(20, true);
                    leftWheels.rotate(-20, true);
                    break;
            }

            System.out.println("Action: "+action);
        }

    }
}
