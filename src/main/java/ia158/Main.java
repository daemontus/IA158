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
    private static Action lastAction = new Action(-1);

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

        float MAX_SPEED = rightWheels.getMaxSpeed();

        // Run
        while (System.currentTimeMillis() < start + 60000) {
            // receive packet
            byte[] buffer = new byte[1];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            Action action = Action.fromByte(buffer[0]);
            System.out.println("Action: "+action);

            if (action.isRight()) {
                lostTargetTime = null;
                targetingTime = null;
                rightWheels.setSpeed(Math.round(((action.getValue() - 50) / 50) * MAX_SPEED));
                leftWheels.setSpeed(Math.round(((action.getValue() - 50) / 50) * MAX_SPEED));
                if (!lastAction.isRight()) {
                    // start rotating right
                    rightWheels.backward();
                    leftWheels.forward();
                    lastAction = action;
                }
            }

            if (action.isLeft()) {
                lostTargetTime = null;
                targetingTime = null;
                rightWheels.setSpeed(Math.round(((action.getValue() + 5)/ 45) * MAX_SPEED));
                leftWheels.setSpeed(Math.round(((action.getValue() + 5)/ 45) * MAX_SPEED));
                if (!lastAction.isLeft()) {
                    // start rotating left
                    rightWheels.forward();
                    leftWheels.backward();
                    lastAction = action;
                }
            }

            if (action.isShoot()) {
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
            }

            if (action.isNone()) {
                targetingTime = null;
                if (lastAction.isShoot()) {
                    if (lostTargetTime != null && System.currentTimeMillis() > lostTargetTime + START_SEARCHING_TIME) {
                        // start searching
                        rightWheels.backward();
                        leftWheels.forward();
                        lastAction = new Action(90);
                        lostTargetTime = null;
                    } else if (lostTargetTime == null) {
                        lostTargetTime = System.currentTimeMillis();
                    }
                }
            }

        }

    }
}
