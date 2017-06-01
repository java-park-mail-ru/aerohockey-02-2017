package com.aerohockey.mechanics;

/**
 * Created by sergeybutorin on 19.04.17.
 */
public class Config {
    public static final int PLAYGROUND_WIDTH = 160;
    public static final int PLAYGROUND_HEIGHT = 240;

    public static final double PLATFORM_SPEED = 0.08;
    public static final double PLATFORM_BENDING = 0.9;
    public static final int PLATFORM_WIDTH = 60;
    public static final double PLATFORM_WIDTH_CHANGE = 1.5;

    public static final byte MAX_SCORE = 7;

    public static final double BALL_RADIUS = 5;
    public static final double BALL_START_SPEED = 0.01;
    public static final double BALL_START_ACCELERATION = 1.05;
    public static final double BALL_NORMAL_SPEED = 0.15;
    public static final double BALL_SPEED_INCREASING = 0.0001;

    public static final byte MAX_BONUS_COUNT = 3;
    public static final byte BONUS_SIZE = 10;
    public static final byte MIN_DISTANCE_BETWEEN_BONUSES = BONUS_SIZE * 4;
    public static final byte BONUS_EXPIRED_TIME = 10;
    public static final byte TIME_BETWEEN_BONUS = 4;
    public static final double BONUS_PROBABILITY = 0.01;
    public static final double SIZE_BONUS_PROBABILITY = 0.5;
}
