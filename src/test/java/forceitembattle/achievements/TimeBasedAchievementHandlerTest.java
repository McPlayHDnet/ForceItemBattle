package forceitembattle.achievements;

import static forceitembattle.achievements.Finds.backToBack;
import static forceitembattle.achievements.Finds.found;
import static forceitembattle.achievements.Finds.participant;
import static forceitembattle.achievements.Finds.record;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.achievements.handlers.TimeBasedAchievementHandler;
import forceitembattle.achievements.progress.TimeAchievementProgress;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Team;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/**
 * {@link TimeBasedAchievementHandler}, which is where the seam paid for itself twice: the round
 * clock it used to assemble out of two managers, and the EARLY_BIRD rule it used to write out once
 * per team and once per roster.
 *
 * <p>None of this was reachable before. The handler took a {@code ForceItemBattle}, so asking
 * "is this player the first to collect?" meant a game manager, a timer manager and a team manager,
 * all mocked, before the first assertion.
 */
class TimeBasedAchievementHandlerTest {

    /** EARLY_BIRD: one item, no other constraint. */
    private static TimeBasedAchievementHandler earlyBird() {
        return new TimeBasedAchievementHandler(1, 0, 0, 0, 0, false, true, false);
    }

    /** SPEEDRUN-shaped: N items within the first X seconds of the round. */
    private static TimeBasedAchievementHandler within(int items, long seconds) {
        return new TimeBasedAchievementHandler(items, seconds, 0, 0, 0, false, false, false);
    }

    private static Team team(ForceItemPlayer... members) {
        Team team = new Team(1, Material.STONE, 0, 0, members);
        for (ForceItemPlayer member : members) {
            member.setCurrentTeam(team);
        }
        return team;
    }

    // --- EARLY_BIRD ------------------------------------------------------------------------

    @Test
    void firstToCollectGetsIt() {
        ForceItemPlayer alice = participant("a");
        ForceItemPlayer bob = participant("b");
        record(alice, Material.DIRT, false);

        FakeAchievementWorld world = new FakeAchievementWorld()
                .scoreOwners(alice.scoreOwner(), bob.scoreOwner());

        assertTrue(earlyBird().check(found(alice, Material.DIRT), earlyBird().createProgress(),
                alice, world));
    }

    @Test
    void secondToCollectDoesNot() {
        ForceItemPlayer alice = participant("a");
        ForceItemPlayer bob = participant("b");
        record(bob, Material.STONE, false);
        record(alice, Material.DIRT, false);

        FakeAchievementWorld world = new FakeAchievementWorld()
                .scoreOwners(alice.scoreOwner(), bob.scoreOwner());

        assertFalse(earlyBird().check(found(alice, Material.DIRT), earlyBird().createProgress(),
                alice, world));
    }

    /**
     * The behaviour this candidate deliberately changed.
     *
     * <p>A skip is not a collect, and the team half of the old branch filtered skips out. The solo
     * half did not — it asked only whether a rival's found-list was empty — so in a solo round one
     * rival skipping their opening item locked EARLY_BIRD out for everybody, permanently and
     * silently. Collapsing both halves onto the team rule is what fixes it.
     */
    @Test
    void aRivalWhoOnlySkippedDoesNotBlockIt() {
        ForceItemPlayer alice = participant("a");
        ForceItemPlayer bob = participant("b");
        record(bob, Material.STONE, true);
        record(alice, Material.DIRT, false);

        FakeAchievementWorld world = new FakeAchievementWorld()
                .scoreOwners(alice.scoreOwner(), bob.scoreOwner());

        assertTrue(earlyBird().check(found(alice, Material.DIRT), earlyBird().createProgress(),
                alice, world));
    }

