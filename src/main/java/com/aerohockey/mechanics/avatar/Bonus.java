package com.aerohockey.mechanics.avatar;

import com.aerohockey.mechanics.GameSession;
import com.aerohockey.mechanics.base.BonusSnap;
import com.aerohockey.mechanics.base.Coords;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.List;

import static com.aerohockey.mechanics.Config.*;
import static java.lang.StrictMath.signum;

/**
 * Created by sergeybutorin on 15/05/2017.
 */
public class Bonus {

    public enum Types {
        BALL_SIZE,
        BALL_MULTIPLY,
        PLATFORM_SIZE,
        SHIELD;

        public static @Nullable Types getRandom(@NotNull List<Bonus> bonuses, @NotNull List<Bonus> activeBonuses) {
            final Types type = values()[(int) (Math.random() * values().length)];
            for (Bonus bonus : bonuses) {
                if (bonus.type == type){
                    return null;
                }
            }
            for (Bonus bonus : activeBonuses) {
                if (bonus.type == type){
                    return null;
                }
            }
            return type;
        }
    }

    public enum ExtendedTypes {
        BALL_INCREASE,
        BALL_DECREASE,
        BALL_MULTIPLY,
        PLATFORM_DECREASE,
        PLATFORM_INCREASE,
        SHIELD;

        public static ExtendedTypes getExtendedType(@NotNull Types type) {
            switch (type) {
                case BALL_SIZE:
                    return Math.random() > SIZE_BONUS_PROBABILITY ? BALL_INCREASE : BALL_DECREASE;
                case PLATFORM_SIZE:
                    return Math.random() > SIZE_BONUS_PROBABILITY ? PLATFORM_INCREASE : PLATFORM_DECREASE;
                case SHIELD:
                    return SHIELD;
                case BALL_MULTIPLY:
                default:
                    return BALL_MULTIPLY;
            }
        }
    }

    private final ZonedDateTime expired;
    private Coords coords;
    private Types type;
    private final ExtendedTypes extendedType;
    private Ball activatedBall;
    private Platform changedPlatform;

    public Bonus(@NotNull GameSession gameSession) {
        while (true) {
            type = Types.getRandom(gameSession.getBonuses(), gameSession.getActiveBonuses());
            if (type == null) {
                continue;
            }
            break;
        }
        extendedType = ExtendedTypes.getExtendedType(type);

        while (true) {
            coords = new Coords(generateCoord(-PLAYGROUND_WIDTH / 5, PLAYGROUND_WIDTH / 5),
                    generateCoord(-PLAYGROUND_HEIGHT / 5, PLAYGROUND_HEIGHT / 5));
            if (Math.abs(coords.x) < BONUS_SIZE || isInBallWay(gameSession.getBalls())
                    || anotherBonusCollision(gameSession.getBonuses())) {
                continue;
            }
            break;
        }

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
        this.changedPlatform = activatedBall.getUser(gameSession).getPlatform();
        switch (extendedType) {
            case BALL_DECREASE:
                activatedBall.setRadius(BALL_RADIUS / 2);
                break;
            case BALL_INCREASE:
                activatedBall.setRadius(BALL_RADIUS * 2);
                break;
            case BALL_MULTIPLY:
                gameSession.addBall(new Ball(signum(activatedBall.getSpeedY())));
                break;
            case PLATFORM_DECREASE:
                changedPlatform.setWidth(PLATFORM_WIDTH / PLATFORM_WIDTH_CHANGE);
                break;
            case PLATFORM_INCREASE:
                changedPlatform.setWidth(PLATFORM_WIDTH * PLATFORM_WIDTH_CHANGE);
                break;
            case SHIELD:
                changedPlatform.activateShield();
                break;
        }
    }

    public void deactivate() {
        switch (type) {
            case BALL_SIZE:
                activatedBall.setRadius(BALL_RADIUS);
                break;
            case PLATFORM_SIZE:
                changedPlatform.setWidth(PLATFORM_WIDTH);
                break;
            case BALL_MULTIPLY:
            case SHIELD:
                break;
        }
    }

    private double generateCoord(int min, int max) {
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
        return new BonusSnap(coords, extendedType.toString());
    }
}
