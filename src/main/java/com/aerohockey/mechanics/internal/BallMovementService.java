package com.aerohockey.mechanics.internal;

import com.aerohockey.mechanics.GameSession;
import com.aerohockey.mechanics.avatar.Ball;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by sergeybutorin on 27.04.17.
 */
@Service
public class BallMovementService {
    private final Map<Ball, GameSession> ballsToMove = new ConcurrentHashMap<>();

    public void registerBallToMove(@NotNull List<Ball> balls, @NotNull GameSession gameSession) {
        for (Ball ball : balls) {
            ballsToMove.put(ball, gameSession);
        }
    }

    public void executeMoves(long frameTime) {
        for (Map.Entry<Ball, GameSession> entry : ballsToMove.entrySet()) {
            final Ball ball = entry.getKey();
            final GameSession gameSession = entry.getValue();
            ball.move(gameSession, frameTime);
            for (Ball secondBall : gameSession.getBalls()) {
                ball.twoBallCollision(secondBall, frameTime);
            }
        }
    }

    public void clear() {
        ballsToMove.clear();
    }
}