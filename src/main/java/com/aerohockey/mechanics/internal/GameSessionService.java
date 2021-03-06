package com.aerohockey.mechanics.internal;

import com.aerohockey.mechanics.GameSession;
import com.aerohockey.model.UserProfile;
import com.aerohockey.websocket.RemotePointService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by sergeybutorin on 15.04.17.
 */
@Service
public class GameSessionService {
    private final @NotNull Map<Long, GameSession> usersMap = new HashMap<>();
    private final @NotNull Set<GameSession> gameSessions = new LinkedHashSet<>();

    private final @NotNull RemotePointService remotePointService;

    private final @NotNull GameInitService gameInitService;

    public GameSessionService(@NotNull RemotePointService remotePointService, @NotNull GameInitService gameInitService) {
        this.remotePointService = remotePointService;
        this.gameInitService = gameInitService;
    }

    public Set<GameSession> getSessions() {
        return gameSessions;
    }

    public @Nullable GameSession getSessionForUser(@NotNull Long userId) {
        return usersMap.get(userId);
    }

    public boolean isPlaying(@NotNull Long userId) {
        return usersMap.containsKey(userId);
    }

    public void notifyGameIsOver(@NotNull GameSession gameSession) {
        final boolean exists = gameSessions.remove(gameSession);
        usersMap.remove(gameSession.getTop().getId());
        usersMap.remove(gameSession.getBottom().getId());
        if (exists) {
            remotePointService.cutDownConnection(gameSession.getTop().getId());
            remotePointService.cutDownConnection(gameSession.getBottom().getId());
        }
    }

    public void startGame(@NotNull UserProfile first, @NotNull UserProfile second) {
        final GameSession gameSession = new GameSession(first, second);
        gameSessions.add(gameSession);
        usersMap.put(gameSession.getTop().getId(), gameSession);
        usersMap.put(gameSession.getBottom().getId(), gameSession);
        gameInitService.initGameFor(gameSession);
    }
}
