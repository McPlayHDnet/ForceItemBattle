package forceitembattle.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Roster;
import forceitembattle.model.Team;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Who is hunting what.
 *
 * <p><b>Headless.</b> Not one line here needs a server: {@link ForceItemAssignment} touches only
 * {@code Material}, the {@link Roster} and {@code ScoreOwner}, which is exactly why it was worth
 * lifting out of {@code Gamemanager} — there it sat beside {@code Bukkit.getOnlinePlayers()} and
 * had no test at all, so every rule below was defended by a comment and nothing else.
 *
 * <p>The pool is a stub handing out a known sequence, so "which item was drawn" and "how many draws
 * were taken" are both assertable — the second matters more than it looks, because drawing once per
 * team <em>member</em> rather than once per owner silently consumes two items to show one.
 */
class ForceItemAssignmentTest {

    /** Hands out a fixed sequence, so a draw is identifiable and the count of draws is observable. */
    private static final class PoolStub {
        private final Deque<Material> random = new ArrayDeque<>(List.of(
                Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT, Material.IRON_INGOT,
                Material.COAL, Material.REDSTONE, Material.LAPIS_LAZULI, Material.QUARTZ));
        private final Deque<Material> seeded = new ArrayDeque<>(List.of(
                Material.STONE, Material.DIRT, Material.SAND, Material.GRAVEL,
                Material.CLAY, Material.OBSIDIAN));
        private int randomDraws;
        private int seededDraws;
    }

    private PoolStub pool;
    private Roster roster;
    private ForceItemAssignment assignment;

    @BeforeEach
    void setUp() {
        this.pool = new PoolStub();
        this.roster = new Roster();

        ItemDifficultiesManager items = mock(ItemDifficultiesManager.class);
        when(items.generateRandomMaterial()).thenAnswer(invocation -> {
            this.pool.randomDraws++;
            return this.pool.random.poll();
        });
        when(items.generateSeededRandomMaterial()).thenAnswer(invocation -> {
            this.pool.seededDraws++;
            return this.pool.seeded.poll();
        });

        this.assignment = new ForceItemAssignment(this.roster, items);
    }

    // --- fixtures -------------------------------------------------------------------------------

    /** A roster entry over a mocked player — nothing here touches the player beyond its uuid. */
    private ForceItemPlayer joinPlaying(String name) {
        Player player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn(name);

        ForceItemPlayer entry = new ForceItemPlayer(player, Material.BEDROCK, 0, 0);
        this.roster.add(uuid, entry);
        return entry;
    }

    private ForceItemPlayer joinSpectating(String name) {
        ForceItemPlayer entry = joinPlaying(name);
        entry.setSpectator(true);
        return entry;
    }

    /** Puts two players on one team, so they share a Score Owner. */
    private Team pairUp(ForceItemPlayer first, ForceItemPlayer second) {
        Team team = new Team(1, Material.BEDROCK, 0, 0, first, second);
        first.setCurrentTeam(team);
        second.setCurrentTeam(team);
        return team;
    }

    // --- the tests ------------------------------------------------------------------------------

    @Nested
    class BeginningARound {

        @Test
        void everyOwnerIsHandedAPair() {
            ForceItemPlayer one = joinPlaying("Understudy1");
            ForceItemPlayer two = joinPlaying("Understudy2");

            assignment.beginRound(false);

            assertNotEquals(Material.BEDROCK, one.activeMaterial());
            assertNotEquals(Material.BEDROCK, two.activeMaterial());
        }

        @Test
        void soloOwnersDrawSeparately() {
            ForceItemPlayer one = joinPlaying("Understudy1");
            ForceItemPlayer two = joinPlaying("Understudy2");

            assignment.beginRound(false);

            assertNotEquals(one.activeMaterial(), two.activeMaterial(),
                    "outside run mode each owner hunts their own item");
            assertEquals(4, pool.randomDraws, "two owners, a current and a next each");
        }

        /**
         * The invariant {@code ScoreOwner.startRound}'s javadoc warns about, from the other side: a
         * two-player team is <b>one</b> owner, so it takes one pair, not two.
         */
        @Test
        @DisplayName("a team draws one pair between them, not one each")
        void aTeamDrawsOncePerOwnerNotPerMember() {
            ForceItemPlayer one = joinPlaying("Understudy1");
            ForceItemPlayer two = joinPlaying("Understudy2");
            pairUp(one, two);

            assignment.beginRound(false);

            assertEquals(2, pool.randomDraws, "one owner, so one current and one next");
            assertEquals(one.activeMaterial(), two.activeMaterial(),
                    "and both members see the same item");
        }

