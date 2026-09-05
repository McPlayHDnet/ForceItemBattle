package forceitembattle.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.manager.Gamemanager;
import forceitembattle.model.GameState;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.entity.ZombieMock;

/**
 * {@link GameRulesListener}: the rules that make a lobby a lobby and a pause a pause.
 *
 * <p>Never testable before — every one of these decisions needs a real player, a real world, or a
 * real mob, and the class held the whole plugin besides. It now holds a {@code RoundPhase} and the
 * settings.
 */
class GameRulesListenerTest extends ListenerTestBase {

    private GameSettings settings;
    private GameRulesListener listener;

    @BeforeEach
    void setUpListener() {
        this.settings = mock(GameSettings.class);
        this.listener = new GameRulesListener(this.roundPhase, this.settings);
    }

    @Nested
    class Hunger {

        /** Nobody loses hunger in the lobby or during the countdown, whatever the setting says. */
        @ParameterizedTest
        @EnumSource(value = GameState.class, names = {"PRE_GAME", "STARTING"})
        void isFrozenBeforeTheRoundRuns(GameState state) {
            phase(state);
            when(settings.isSettingEnabled(GameSetting.FOOD)).thenReturn(true);
            PlayerMock player = player("Understudy1");

            FoodLevelChangeEvent event = new FoodLevelChangeEvent(player, 15);
            listener.onFoodLevelChange(event);

            assertTrue(event.isCancelled(), state + " counts as lobby for hunger");
        }

        @Test
        void followsTheSettingOnceTheRoundIsRunning() {
            phase(GameState.MID_GAME);
            when(settings.isSettingEnabled(GameSetting.FOOD)).thenReturn(true);

            FoodLevelChangeEvent event = new FoodLevelChangeEvent(player("Understudy1"), 15);
            listener.onFoodLevelChange(event);

            assertFalse(event.isCancelled());
        }

        @Test
        void isCancelledMidRoundWhenFoodIsOff() {
            phase(GameState.MID_GAME);
            when(settings.isSettingEnabled(GameSetting.FOOD)).thenReturn(false);

            FoodLevelChangeEvent event = new FoodLevelChangeEvent(player("Understudy1"), 15);
            listener.onFoodLevelChange(event);

            assertTrue(event.isCancelled());
        }
    }

    @Nested
    class MobTargeting {

        /**
         * Mobs let go of players outside a running round — the half of the pause behaviour that
         * stops them re-acquiring, paired with {@code Gamemanager.clearMobTargets} dropping the
         * aggro they already had.
         */
        @ParameterizedTest
        @EnumSource(value = GameState.class, names = {"PRE_GAME", "STARTING", "PAUSED_GAME", "END_GAME"})
        void aMobCannotTargetAPlayerOutsideARunningRound(GameState state) {
            phase(state);
            PlayerMock player = player("Understudy1");
            ZombieMock zombie = (ZombieMock) world.spawn(at(0, 64, 0), org.bukkit.entity.Zombie.class);

            EntityTargetLivingEntityEvent event = new EntityTargetLivingEntityEvent(
                    zombie, player, EntityTargetLivingEntityEvent.TargetReason.CLOSEST_PLAYER);
            listener.onEntityTargetLivingEntity(event);

            assertTrue(event.isCancelled());
            assertNull(event.getTarget());
        }

        @Test
        void aMobMayTargetAPlayerWhileTheRoundRuns() {
            phase(GameState.MID_GAME);
            PlayerMock player = player("Understudy1");
            ZombieMock zombie = (ZombieMock) world.spawn(at(0, 64, 0), org.bukkit.entity.Zombie.class);

            EntityTargetLivingEntityEvent event = new EntityTargetLivingEntityEvent(
                    zombie, player, EntityTargetLivingEntityEvent.TargetReason.CLOSEST_PLAYER);
            listener.onEntityTargetLivingEntity(event);

            assertFalse(event.isCancelled());
            assertEquals(player, event.getTarget());
        }
    }

    @Nested
    class PauseFreeze {

        /**
         * A pause pins players to the block they are standing on, so they cannot walk anywhere even
         * though the world keeps ticking around them.
         */
        @Test
        void aPausedPlayerIsSnappedBackToTheirBlock() {
            phase(GameState.PAUSED_GAME);
            PlayerMock player = player("Understudy1");

            Location from = at(10.5, 64, 10.5);
            Location to = at(11.5, 64, 12.5);
            PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);
            listener.onMove(event);

            assertNotEquals(to, event.getTo(), "the move must not stand");
            assertEquals(10, event.getTo().getBlockX());
            assertEquals(10, event.getTo().getBlockZ());
        }

        /** Vertical movement is untouched — falling and jumping in place are not walking. */
        @Test
        void aPausedPlayerMayStillMoveWithinTheirBlock() {
            phase(GameState.PAUSED_GAME);
            PlayerMock player = player("Understudy1");

            Location from = at(10.2, 64, 10.2);
            Location to = at(10.8, 66, 10.8);
            PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);
            listener.onMove(event);

            assertEquals(to, event.getTo(), "same block, so nothing to correct");
        }

        @Test
        void aRunningRoundDoesNotRestrictMovement() {
            phase(GameState.MID_GAME);
            PlayerMock player = player("Understudy1");

            Location to = at(99.5, 64, 99.5);
            PlayerMoveEvent event = new PlayerMoveEvent(player, at(10.5, 64, 10.5), to);
            listener.onMove(event);

            assertEquals(to, event.getTo());
        }
    }
}
