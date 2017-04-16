package com.aerohockey.websocket;

import org.jetbrains.annotations.NotNull;

/**
 * Created by sergeybutorin on 09.04.17.
 */

public interface MessageHandlerContainer {

    void handle(@NotNull Message message, long forUser) throws HandleException;

    <T> void registerHandler(@NotNull Class<T> clazz, MessageHandler<T> handler);
}
