package forceitembattle.commands;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.player.CommandResult;
import forceitembattle.manager.Gamemanager;
import forceitembattle.manager.TeamsManager;
import forceitembattle.manager.TimerManager;
import forceitembattle.model.Roster;
import forceitembattle.model.Team;
import forceitembattle.service.MatchHistoryReporter;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@code /result}'s argument handling — the paths that refuse before anything is opened.
 *
 * <p>Only the refusals are reachable here. Opening a result screen builds a {@code ResultScreen} or a
 * {@code ResultReveal}, which build {@code ItemStack}s, which needs a running server — see {@code HeadlessBoundaryTest}.
 * That is not a gap in this test so much as the reason these three bugs survived: every path that
 * could be tested was a path that returned early, and none of them did.
 *
 * <p>All three were the same shape — an argument that does not name anything, handed onward as if
 * it did.
 */
class CommandResultTest {

    private ServerMock server;
    private ForceItemBattle plugin;
    private GameSettings settings;
    private TeamsManager teamManager;
    private Roster roster;
    private CommandResult command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = mock(ForceItemBattle.class);
        this.settings = mock(GameSettings.class);
        this.teamManager = mock(TeamsManager.class);
        this.roster = new Roster();

        Gamemanager gamemanager = mock(Gamemanager.class);
        MatchHistoryReporter matchHistory = mock(MatchHistoryReporter.class);
        TimerManager timerManager = mock(TimerManager.class);

        when(this.plugin.getSettings()).thenReturn(this.settings);
        when(this.plugin.getTeamManager()).thenReturn(this.teamManager);
        when(this.plugin.getRoster()).thenReturn(this.roster);
        when(this.plugin.getGamemanager()).thenReturn(gamemanager);
        when(this.plugin.getTimerManager()).thenReturn(timerManager);
        when(gamemanager.getMatchHistory()).thenReturn(matchHistory);
        when(matchHistory.getMatchId()).thenReturn(UUID.randomUUID());
        // The reveal only runs once the round is over.
        when(timerManager.getTimeLeft()).thenReturn(0);

        this.command = new CommandResult(this.plugin);
        ((CustomCommand) this.command).setContext(new CommandContext(
                new forceitembattle.model.RoundPhase(), null, this.roster));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void assertTold(PlayerMock player, String expected) {
        String said = player.nextMessage();
        assertTrue(said != null && PlainTextComponentSerializer.plainText()
                        .serialize(net.kyori.adventure.text.Component.text(said)).contains(expected),
                "expected to be told " + expected + " but got " + said);
    }

    private PlayerMock run(String argument) {
        PlayerMock player = this.server.addPlayer("Understudy1");
        this.command.onCommand(player, null, "result", new String[]{argument});
        return player;
    }

    @Nested
    class TeamArguments {

        @BeforeEach
        void teamsAreOn() {
            when(settings.isSettingEnabled(GameSetting.TEAM)).thenReturn(true);
            when(teamManager.getTeams()).thenReturn(List.of(mock(Team.class)));
        }

        /** A failed parse must not fall through into a result screen built from two nulls. */
        @Test
        void textThatIsNotANumberIsRefused() {
            assertTold(run("#banana"), "Invalid team.");
        }

        /**
         * {@code List.get()} throws {@code IndexOutOfBoundsException}, which does <b>not</b> extend
         * {@code IllegalArgumentException} — so this escaped the catch entirely and threw out of
         * the command.
         */
        @Test
        void aTeamNumberPastTheEndIsRefused() {
            assertDoesNotThrow(() -> assertTold(run("#99"), "Invalid team."));
        }

        @Test
        void teamZeroIsRefused() {
            assertTold(run("#0"), "Invalid team.");
        }

        @Test
        void aNegativeTeamNumberIsRefused() {
            assertTold(run("#-1"), "Invalid team.");
        }
    }

    @Nested
    class SoloArguments {

        @BeforeEach
        void teamsAreOff() {
            when(settings.isSettingEnabled(GameSetting.TEAM)).thenReturn(false);
        }

        @Test
        void textThatIsNotAUuidIsRefused() {
            assertTold(run("not-a-uuid"), "Invalid UUID.");
        }

        /**
         * A well-formed id for somebody who never played resolved to a null roster entry, which the
         * result screen then dereferenced.
         */
        @Test
        void aUuidNobodyOnTheRosterHoldsIsRefused() {
            assertDoesNotThrow(() ->
                    assertTold(run(UUID.randomUUID().toString()), "Nobody with that id played this round."));
        }
    }
}
