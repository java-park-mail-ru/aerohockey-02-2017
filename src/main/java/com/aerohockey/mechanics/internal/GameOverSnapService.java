package com.aerohockey.mechanics.internal;

import com.aerohockey.mechanics.GameSession;
import com.aerohockey.mechanics.avatar.GameUser;
import com.aerohockey.mechanics.base.GameOverSnap;
import com.aerohockey.websocket.Message;
import com.aerohockey.websocket.RemotePointService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static com.aerohockey.mechanics.Config.MAX_SCORE;

/**
 * Created by sergeybutorin on 01.05.17.
 */
@Service
public class GameOverSnapService {
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(GameOverSnapService.class);

    private final @NotNull RemotePointService remotePointService;

    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();

    public GameOverSnapService(@NotNull RemotePointService remotePointService) {
        this.remotePointService = remotePointService;
    }

    public void sendSnapshotsFor(@NotNull GameSession gameSession, @Nullable Long leftPlayerId) {
        final Collection<GameUser> players = new ArrayList<>();
        players.add(gameSession.getTop());
        players.add(gameSession.getBottom());

        final GameOverSnap snap = new GameOverSnap();

        try {
            for (GameUser player : players) {
                final int opponentRating = gameSession.getOpponent(player).getRating();
                int value = (opponentRating < 10) ? 1 : opponentRating/10;
                if (player.getScore() >= MAX_SCORE || (leftPlayerId != null && leftPlayerId != player.getId())) { //winner
                    value += 1;
                } else { //loser
                    value *= -1;
                }
                player.changeRating(value);

                snap.setId(player.getId());
                snap.setChangeRating(value);

                final Message message = new Message(GameOverSnap.class.getName(), objectMapper.writeValueAsString(snap));
                try {
                    remotePointService.sendMessageToUser(player.getId(), message);
                } catch (IOException ex) {
                    LOGGER.error("Failed send snapshots - user disconnected");
                }
            }
        } catch (JsonProcessingException ex) {
            LOGGER.error("Failed send snapshots", ex);
        }
    }
}
