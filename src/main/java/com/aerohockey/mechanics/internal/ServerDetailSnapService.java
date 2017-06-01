package com.aerohockey.mechanics.internal;

import com.aerohockey.mechanics.GameSession;
import com.aerohockey.mechanics.avatar.Ball;
import com.aerohockey.mechanics.avatar.GameUser;
import com.aerohockey.mechanics.base.ServerDetailSnap;
import com.aerohockey.mechanics.base.ServerPlayerDetailSnap;
import com.aerohockey.websocket.Message;
import com.aerohockey.websocket.RemotePointService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by sergeybutorin on 16/05/2017.
 */

@Service
public class ServerDetailSnapService {
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(ServerDetailSnapService.class);

    private final @NotNull RemotePointService remotePointService;

    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();

    public ServerDetailSnapService(@NotNull RemotePointService remotePointService) {
        this.remotePointService = remotePointService;
    }

    public @Nullable Long sendSnapshotsFor(@NotNull GameSession gameSession, long frameTime) {
        final Collection<GameUser> players = new ArrayList<>();
        players.add(gameSession.getTop());
        players.add(gameSession.getBottom());

        final ServerDetailSnap snap = new ServerDetailSnap();
        snap.setServerFrameTime(frameTime);

        for (GameUser player : players) {
            //noinspection OverlyBroadCatchBlock
            try {
                snap.setBonuses(gameSession.getBonuses());
                snap.setBalls(gameSession.getBalls().stream().map(Ball::getSnap).collect(Collectors.toList()));
                final List<ServerPlayerDetailSnap> playersSnaps = new ArrayList<>();
                for (GameUser p : players) {
                    playersSnaps.add(p.generateDetailSnap());
                }
                snap.setPlayers(playersSnaps);
                final Message message = new Message(ServerDetailSnap.class.getName(), objectMapper.writeValueAsString(snap));
                remotePointService.sendMessageToUser(player.getId(), message);
            } catch (IOException ex) {
                LOGGER.error("Failed send snapshots - user with id = {} disconnected", player.getId());
                return player.getId();
            }
        }
        return null;
    }
}
