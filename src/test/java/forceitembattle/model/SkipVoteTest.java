package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The tally behind {@code /voteskip}.
 *
 * <p>All of this used to be five bare fields on {@code VoteSkipManager} with the rules written
 * between Bukkit broadcasts, so none of it had a test — including the coin flip that settles a tie
 * and the quorum rule that had a live defect in it.
 */
class SkipVoteTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID CAROL = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    private static final UUID WATCHER = UUID.fromString("00000000-0000-0000-0000-00000000000f");

    /** A vote over the given eligible voters, opened by Alice. */
    private static SkipVote openedOver(UUID... eligible) {
        SkipVote vote = new SkipVote(new Random(1));
        vote.open(ALICE, Material.DIAMOND, Set.of(eligible));
        return vote;
    }

    @Nested
    @DisplayName("opening")
    class Opening {

        @Test
        void theInitiatorsYesIsAlreadyCast() {
            SkipVote vote = openedOver(ALICE, BOB);

            assertEquals(SkipVote.Cast.ALREADY_VOTED, vote.cast(ALICE, true));
        }

        /**
         * A vote of one does not resolve on the spot: it would close before anybody could read the
         * message announcing it, and the sixty seconds are the point of the feature.
         */
        @Test
        void aVoteOfOneStaysOpen() {
            assertTrue(openedOver(ALICE).isOpen());
        }

        @Test
        void openingCarriesTheItemAndTheInitiator() {
            SkipVote vote = openedOver(ALICE, BOB);

            assertEquals(Material.DIAMOND, vote.material());
            assertEquals(ALICE, vote.initiator());
        }

        /** A second vote starts clean rather than inheriting the last one's ballots. */
        @Test
        void reopeningForgetsTheLastVote() {
            SkipVote vote = openedOver(ALICE, BOB);
            vote.cast(BOB, false);
            vote.close();

            vote.open(ALICE, Material.STONE, Set.of(ALICE, BOB));

            assertEquals(1, vote.close().yes());
        }
    }

    @Nested
    @DisplayName("casting")
    class Casting {

        @Test
        void aSecondVoteFromTheSamePlayerIsRefused() {
            SkipVote vote = openedOver(ALICE, BOB, CAROL);
            vote.cast(BOB, true);

            assertEquals(SkipVote.Cast.ALREADY_VOTED, vote.cast(BOB, false));
        }

        /** A refused vote does not count, so it cannot fill the quorum either. */
        @Test
        void aRefusedVoteChangesNothing() {
            SkipVote vote = openedOver(ALICE, BOB, CAROL);
            vote.cast(BOB, true);
            vote.cast(BOB, false);

            assertTrue(vote.isOpen());
            SkipVote.Tally tally = vote.close();
            assertEquals(2, tally.yes());
            assertEquals(0, tally.no());
        }

        /** The defect this module exists to close: a spectator is not an eligible voter. */
        @Test
        void someoneOutsideTheRoundCannotVote() {
            SkipVote vote = openedOver(ALICE, BOB);

            assertEquals(SkipVote.Cast.NOT_ELIGIBLE, vote.cast(WATCHER, true));
            assertEquals(0, vote.close().no());
        }

        @Test
        void theLastEligibleVoterClosesIt() {
            SkipVote vote = openedOver(ALICE, BOB);

            assertEquals(SkipVote.Cast.CLOSES_THE_VOTE, vote.cast(BOB, false));
        }

        @Test
        void anEarlierVoterDoesNot() {
            SkipVote vote = openedOver(ALICE, BOB, CAROL);

            assertEquals(SkipVote.Cast.COUNTED, vote.cast(BOB, false));
        }

        /**
         * The eligible set is fixed at open. A participant who disconnects mid-vote used to leave a
         * quorum nobody could reach; now the vote simply runs its minute, and someone who joined
         * after it opened does not get a ballot.
         */
        @Test
        void theRosterChangingMidVoteDoesNotMoveTheGoalposts() {
            SkipVote vote = openedOver(ALICE, BOB);

            assertEquals(SkipVote.Cast.NOT_ELIGIBLE, vote.cast(CAROL, true));
            assertEquals(SkipVote.Cast.CLOSES_THE_VOTE, vote.cast(BOB, true));
        }

        @Test
        void nothingCanBeCastBeforeAVoteOpens() {
            assertEquals(SkipVote.Cast.NO_VOTE_OPEN, new SkipVote(new Random(1)).cast(ALICE, true));
        }
    }

    @Nested
    @DisplayName("the result")
    class Result {

        @Test
        void aMajorityCarries() {
            SkipVote vote = openedOver(ALICE, BOB, CAROL);
            vote.cast(BOB, true);
            vote.cast(CAROL, false);

            SkipVote.Tally tally = vote.close();

            assertEquals(2, tally.yes());
            assertEquals(1, tally.no());
            assertFalse(tally.tie());
            assertTrue(tally.carried());
        }

        @Test
        void aMinorityDoesNot() {
            SkipVote vote = openedOver(ALICE, BOB, CAROL);
            vote.cast(BOB, false);
            vote.cast(CAROL, false);

            assertFalse(vote.close().carried());
        }

        /** A tie is a coin flip, so over many draws it must land both ways. */
        @Test
        void aTieIsSettledByACoinFlip() {
            Random random = new Random(20260903L);
            List<Boolean> outcomes = new java.util.ArrayList<>();

            for (int i = 0; i < 200; i++) {
                SkipVote vote = new SkipVote(random);
                vote.open(ALICE, Material.DIAMOND, Set.of(ALICE, BOB));
                vote.cast(BOB, false);
                SkipVote.Tally tally = vote.close();
                assertTrue(tally.tie());
                outcomes.add(tally.carried());
            }

            assertTrue(outcomes.contains(true) && outcomes.contains(false),
                    "a tie must be able to go either way");
        }

        /** Nobody voting at all is 0–0, which is a tie rather than a refusal. */
        @Test
        void anEmptyVoteIsATie() {
            SkipVote vote = new SkipVote(new Random(1));

            assertTrue(vote.close().tie());
        }

        @Test
        void closingEndsTheVote() {
            SkipVote vote = openedOver(ALICE, BOB);
            vote.close();

            assertFalse(vote.isOpen());
        }
    }

    @Nested
    @DisplayName("cancelling")
    class Cancelling {

        @Test
        void aCancelledVoteIsClosedAndEmpty() {
            SkipVote vote = openedOver(ALICE, BOB);
            vote.cancel();

            assertFalse(vote.isOpen());
            assertEquals(SkipVote.Cast.NO_VOTE_OPEN, vote.cast(BOB, true));
        }

        @Test
        void aCancelledVoteKeepsNoItemOrInitiator() {
            SkipVote vote = openedOver(ALICE, BOB);
            vote.cancel();

            org.junit.jupiter.api.Assertions.assertNull(vote.material());
            org.junit.jupiter.api.Assertions.assertNull(vote.initiator());
        }
    }
}
