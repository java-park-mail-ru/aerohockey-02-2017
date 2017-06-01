package com.aerohockey.mechanics.avatar;

import com.aerohockey.mechanics.base.Coords;
import com.aerohockey.mechanics.base.PlatformCoords;
import org.jetbrains.annotations.NotNull;

import static com.aerohockey.mechanics.Config.PLATFORM_WIDTH;
import static com.aerohockey.mechanics.Config.PLAYGROUND_HEIGHT;
import static com.aerohockey.mechanics.Config.PLAYGROUND_WIDTH;

/**
 * Created by sergeybutorin on 23.04.17.
 */
public class Platform {
    private final PlatformCoords coords;
    @SuppressWarnings("InstanceVariableNamingConvention")
    private final double y;
    private double width;
    private final double height;
    private boolean shield;

    public Platform(boolean isTop) {
        this.coords = new PlatformCoords();
        this.width = PLATFORM_WIDTH;
        this.height = 15;
        this.y = isTop ? PLAYGROUND_HEIGHT / 2 : -PLAYGROUND_HEIGHT / 2;
        this.shield = true;
    }

    public void move(double dx) {
        coords.x += dx;
        checkSideCollision();
    }

    private void checkSideCollision() {
        if (coords.x > PLAYGROUND_WIDTH / 2 - width / 2) {
            coords.x = PLAYGROUND_WIDTH / 2 - width / 2;
        } else if (coords.x < -PLAYGROUND_WIDTH / 2 + width / 2) {
            coords.x = -PLAYGROUND_WIDTH / 2 + width / 2;
        }
    }

    public boolean checkBallCollision(@NotNull Coords ballCoords, double radius) {
        return Math.abs(y - ballCoords.y) < height + radius &&
                Math.abs(this.coords.x - ballCoords.x) < width / 2 + radius;
    }

    public void setWidth(double width) {
        this.width = width;
        checkSideCollision();
    }

    public PlatformCoords getCoords() {
        return coords;
    }

    public double getWidth() {
        return width;
    }

    public boolean isShield() {
        return shield;
    }

    public void activateShield() {
        this.shield = true;
    }

    public void destroyShield() {
        this.shield = false;
    }
}
