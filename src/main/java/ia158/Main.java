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
        MutableLong lostTargetTime = null;
        FixedPriorityScheduler scheduler = new FixedPriorityScheduler();
        scheduler.addResource("lostTargetTime", lostTargetTime);
        scheduler.addResource("rightWheels", rightWheels);
        scheduler.addResource("leftWheels", leftWheels);
        scheduler.addResource("shoot", shoot);

        // prepare jobs
        Job rotateJob = new Job(10) {
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

        Job stopRotateJob = new Job(10) {
            @Override
            public void run() {
                // get resources
                RegulatedMotor rightWheels = getResource("rightWheels", RegulatedMotor.class);
                RegulatedMotor leftWheels = getResource("leftWheels", RegulatedMotor.class);
                // stop rotate
                rightWheels.stop();
                leftWheels.stop();
            }
        };

        // plan jobs
        scheduler.planJob(rotateJob);
        scheduler.planTask(new ReceiveJob(10), 20);
        scheduler.planJob(stopRotateJob, 15000);

        // run
        scheduler.run(16000);
    }

}
