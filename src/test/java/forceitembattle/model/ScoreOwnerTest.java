package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import forceitembattle.Players;
import java.util.List;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The {@link ScoreOwner} mutators, and the property the collapsed loops in {@code Gamemanager}
 * depend on.
 *
 * <p>Both implementations get the same assertions, because "a team and a solo player are
 * interchangeable as the thing that scores" is the whole claim the interface makes. Anything that
 * held for one and not the other would mean the branch is still there, just hidden.
 *
 * <p>The double-application tests are not hypothetical. Before these methods existed, the team half
 * of {@code advanceMaterials} ran once per member, so a two-player team advanced twice on a single
 * find; and the team half of {@code initializeMaterials} drew a fresh material pair per member and
 * let the last win. Both are pinned here as owner-level behaviour so the loops above cannot quietly
 * reintroduce them.
 */
class ScoreOwnerTest {

    private static final Material FIRST = Material.DIRT;
    private static final Material SECOND = Material.STONE;
    private static final Material THIRD = Material.OAK_LOG;
    private static final Material FOURTH = Material.SAND;

    private static ForceItemPlayer player(String name) {
        return new ForceItemPlayer(Players.mockPlayer(name), FIRST, 3, 7);
    }

    /** A solo player's own score, reached the way every caller now reaches it. */
    private static ScoreOwner solo() {
        return player("a").scoreOwner();
    }

    private static ScoreOwner team() {
        ForceItemPlayer alice = player("a");
        ForceItemPlayer bob = player("b");
        Team team = new Team(1, FIRST, 7, 3, alice, bob);
        alice.setCurrentTeam(team);
        bob.setCurrentTeam(team);
        return team;
    }

    private static void bothImplementations(java.util.function.Consumer<ScoreOwner> assertions) {
        assertions.accept(solo());
        assertions.accept(team());
    }

    @Nested
    class StartRound {

        @Test
        void assignsThePairAndZeroesTheScore() {
            bothImplementations(owner -> {
                owner.record(new ForceItem(SECOND, "00:10", 1L, new BackToBack(false), false, null));

                owner.startRound(FIRST, SECOND, 5_000L);

                assertEquals(0, owner.score());
                assertEquals(FIRST, owner.material());
                assertEquals(SECOND, owner.nextMaterial());
                assertEquals(5_000L, owner.itemAssignedAt());
            });
        }
    }

    @Nested
    class Advance {

        @Test
        void movesCurrentToPreviousAndNextToCurrent() {
            bothImplementations(owner -> {
                owner.startRound(FIRST, SECOND, 1_000L);

                owner.advance(THIRD, 2_000L);

                assertEquals(FIRST, owner.previousMaterial());
                assertEquals(SECOND, owner.material());
                assertEquals(THIRD, owner.nextMaterial());
                assertEquals(2_000L, owner.itemAssignedAt());
            });
        }

        /** Why advance is called once per owner and not once per member. */
        @Test
        void applyingItTwiceSkipsTheQueuedItemRatherThanAdvancingTwice() {
            bothImplementations(owner -> {
                owner.startRound(FIRST, SECOND, 1_000L);

                owner.advance(THIRD, 2_000L);
                owner.advance(THIRD, 2_000L);

                // SECOND was never hunted; current and next both collapsed onto THIRD
                assertEquals(THIRD, owner.material());
                assertEquals(THIRD, owner.nextMaterial());
            });
        }

        @Test
        void previousIsNullBeforeTheFirstAdvance() {
            bothImplementations(owner -> {
                owner.startRound(FIRST, SECOND, 1_000L);

                assertNull(owner.previousMaterial());
            });
        }
    }

    @Nested
    class AssignMaterials {

        /** A skip is not a find, so it must not restart the clock or touch the score. */
        @Test
        void replacesThePairAndLeavesTheScoreAndClockAlone() {
            bothImplementations(owner -> {
                owner.startRound(FIRST, SECOND, 1_000L);
                owner.record(new ForceItem(FIRST, "00:10", 1L, new BackToBack(false), false, null));

                owner.assignMaterials(THIRD, FOURTH);

                assertEquals(THIRD, owner.material());
                assertEquals(FOURTH, owner.nextMaterial());
                assertEquals(1, owner.score());
                assertEquals(1_000L, owner.itemAssignedAt());
            });
        }
    }

    @Nested
    class Jokers {

        @Test
        void spendingDrawsDownAndStopsAtZero() {
            bothImplementations(owner -> {
                owner.setJokers(2);

                assertEquals(1, owner.spendJoker());
                assertEquals(0, owner.spendJoker());
                assertEquals(0, owner.spendJoker());
            });
        }
    }

    /**
     * What {@code Gamemanager.activeScoreOwners()} relies on: a roster hands out one entry per
     * player, and de-duplicating by owner is what turns that back into one entry per team.
     */
    @Nested
    class Deduplication {

        @Test
        void bothMembersOfATeamCollapseToOneOwner() {
            ForceItemPlayer alice = player("a");
            ForceItemPlayer bob = player("b");
            Team team = new Team(1, FIRST, 0, 0, alice, bob);
            alice.setCurrentTeam(team);
            bob.setCurrentTeam(team);

            List<ScoreOwner> owners = Stream.of(alice, bob)
                    .map(ForceItemPlayer::scoreOwner)
                    .distinct()
                    .toList();

            assertEquals(1, owners.size());
            assertSame(team, owners.get(0));
        }

        @Test
        void soloPlayersStayDistinct() {
            ForceItemPlayer alice = player("a");
            ForceItemPlayer bob = player("b");

            List<ScoreOwner> owners = Stream.of(alice, bob)
                    .map(ForceItemPlayer::scoreOwner)
                    .distinct()
                    .toList();

            assertEquals(2, owners.size());
        }
    }
}
