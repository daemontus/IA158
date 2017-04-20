package ia158;

public class ControlJob extends Job {

    private final Action action;

    enum Action {
        LEFT, RIGHT, NONE
    }

    public ControlJob(int priority, Action action) {
        super(priority);
        this.action = action;
    }

    @Override
    public void run() {

    }
}