package com.aerohockey.mechanics;

import com.aerohockey.mechanics.base.ClientSnap;
import com.aerohockey.mechanics.internal.*;
import com.aerohockey.model.UserProfile;
import com.aerohockey.services.AccountService;
import com.aerohockey.websocket.RemotePointService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by sergeybutorin on 14.04.17.
 */
@Service
public class GameMechanicsImpl implements GameMechanics {
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(GameMechanicsImpl.class);

    private final @NotNull AccountService accountService;

    private final @NotNull ClientSnapService clientSnapService;

    private final @NotNull ServerSnapService serverSnapService;

    private final @NotNull ServerDetailSnapService serverDetailSnapService;

    private final @NotNull RemotePointService remotePointService;

    private final @NotNull BallMovementService ballMovementService;

    private final @NotNull GameSessionService gameSessionService;

    private final @NotNull GameOverSnapService gameOverSnapService;

    private final @NotNull Queue<Long> waiters = new ConcurrentLinkedQueue<>();

    private final @NotNull Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    public GameMechanicsImpl(@NotNull AccountService accountService, @NotNull ServerSnapService serverSnapService,
                             @NotNull RemotePointService remotePointService,
                             @NotNull GameOverSnapService gameOverSnapService,
                             @NotNull ServerDetailSnapService serverDetailSnapService) {
        this.accountService = accountService;
        this.serverSnapService = serverSnapService;
        this.serverDetailSnapService = serverDetailSnapService;
        this.remotePointService = remotePointService;
        this.ballMovementService = new BallMovementService();
        this.gameSessionService = new GameSessionService(remotePointService, new GameInitService(remotePointService));
        this.gameOverSnapService = gameOverSnapService;
        this.clientSnapService = new ClientSnapService(ballMovementService);
    }

    @Override
    public void addClientSnapshot(@NotNull Long userId, @NotNull ClientSnap clientSnap) {
        tasks.add(() -> {
            if (gameSessionService.isPlaying(userId)) {
                clientSnapService.pushClientSnap(userId, clientSnap);
            }
        });
    }

    @Override
    public void addUser(@NotNull Long userId) {
        if (gameSessionService.isPlaying(userId)) {
            return;
        }
        waiters.add(userId);
    }

    @Override
    public boolean isUserPlaying(@NotNull Long userId) {
        return gameSessionService.isPlaying(userId);
    }

    @Override
    public boolean isCandidatesExists() {
        return !waiters.isEmpty();
    }

    @Override
    public boolean isUserWaiting(@NotNull Long userId) {
        return waiters.contains(userId);
    }

    @Override
    public int getSessionCount() {
        return gameSessionService.getSessions().size();
    }

    @Override
    public @Nullable GameSession getSessionForUser(@NotNull Long userId) {
        return gameSessionService.getSessionForUser(userId);
    }

    private void tryStartGames() {
        final Set<UserProfile> matchedPlayers = new LinkedHashSet<>();

        while (waiters.size() >= 2 || waiters.size() >= 1 && matchedPlayers.size() >= 1) {
            final Long candidateId = waiters.poll();
            if (!insureCandidate(candidateId)) {
                continue;
            }
            matchedPlayers.add(accountService.getUserById(candidateId));
            if (matchedPlayers.size() == 2) {
                final Iterator<UserProfile> iterator = matchedPlayers.iterator();
                gameSessionService.startGame(iterator.next(), iterator.next());
                matchedPlayers.clear();
            }
        }
        matchedPlayers.stream().map(UserProfile::getId).forEach(waiters::add);
    }

    private void finishGames(@NotNull GameSession gameSession, @Nullable Long leftPlayerId) {
        gameOverSnapService.sendSnapshotsFor(gameSession, leftPlayerId);
        accountService.updateRating(gameSession.getTop().getId(), gameSession.getTop().getRating());
        accountService.updateRating(gameSession.getBottom().getId(), gameSession.getBottom().getRating());
        gameSessionService.notifyGameIsOver(gameSession);
    }

    private boolean insureCandidate(@NotNull Long candidateId) {
        return remotePointService.isConnected(candidateId) &&
                accountService.getUserById(candidateId) != null;
    }

    @Override
    public void gmStep(long frameTime) {
        while (!tasks.isEmpty()) {
            final Runnable nextTask = tasks.poll();
            if (nextTask != null) {
                try {
                    nextTask.run();
                } catch (RuntimeException ex) {
                    LOGGER.error("Can't handle game task", ex);
                }
            }
        }

        for (GameSession session : gameSessionService.getSessions()) {
            clientSnapService.processSnapshotsFor(session);
        }

        ballMovementService.executeMoves(frameTime);

        final Iterator<GameSession> iterator = gameSessionService.getSessions().iterator();
        final HashMap<GameSession, Long> sessionsToTerminate = new HashMap<>();
        while (iterator.hasNext()) {
            final GameSession session = iterator.next();
            session.bonusManagement();
            try {
                final Long leftPlayerId;
                if (session.isStateChanged()) {
                    leftPlayerId = serverDetailSnapService.sendSnapshotsFor(session, frameTime);
                    session.setStateChanged(false);
                } else {
                    leftPlayerId = serverSnapService.sendSnapshotsFor(session, frameTime);
                }
                if (session.isGameOver() || leftPlayerId != null) {
                    sessionsToTerminate.put(session, leftPlayerId);
                }
            } catch (RuntimeException ex) {
                LOGGER.error("Failed send snapshots, terminating the session", ex);
                sessionsToTerminate.put(session, null);
            }
        }

        sessionsToTerminate.forEach(this::finishGames);

        tryStartGames();
        clientSnapService.clear();
        ballMovementService.clear();
    }

    @Override
    public void reset() {

    }
}
