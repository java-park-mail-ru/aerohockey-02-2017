package com.aerohockey.mechanics;

import com.aerohockey.mechanics.avatar.Ball;
import com.aerohockey.mechanics.avatar.Bonus;
import com.aerohockey.mechanics.avatar.GameUser;
import com.aerohockey.model.UserProfile;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private final @NotNull List<Ball> balls;
    private final @NotNull Map<ZonedDateTime, Bonus> activeBonuses;
    private ZonedDateTime lastBonusCreated;
    private boolean stateChanged;

    public GameSession(@NotNull UserProfile user1, @NotNull UserProfile user2) {
        this.sessionId = ID_GENERATOR.getAndIncrement();
        this.top = new GameUser(user1, true);
        this.bottom = new GameUser(user2, false);
        this.balls = new ArrayList<>();
        balls.add(new Ball());
        this.bonuses = new ArrayList<>();
        this.lastBonusCreated = ZonedDateTime.now();
        this.activeBonuses = new ConcurrentHashMap<>();
        stateChanged = true;
    }

    public @NotNull GameUser getTop() {
        return top;
    }

    public @NotNull GameUser getBottom() {
        return bottom;
    }

    public @NotNull List<Ball> getBalls() {
        return balls;
    }

    public @NotNull GameUser getOpponent(@NotNull GameUser user) {
        if (user.equals(top)) {
            return bottom;
        } else {
            return top;
        }
    }

    public boolean isGameOver() {
        if (top.getScore() >= MAX_SCORE || bottom.getScore() >= MAX_SCORE) {
            LOGGER.info("Game over: session = " + sessionId + "; " + top.getLogin() + " with score " + top.getScore() +
                    "; " + bottom.getLogin() + " with score " + bottom.getScore());
            return true;
        }
        return false;
    }

    public @NotNull List<Bonus> getBonuses() {
        return bonuses;
    }

    public @NotNull List<Bonus> getActiveBonuses() {
        return new ArrayList<>(activeBonuses.values());
    }

    public void bonusManagement() {
        generateBonuses();
        removeExpiredBonuses();
        activateBonuses();
        deactivateBonuses();
    }

    private void generateBonuses() {
        if (lastBonusCreated.plusSeconds(TIME_BETWEEN_BONUS).isBefore(ZonedDateTime.now()) &&
                bonuses.size() + activeBonuses.size() < MAX_BONUS_COUNT &&
                Math.random() < BONUS_PROBABILITY) {
            lastBonusCreated = ZonedDateTime.now();
            bonuses.add(new Bonus(this));
            stateChanged = true;
        }
    }

    private void removeExpiredBonuses() {
        if (!bonuses.isEmpty() && bonuses.get(0).getExpired().isBefore(ZonedDateTime.now())) {
            bonuses.remove(bonuses.get(0));
            stateChanged = true;
        }
    }

    private void activateBonuses() {
        final List<Ball> ballsToCheck = new ArrayList<>(balls);
        for (Ball ball : ballsToCheck) {
            final Iterator<Bonus> bonusIterator = bonuses.iterator();
            while (bonusIterator.hasNext()) {
                final Bonus bonus = bonusIterator.next();
                if (bonus.checkBallBonusCollision(ball)) {
                    bonus.activate(this, ball);
                    activeBonuses.put(ZonedDateTime.now().plusSeconds(BONUS_EXPIRED_TIME), bonus);
                    bonusIterator.remove();
                    stateChanged = true;
                }
            }
        }
    }

    private void deactivateBonuses() {
        final Iterator<Map.Entry<ZonedDateTime, Bonus>> bonusIterator = activeBonuses.entrySet().iterator();
        while (bonusIterator.hasNext()) {
            final Map.Entry<ZonedDateTime, Bonus> bonusEntry = bonusIterator.next();
            if (bonusEntry.getKey().isBefore(ZonedDateTime.now())) {
                bonusEntry.getValue().deactivate();
                stateChanged = true;
                bonusIterator.remove();
            }
        }
    }

    public boolean isStateChanged() {
        return stateChanged;
    }

    public void setStateChanged(boolean stateChanged) {
        this.stateChanged = stateChanged;
    }

    public void addBall(@NotNull Ball ball) {
        balls.add(ball);
    }

    public boolean removeBall(@NotNull Ball ball) {
        if (balls.size() > 1) {
            balls.remove(ball);
            return true;
        }
        return false;
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
