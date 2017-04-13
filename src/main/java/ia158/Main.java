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
        State state = State.SEARCHING;
        FixedPrirotyScheduler scheduler = new FixedPrirotyScheduler();
        scheduler.addResource("rightWheels", rightWheels);
        scheduler.addResource("leftWheels", leftWheels);
        scheduler.addResource("shoot", shoot);

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

        Job shootJob = new Job(10) {
            @Override
            public void run() {
                RegulatedMotor shoot = (RegulatedMotor) resources.get("shoot").getResource();

                System.out.println("Fire in the hole!!");

                shoot.forward();
            }
        };
        Job shootStopJob = new Job(10) {
            @Override
            public void run() {
                RegulatedMotor shoot = (RegulatedMotor) resources.get("shoot").getResource();

                shoot.stop();
            }
        };

        Job stop = new Job(6) {
            @Override
            public void run() {
                // get resources
                RegulatedMotor rightWheels = (RegulatedMotor) resources.get("rightWheels").getResource();
                RegulatedMotor leftWheels = (RegulatedMotor) resources.get("leftWheels").getResource();
                // rotate
                rightWheels.stop();
                leftWheels.stop();
            }
        };

        // init jobs
        scheduler.planJob(rotateJob);

        // run
        long start = System.currentTimeMillis();
        while (true) {
            scheduler.executeNext();
            if (System.currentTimeMillis() > start + 5000) {
                scheduler.planJob(stop);
                scheduler.planJob(shootJob);
            }

            if (System.currentTimeMillis() > start + 10000) {
                scheduler.planJob(shootStopJob);
            }
            if (System.currentTimeMillis() > start + 15000) {
                break;
            }
        }

    }

}
