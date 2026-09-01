package forceitembattle.commands;

import static forceitembattle.commands.CommandTestSupport.assertSaid;
import static forceitembattle.commands.CommandTestSupport.screenOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.player.CommandShout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@code /shout}: talking past team chat, either once or until you turn it off.
 *
 * <p>Two forms in one command and they do not interact: with a message it broadcasts once and
 * leaves shout mode exactly as it was; with no argument it flips the mode and says nothing to
 * anybody else. Conflating them â€” having the one-shot form also turn the mode on â€” is the mistake
 * the shape invites, so both directions are asserted.
 *
 * <p>The mode itself lives in a {@code static Set<Player>} on the command class, which is why
 * {@link #eachTestStartsWithShoutModeOff} exists: the set outlives every instance, so it survives
 * from one test to the next exactly as it survives a plugin reload on a live server. That set also
 * holds {@code Player} objects rather than UUIDs and is never cleaned on quit, so a player who
 * leaves stays in it. Nothing here asserts that is correct; it is noted because a test that
 * appeared to pass in isolation and failed in a suite would otherwise be a mystery.
 */
class CommandShoutTest {

    private ServerMock server;
    private CommandShout command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.command = new CommandShout(mock(ForceItemBattle.class));
        ((CustomCommand) this.command).setContext(new CommandContext(null, null, null));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private PlayerMock join(String name) {
        return this.server.addPlayer(name);
    }

    private void run(PlayerMock player, String... args) {
        this.command.onCommand(player, null, "shout", args);
    }

    /** A fresh player is not in the static set, which is what makes these tests independent. */
    @Test
    void eachTestStartsWithShoutModeOff() {
        assertFalse(CommandShout.isShouting(join("Understudy1")));
    }

    @Nested
    class TheMode {

        @Test
        void noArgumentTurnsItOn() {
            PlayerMock player = join("Understudy1");

            run(player);

            assertTrue(CommandShout.isShouting(player));
            assertSaid(player, "ON");
        }

        @Test
        void runningItAgainTurnsItOff() {
            PlayerMock player = join("Understudy1");

            run(player);
            screenOf(player);
            run(player);

            assertFalse(CommandShout.isShouting(player));
            assertSaid(player, "OFF");
        }

        /** The mode is per player, not per server. */
        @Test
        void oneShoutingPlayerDoesNotShoutForEveryone() {
            PlayerMock shouter = join("Understudy1");
            PlayerMock quiet = join("Understudy2");

            run(shouter);

            assertTrue(CommandShout.isShouting(shouter));
            assertFalse(CommandShout.isShouting(quiet));
        }

        /** Toggling is silent to everybody else. */
        @Test
        void nobodyElseHearsTheToggle() {
            PlayerMock shouter = join("Understudy1");
            PlayerMock bystander = join("Understudy2");

            run(shouter);

            assertTrue(screenOf(bystander).isEmpty());
        }
    }

    @Nested
    class TheOneShot {

        @Test
        void aMessageIsBroadcastToEveryone() {
            PlayerMock shouter = join("Understudy1");
            PlayerMock bystander = join("Understudy2");

            run(shouter, "hello", "everyone");

            String heard = screenOf(bystander);
            assertTrue(heard.contains("Understudy1"), heard);
            assertTrue(heard.contains("hello everyone"), "the words are rejoined:\n" + heard);
        }

        /** The one-shot form must not silently leave shout mode on behind it. */
        @Test
        void itDoesNotTurnTheModeOn() {
            PlayerMock shouter = join("Understudy1");

            run(shouter, "hello");

            assertFalse(CommandShout.isShouting(shouter));
        }

        /** Nor off, for someone who had deliberately turned it on. */
        @Test
        void itDoesNotTurnTheModeOffEither() {
            PlayerMock shouter = join("Understudy1");
            run(shouter);

            run(shouter, "hello");

            assertTrue(CommandShout.isShouting(shouter));
        }

        @Test
        void theShouterHearsTheirOwnShout() {
            PlayerMock shouter = join("Understudy1");

            run(shouter, "hello");

            assertSaid(shouter, "hello");
        }
    }
}
