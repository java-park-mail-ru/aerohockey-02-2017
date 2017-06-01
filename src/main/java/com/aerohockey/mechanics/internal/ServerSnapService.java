package com.aerohockey.mechanics.internal;

import com.aerohockey.mechanics.GameSession;
import com.aerohockey.mechanics.avatar.Ball;
import com.aerohockey.mechanics.avatar.GameUser;
import com.aerohockey.mechanics.base.ServerPlayerSnap;
import com.aerohockey.mechanics.base.ServerSnap;
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
 * Created by sergeybutorin on 15.04.17.
 */
@Service
public class ServerSnapService {
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(ServerSnapService.class);

    private final @NotNull RemotePointService remotePointService;

    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();

    public ServerSnapService(@NotNull RemotePointService remotePointService) {
        this.remotePointService = remotePointService;
    }

    public @Nullable Long sendSnapshotsFor(@NotNull GameSession gameSession, long frameTime) {
        final Collection<GameUser> players = new ArrayList<>();
        players.add(gameSession.getTop());
        players.add(gameSession.getBottom());

        final ServerSnap snap = new ServerSnap();
        snap.setServerFrameTime(frameTime);

        for (GameUser player : players) {
            //noinspection OverlyBroadCatchBlock
            try {
                snap.setBalls(gameSession.getBalls().stream().map(Ball::getCoords).collect(Collectors.toList()));
                final List<ServerPlayerSnap> playersSnaps = new ArrayList<>();
                for (GameUser p : players) {
                    playersSnaps.add(p.generateSnap());
                }
                snap.setPlayers(playersSnaps);
                final Message message = new Message(ServerSnap.class.getName(), objectMapper.writeValueAsString(snap));
                remotePointService.sendMessageToUser(player.getId(), message);
            } catch (IOException ex) {
                LOGGER.error("Failed send snapshots - user with id = {} disconnected", player.getId());
                return player.getId();
            }
        }
        return null;
    }
}
