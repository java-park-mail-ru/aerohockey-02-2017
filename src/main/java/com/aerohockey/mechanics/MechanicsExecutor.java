package com.aerohockey.mechanics;

import com.aerohockey.mechanics.base.ClientSnap;
import com.aerohockey.mechanics.internal.*;
import com.aerohockey.mechanics.utils.TimeHelper;
import com.aerohockey.services.AccountService;
import com.aerohockey.websocket.RemotePointService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Clock;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * Created by sergeybutorin on 16.04.17.
 */
@Service
public class MechanicsExecutor {
    private static final long STEP_TIME = 30;

    private static final int THREADS = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);

    private final AccountService accountService;

    private final ServerSnapService serverSnapService;

    private final ServerDetailSnapService serverDetailSnapService;

    private final RemotePointService remotePointService;

    private final GameOverSnapService gameOverSnapService;

    private final ExecutorService tickExecutors = Executors.newFixedThreadPool(THREADS, Thread::new);

    private final GameMechanics[] gameMechanics = new GameMechanics[THREADS];

    @Autowired
    public MechanicsExecutor(AccountService accountService, ServerSnapService serverSnapService,
                             ServerDetailSnapService serverDetailSnapService, RemotePointService remotePointService,
                             GameOverSnapService gameOverSnapService) {
        this.accountService = accountService;
        this.serverSnapService = serverSnapService;
        this.serverDetailSnapService = serverDetailSnapService;
        this.remotePointService = remotePointService;
        this.gameOverSnapService = gameOverSnapService;
    }

    @PostConstruct
    public void initAfterStartup() {
        for (int i = 0; i < gameMechanics.length; i++) {
            gameMechanics[i] = new GameMechanicsImpl(accountService, serverSnapService,
                    remotePointService,
                    gameOverSnapService,
                    serverDetailSnapService);
            final Runnable mechanicsRunnable = new MechanicsRunnable(gameMechanics[i]);
            tickExecutors.execute(mechanicsRunnable);
        }
    }

    public synchronized void addUser(@NotNull Long userId) {
        for (GameMechanics mechanics : gameMechanics) {
            if (mechanics.isUserWaiting(userId) || mechanics.isUserPlaying(userId)) {
                return;
            } else if (mechanics.isCandidatesExists()) {
                mechanics.addUser(userId);
                return;
            }
        }

        final Optional<GameMechanics> mechanics = Stream.of(gameMechanics).min(Comparator.comparingInt(GameMechanics::getSessionCount));
        mechanics.ifPresent(gameMechanics -> gameMechanics.addUser(userId));
    }

    public void addClientSnapshot(@NotNull Long userId, @NotNull ClientSnap clientSnap) {
        for (GameMechanics mechanics: gameMechanics) {
            if (mechanics.isUserPlaying(userId)) {
                mechanics.addClientSnapshot(userId, clientSnap);
                return;
            }
        }
    }

    @TestOnly
    public @Nullable GameMechanics getMechanicsForUser(@NotNull Long userId) {
        for (GameMechanics mechanics: gameMechanics) {
            if (mechanics.isUserPlaying(userId) || mechanics.isUserWaiting(userId)) {
                return mechanics;
            }
        }
        return null;
    }

    private static class MechanicsRunnable implements Runnable {

        private final GameMechanics gameMechanics;

        @Autowired
        MechanicsRunnable(GameMechanics gameMechanics) {
            this.gameMechanics = gameMechanics;
        }

        private final Clock clock = Clock.systemDefaultZone();

        @Override
        public void run() {
            long lastFrameMillis = STEP_TIME;
            while (true) {
                final long before = clock.millis();

                gameMechanics.gmStep(lastFrameMillis);

                final long after = clock.millis();
                TimeHelper.sleep(STEP_TIME - (after - before));

                if (Thread.currentThread().isInterrupted()) {
                    gameMechanics.reset();
                    return;
                }
                final long afterSleep = clock.millis();
                lastFrameMillis = afterSleep - before;
            }
        }
    }
}
