package forceitembattle.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Roster;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Scheduler;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * The 60-second life of a skip vote.
 *
 * <p>A vote ends one of two ways: everyone votes, or the clock runs out. The second was untestable
 * for as long as {@code Scheduler} could not be driven from a test — and it is the one that decides
 * what happens to a vote nobody finishes, which is the common case in a real round. Ticking the
 * MockBukkit scheduler is what makes 60 seconds cost nothing to assert.
 *
 * <p>One rule is recorded here rather than endorsed — {@link Quorum#aSpectatorsVoteCountsTowardsQuorum}.
 * It is pre-existing and not a regression from the scheduler work.
 */
class VoteSkipExpiryTest {

    private static final long SIXTY_SECONDS_IN_TICKS = 20L * 60L;

    private ServerMock server;
    private Roster roster;
    private ForceItemAssignment assignment;
    private VoteSkipManager votes;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        Scheduler.init(MockBukkit.createMockPlugin());

        this.roster = new Roster();
        this.assignment = mock(ForceItemAssignment.class);

        ItemDifficultiesManager items = mock(ItemDifficultiesManager.class);
        when(items.getUnicodeFromMaterial(true, Material.DIRT)).thenReturn("");

        GameSettings settings = mock(GameSettings.class);
        this.votes = new VoteSkipManager(this.roster, this.assignment, settings, items);
    }

    @AfterEach
    void tearDown() {
        Scheduler.reset();
        MockBukkit.unmock();
    }

    private ForceItemPlayer joinPlaying(String name) {
        PlayerMock player = this.server.addPlayer(name);
        ForceItemPlayer entry = new ForceItemPlayer(player, Material.DIRT, 3, 0);
        this.roster.add(player.getUniqueId(), entry);
        return entry;
    }

    private static PlayerMock playerOf(ForceItemPlayer entry) {
        return (PlayerMock) entry.player();
    }

    /** Everything this player has been told since it was last called. */
    private static String screenOf(PlayerMock player) {
        StringBuilder said = new StringBuilder();
        String line;
        while ((line = player.nextMessage()) != null) {
            said.append(line).append('\n');
        }
        return said.toString();
    }

    @Nested
    class AVoteNobodyFinishes {

        @Test
        @DisplayName("is still running one tick before the minute is up")
        void isStillOpenJustBeforeTheMinute() {
            ForceItemPlayer starter = joinPlaying("Understudy1");
            joinPlaying("Understudy2");

            votes.startVoting(playerOf(starter));
            server.getScheduler().performTicks(SIXTY_SECONDS_IN_TICKS - 1);

            assertTrue(votes.isVoteInProgress(), "the vote should outlive 59.95 seconds");
        }

        @Test
        @DisplayName("closes itself when the minute elapses")
        void closesOnTheMinute() {
            ForceItemPlayer starter = joinPlaying("Understudy1");
            joinPlaying("Understudy2");

            votes.startVoting(playerOf(starter));
            server.getScheduler().performTicks(SIXTY_SECONDS_IN_TICKS + 1);

            assertTrue(!votes.isVoteInProgress(), "the vote should have expired on its own");
        }

        /**
         * The initiator's own YES is the only vote cast, so the tally is 1–0 and carries without any
         * coin flip. Asserted through the manager rather than the message, because the skip is what
         * the vote is for.
         */
        @Test
        void anUnopposedVoteCarriesAndSkipsTheItem() {
            ForceItemPlayer starter = joinPlaying("Understudy1");
            joinPlaying("Understudy2");

            votes.startVoting(playerOf(starter));
            server.getScheduler().performTicks(SIXTY_SECONDS_IN_TICKS + 1);

            verify(assignment).skipAll(starter, false);
        }

        /** The vote costs a joker whether or not it carried — the comment at :127 says so. */
        @Test
        void theJokerIsSpentOnExpiry() {
            ForceItemPlayer starter = joinPlaying("Understudy1");
            joinPlaying("Understudy2");
            int before = starter.activeJokers();

            votes.startVoting(playerOf(starter));
            server.getScheduler().performTicks(SIXTY_SECONDS_IN_TICKS + 1);

            assertEquals(before - 1, starter.activeJokers(),
                    "the initiator pays for the vote even when nobody else turns up");
        }

        @Test
        void andEveryoneIsToldItEnded() {
            ForceItemPlayer starter = joinPlaying("Understudy1");
            ForceItemPlayer bystander = joinPlaying("Understudy2");

            votes.startVoting(playerOf(starter));
            server.getScheduler().performTicks(SIXTY_SECONDS_IN_TICKS + 1);

            assertTrue(screenOf(playerOf(bystander)).contains("skip voting has been ended"),
                    "the result is broadcast, not just sent to the initiator");
        }
    }

    @Nested
    class Quorum {

        /** Everyone having voted ends it early, and the pending expiry must not fire again after. */
        @Test
        void everyoneVotingEndsItBeforeTheMinute() {
            ForceItemPlayer starter = joinPlaying("Understudy1");
            ForceItemPlayer other = joinPlaying("Understudy2");

            votes.startVoting(playerOf(starter));
            votes.castVote(playerOf(other), true);

            assertTrue(!votes.isVoteInProgress(), "quorum closes the vote immediately");

            server.getScheduler().performTicks(SIXTY_SECONDS_IN_TICKS + 1);
            verify(assignment).skipAll(starter, false);
        }

        /**
         * <b>Recorded, not endorsed.</b> Quorum counts {@code roster.players()}, which includes
         * spectators, and {@code castVote} never asks the roster whether the voter is playing. So a
         * spectator both inflates the denominator and can fill it. This predates the scheduler work;
         * it is candidate 6 in the pass-4 review, where the fix is an eligible-voter set handed in
         * at {@code open}.
         */
        @Test
        void aSpectatorsVoteCountsTowardsQuorum() {
            ForceItemPlayer starter = joinPlaying("Understudy1");
            ForceItemPlayer spectator = joinPlaying("Understudy2");
            spectator.setSpectator(true);

            votes.startVoting(playerOf(starter));
            votes.castVote(playerOf(spectator), true);

            assertTrue(!votes.isVoteInProgress(),
                    "current behaviour: a spectator's vote closes the poll");
        }

        @Test
        void votingTwiceIsRefused() {
            ForceItemPlayer starter = joinPlaying("Understudy1");
            ForceItemPlayer other = joinPlaying("Understudy2");
            joinPlaying("Understudy3");

            votes.startVoting(playerOf(starter));
            votes.castVote(playerOf(other), true);
            screenOf(playerOf(other));
            votes.castVote(playerOf(other), false);

            assertTrue(screenOf(playerOf(other)).contains("already voted"));
        }
    }

    @Nested
    class Cancelling {

        @Test
        void aCancelledVoteDoesNotFireWhenTheMinuteElapses() {
            ForceItemPlayer starter = joinPlaying("Understudy1");
            joinPlaying("Understudy2");

            votes.startVoting(playerOf(starter));
            votes.cancelVote();
            server.getScheduler().performTicks(SIXTY_SECONDS_IN_TICKS + 1);

            verify(assignment, never()).skipAll(any(), anyBoolean());
        }

        @Test
        void andDisablingTheManagerCancelsThePendingVoteToo() {
            ForceItemPlayer starter = joinPlaying("Understudy1");
            joinPlaying("Understudy2");

            votes.startVoting(playerOf(starter));
            votes.disable();
            server.getScheduler().performTicks(SIXTY_SECONDS_IN_TICKS + 1);

            verify(assignment, never()).skipAll(any(), anyBoolean());
        }
    }
}
