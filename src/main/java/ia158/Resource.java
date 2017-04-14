package ia158;

/**
 * Resource of the robot, which is used by jobs.
 */
public class Resource {
    private     String name;
    private boolean locked;
    private Object resource;

    public Resource(String name, Object resource) {
        this.name = name;
        this.resource = resource;
    }

    public Object getResource() {
        return resource;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}
