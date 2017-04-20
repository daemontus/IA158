package ia158;

/**
 * Long class packed into an object.
 */
public class MutableLong {
    private Long mutableLong = null;

    public Long get() {
        return mutableLong;
    }

    public void set(Long mutableLong) {
        this.mutableLong = mutableLong;
    }
}
