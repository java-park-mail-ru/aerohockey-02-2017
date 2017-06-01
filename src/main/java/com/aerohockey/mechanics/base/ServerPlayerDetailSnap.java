package com.aerohockey.mechanics.base;

/**
 * Created by sergeybutorin on 16/05/2017.
 */
@SuppressWarnings("unused")
public class ServerPlayerDetailSnap extends ServerPlayerSnap {
    private byte score;
    private double width;
    private boolean shield;

    public byte getScore() {
        return score;
    }

    public void setScore(byte score) {
        this.score = score;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public boolean isShield() {
        return shield;
    }

    public void setShield(boolean shield) {
        this.shield = shield;
    }
}
