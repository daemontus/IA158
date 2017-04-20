package ia158;

import lejos.robotics.RegulatedMotor;

public class ControlJob extends Job {

    private final Action action;
    private long START_SEARCHING_TIME = 3000;

    enum Action {
        LEFT, RIGHT, SHOOT, NONE
    }

    private Job SHOOT_JOB = new Job(9) {
        @Override
        public void run() {
            RegulatedMotor shoot = getResource("shoot", RegulatedMotor.class);
            shoot.rotate(360, true);
        }
    };

    public ControlJob(int priority, Action action) {
        super(priority);
        this.action = action;
    }

    @Override
    public void run() {
        // get resources
        RegulatedMotor rightWheels = getResource("rightWheels", RegulatedMotor.class);
        RegulatedMotor leftWheels = getResource("leftWheels", RegulatedMotor.class);
        FixedPriorityScheduler scheduler = getResource("scheduler", FixedPriorityScheduler.class);
        MutableLong lostTargetTime = getResource("lostTargetTime", MutableLong.class);
        switch (action) {
            case LEFT:
                // stop rotating
                rightWheels.stop();
                leftWheels.stop();
                // move left
                rightWheels.rotate(10);
                leftWheels.rotate(-10);
                break;
            case RIGHT:
                // stop rotating
                rightWheels.stop();
                leftWheels.stop();
                // move right
                rightWheels.rotate(-10);
                leftWheels.rotate(10);
                break;
            case SHOOT:
                scheduler.planJob(SHOOT_JOB);
            case NONE:
                if (lostTargetTime.get() == null) {
                    lostTargetTime.set(System.currentTimeMillis());
                    break;
                }
                if (System.currentTimeMillis() - lostTargetTime.get() > START_SEARCHING_TIME) {
                    // start rotating
                    rightWheels.forward();
                    leftWheels.forward();
                    lostTargetTime.set(null);
                }
        }
    }
}