        @Test
        void spectatorsAreNotHandedAnything() {
            joinSpectating("Understudy1");

            assignment.beginRound(false);

            assertEquals(0, pool.randomDraws);
        }

        @Test
        @DisplayName("run mode gives the whole server one shared pair")
        void runModeSharesOnePair() {
            ForceItemPlayer one = joinPlaying("Understudy1");
            ForceItemPlayer two = joinPlaying("Understudy2");

            assignment.beginRound(true);

            assertEquals(one.activeMaterial(), two.activeMaterial());
            assertEquals(one.activeNextMaterial(), two.activeNextMaterial());
            assertEquals(2, pool.seededDraws, "one pair for the server, drawn from the seeded pool");
            assertEquals(0, pool.randomDraws);
        }
    }

    @Nested
    class Advancing {

        @Test
        void onlyTheFindersOwnerMoves() {
            ForceItemPlayer finder = joinPlaying("Understudy1");
            ForceItemPlayer other = joinPlaying("Understudy2");
            assignment.beginRound(false);
            Material othersItem = other.activeMaterial();

            assignment.advanceFor(finder, false);

            assertEquals(othersItem, other.activeMaterial(), "someone else's find is not my problem");
        }

        @Test
        @DisplayName("in run mode one find advances everybody, on a single draw")
        void runModeAdvancesEveryone() {
            ForceItemPlayer finder = joinPlaying("Understudy1");
            ForceItemPlayer other = joinPlaying("Understudy2");
            assignment.beginRound(true);
            int drawsAfterStart = pool.seededDraws;

            assignment.advanceFor(finder, true);

            assertEquals(finder.activeMaterial(), other.activeMaterial());
            assertEquals(drawsAfterStart + 1, pool.seededDraws, "one draw for the whole server");
        }

        /**
         * {@code ScoreOwner.advance}'s javadoc: running it twice on one team discards the queued item
         * and leaves current and next both holding next. Walking owners rather than members is what
         * prevents it.
         */
        @Test
        void aTeamAdvancesOnceWhenOneMemberFinds() {
            ForceItemPlayer one = joinPlaying("Understudy1");
            ForceItemPlayer two = joinPlaying("Understudy2");
            pairUp(one, two);
            assignment.beginRound(false);
            Material queued = one.activeNextMaterial();

            assignment.advanceFor(one, false);

            assertEquals(queued, one.activeMaterial(),
                    "the queued item becomes current; advancing twice would have skipped it");
            assertNotEquals(one.activeMaterial(), one.activeNextMaterial());
        }
    }

    @Nested
    class SkippingEveryone {

        @Test
        void everyOwnerGetsTheSameNewPair() {
            ForceItemPlayer one = joinPlaying("Understudy1");
            ForceItemPlayer two = joinPlaying("Understudy2");
            assignment.beginRound(false);

            assignment.skipAll(one, false);

            assertEquals(one.activeMaterial(), two.activeMaterial(),
                    "a skip resets the whole server to one item");
        }

        @Test
        void someoneNotInTheRoundSkipsNothing() {
            ForceItemPlayer inRound = joinPlaying("Understudy1");
            assignment.beginRound(false);
            Material before = inRound.activeMaterial();

            Player stranger = mock(Player.class);
            when(stranger.getUniqueId()).thenReturn(UUID.randomUUID());
            assignment.skipAll(new ForceItemPlayer(stranger, Material.BEDROCK, 0, 0), false);

            assertEquals(before, inRound.activeMaterial());
        }

        /** A skip is not a find, so it must not restart the clock — {@code assignMaterials} is why. */
        @Test
        void aSkipLeavesTheFindClockAlone() {
            ForceItemPlayer one = joinPlaying("Understudy1");
            assignment.beginRound(false);
            long assignedAt = one.scoreOwner().itemAssignedAt();

            assignment.skipAll(one, false);

            assertEquals(assignedAt, one.scoreOwner().itemAssignedAt());
        }

        @Test
        void andLeavesTheScoreAlone() {
            ForceItemPlayer one = joinPlaying("Understudy1");
            assignment.beginRound(false);
            int score = one.scoreOwner().score();

            assignment.skipAll(one, false);

            assertEquals(score, one.scoreOwner().score());
        }
    }

    @Nested
    class AForcedRow {

