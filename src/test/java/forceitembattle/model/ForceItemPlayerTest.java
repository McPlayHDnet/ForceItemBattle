package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.Players;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The two accessor families on {@link ForceItemPlayer}.
 *
 * The whole point of the split is that {@code active*} follows the team when there is one and the
 * plain accessors never do. A mistake either way is silent — the wrong number goes on a scoreboard
 * or into a stats row, nothing throws — so both directions are pinned here.
 */
class ForceItemPlayerTest {

    private static final Material OWN = Material.DIRT;
    private static final Material TEAM_CURRENT = Material.STONE;
    private static final Material TEAM_NEXT = Material.OAK_LOG;
    private static final Material TEAM_PREVIOUS = Material.SAND;

    private static ForceItemPlayer solo() {
        ForceItemPlayer player = new ForceItemPlayer(Players.mockPlayer("a"), OWN, 3, 7);
        player.setNextMaterial(Material.GRAVEL);
        player.setPreviousMaterial(Material.CLAY);
        player.setLastItemAssignedAt(1_000L);
        return player;
    }

    private static Team teamOf(ForceItemPlayer... members) {
        Team team = new Team(1, TEAM_CURRENT, 42, 9, members);
        team.setNextMaterial(TEAM_NEXT);
        team.setPreviousMaterial(TEAM_PREVIOUS);
        team.setLastItemAssignedAt(5_000L);
        for (ForceItemPlayer member : members) {
            member.setCurrentTeam(team);
        }
        return team;
    }

    @Nested
    class Solo {

        @Test
        void activeAccessorsFallBackToOwnFields() {
            ForceItemPlayer player = solo();

            assertFalse(player.isInTeam());
            assertEquals(OWN, player.activeMaterial());
            assertEquals(Material.GRAVEL, player.activeNextMaterial());
            assertEquals(Material.CLAY, player.activePreviousMaterial());
            assertEquals(3, player.activeJokers());
            assertEquals(7, player.activeScore());
            assertEquals(1_000L, player.activeItemAssignedAt());
        }

        /**
         * Whether this player has a team is a fact about the player, not the setting. Asking
         * {@code isSettingEnabled(TEAM)} and then dereferencing {@code currentTeam()} throws for a
         * player who joined during the countdown: they hold a roster spot and no team.
         */
        @Test
        void aParticipantWithNoTeamIsSoloWhateverTheRoundWasConfiguredFor() {
            ForceItemPlayer player = solo();

            assertFalse(player.isInTeam());
            assertNull(player.currentTeam());
            assertEquals(List.of(player), player.squad());
            assertTrue(player.teammate().isEmpty());
        }

        @Test
        void squadIsJustThisPlayerAndThereIsNoTeammate() {
            ForceItemPlayer player = solo();

            assertEquals(1, player.squad().size());
            assertSame(player, player.squad().get(0));
            assertTrue(player.teammate().isEmpty());
        }

        @Test
        void spendingAJokerDrawsFromOwnPool() {
            ForceItemPlayer player = solo();

            assertEquals(2, player.spendJoker());
            assertEquals(2, player.activeJokers());
            assertEquals(2, player.remainingJokers());
        }

        @Test
        void jokersNeverGoNegative() {
            ForceItemPlayer player = new ForceItemPlayer(Players.mockPlayer("a"), OWN, 0, 0);

            assertEquals(0, player.spendJoker());
            assertEquals(0, player.activeJokers());
        }

        @Test
        void foundItemIsCreditedToThePlayer() {
            ForceItemPlayer player = solo();
            ForceItem item = forceItem(Material.STONE);

            player.recordFoundItem(item);

            assertEquals(8, player.activeScore());
            assertEquals(8, player.currentScore());
            assertEquals(1, player.foundItems().size());
            assertSame(item, player.foundItems().get(0));
        }
    }

    @Nested
    class InATeam {

        @Test
        void activeAccessorsFollowTheTeam() {
            ForceItemPlayer player = solo();
            teamOf(player);

            assertTrue(player.isInTeam());
            assertEquals(TEAM_CURRENT, player.activeMaterial());
            assertEquals(TEAM_NEXT, player.activeNextMaterial());
            assertEquals(TEAM_PREVIOUS, player.activePreviousMaterial());
            assertEquals(9, player.activeJokers());
            assertEquals(42, player.activeScore());
            assertEquals(5_000L, player.activeItemAssignedAt());
        }

        @Test
        void plainAccessorsStillReportOwnFields() {
            ForceItemPlayer player = solo();
            teamOf(player);

            assertEquals(OWN, player.currentMaterial());
            assertEquals(Material.GRAVEL, player.nextMaterial());
            assertEquals(Material.CLAY, player.previousMaterial());
            assertEquals(3, player.remainingJokers());
            assertEquals(7, player.currentScore());
            assertEquals(1_000L, player.lastItemAssignedAt());
        }

