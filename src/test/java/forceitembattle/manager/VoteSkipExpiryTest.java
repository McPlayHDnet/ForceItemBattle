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
 * The 60-second life of a skip vote: the wiring — the clock, the chat and the joker charge. The
 * tally itself is {@code model/SkipVote} and is tested there.
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
        return joinPlaying(name, 3);
    }

    private ForceItemPlayer joinPlaying(String name, int jokers) {
        PlayerMock player = this.server.addPlayer(name);
        ForceItemPlayer entry = new ForceItemPlayer(player, Material.DIRT, jokers, 0);
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

        /** The initiator's own YES is the only vote, so 1–0 carries without a coin flip. */
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

        /** Quorum once counted {@code roster.players()}, so a spectator could close the poll early. */
        @Test
        void aSpectatorCannotVoteAndCannotCloseThePoll() {
            ForceItemPlayer starter = joinPlaying("Understudy1");
            ForceItemPlayer spectator = joinPlaying("Understudy2");
            spectator.setSpectator(true);

            votes.startVoting(playerOf(starter));
            votes.castVote(playerOf(spectator), true);

            assertTrue(votes.isVoteInProgress(),
                    "a spectator is not an eligible voter and must not close the poll");
            assertTrue(screenOf(playerOf(spectator)).contains("players in the round"));
        }

        /** The other half: a spectator does not pad the denominator either. */
        @Test
        void aSpectatorDoesNotInflateTheQuorum() {
            ForceItemPlayer starter = joinPlaying("Understudy1");
            ForceItemPlayer other = joinPlaying("Understudy2");
            ForceItemPlayer spectator = joinPlaying("Understudy3");
            spectator.setSpectator(true);

            votes.startVoting(playerOf(starter));
            votes.castVote(playerOf(other), true);

            assertTrue(!votes.isVoteInProgress(),
                    "both eligible voters have voted, so the poll is done");
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

    /**
     * The button and the pool, after a vote. {@code endVoting} once charged the pool and left the
     * hotbar stack alone, so the button read one too high until {@code /fixskips} repaired it. A test
     * of {@code JokerSpend} alone passes throughout: the arithmetic was never the problem, the wiring
     * was.
     */
    @Nested
    class TheButtonAgreesWithThePool {

        private int jokerStackOf(ForceItemPlayer entry) {
            return PlayerOutfitter.jokerStackIn(playerOf(entry)).orElse(0);
        }

        @Test
        @DisplayName("after a vote, the number on the button equals the number in the pool")
        void theStackFollowsTheCharge() {
            ForceItemPlayer starter = joinPlaying("Understudy1");
            joinPlaying("Understudy2");
            PlayerOutfitter.setJokerStack(playerOf(starter), starter.activeJokers());

            votes.startVoting(playerOf(starter));
            server.getScheduler().performTicks(SIXTY_SECONDS_IN_TICKS + 1);

            assertEquals(starter.activeJokers(), jokerStackOf(starter),
                    "the button must not outlive the joker it spent");
        }

        @Test
        void andTheChargeActuallyHappened() {
            ForceItemPlayer starter = joinPlaying("Understudy1");
            joinPlaying("Understudy2");
            PlayerOutfitter.setJokerStack(playerOf(starter), 3);

            votes.startVoting(playerOf(starter));
            server.getScheduler().performTicks(SIXTY_SECONDS_IN_TICKS + 1);

            assertEquals(2, jokerStackOf(starter));
        }

        /** Spending the last one leaves no button at all, rather than a stack of zero. */
        @Test
        void theLastJokerLeavesNoButton() {
            ForceItemPlayer starter = joinPlaying("Understudy1", 1);
            joinPlaying("Understudy2");
            PlayerOutfitter.setJokerStack(playerOf(starter), 1);

            votes.startVoting(playerOf(starter));
            server.getScheduler().performTicks(SIXTY_SECONDS_IN_TICKS + 1);

            assertTrue(PlayerOutfitter.jokerStackIn(playerOf(starter)).isEmpty());
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
