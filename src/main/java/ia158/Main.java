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

    private static final long START_SEARCHING_TIME = 3000;
    private static final long TIME_BEFORE_SHOOT = 1000;
    // Speed: fastest 50, the bigger the slower
    private static final int SPEED = 90;

    private static RegulatedMotor direction;
    private static RegulatedMotor shoot;
    private static RegulatedMotor aim;

    private static String eduroam = "147.251.45.255";
    private static String robot = "10.0.1.255";
    private static Long lostTargetTime;
    private static Long targetingTime;
    private static Action lastAction = new Action(-1);

    // TESTING
    private static byte get_value(long starttime) {
        int step = 3000;
        if (System.currentTimeMillis() - starttime < step) {
            return (byte) 0;
        }
        if (System.currentTimeMillis() - starttime < 2*step) {
            return (byte) 255;
        }
        if (System.currentTimeMillis() - starttime < 3*step) {
            return (byte) 0;
        }
        if (System.currentTimeMillis() - starttime < 4*step) {
            return (byte) 30;
        }
        if (System.currentTimeMillis() - starttime < 5*step) {
            return (byte) 40;
        }
        return 50;
    }
    // TESTING

    public static void main(String[] args) throws IOException {

        // Init
        //InetAddress group = InetAddress.getByName(robot);
        //DatagramSocket socket = new DatagramSocket(9999, group);
        long start = System.currentTimeMillis();
        lostTargetTime = start;
        targetingTime = null;

        // Motors
        direction = new EV3LargeRegulatedMotor(MotorPort.B);
        shoot = new EV3LargeRegulatedMotor(MotorPort.D);
        aim = new EV3MediumRegulatedMotor(MotorPort.C);

        float MAX_SPEED = direction.getMaxSpeed() / 4;
        direction.setSpeed(Math.round(MAX_SPEED) / 2);

        System.out.println("MAX SPEED" + MAX_SPEED);

        // Run
        while (System.currentTimeMillis() < start + 18000) {
            // receive packet
            byte[] buffer = new byte[1];
            //DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            //socket.receive(packet);

            // TESTING
            buffer[0] = get_value(start);
            System.out.println("value: " + (int) buffer[0]);
            // TESTING

            Action action = Action.fromByte(buffer[0]);
            System.out.println("Action: "+action);

            if (action.isRight()) {
                lostTargetTime = null;
                targetingTime = null;
                direction.setSpeed(Math.round(((float) (action.getValue() - 50) / SPEED) * MAX_SPEED));
                System.out.println(Math.round(((float) (action.getValue() - 50) / SPEED) * MAX_SPEED));
                if (!lastAction.isRight()) {
                    // start rotating right
                    direction.backward();
                    lastAction = action;
                }
            }

            if (action.isLeft()) {
                lostTargetTime = null;
                targetingTime = null;
                direction.setSpeed(Math.round(((float) (50 - action.getValue())/ SPEED) * MAX_SPEED));
                System.out.println(Math.round(((float) (50 - action.getValue())/ SPEED) * MAX_SPEED));
                if (!lastAction.isLeft()) {
                    // start rotating left
                    direction.forward();
                    lastAction = action;
                }
            }

            if (action.isShoot()) {
                // stop rotating
                direction.stop(true);

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
                        direction.forward();
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
