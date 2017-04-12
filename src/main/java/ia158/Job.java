package ia158;

import java.util.Map;

/**
 * A job to be scheduled for the robot.
 */
public abstract class Job implements Runnable {

    private int priority;
    protected Map<java.lang.String, Resource> resources;

    public int getPriority() {
        return priority;
    }

    public Job(int priority) {
        this.priority = priority;
    }

    public void setResources(Map<java.lang.String, Resource> resources) {
        this.resources = resources;
    }

    @Override
    public abstract void run();
}
