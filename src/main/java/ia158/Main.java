package ia158;

import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

    public static void main(String[] args) throws IOException, InterruptedException {

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

        float MAX_DIR = direction.getMaxSpeed() / 4;
        float MAX_AIM = aim.getMaxSpeed() / 20;
        direction.setSpeed((int) MAX_DIR);
        aim.setSpeed((int) MAX_AIM);

        AtomicInteger correctionH = new AtomicInteger(0);
        AtomicInteger horizontal = new AtomicInteger(0);
        AtomicInteger vertical = new AtomicInteger(0);
        AtomicBoolean gone = new AtomicBoolean(true);

        int timeout = 60000;

        Thread network = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[2];
                // receive UDP packets and update global state
                while (System.currentTimeMillis() < start + timeout) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        socket.receive(packet);
                        gone.set(buffer[0] == -1 || buffer[1] == -1);
                        if (buffer[0] != -1 && buffer[1] != -1) {
                            int H = buffer[0] - 50;
                            System.out.println("Received: "+V);
                            H += correctionH.getAndSet(0);
                            System.out.println("Corrected: "+V);
                            horizontal.set(H);
                            vertical.set(buffer[1] - 50);
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        });

        Thread controlD = new Thread(() -> {
            // Check global state and tachometers, update motor status accordingly
            int lastTacho = 0;
            while (System.currentTimeMillis() < start + timeout) {
                int newTacho = direction.getTachoCount();
                int lastMove = newTacho - lastTacho;
                correctionH.addAndGet(lastMove);
                lastTacho = newTacho;
                int todo = horizontal.addAndGet(-lastMove/4);
                System.out.println("D: Todo: "+todo+" last move: "+lastMove);
                if (Math.abs(todo) > 10) {
                    //direction.setSpeed((int) (Math.min(1.0, Math.abs(todo) / 25.0) * MAX_DIR));
                    if (todo > 0) direction.forward();
                    else direction.backward();
                } else {
                    direction.stop(true);
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        });

        Thread controlA = new Thread(() -> {
            // Check global state and tachometers, update motor status accordingly
            int lastTacho = 0;
            while (System.currentTimeMillis() < start + timeout) {
                int newTacho = aim.getTachoCount();
                int lastMove = newTacho - lastTacho;
                lastTacho = newTacho;
                int todo = vertical.addAndGet(-lastMove);
                System.out.println("V: Todo: "+todo+" last move: "+lastMove);
                if (Math.abs(todo) > 10) {
                    //direction.setSpeed((int) (Math.min(1.0, Math.abs(todo) / 50.0) * MAX_AIM));
                    if (todo > 0) aim.forward();
                    else aim.backward();
                    //aim.rotate(todo, true);
                } else {
                    aim.stop(true);
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        });

        Thread controlS = new Thread(() -> {
            // monitor the H/V coordinates and shoot if locked for more than 1s
            int lock = 0;
            while (System.currentTimeMillis() < start + timeout) {
                int h = horizontal.get();
                int v = vertical.get();
                if (Math.abs(h) < 10 && Math.abs(v) < 10 && !gone.get()) {
                    System.out.println("Lock: "+lock);
                    lock += 1;
                } else {
                    lock = 0;
                }
                if (lock > 20) {    // locked for 1s
                    shoot.rotate(360);
                    lock = 0;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        network.start();
        controlD.start();
        controlA.start();
        controlS.start();

        network.join();
        controlD.join();
        controlA.join();
        controlS.join();
    }
}
