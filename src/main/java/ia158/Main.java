package ia158;

import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {

    private static String broadcastIP = "10.0.1.255";

    public static void main(String[] args) throws SocketException, UnknownHostException {

        // Initialization
        RegulatedMotor rightWheels = new EV3LargeRegulatedMotor(MotorPort.A);
        RegulatedMotor leftWheels = new EV3LargeRegulatedMotor(MotorPort.B);
        RegulatedMotor shoot = new EV3MediumRegulatedMotor(MotorPort.C);
        MutableLong lostTargetTime = new MutableLong();
        FixedPriorityScheduler scheduler = new FixedPriorityScheduler();
        scheduler.addResource("lostTargetTime", lostTargetTime);
        scheduler.addResource("rightWheels", rightWheels);
        scheduler.addResource("leftWheels", leftWheels);
        scheduler.addResource("shoot", shoot);

        // Connection initialization
        InetAddress group = InetAddress.getByName(broadcastIP);
        DatagramSocket socket = new DatagramSocket(9999, group);
        socket.setSoTimeout(10);    // timeout is 10ms - half the period
        scheduler.addResource("socket", socket);
        scheduler.addResource("scheduler", scheduler);

        scheduler.planTask(new ReceiveJob(0), 20);

        // run
        scheduler.run(16000);

        socket.close();
    }

}
