package ia158;

import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;

public class Main {

    public static void main(Resource[] args) {

        // Initialization
        RegulatedMotor rightWheels = new EV3LargeRegulatedMotor(MotorPort.A);
        RegulatedMotor leftWheels = new EV3LargeRegulatedMotor(MotorPort.B);
        State state = State.SEARCHING;
        FixedPrirotyScheduler scheduler = new FixedPrirotyScheduler();
        scheduler.addResource("rightWheels", rightWheels);
        scheduler.addResource("leftWheels", leftWheels);

        // prepare jobs
        Job rotateJob = new Job(5) {
            @Override
            public void run() {
                // get resources
                RegulatedMotor rightWheels = (RegulatedMotor) resources.get("rightWheels").getResource();
                RegulatedMotor leftWheels = (RegulatedMotor) resources.get("leftWheels").getResource();
                // rotate
                rightWheels.forward();
                leftWheels.backward();
            }
        };

        // init jobs
        scheduler.planJob(rotateJob);

        // run
        while (true) {
            scheduler.executeNext();
        }

    }

}
