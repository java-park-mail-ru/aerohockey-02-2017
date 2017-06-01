package com.aerohockey.mechanics.handlers;

import com.aerohockey.mechanics.MechanicsExecutor;
import com.aerohockey.mechanics.requests.JoinGame;
import com.aerohockey.websocket.MessageHandler;
import com.aerohockey.websocket.MessageHandlerContainer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by sergeybutorin on 13.04.17.
 */
@Component
public class JoinGameHandler extends MessageHandler<JoinGame.Request> {
    private final @NotNull MechanicsExecutor mechanicsExecutor;
    private final @NotNull MessageHandlerContainer messageHandlerContainer;

    public JoinGameHandler(@NotNull MechanicsExecutor mechanicsExecutor, @NotNull MessageHandlerContainer messageHandlerContainer) {
        super(JoinGame.Request.class);
        this.mechanicsExecutor = mechanicsExecutor;
        this.messageHandlerContainer = messageHandlerContainer;
    }

    @PostConstruct
    private void init() {
        messageHandlerContainer.registerHandler(JoinGame.Request.class, this);
    }

    @Override
    public void handle(@NotNull JoinGame.Request message, @NotNull Long forUser) {
        mechanicsExecutor.addUser(forUser);
    }
}
