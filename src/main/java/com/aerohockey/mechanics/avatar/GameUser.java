package com.aerohockey.mechanics.avatar;

import com.aerohockey.mechanics.base.PlatformCoords;
import com.aerohockey.mechanics.base.ServerPlayerDetailSnap;
import com.aerohockey.mechanics.base.ServerPlayerSnap;
import com.aerohockey.model.UserProfile;
import org.jetbrains.annotations.NotNull;

/**
 * Created by sergeybutorin on 15.04.17.
 */
public class GameUser {
    private final UserProfile userProfile;
    private final Platform platform;
    private byte score;
    private final byte coordsTransform;

    public GameUser(@NotNull UserProfile userProfile, boolean isTop) {
        this.userProfile = userProfile;
        this.score = 0;
        this.coordsTransform = isTop ? (byte) 1 : -1;
        platform = new Platform(isTop);
    }

    public int getRating() {
        return userProfile.getRating();
    }

    public String getLogin() {
        return userProfile.getLogin();
    }

    public long getId() {
        return userProfile.getId();
    }

    public void addScore() {
        this.score += 1;
    }

    public byte getScore() {
        return score;
    }

    public byte getCoordsTransform() {
        return coordsTransform;
    }

    public void changeRating(int value) {
        userProfile.changeRating(value);
    }

    public @NotNull ServerPlayerSnap generateSnap() {
        final PlatformCoords platformCoords = new PlatformCoords(platform.getCoords().x);

        final ServerPlayerSnap result = new ServerPlayerSnap();
        result.setUserId(getId());
        result.setCoords(platformCoords);
        return result;
    }

    public @NotNull ServerPlayerDetailSnap generateDetailSnap() {
        final PlatformCoords platformCoords = new PlatformCoords(platform.getCoords().x);

        final ServerPlayerDetailSnap result = new ServerPlayerDetailSnap();
        result.setUserId(getId());
        result.setScore(score);
        result.setCoords(platformCoords);
        result.setWidth(platform.getWidth());
        result.setShield(platform.isShield());
        return result;
    }

    public Platform getPlatform() {
        return platform;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final GameUser other = (GameUser) o;
        return this.getId() == other.getId();
    }

    @Override
    public int hashCode() {
        return (int)this.getId();
    }
}