package forceitembattle.commands;

import static forceitembattle.commands.CommandTestSupport.assertSaid;
import static forceitembattle.commands.CommandTestSupport.contextWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.player.CommandPause;
import forceitembattle.commands.player.CommandResume;
import forceitembattle.manager.Gamemanager;
import forceitembattle.model.GameState;
import forceitembattle.model.Roster;
import forceitembattle.settings.GameSetting;
import org.bukkit.GameRules;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * {@code /pause} and {@code /resume}: the two halves of stopping the clock.
 *
 * <p>They are one test because they are one mechanism read in both directions, and the mistakes
 * available are symmetry mistakes â€” a gate that lets you pause an already-paused round, or a
 * resume that starts the clock without starting the world's. The daylight and weather gamerules
 * are the part with no other cover at all: a resume that forgets to turn them back on leaves the
 * round running at a frozen midnight, which nothing in the plugin would report.
 *
 * <p>Both are gated by {@code OP_WHEN_EVENT}, the one conditional gate in the codebase, so both
 * are exercised with the EVENT setting off (open to everyone) and on (ops only).
 */
class CommandPauseTest {

    private ServerMock server;
    private WorldMock overworld;
    private Gamemanager gamemanager;
    private CommandPause pause;
    private CommandResume resume;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        // Dimension.OVERWORLD resolves by name, so the world has to be called "world".
        this.overworld = this.server.addSimpleWorld("world");

        ForceItemBattle plugin = mock(ForceItemBattle.class);
        this.gamemanager = mock(Gamemanager.class);
        when(plugin.getGamemanager()).thenReturn(this.gamemanager);

        this.pause = new CommandPause(plugin);
        this.resume = new CommandResume(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- fixtures ---------------------------------------------------------------------------

    private PlayerMock join(String name) {
        return this.server.addPlayer(name);
    }

    private PlayerMock joinOp(String name) {
        PlayerMock player = join(name);
        player.setOp(true);
        return player;
    }

    /** Puts both commands in the same round, and runs one of them. */
    private void inA(GameState state, CustomCommand command, PlayerMock player,
                     GameSetting... settings) {
        command.setContext(contextWith(state, new Roster(), settings));
        command.onCommand(player, null, command.getName(), new String[0]);
    }

    private boolean timeAdvances() {
        return Boolean.TRUE.equals(this.overworld.getGameRuleValue(GameRules.ADVANCE_TIME));
    }

    private boolean weatherAdvances() {
        return Boolean.TRUE.equals(this.overworld.getGameRuleValue(GameRules.ADVANCE_WEATHER));
    }

    // --- the tests --------------------------------------------------------------------------

    @Nested
    class Pausing {

        @Test
        void aRunningRoundIsPaused() {
            PlayerMock player = join("Understudy1");

            inA(GameState.MID_GAME, pause, player);

            verify(gamemanager).pauseGame();
        }

        /** The world's clock stops with the round's, or the round resumes into a different sky. */
        @Test
        void theWorldStopsAdvancingToo() {
            PlayerMock player = join("Understudy1");

            inA(GameState.MID_GAME, pause, player);

            assertFalse(timeAdvances(), "daylight must stop with the round");
            assertFalse(weatherAdvances(), "weather must stop with the round");
        }

        @Test
        void everyoneIsToldTheGameIsPaused() {
            PlayerMock player = join("Understudy1");
            PlayerMock bystander = join("Understudy2");

            inA(GameState.MID_GAME, pause, player);

            assertSaid(bystander, "The game has been paused");
        }

        /** Pausing an already-paused round is refused, in its own words. */
        @Test
        void anAlreadyPausedRoundIsRefused() {
            PlayerMock player = join("Understudy1");

            inA(GameState.PAUSED_GAME, pause, player);

            assertSaid(player, "already paused");
            verify(gamemanager, never()).pauseGame();
        }

        @Test
        void thereIsNothingToPauseBeforeARoundStarts() {
            PlayerMock player = join("Understudy1");

            inA(GameState.PRE_GAME, pause, player);

            assertSaid(player, "already paused");
            verify(gamemanager, never()).pauseGame();
        }
    }

    @Nested
    class Resuming {

        @Test
        void aPausedRoundIsResumed() {
            PlayerMock player = join("Understudy1");

            inA(GameState.PAUSED_GAME, resume, player);

            verify(gamemanager).resumeGame();
        }

        /**
         * The half with no other cover: a resume that forgets these leaves the round running at a
         * frozen midnight, and nothing else in the plugin would notice.
         */
        @Test
        void theWorldStartsAdvancingAgain() {
            PlayerMock player = join("Understudy1");
            inA(GameState.MID_GAME, pause, player);

            inA(GameState.PAUSED_GAME, resume, player);

            assertTrue(timeAdvances(), "daylight must start again with the round");
            assertTrue(weatherAdvances(), "weather must start again with the round");
        }

        @Test
        void everyoneIsToldTheTimerResumed() {
            PlayerMock player = join("Understudy1");
            PlayerMock bystander = join("Understudy2");

            inA(GameState.PAUSED_GAME, resume, player);

            assertSaid(bystander, "resumed");
        }

        @Test
        void aRunningRoundIsRefused() {
            PlayerMock player = join("Understudy1");

            inA(GameState.MID_GAME, resume, player);

            assertSaid(player, "not paused");
            verify(gamemanager, never()).resumeGame();
        }
    }

    /**
     * {@code OP_WHEN_EVENT}: open to everyone on a normal round, ops only on an event round. Both
     * commands declare it, so both are checked.
     */
    @Nested
    class TheConditionalGate {

        @Test
        void onANormalRoundAnyoneMayPauseOrResume() {
            PlayerMock player = join("Understudy1");

            inA(GameState.MID_GAME, pause, player);
            verify(gamemanager).pauseGame();

            inA(GameState.PAUSED_GAME, resume, player);
            verify(gamemanager).resumeGame();
        }

        @Test
        void onAnEventRoundANonOpMayNotPause() {
            PlayerMock player = join("Understudy1");

            inA(GameState.MID_GAME, pause, player, GameSetting.EVENT);

            assertSaid(player, "permission");
            verify(gamemanager, never()).pauseGame();
        }

        @Test
        void onAnEventRoundANonOpMayNotResume() {
            PlayerMock player = join("Understudy1");

            inA(GameState.PAUSED_GAME, resume, player, GameSetting.EVENT);

            assertSaid(player, "permission");
            verify(gamemanager, never()).resumeGame();
        }

        @Test
        void onAnEventRoundAnOpStillMay() {
            PlayerMock admin = joinOp("Admin");

            inA(GameState.MID_GAME, pause, admin, GameSetting.EVENT);
            verify(gamemanager).pauseGame();

            inA(GameState.PAUSED_GAME, resume, admin, GameSetting.EVENT);
            verify(gamemanager).resumeGame();
        }

        /** The gate runs before the phase check, so a non-op is told about permission first. */
        @Test
        void permissionIsCheckedBeforeThePhase() {
            PlayerMock player = join("Understudy1");

            inA(GameState.PRE_GAME, pause, player, GameSetting.EVENT);

            assertSaid(player, "permission");
        }
    }
}
