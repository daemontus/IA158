package ia158;

import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Main class for the robot controll.
 */
public class Main {

    private static long START_SEARCHING_TIME = 3000;
    private static long TIME_BEFORE_SHOOT = 1000;

    private static String eduroam = "147.251.45.255";
    private static String robot = "10.0.1.255";
    private static Long lostTargetTime;
    private static Long targetingTime;
    private static Action lastAction = Action.NONE;

    public static void main(String[] args) throws IOException {

        // Init
        InetAddress group = InetAddress.getByName(robot);
        DatagramSocket socket = new DatagramSocket(9999, group);
        long start = System.currentTimeMillis();
        lostTargetTime = start;
        targetingTime = null;

        // Motors
        RegulatedMotor rightWheels = new EV3LargeRegulatedMotor(MotorPort.A);
        RegulatedMotor leftWheels = new EV3LargeRegulatedMotor(MotorPort.B);
        RegulatedMotor shoot = new EV3MediumRegulatedMotor(MotorPort.C);

        rightWheels.setSpeed(rightWheels.getSpeed() / 2);
        leftWheels.setSpeed(leftWheels.getSpeed() / 2);

        // Run
        while (System.currentTimeMillis() < start + 60000) {
            // receive packet
            byte[] buffer = new byte[1];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            Action action = Action.fromByte(buffer[0]);
            System.out.println("Action: "+action);

            switch (action) {
                case RIGHT:
                    lostTargetTime = null;
                    targetingTime = null;
                    if (lastAction.equals(Action.RIGHT)) {
                        break;
                    }
                    // start rotating right
                    rightWheels.backward();
                    leftWheels.forward();
                    lastAction = action;
                    break;
                case LEFT:
                    lostTargetTime = null;
                    targetingTime = null;
                    if (lastAction.equals(Action.LEFT)) {
                        break;
                    }
                    // start rotating left
                    rightWheels.forward();
                    leftWheels.backward();
                    lastAction = action;
                    break;
                case SHOOT:
                    // stop rotating
                    rightWheels.stop(true);
                    leftWheels.stop(true);

                    lostTargetTime = null;
                    if (targetingTime == null) {
                        targetingTime = System.currentTimeMillis();
                    } else if (System.currentTimeMillis() > targetingTime + TIME_BEFORE_SHOOT) {
                        shoot.rotate(360, true);
                        targetingTime = null;
                    }
                    lastAction = action;
                    break;
                case NONE:
                    targetingTime = null;
                    if (Action.SHOOT.equals(lastAction)) {
                        if (lostTargetTime != null && System.currentTimeMillis() > lostTargetTime + START_SEARCHING_TIME) {
                            // start searching
                            rightWheels.backward();
                            leftWheels.forward();
                            lastAction = Action.RIGHT;
                            lostTargetTime = null;
                        } else if (lostTargetTime == null) {
                            lostTargetTime = System.currentTimeMillis();
                        }
                    }
                    break;
            }

        }

    }
}
