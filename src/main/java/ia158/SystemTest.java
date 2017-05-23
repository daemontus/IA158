package ia158;

import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.RegulatedMotorListener;

public class SystemTest {

    private static RegulatedMotor direction;
    private static RegulatedMotor shoot;
    private static RegulatedMotor aim;

    public static void main(String[] args) throws InterruptedException {
        direction = new EV3LargeRegulatedMotor(MotorPort.B);
        shoot = new EV3LargeRegulatedMotor(MotorPort.D);
        aim = new EV3MediumRegulatedMotor(MotorPort.C);

        //direction.setSpeed((int) (direction.getMaxSpeed() / 5));
        System.out.println("Aim speed: "+aim.getMaxSpeed());
        aim.setSpeed((int) (aim.getMaxSpeed() / 15));
        direction.setSpeed((int) (direction.getMaxSpeed() / 4));

        RegulatedMotorListener logListener = new RegulatedMotorListener() {
            @Override
            public void rotationStarted(RegulatedMotor motor, int tachoCount, boolean stalled, long timeStamp) {
                System.out.println("Rotation started on "+motorString(motor)+" stalled: "+stalled+" count: "+tachoCount);
            }

            @Override
            public void rotationStopped(RegulatedMotor motor, int tachoCount, boolean stalled, long timeStamp) {
                System.out.println("Rotation stopped on "+motorString(motor)+" stalled: "+stalled+" count: "+tachoCount);
            }
        };

        direction.addListener(logListener);
        aim.addListener(logListener);
        shoot.addListener(logListener);

        for (int i=0; i<2; i++) {
            aim.rotate(90);
            aim.rotate(-90);

            direction.forward();

            Thread.sleep(5000);

            direction.stop();

            shoot.rotate(360);
        }

       //aim.forward();
       //Thread.sleep(20);
       //aim.stop();
        //aim.rotate(-90);
        //direction.rotate(360);
        /*direction.forward();
        Thread.sleep(10000);
        direction.stop();*/
    }

    private static String motorString(RegulatedMotor motor) {
        if (motor == direction) return "direction";
        else if (motor == shoot) return "shoot";
        else if (motor == aim) return "aim";
        else return "unknown";
    }
}
