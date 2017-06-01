package com.aerohockey.mechanics;

import com.aerohockey.mechanics.base.ClientSnap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by sergeybutorin on 13.04.17.
 */
public interface GameMechanics {
    void addClientSnapshot(@NotNull Long userId, @NotNull ClientSnap gameSnap);

    void addUser(@NotNull Long userId);

    int getSessionCount();

    boolean isUserWaiting(@NotNull Long userId);

    boolean isUserPlaying(@NotNull Long userId);

    boolean isCandidatesExists();

    @Nullable GameSession getSessionForUser(@NotNull Long userId);

    void gmStep(long frameTime);

    @SuppressWarnings("EmptyMethod")
    void reset();
}
