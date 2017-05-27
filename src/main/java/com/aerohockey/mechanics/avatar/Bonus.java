package com.aerohockey.mechanics.avatar;

import com.aerohockey.mechanics.GameSession;
import com.aerohockey.mechanics.base.BonusSnap;
import com.aerohockey.mechanics.base.Coords;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.util.List;

import static com.aerohockey.mechanics.Config.*;
import static java.lang.StrictMath.signum;

/**
 * Created by sergeybutorin on 15/05/2017.
 */
public class Bonus {

    public enum Types {
        BALL_INCREASE,
        BALL_DECREASE,
        BALL_MULTIPLY,
        PLATFORM_INCREASE,
        PLATFORM_DECREASE;

        public static Types getRandom() {
            return values()[(int) (Math.random() * values().length)];
        }
    }

    private final ZonedDateTime expired;
    private Coords coords;
    private final Types type;
    private Ball activatedBall;

    public Bonus(@NotNull GameSession gameSession) {
        while (true) {
            this.coords = new Coords(generateCoord(-PLAYGROUND_WIDTH / 4, PLAYGROUND_WIDTH / 4),
                    generateCoord(-PLAYGROUND_HEIGHT / 4, PLAYGROUND_HEIGHT / 4));
            if (isInBallWay(gameSession.getBalls()) || anotherBonusCollision(gameSession.getBonuses())) {
                continue;
            }
            break;
        }
        type = Types.getRandom();
        expired = ZonedDateTime.now().plusSeconds(BONUS_EXPIRED_TIME);
    }

    public boolean checkBallBonusCollision(@NotNull Ball ball) {
        return (Math.pow((ball.getCoords().x - coords.x), 2) + Math.pow((ball.getCoords().y - coords.y), 2)) < Math.pow((ball.getRadius() + BONUS_SIZE), 2);
    }

    public ZonedDateTime getExpired() {
        return expired;
    }

    public void activate(@NotNull GameSession gameSession, @NotNull Ball pickedUpBall) {
        this.activatedBall = pickedUpBall;
        switch (type) {
            case BALL_DECREASE:
                activatedBall.setRadius(BALL_RADIUS / 2);
                break;
            case BALL_INCREASE:
                activatedBall.setRadius(BALL_RADIUS * 2);
                break;
            case BALL_MULTIPLY:
                gameSession.addBall(new Ball(-signum(activatedBall.getSpeedY())));
                break;
            case PLATFORM_DECREASE:
                activatedBall.getUser(gameSession).getPlatform().setWidth(PLATFORM_WIDTH / PLATFORM_WIDTH_CHANGE);
                break;
            case PLATFORM_INCREASE:
                activatedBall.getUser(gameSession).getPlatform().setWidth(PLATFORM_WIDTH * PLATFORM_WIDTH_CHANGE);
                break;
        }
    }

    public void deactivate(@NotNull GameSession gameSession) {
        switch (type) {
            case BALL_DECREASE:
            case BALL_INCREASE:
                activatedBall.setRadius(BALL_RADIUS);
                break;
            case BALL_MULTIPLY:
                break;
            case PLATFORM_DECREASE:
            case PLATFORM_INCREASE:
                gameSession.getTop().getPlatform().setWidth(PLATFORM_WIDTH);
                gameSession.getBottom().getPlatform().setWidth(PLATFORM_WIDTH);
                break;
        }
    }

    public double generateCoord(int min, int max) {
        max -= min;
        return (Math.random() * ++max) + min;
    }

    private boolean isInBallWay(@NotNull List<Ball> balls) {
        for (Ball ball : balls) {
            final double dx = coords.x - ball.getCoords().x;
            final double dy = coords.y - ball.getCoords().y;
            if (checkBallBonusCollision(new Ball(new Coords((
                    ball.getCoords().x + dx),
                    ball.getCoords().y + (dx / ball.getSpeedX()) * ball.getSpeedY()), ball.getRadius())) ||
                    checkBallBonusCollision(new Ball(new Coords(
                            ball.getCoords().x + (dy / ball.getSpeedY()) * ball.getSpeedX(),
                            ball.getCoords().y + dy), ball.getRadius()))) {
                return true;
            }
        }
        return false;
    }

    private boolean anotherBonusCollision(@NotNull List<Bonus> bonuses) {
        for (Bonus bonus : bonuses) {
            if ((Math.pow((bonus.coords.x - coords.x), 2) + Math.pow((bonus.coords.y - coords.y), 2)) < Math.pow(MIN_DISTANCE_BETWEEN_BONUSES, 2)) {
                return true;
            }
        }
        return false;
    }

    public BonusSnap getSnap() {
        return new BonusSnap(coords, type.toString());
    }
}
