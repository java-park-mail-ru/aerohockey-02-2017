package com.aerohockey.mechanics.internal;

import com.aerohockey.mechanics.GameSession;
import com.aerohockey.mechanics.avatar.GameUser;
import com.aerohockey.mechanics.base.GameOverSnap;
import com.aerohockey.websocket.Message;
import com.aerohockey.websocket.RemotePointService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
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
    private final @NotNull RemotePointService remotePointService;

    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();

    public GameOverSnapService(@NotNull RemotePointService remotePointService) {
        this.remotePointService = remotePointService;
    }

    public void sendSnapshotsFor(@NotNull GameSession gameSession) {
        final Collection<GameUser> players = new ArrayList<>();
        players.add(gameSession.getTop());
        players.add(gameSession.getBottom());

        final GameOverSnap snap = new GameOverSnap();

        //noinspection OverlyBroadCatchBlock
        try {
            for (GameUser player : players) {
                final int value;
                if (player.getScore() == MAX_SCORE) { //winner
                    value = 1 + gameSession.getOpponent(player).getRating() / (gameSession.getOpponent(player).getScore() + 1);
                } else { //loser
                    value = -gameSession.getOpponent(player).getRating()/10;
                }
                player.changeRating(value);

                snap.setId(player.getId());
                snap.setChangeRating(value);

                final Message message = new Message(GameOverSnap.class.getName(), objectMapper.writeValueAsString(snap));
                remotePointService.sendMessageToUser(player.getId(), message);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed sending snapshot", ex);
        }
    }
}
