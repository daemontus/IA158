package ia158;

/**
 * Action taken by robot according to the received byte.
 */
class Action {

    private final int horizontal;
    private final int vertical;

    Action(int horizontal, int vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    static Action fromByte(byte h, byte v) {
        return new Action((int) h, (int) v);
    }

    public boolean isHorizontalNone() {
        return horizontal == -1;
    }

    public boolean isShoot() {
        return horizontal >= 45 && horizontal <= 55;
    }

    public boolean isLeft() {
        return horizontal < 45 && horizontal >= 0;
    }

    public boolean isRight() {
        return  horizontal > 55 && horizontal <= 100;
    }

    public boolean isVerticalNone() {
        return vertical == -1;
    }

    public boolean isUp() {
        return vertical > 55 && vertical <= 100;
    }

    public boolean isDown() {
        return vertical < 45 && vertical >= 0;
    }

    public int getHorizontal() {
        return this.horizontal;
    }

    public int getVertical() {
        return vertical;
    }

    @Override
    public String toString() {
        return "Horizonatal: " + Integer.toString(horizontal) + ", Vertical: " + Integer.toString(vertical);
    }
}