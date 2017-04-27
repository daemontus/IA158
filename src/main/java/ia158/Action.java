package ia158;

/**
 * Action taken by robot according to the received byte.
 */
public enum Action {
    LEFT, RIGHT, SHOOT, NONE;

    public static Action fromByte(byte b) {
        switch (b) {
            case 1: return Action.LEFT;
            case 2: return Action.RIGHT;
            case 3: return Action.SHOOT;
            case 4: return Action.NONE;
            default: throw new IllegalStateException("Unknown action: "+b);
        }
    }
}
