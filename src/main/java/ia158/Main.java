package ia158;

import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;

public class Main {

    public static void main(String[] args) {

        // Initialization
        RegulatedMotor rightWheels = new EV3LargeRegulatedMotor(MotorPort.A);
        RegulatedMotor leftWheels = new EV3LargeRegulatedMotor(MotorPort.B);
        RegulatedMotor shoot = new EV3MediumRegulatedMotor(MotorPort.C);
        FixedPrirotyScheduler scheduler = new FixedPrirotyScheduler();
        scheduler.addResource("rightWheels", rightWheels);
        scheduler.addResource("leftWheels", leftWheels);
        scheduler.addResource("shoot", shoot);

        // prepare jobs
        Job rotateJob = new Job(5) {
            @Override
            public void run() {
                // get resources
                RegulatedMotor rightWheels = getResource("rightWheels", RegulatedMotor.class);
                RegulatedMotor leftWheels = getResource("leftWheels", RegulatedMotor.class);
                // rotate
                rightWheels.forward();
                leftWheels.backward();
            }
        };

        Job shootJob = new Job(10) {
            @Override
            public void run() {
                RegulatedMotor shoot = getResource("shoot", RegulatedMotor.class);

                shoot.forward();
            }
        };
        Job shootStopJob = new Job(10) {
            @Override
            public void run() {
                RegulatedMotor shoot = getResource("shoot", RegulatedMotor.class);

                shoot.stop();
            }
        };

        Job stopRotateJob = new Job(6) {
            @Override
            public void run() {
                // get resources
                RegulatedMotor rightWheels = getResource("rightWheels", RegulatedMotor.class);
                RegulatedMotor leftWheels = getResource("leftWheels", RegulatedMotor.class);
                // rotate
                rightWheels.stop();
                leftWheels.stop();
            }
        };

        // plan jobs
        scheduler.planJob(rotateJob);
        scheduler.planJob(stopRotateJob, 5000);
        scheduler.planJob(shootJob, 5000);
        scheduler.planJob(shootStopJob, 10000);

        // run
        scheduler.run(15000);
    }

}