        @Test
        void spendingAJokerDrawsFromTheSharedPoolNotTheOwnField() {
            ForceItemPlayer alice = solo();
            ForceItemPlayer bob = new ForceItemPlayer(Players.mockPlayer("b"), OWN, 3, 0);
            teamOf(alice, bob);

            assertEquals(8, alice.spendJoker());

            // both members see the spend...
            assertEquals(8, alice.activeJokers());
            assertEquals(8, bob.activeJokers());
            // ...and neither player's own field was touched
            assertEquals(3, alice.remainingJokers());
            assertEquals(3, bob.remainingJokers());
        }

        @Test
        void foundItemIsCreditedToTheTeamNotThePlayer() {
            ForceItemPlayer alice = solo();
            ForceItemPlayer bob = new ForceItemPlayer(Players.mockPlayer("b"), OWN, 3, 0);
            Team team = teamOf(alice, bob);
            ForceItem item = forceItem(Material.STONE);

            alice.recordFoundItem(item);

            assertEquals(43, team.getCurrentScore());
            assertEquals(43, alice.activeScore());
            assertEquals(43, bob.activeScore());
            // the player's own score and found-list stay empty; the team owns both
            assertEquals(7, alice.currentScore());
            assertTrue(alice.foundItems().isEmpty());
            assertEquals(1, team.getFoundItems().size());
        }

        @Test
        void squadIsTheWholeTeamAndTeammateIsTheOtherMember() {
            ForceItemPlayer alice = solo();
            ForceItemPlayer bob = new ForceItemPlayer(Players.mockPlayer("b"), OWN, 3, 0);
            teamOf(alice, bob);

            assertEquals(2, alice.squad().size());
            assertSame(bob, alice.teammate().orElseThrow());
            assertSame(alice, bob.teammate().orElseThrow());
        }

        /**
         * An odd player count leaves someone alone in a team. They are "in a team" for every read,
         * but have no teammate — which is what suppresses their member/team stat writes.
         */
        @Test
        void aOnePersonTeamHasNoTeammate() {
            ForceItemPlayer lonely = solo();
            teamOf(lonely);

            assertTrue(lonely.isInTeam());
            assertTrue(lonely.teammate().isEmpty());
            assertEquals(1, lonely.squad().size());
        }
    }

    /**
     * The family split above pins <em>what</em> each accessor returns; these pin <em>how</em> the
     * {@link ScoreOwner} behind it is chosen.
     */
    @Nested
    class Routing {

        @Test
        void aSoloPlayerOwnsTheirOwnScore() {
            ForceItemPlayer player = solo();

            assertFalse(player.scoreOwner() instanceof Team);
            assertEquals(1, player.scoreOwner().members().size());
            assertSame(player, player.scoreOwner().members().get(0));
        }

        @Test
        void joiningATeamHandsTheTeamTheOwnership() {
            ForceItemPlayer player = solo();
            Team team = teamOf(player);

            assertSame(team, player.scoreOwner());
        }

        /**
         * Leaving a team restores the player's own values untouched. Under the old field-based
         * design this fell out of the ternaries for free; now that ownership is a reference, the
         * SoloScore has to be retained rather than rebuilt, and nothing else pins that.
         */
        @Test
        void leavingATeamRestoresTheOwnValuesUnchanged() {
            ForceItemPlayer player = solo();
            Team team = teamOf(player);
            player.recordFoundItem(forceItem(Material.STONE));
            player.spendJoker();

            assertEquals(43, player.activeScore());
            assertEquals(8, player.activeJokers());

            player.setCurrentTeam(null);

            assertFalse(player.isInTeam());
            assertEquals(7, player.activeScore());
            assertEquals(3, player.activeJokers());
            assertEquals(OWN, player.activeMaterial());
            assertTrue(player.foundItems().isEmpty());
            // ...and the team kept everything it was credited with
            assertEquals(43, team.getCurrentScore());
            assertEquals(1, team.getFoundItems().size());
        }

        /**
         * The hazard the old javadoc warned about, now only reachable from inside this package.
         * Writing a player's own values while they sit on a team still updates a copy nobody reads
         * — that has not changed and is not a bug — but no caller outside {@code model/} can do it
         * any more, because every one of them now addresses the {@link ScoreOwner}. If this test
         * ever stops compiling from outside the package, the seal has been broken.
         */
        @Test
        void writingTheOwnValuesWhileOnATeamLeavesTheActiveOnesAlone() {
            ForceItemPlayer player = solo();
            teamOf(player);

            player.setCurrentScore(99);

            assertEquals(99, player.currentScore());
            assertEquals(42, player.activeScore());
        }

        @Test
        void bothMembersShareOneOwner() {
            ForceItemPlayer alice = solo();
            ForceItemPlayer bob = new ForceItemPlayer(Players.mockPlayer("b"), OWN, 3, 0);
            teamOf(alice, bob);

            assertSame(alice.scoreOwner(), bob.scoreOwner());
        }
    }

    @Test
    void previousMaterialIsNullBeforeTheFirstAdvance() {
        ForceItemPlayer player = new ForceItemPlayer(Players.mockPlayer("a"), OWN, 0, 0);

        assertNull(player.previousMaterial());
        assertNull(player.activePreviousMaterial());
    }

    private static ForceItem forceItem(Material material) {
        return new ForceItem(material, "00:10", System.currentTimeMillis(),
                new BackToBack(false), false, UUID.randomUUID());
    }
}
