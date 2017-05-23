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
    private static Action lastAction = new Action(-1, -1);

    public static void main(String[] args) throws IOException {

        // Init
        InetAddress group = InetAddress.getByName(robot);
        DatagramSocket socket = new DatagramSocket(9999, group);
        long start = System.currentTimeMillis();
        lostTargetTime = start;
        targetingTime = null;

        // Motors
        direction = new EV3LargeRegulatedMotor(MotorPort.B);
        shoot = new EV3LargeRegulatedMotor(MotorPort.D);
        aim = new EV3MediumRegulatedMotor(MotorPort.C);

        float MAX_SPEED_DIR = direction.getMaxSpeed() / 3;
        float MAX_SPEED_AIM = aim.getMaxSpeed() / 10;
        direction.setSpeed(Math.round(MAX_SPEED_DIR) / 2);
        aim.setSpeed(Math.round(MAX_SPEED_AIM));

        // Run
        while (System.currentTimeMillis() < start + 22000) {
            // receive packet
            byte[] buffer = new byte[2];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            Action action = Action.fromByte(buffer[0], buffer[1]);
            System.out.println("Action: "+action);

            if (action.isRight()) {
                lostTargetTime = null;
                targetingTime = null;
                direction.setSpeed(Math.round(((float) (action.getHorizontal() - 50) / SPEED) * MAX_SPEED_DIR));
                System.out.println(Math.round(((float) (action.getHorizontal() - 50) / SPEED) * MAX_SPEED_DIR));
                if (!lastAction.isRight()) {
                    // start rotating right
                    direction.forward();
                    lastAction = action;
                }
            }

            if (action.isLeft()) {
                lostTargetTime = null;
                targetingTime = null;
                direction.setSpeed(Math.round(((float) (50 - action.getHorizontal())/ SPEED) * MAX_SPEED_DIR));
                System.out.println(Math.round(((float) (50 - action.getHorizontal())/ SPEED) * MAX_SPEED_DIR));
                if (!lastAction.isLeft()) {
                    // start rotating left
                    direction.backward();
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

            if (action.isHorizontalNone()) {
                targetingTime = null;
                if (lastAction.isShoot()) {
                    if (lostTargetTime != null && System.currentTimeMillis() > lostTargetTime + START_SEARCHING_TIME) {
                        // start searching
                        direction.forward();
                        lastAction = new Action(90, -1);
                        lostTargetTime = null;
                    } else if (lostTargetTime == null) {
                        lostTargetTime = System.currentTimeMillis();
                    }
                }
            }

            if (action.isUp()) {
                if (!aim.isStalled()) {
                    aim.forward();
                    aim.setSpeed(Math.round(((float) (action.getVertical() - 50) / SPEED) * MAX_SPEED_AIM));
                } else {
                    aim.stop(true);
                }
            }

            if (action.isDown()) {
                if (!aim.isStalled()) {
                    aim.backward();
                    aim.setSpeed(Math.round(((float) (50 - action.getVertical())/ SPEED) * MAX_SPEED_AIM));
                } else {
                    aim.stop(true);
                }
            }

            if (action.isVerticalNone()) {
                aim.stop(true);
            }

        }

    }
}
