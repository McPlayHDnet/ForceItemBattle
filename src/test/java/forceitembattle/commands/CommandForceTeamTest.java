package forceitembattle.commands;

import static forceitembattle.commands.CommandTestSupport.assertSaid;
import static forceitembattle.commands.CommandTestSupport.contextWith;
import static forceitembattle.commands.CommandTestSupport.screenOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.admin.CommandForceTeam;
import forceitembattle.manager.TeamsManager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.model.Roster;
import forceitembattle.settings.GameSetting;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@code /forceteam}: putting one or two named players into a team before the round starts.
 *
 * <p>Three gates stack on it and the order they are declared in is what a player sees: not an op,
 * teams not enabled, round already started. All three are asserted, because a command that can
 * rewrite the roster is one where "refused for the wrong reason" and "not refused at all" are hard
 * to tell apart from the outside.
 *
 * <p>{@link Solo} pins the two-argument form, which passes {@code null} as the second member. That
 * is the intended shape â€” {@code TeamsManager.create}'s second parameter is {@code @Nullable} â€”
 * and it is the half that is easy to lose to a "surely both are required" tidy-up.
 */
class CommandForceTeamTest {

    private ServerMock server;
    private Roster roster;
    private TeamsManager teams;
    private CommandForceTeam command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();

        ForceItemBattle plugin = mock(ForceItemBattle.class);
        this.roster = new Roster();
        this.teams = mock(TeamsManager.class);

        when(plugin.getRoster()).thenReturn(this.roster);
        when(plugin.getTeamManager()).thenReturn(this.teams);

        this.command = new CommandForceTeam(plugin);
        inARoundThatIs(GameState.PRE_GAME, GameSetting.TEAM);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- fixtures ---------------------------------------------------------------------------

    private void inARoundThatIs(GameState state, GameSetting... settings) {
        ((CustomCommand) this.command).setContext(contextWith(state, this.roster, settings));
    }

    private PlayerMock joinOp(String name) {
        PlayerMock player = this.server.addPlayer(name);
        player.setOp(true);
        return player;
    }

    /** Joins someone and puts them on the roster, which is where {@code create} reads them from. */
    private ForceItemPlayer joinOnRoster(String name) {
        PlayerMock player = this.server.addPlayer(name);
        ForceItemPlayer entry = new ForceItemPlayer(player, Material.DIRT, 0, 0);
        this.roster.add(player.getUniqueId(), entry);
        return entry;
    }

    private void run(PlayerMock player, String... args) {
        this.command.onCommand(player, null, "forceteam", args);
    }

    // --- the tests --------------------------------------------------------------------------

    @Nested
    class APair {

        @Test
        void bothNamedPlayersAreTeamed() {
            PlayerMock admin = joinOp("Admin");
            ForceItemPlayer first = joinOnRoster("Understudy1");
            ForceItemPlayer second = joinOnRoster("Understudy2");

            run(admin, "Redstone", "Understudy1", "Understudy2");

            verify(teams).create(first, second, "Redstone");
            assertSaid(admin, "Redstone");
        }

        /** The team name is taken verbatim, not lowercased or normalised. */
        @Test
        void theTeamNameIsTakenAsTyped() {
            PlayerMock admin = joinOp("Admin");
            joinOnRoster("Understudy1");
            joinOnRoster("Understudy2");

            run(admin, "RedStone", "Understudy1", "Understudy2");

            verify(teams).create(any(), any(), eq("RedStone"));
        }
    }

    @Nested
    class Solo {

        /** Two arguments make a team of one, which {@code create} accepts as a null partner. */
        @Test
        void oneNamedPlayerGetsATeamOfTheirOwn() {
            PlayerMock admin = joinOp("Admin");
            ForceItemPlayer only = joinOnRoster("Understudy1");

            run(admin, "Redstone", "Understudy1");

            verify(teams).create(eq(only), isNull(), eq("Redstone"));
            assertSaid(admin, "solo team");
        }
    }

    @Nested
    class Refusals {

        @Test
        void tooFewArgumentsShowTheForm() {
            PlayerMock admin = joinOp("Admin");

            run(admin, "Redstone");

            assertSaid(admin, "/forceteam");
            verifyNoInteractions(teams);
        }

        @Test
        void tooManyArgumentsShowTheFormToo() {
            PlayerMock admin = joinOp("Admin");
            joinOnRoster("Understudy1");
            joinOnRoster("Understudy2");

            run(admin, "Redstone", "Understudy1", "Understudy2", "Understudy3");

            assertSaid(admin, "/forceteam");
            verifyNoInteractions(teams);
        }

        @Test
        void anOfflineFirstPlayerIsRefused() {
            PlayerMock admin = joinOp("Admin");
            joinOnRoster("Understudy2");

            run(admin, "Redstone", "NobodyAtAll", "Understudy2");

            String said = screenOf(admin);
            assertTrue(said.contains("NobodyAtAll"), said);
            assertTrue(said.contains("is not online"), said);
            verifyNoInteractions(teams);
        }

        /** And the second is checked before anything is created, not after. */
        @Test
        void anOfflineSecondPlayerIsRefusedBeforeTheTeamIsMade() {
            PlayerMock admin = joinOp("Admin");
            joinOnRoster("Understudy1");

            run(admin, "Redstone", "Understudy1", "NobodyAtAll");

            assertSaid(admin, "is not online");
            verifyNoInteractions(teams);
        }

        @Test
        void aNonOpIsRefused() {
            PlayerMock player = server.addPlayer("Understudy1");
            joinOnRoster("Understudy2");

            run(player, "Redstone", "Understudy2");

            assertSaid(player, "permission");
            verifyNoInteractions(teams);
        }

        @Test
        void withTeamsOffThereIsNothingToForce() {
            PlayerMock admin = joinOp("Admin");
            joinOnRoster("Understudy1");
            inARoundThatIs(GameState.PRE_GAME);

            run(admin, "Redstone", "Understudy1");

            assertSaid(admin, "Teams are not enabled");
            verifyNoInteractions(teams);
        }

        /** Teams are settled at the countdown, so this is a pre-game command only. */
        @Test
        void aRoundAlreadyUnderWayIsTooLate() {
            PlayerMock admin = joinOp("Admin");
            joinOnRoster("Understudy1");
            inARoundThatIs(GameState.MID_GAME, GameSetting.TEAM);

            run(admin, "Redstone", "Understudy1");

            assertSaid(admin, "already started");
            verifyNoInteractions(teams);
        }

        /**
         * The countdown counts as started: {@code STARTING} has already frozen the roster and
         * assigned teams, so editing them here would leave the two disagreeing.
         */
        @Test
        void soIsTheCountdown() {
            PlayerMock admin = joinOp("Admin");
            joinOnRoster("Understudy1");
            inARoundThatIs(GameState.STARTING, GameSetting.TEAM);

            run(admin, "Redstone", "Understudy1");

            assertSaid(admin, "already started");
            verifyNoInteractions(teams);
        }
    }
}
