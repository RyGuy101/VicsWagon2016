package ioio.examples.hello;

public class GridSquare {
    private int myX = 0;
    private int myY = 0;
    private int myDirection = 0;

    public GridSquare(int x, int y, int direction) {
        myX = x;
        myY = y;
        myDirection = direction;
    }

    public int getX() {
        return myX;
    }

    // THIS IS A CHANGE
    public int getY() {
        return myY;
    }

    public int getDirection() {
        return myDirection;
    }

    public void setX(int newX) {
        myX = newX;
    }

    public void setY(int newY) {
        myY = newY;
    }

    public void setDirection(int newDirection) {
        myDirection = newDirection;
    }
}
