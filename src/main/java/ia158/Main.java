package ia158;

import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;
import lejos.utility.Delay;

public class Main {

    public static void main(String[] args) {

        System.out.println("Hello world from EV3");

        RegulatedMotor motor = new EV3LargeRegulatedMotor(MotorPort.B);

        motor.forward();

        Delay.msDelay(5000);

        motor.stop();

    }

}
