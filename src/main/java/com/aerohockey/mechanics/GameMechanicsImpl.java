package com.aerohockey.mechanics;

import com.aerohockey.mechanics.base.ClientSnap;
import com.aerohockey.mechanics.internal.*;
import com.aerohockey.model.UserProfile;
import com.aerohockey.services.AccountService;
import com.aerohockey.websocket.RemotePointService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by sergeybutorin on 14.04.17.
 */
@SuppressWarnings({"unused", "FieldMayBeFinal"})
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

    private @NotNull Set<Long> playingUsers = new HashSet<>();

    private final @NotNull ConcurrentLinkedQueue<Long> waiters = new ConcurrentLinkedQueue<>();

    private final @NotNull Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    public GameMechanicsImpl(@NotNull AccountService accountService, @NotNull ServerSnapService serverSnapService,
                             @NotNull RemotePointService remotePointService, @NotNull BallMovementService ballMovementService,
                             @NotNull GameSessionService gameSessionService, @NotNull GameOverSnapService gameOverSnapService,
                             @NotNull ServerDetailSnapService serverDetailSnapService) {
        this.accountService = accountService;
        this.serverSnapService = serverSnapService;
        this.serverDetailSnapService = serverDetailSnapService;
        this.remotePointService = remotePointService;
        this.ballMovementService = ballMovementService;
        this.gameSessionService = gameSessionService;
        this.gameOverSnapService = gameOverSnapService;
        this.clientSnapService = new ClientSnapService(ballMovementService);
    }

    @Override
    public void addClientSnapshot(@NotNull Long userId, @NotNull ClientSnap clientSnap) {
        tasks.add(() -> clientSnapService.pushClientSnap(userId, clientSnap));
    }

    @Override
    public void addUser(@NotNull Long user) {
        if (gameSessionService.isPlaying(user)) {
            return;
        }
        waiters.add(user);
    }

    private void tryStartGames() {
        final Set<UserProfile> matchedPlayers = new LinkedHashSet<>();

        while (waiters.size() >= 2 || waiters.size() >= 1 && matchedPlayers.size() >= 1) {
            final Long candidateId = waiters.poll();
            if (!insureCandidate(candidateId)) {
                continue;
            }
            matchedPlayers.add(accountService.getUserById(candidateId));
            if(matchedPlayers.size() == 2) {
                final Iterator<UserProfile> iterator = matchedPlayers.iterator();
                gameSessionService.startGame(iterator.next(), iterator.next());
                matchedPlayers.clear();
            }
        }
        matchedPlayers.stream().map(UserProfile::getId).forEach(waiters::add);
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
        final List<GameSession> sessionsToTerminate = new ArrayList<>();
        while (iterator.hasNext()) {
            final GameSession session = iterator.next();
            session.bonusManagement();
            try {
                if(session.isStateChanged()) {
                    serverDetailSnapService.sendSnapshotsFor(session, frameTime);
                    session.setStateChanged(false);
                } else {
                    serverSnapService.sendSnapshotsFor(session, frameTime);
                }
                if (session.isGameOver()) {
                    gameOverSnapService.sendSnapshotsFor(session);
                    sessionsToTerminate.add(session);
                    accountService.updateRating(session.getTop().getId(), session.getTop().getRating());
                    accountService.updateRating(session.getBottom().getId(), session.getBottom().getRating());
                }
            } catch (RuntimeException ex) {
                LOGGER.error("Failed send snapshots, terminating the session", ex);
                sessionsToTerminate.add(session);
            }
        }
        sessionsToTerminate.forEach(gameSessionService::notifyGameIsOver);

        tryStartGames();
        clientSnapService.clear();
        ballMovementService.clear();
    }

    @Override
    public void reset() {

    }
}