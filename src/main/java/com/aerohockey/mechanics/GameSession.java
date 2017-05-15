package com.aerohockey.mechanics;

import com.aerohockey.mechanics.avatar.Ball;
import com.aerohockey.mechanics.avatar.Bonus;
import com.aerohockey.mechanics.avatar.GameUser;
import com.aerohockey.mechanics.base.Coords;
import com.aerohockey.model.UserProfile;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.aerohockey.mechanics.Config.*;

/**
 * Created by sergeybutorin on 14.04.17.
 */
public class GameSession {
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(GameSession.class);

    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);
    private final @NotNull Long sessionId;
    private final @NotNull GameUser top;
    private final @NotNull GameUser bottom;
    private final @NotNull List<Bonus> bonuses;
    private final @NotNull Ball ball;
    private ZonedDateTime lastBonusCreated;

    public GameSession(@NotNull UserProfile user1, @NotNull UserProfile user2) {
        this.sessionId = ID_GENERATOR.getAndIncrement();
        this.top = new GameUser(user1, true);
        this.bottom = new GameUser(user2, false);
        this.ball = new Ball(new Coords(0, 0));
        this.bonuses = new ArrayList<>();
        this.lastBonusCreated = ZonedDateTime.now();
    }

    public @NotNull GameUser getTop() {
        return top;
    }

    public @NotNull GameUser getBottom() {
        return bottom;
    }

    public @NotNull Ball getBall() {
        return ball;
    }

    public @NotNull GameUser getOpponent(@NotNull GameUser user) {
        if (user.equals(top)) {
            return bottom;
        } else {
            return top;
        }
    }

    public boolean isGameOver() {
        if (top.getScore() == MAX_SCORE || bottom.getScore() == MAX_SCORE) {
            LOGGER.info("Game over: session = " + sessionId + "; " + top.getLogin() + " with score " + top.getScore() +
                    "; " + bottom.getLogin() + " with score " + bottom.getScore());
            return true;
        }
        return false;
    }

    public void bonusManagement() {
        if (lastBonusCreated.plusSeconds(TIME_BETWEEN_BONUS).isBefore(ZonedDateTime.now()) &&
                bonuses.size() < MAX_BONUS_COUNT &&
                Math.random() < BONUS_PROBABILITY) {
            lastBonusCreated = ZonedDateTime.now();
            bonuses.add(new Bonus());
            LOGGER.info("New bonus created");
        }

        if (!bonuses.isEmpty() && bonuses.get(0).getExpired().isBefore(ZonedDateTime.now())) {
            LOGGER.info("Bonus removed");
            bonuses.remove(bonuses.get(0));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final GameSession session = (GameSession) o;

        return sessionId.equals(session.sessionId);
    }

    @Override
    public int hashCode() {
        return sessionId.hashCode();
    }
}