        @Test
        void theFirstTwoAreCurrentAndNext() {
            ForceItemPlayer one = joinPlaying("Understudy1");

            assignment.force(one.scoreOwner(), List.of(Material.CAKE, Material.TORCH), false);

            assertEquals(Material.CAKE, one.activeMaterial());
            assertEquals(Material.TORCH, one.activeNextMaterial());
        }

        @Test
        void aRowOfOneTakesADrawnItemAsItsSecond() {
            ForceItemPlayer one = joinPlaying("Understudy1");

            assignment.force(one.scoreOwner(), List.of(Material.CAKE), false);

            assertEquals(Material.CAKE, one.activeMaterial());
            assertEquals(Material.DIAMOND, one.activeNextMaterial(), "the first item off the pool stub");
        }

        @Test
        void therestIsDrainedInOrder() {
            ForceItemPlayer one = joinPlaying("Understudy1");
            assignment.force(one.scoreOwner(),
                    List.of(Material.CAKE, Material.TORCH, Material.ANVIL, Material.BEACON), false);

            List<Material> drawn = new ArrayList<>();
            assignment.advanceFor(one, false);
            drawn.add(one.activeNextMaterial());
            assignment.advanceFor(one, false);
            drawn.add(one.activeNextMaterial());

            assertEquals(List.of(Material.ANVIL, Material.BEACON), drawn);
        }

        @Test
        void anEmptyRowIsRefused() {
            ForceItemPlayer one = joinPlaying("Understudy1");

            assertThrows(IllegalArgumentException.class,
                    () -> assignment.force(one.scoreOwner(), List.of(), false));
        }

        /**
         * <b>The defect this module was lifted out to fix.</b> The forced row used to live in one
         * server-wide deque that {@code /forceitem} cleared and filled directly. Because the draw is
         * per owner outside run mode, a row forced for one player was drained by whichever player
         * found something next — so an admin queueing items for themselves handed them to someone
         * else, and the command's promise that "the row is walked through in order" was false for the
         * caller.
         */
        @Test
        @DisplayName("one player's forced row never reaches another player")
        void aForcedRowStaysWithItsOwner() {
            ForceItemPlayer admin = joinPlaying("Admin");
            ForceItemPlayer bystander = joinPlaying("Understudy1");
            assignment.beginRound(false);

            assignment.force(admin.scoreOwner(),
                    List.of(Material.CAKE, Material.TORCH, Material.ANVIL), false);

            // The bystander finds something first — under the old shared queue this drained ANVIL.
            assignment.advanceFor(bystander, false);
            assertNotEquals(Material.ANVIL, bystander.activeNextMaterial(),
                    "the bystander must not be handed the admin's queued item");

            assignment.advanceFor(admin, false);
            assertEquals(Material.ANVIL, admin.activeNextMaterial(),
                    "and the admin still gets it, in order");
        }

        @Test
        void aSecondRowReplacesTheFirstRatherThanQueueingBehindIt() {
            ForceItemPlayer one = joinPlaying("Understudy1");

            assignment.force(one.scoreOwner(),
                    List.of(Material.CAKE, Material.TORCH, Material.ANVIL), false);
            assignment.force(one.scoreOwner(),
                    List.of(Material.SPONGE, Material.LADDER, Material.BEACON), false);

            assignment.advanceFor(one, false);

            assertEquals(Material.BEACON, one.activeNextMaterial(),
                    "the first row's leftovers are gone, not draining behind the second");
        }

        /** In run mode everyone races the same item, so there the queue really is shared. */
        @Test
        void runModeSharesTheForcedRowToo() {
            ForceItemPlayer admin = joinPlaying("Admin");
            ForceItemPlayer other = joinPlaying("Understudy1");
            assignment.beginRound(true);

            assignment.force(admin.scoreOwner(),
                    List.of(Material.CAKE, Material.TORCH, Material.ANVIL), true);
            assignment.advanceFor(other, true);

            assertEquals(Material.ANVIL, other.activeNextMaterial());
        }

        /** A round wipes forced rows; otherwise last round's leftovers open the new one. */
        @Test
        void beginningARoundClearsForcedRows() {
            ForceItemPlayer one = joinPlaying("Understudy1");
            assignment.force(one.scoreOwner(),
                    List.of(Material.CAKE, Material.TORCH, Material.ANVIL), false);

            assignment.beginRound(false);

            assertTrue(one.activeMaterial() != Material.ANVIL && one.activeNextMaterial() != Material.ANVIL,
                    "a row queued before the round must not open the next one");
        }
    }
}
