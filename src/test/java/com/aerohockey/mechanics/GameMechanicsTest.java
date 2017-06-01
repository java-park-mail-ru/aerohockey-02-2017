package com.aerohockey.mechanics;

import com.aerohockey.mechanics.avatar.GameUser;
import com.aerohockey.mechanics.base.ClientSnap;
import com.aerohockey.model.UserProfile;
import com.aerohockey.services.AccountService;
import com.aerohockey.websocket.RemotePointService;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static com.aerohockey.mechanics.Config.PLATFORM_SPEED;
import static com.aerohockey.mechanics.Config.PLAYGROUND_WIDTH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by sergeybutorin on 24.04.17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
public class GameMechanicsTest {
    @MockBean
    private RemotePointService remotePointService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private MechanicsExecutor mechanicsExecutor;
    private GameMechanics gameMechanics;

    private UserProfile user1;
    private UserProfile user2;

    private final double delta = 0.1;

    @Before
    public void setUp () {
        when(remotePointService.isConnected(any())).thenReturn(true);
        user1 = accountService.addUser("user1", "user1@mail.ru", "123");
        user2 = accountService.addUser("user2", "user2@mail.ru", "123");
    }

    private GameSession startGame(long player1, long  player2) {
        mechanicsExecutor.addUser(player1);
        mechanicsExecutor.addUser(player2);
        this.gameMechanics = mechanicsExecutor.getMechanicsForUser(player1);
        assertNotNull(gameMechanics);
        assertEquals("Users should be in the same mechanics", gameMechanics, mechanicsExecutor.getMechanicsForUser(player2));
        gameMechanics.gmStep(0);
        final GameSession gameSession = gameMechanics.getSessionForUser(player1);
        assertNotNull(gameSession);
        assertEquals("Users should be in the same session", gameSession, gameMechanics.getSessionForUser(player2));
        return gameSession;
    }

    @Test
    public void gameStartTest() {
        startGame(user1.getId(), user2.getId());
    }

    @Test
    public void movementTest() {
        final GameSession gameSession = startGame(user1.getId(), user2.getId());
        final @NotNull GameUser firstPlayer = gameSession.getBottom();
        assertEquals(0, firstPlayer.getPlatform().getCoords().x, delta);

        gameMechanics.addClientSnapshot(firstPlayer.getId(), new ClientSnap("right", 1));
        gameMechanics.gmStep(0);
        assertEquals(-PLATFORM_SPEED, firstPlayer.getPlatform().getCoords().x, delta);

        gameMechanics.addClientSnapshot(firstPlayer.getId(), new ClientSnap("left", 2));
        gameMechanics.gmStep(0);
        assertEquals(PLATFORM_SPEED, firstPlayer.getPlatform().getCoords().x, delta);
    }

    @Test
    public void platformWallCollisionTest() {
        final GameSession gameSession = startGame(user1.getId(), user2.getId());
        final @NotNull GameUser firstPlayer = gameSession.getBottom();
        final double movementLength = PLAYGROUND_WIDTH/2 - firstPlayer.getPlatform().getWidth()/2;

        for (int i = 0; i < (movementLength) / PLATFORM_SPEED; i++) {
            gameMechanics.addClientSnapshot(firstPlayer.getId(), new ClientSnap("right", 1));
            gameMechanics.gmStep(0);
        }
        assertEquals(-movementLength, firstPlayer.getPlatform().getCoords().x, delta);

        gameMechanics.addClientSnapshot(firstPlayer.getId(), new ClientSnap("right", 1));
        gameMechanics.gmStep(0);
        assertEquals(-movementLength, firstPlayer.getPlatform().getCoords().x, delta);
    }
}
