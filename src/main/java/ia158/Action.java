package ia158;

/**
 * Action taken by robot according to the received byte.
 */
class Action {

    private final int value;

    Action(int value) {
        this.value = value;
    }

    static Action fromByte(byte b) {
        return new Action((int) b);
    }

    public boolean isNone() {
        return value == -1;
    }

    public boolean isShoot() {
        return value >= 45 && value <= 55;
    }

    public boolean isLeft() {
        return value < 45 && value >= 0;
    }

    public boolean isRight() {
        return  value > 55 && value <= 100;
    }
    
}