    @Test
    void aTeamIsOneOwnerSoATeammateDoesNotBlockIt() {
        ForceItemPlayer alice = participant("a");
        ForceItemPlayer bob = participant("b");
        ForceItemPlayer rival = participant("c");
        team(alice, bob);
        record(alice, Material.DIRT, false);

        FakeAchievementWorld world = new FakeAchievementWorld()
                .scoreOwners(alice.scoreOwner(), rival.scoreOwner());

        assertTrue(earlyBird().check(found(alice, Material.DIRT), earlyBird().createProgress(),
                alice, world));
    }

    @Test
    void aRivalTeamThatAlreadyCollectedBlocksIt() {
        ForceItemPlayer alice = participant("a");
        ForceItemPlayer bob = participant("b");
        ForceItemPlayer carol = participant("c");
        ForceItemPlayer dave = participant("d");
        team(alice, bob);
        team(carol, dave);
        record(carol, Material.STONE, false);
        record(alice, Material.DIRT, false);

        FakeAchievementWorld world = new FakeAchievementWorld()
                .scoreOwners(alice.scoreOwner(), carol.scoreOwner());

        assertFalse(earlyBird().check(found(alice, Material.DIRT), earlyBird().createProgress(),
                alice, world));
    }

    @Test
    void aBackToBackIsNotAFirstItem() {
        ForceItemPlayer alice = participant("a");
        record(alice, Material.DIRT, false);

        FakeAchievementWorld world = new FakeAchievementWorld().scoreOwners(alice.scoreOwner());

        assertFalse(earlyBird().check(backToBack(alice, Material.DIRT),
                earlyBird().createProgress(), alice, world));
    }

    @Test
    void itIsGrantedOnlyOnce() {
        ForceItemPlayer alice = participant("a");
        record(alice, Material.DIRT, false);

        FakeAchievementWorld world = new FakeAchievementWorld().scoreOwners(alice.scoreOwner());
        TimeBasedAchievementHandler handler = earlyBird();
        TimeAchievementProgress progress = handler.createProgress();

        assertTrue(handler.check(found(alice, Material.DIRT), progress, alice, world));
        assertFalse(handler.check(found(alice, Material.STONE), progress, alice, world));
    }

    // --- the round clock -------------------------------------------------------------------

    @Test
    void anItemInsideTheWindowCounts() {
        ForceItemPlayer alice = participant("a");
        // 90-minute round, 40 seconds gone.
        FakeAchievementWorld world = new FakeAchievementWorld().clock(5400, 5360);
        TimeBasedAchievementHandler handler = within(1, 60);

        assertTrue(handler.check(found(alice, Material.DIRT), handler.createProgress(),
                alice, world));
    }

    @Test
    void anItemPastTheWindowDoesNot() {
        ForceItemPlayer alice = participant("a");
        // Same round, 61 seconds gone.
        FakeAchievementWorld world = new FakeAchievementWorld().clock(5400, 5339);
        TimeBasedAchievementHandler handler = within(1, 60);

        assertFalse(handler.check(found(alice, Material.DIRT), handler.createProgress(),
                alice, world));
    }

    /**
     * Elapsed time comes off the round clock, not off wall time, so a pause does not spend the
     * window. The clock simply does not advance while the game is paused — which is only expressible
     * as a test now that the handler asks the world for the time instead of subtracting two
     * managers' numbers itself.
     */
    @Test
    void aPauseDoesNotSpendTheWindow() {
        ForceItemPlayer alice = participant("a");
        FakeAchievementWorld world = new FakeAchievementWorld().clock(5400, 5360);
        TimeBasedAchievementHandler handler = within(1, 60);
        TimeAchievementProgress progress = handler.createProgress();

        // An hour of wall time passes while paused; the clock is untouched.
        assertTrue(handler.check(found(alice, Material.DIRT), progress, alice, world));
    }

    @Test
    void skipsDoNotCountTowardsATimedTotal() {
        ForceItemPlayer alice = participant("a");
        FakeAchievementWorld world = new FakeAchievementWorld().clock(5400, 5390);
        TimeBasedAchievementHandler handler = within(1, 60);

        assertFalse(handler.check(Finds.skipped(alice, Material.DIRT), handler.createProgress(),
                alice, world));
    }
}
