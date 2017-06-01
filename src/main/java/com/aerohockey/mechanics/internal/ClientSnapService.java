package com.aerohockey.mechanics.internal;

import com.aerohockey.mechanics.GameSession;
import com.aerohockey.mechanics.avatar.GameUser;
import com.aerohockey.mechanics.base.ClientSnap;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.aerohockey.mechanics.Config.PLATFORM_SPEED;

/**
 * Created by sergeybutorin on 15.04.17.
 */
@Service
public class ClientSnapService {
    private final Map<Long, List<ClientSnap>> snaps = new HashMap<>();

    private final @NotNull BallMovementService ballMovementService;

    public ClientSnapService(@NotNull BallMovementService ballMovementService) {
        this.ballMovementService = ballMovementService;
    }

    public void pushClientSnap(@NotNull Long user, @NotNull ClientSnap snap) {
        this.snaps.putIfAbsent(user, new ArrayList<>());
        final List<ClientSnap> clientSnaps = snaps.get(user);
        clientSnaps.add(snap);
    }

    public @NotNull List<ClientSnap> getSnapForUser(@NotNull Long user) {
        return snaps.getOrDefault(user, Collections.emptyList());
    }

    public synchronized void processSnapshotsFor(@NotNull GameSession gameSession) {
        final Collection<GameUser> players = new ArrayList<>();
        players.add(gameSession.getTop());
        players.add(gameSession.getBottom());
        for (GameUser player : players) {
            final List<ClientSnap> playerSnaps = getSnapForUser(player.getId());
            if (playerSnaps.isEmpty()) {
                continue;
            }
            for (ClientSnap snap : playerSnaps) {
                processMovement(player, snap.getDirection(), snap.getFrameTime());
            }
        }
        ballMovementService.registerBallToMove(gameSession.getBalls(), gameSession);
    }

    private void processMovement(@NotNull GameUser gameUser, @NotNull String direction, long clientFrameTime) {
        switch (direction) {
            case "left": {
                gameUser.getPlatform().move(PLATFORM_SPEED * clientFrameTime);
                break;
            }
            case "right": {
                gameUser.getPlatform().move(-PLATFORM_SPEED * clientFrameTime);
                break;
            }
            default: {
                break;
            }
        }
    }

    public void clear() {
        snaps.clear();
    }
}
