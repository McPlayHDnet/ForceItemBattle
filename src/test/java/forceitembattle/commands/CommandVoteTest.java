package forceitembattle.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.player.CommandVote;
import forceitembattle.commands.player.CommandVoteSkip;
import forceitembattle.manager.VoteSkipManager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Roster;
import forceitembattle.settings.ConfigSource;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.Ruleset;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@code /vote} and {@code /voteskip}: casting, starting, and who may cancel.
 *
 * <p>{@code /vote cancel} is one of the three surviving subcommand gates — it hangs off
 * {@code args[0]}, which a declared precondition cannot reach, so it uses
 * {@code requireOp(Player, Runnable)} at the dispatch site. That form was chosen because the
 * boolean it replaced is what inverted {@code /skip}; these pin that it gates the right way round.
 */
class CommandVoteTest {

    private ServerMock server;
    private Roster roster;
    private VoteSkipManager voteSkip;
    private CommandVote vote;
    private CommandVoteSkip voteSkipCommand;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        ForceItemBattle plugin = mock(ForceItemBattle.class);
        this.roster = new Roster();
        this.voteSkip = mock(VoteSkipManager.class);

        when(plugin.getRoster()).thenReturn(this.roster);
        when(plugin.getVoteSkipManager()).thenReturn(this.voteSkip);

        Map<String, Object> settings = new HashMap<>();
        settings.put(GameSetting.RUN.configPath(), true);
        RoundPhase phase = new RoundPhase();
        phase.moveTo(GameState.MID_GAME);
        CommandContext context = new CommandContext(phase, new Ruleset(new MapConfig(settings)), this.roster);

        this.vote = new CommandVote(plugin);
        ((CustomCommand) this.vote).setContext(context);
        this.voteSkipCommand = new CommandVoteSkip(plugin);
        ((CustomCommand) this.voteSkipCommand).setContext(context);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private record MapConfig(Map<String, Object> values) implements ConfigSource {
        @Override
        public boolean getBoolean(String path) {
            return Boolean.TRUE.equals(this.values.get(path));
        }

        @Override
        public int getInt(String path) {
            return 0;
        }

        @Override
        public void set(String path, Object value) {
            this.values.put(path, value);
        }

        @Override
        public void save() {
        }
    }

    private PlayerMock onRoster(String name, int jokers) {
        PlayerMock player = this.server.addPlayer(name);
        this.roster.add(player.getUniqueId(), new ForceItemPlayer(player, Material.DIRT, jokers, 0));
        return player;
    }

    @Nested
    class Casting {

        @BeforeEach
        void aVoteIsRunning() {
            when(voteSkip.isVoteInProgress()).thenReturn(true);
        }

        @Test
        void yesIsCastAsYes() {
            PlayerMock player = onRoster("Understudy1", 1);

            vote.onCommand(player, null, "vote", new String[]{"yes"});

            verify(voteSkip).castVote(player, true);
        }

        @Test
        void noIsCastAsNo() {
            PlayerMock player = onRoster("Understudy1", 1);

            vote.onCommand(player, null, "vote", new String[]{"no"});

            verify(voteSkip).castVote(player, false);
        }

        @Test
        void anUnknownOptionIsRefused() {
            PlayerMock player = onRoster("Understudy1", 1);

            vote.onCommand(player, null, "vote", new String[]{"maybe"});

            assertTrue(player.nextMessage().contains("Invalid vote option"));
            verify(voteSkip, never()).castVote(any(), anyBoolean());
        }

        @Test
        void noArgumentGetsTheUsage() {
            PlayerMock player = onRoster("Understudy1", 1);

            vote.onCommand(player, null, "vote", new String[0]);

            assertTrue(player.nextMessage().contains("Usage"));
            verify(voteSkip, never()).castVote(any(), anyBoolean());
        }
    }

    @Nested
    class CancellingIsOpOnly {

        @BeforeEach
        void aVoteIsRunning() {
            when(voteSkip.isVoteInProgress()).thenReturn(true);
        }

        @Test
        void anOpCancels() {
            PlayerMock player = onRoster("Understudy1", 1);
            player.setOp(true);

            vote.onCommand(player, null, "vote", new String[]{"cancel"});

            verify(voteSkip).cancelVote();
        }

        /** The gate must not be the way round that broke {@code /skip}. */
        @Test
        void aNonOpCannotCancel() {
            PlayerMock player = onRoster("Understudy1", 1);
            player.setOp(false);

            vote.onCommand(player, null, "vote", new String[]{"cancel"});

            assertTrue(player.nextMessage().contains("permission"));
            verify(voteSkip, never()).cancelVote();
        }
    }

    @Nested
    class WithNoVoteRunning {

        @Test
        void castingIsRefused() {
            when(voteSkip.isVoteInProgress()).thenReturn(false);
            PlayerMock player = onRoster("Understudy1", 1);

            vote.onCommand(player, null, "vote", new String[]{"yes"});

            assertTrue(player.nextMessage().contains("No skip vote"));
            verify(voteSkip, never()).castVote(any(), anyBoolean());
        }
    }

    @Nested
    class StartingAVote {

        @Test
        void aParticipantWithJokersStartsOne() {
            when(voteSkip.isVoteInProgress()).thenReturn(false);
            PlayerMock player = onRoster("Understudy1", 3);

            voteSkipCommand.onCommand(player, null, "voteskip", new String[0]);

            verify(voteSkip).startVoting(player);
        }

        @Test
        void withoutJokersNothingStarts() {
            PlayerMock player = onRoster("Understudy1", 0);

            voteSkipCommand.onCommand(player, null, "voteskip", new String[0]);

            assertTrue(player.nextMessage().contains("dont have any jokers"));
            verify(voteSkip, never()).startVoting(any());
        }

        @Test
        void aSecondVoteCannotStartWhileOneRuns() {
            when(voteSkip.isVoteInProgress()).thenReturn(true);
            PlayerMock player = onRoster("Understudy1", 3);

            voteSkipCommand.onCommand(player, null, "voteskip", new String[0]);

            assertTrue(player.nextMessage().contains("currently in progress"));
            verify(voteSkip, never()).startVoting(any());
        }

        /** A mid-round joiner holds no entry, and would have NPE'd inside startVoting. */
        @Test
        void someoneNotInTheRoundCannotStartOne() {
            PlayerMock player = server.addPlayer("Latecomer");

            voteSkipCommand.onCommand(player, null, "voteskip", new String[0]);

            assertTrue(player.nextMessage().contains("not playing"));
            verify(voteSkip, never()).startVoting(any());
        }
    }
}
