package forceitembattle.commands;

import static forceitembattle.commands.CommandTestSupport.assertSaid;
import static forceitembattle.commands.CommandTestSupport.screenOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import forceitembattle.commands.admin.CommandForceItem;
import forceitembattle.manager.ForceItemAssignment;
import forceitembattle.manager.ItemDifficultiesManager;
import forceitembattle.manager.ScoreboardManager;
import forceitembattle.manager.TimerManager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Roster;
import forceitembattle.settings.GameSettings;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@code /forceitem}: hand yourself a chosen item, and optionally queue the ones after it.
 *
 * <p>A dev command, but the one every other test of the running game leans on to set up a
 * situation, so what it does with its arguments is worth pinning precisely. Three things:
 *
 * <ul>
 *   <li><b>Validate the whole row before applying any of it.</b> The command says so in a comment,
 *       and it matters because the alternative leaves the round half-rewritten: a current item
 *       assigned, a queue partly filled, and a refusal on screen.</li>
 *   <li><b>The row splits three ways.</b> The first argument is the item now, the second is the one
 *       after it, and everything from the third on is queued. Getting that boundary wrong either
 *       drops an item or replays one, and neither is visible without counting.</li>
 *   <li><b>The queue is cleared first</b>, so a second {@code /forceitem} replaces the row rather
 *       than appending to a row that is still draining.</li>
 * </ul>
 *
 * <p>Everything happens through a real {@link ForceItemPlayer}, so the assignment is read back off
 * the roster entry rather than verified on a mock.
 */
class CommandForceItemTest {

    private ServerMock server;
    private RoundPhase roundPhase;
    private Roster roster;
    private ForceItemAssignment assignment;
    private CommandForceItem command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();

        this.roundPhase = new RoundPhase();
        this.roster = new Roster();
        // A real assignment module over a stubbed pool. The forced row is its private state now,
        // so these tests ask what the owner ends up hunting rather than reading a leaked deque.
        ItemDifficultiesManager items = mock(ItemDifficultiesManager.class);
        when(items.generateRandomMaterial()).thenReturn(Material.BEDROCK);
        this.assignment = new ForceItemAssignment(this.roster, items);

        this.roundPhase.moveTo(GameState.MID_GAME);

        this.command = new CommandForceItem(this.assignment, mock(GameSettings.class), mock(TimerManager.class), this.roster, mock(ScoreboardManager.class));
        ((CustomCommand) this.command).setContext(
                new CommandContext(this.roundPhase, null, this.roster));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- fixtures ---------------------------------------------------------------------------

    /** An op on the roster, hunting {@link Material#DIRT}. */
    private ForceItemPlayer joinPlaying(String name) {
        PlayerMock player = this.server.addPlayer(name);
        player.setOp(true);
        ForceItemPlayer entry = new ForceItemPlayer(player, Material.DIRT, 0, 0);
        this.roster.add(player.getUniqueId(), entry);
        return entry;
    }

    private static PlayerMock mockOf(ForceItemPlayer entry) {
        return (PlayerMock) entry.player();
    }

    private void run(PlayerMock player, String... args) {
        this.command.onCommand(player, null, "forceitem", args);
    }

    /**
     * The next two items drawn for this owner, by advancing twice. The forced row is private to
     * {@link ForceItemAssignment}, so what it holds is observed the way the game observes it — by
     * finding things — rather than by reading the deque.
     *
     * <p>Reads {@code activeNextMaterial()}, not {@code activeMaterial()}: {@code advance} shifts next
     * into current and puts the freshly drawn item in next, so the drawn one is always the queued
     * slot. And the <i>active</i> family, because on a team the plain accessors read the player's own
     * untouched fields rather than the Score Owner's.
     */
    private List<Material> nextTwoItems(ForceItemPlayer entry) {
        List<Material> drawn = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            this.assignment.advanceFor(entry, false);
            drawn.add(entry.activeNextMaterial());
        }
        return drawn;
    }

    /** Asserts nothing is queued behind this owner, so the next draw comes from the pool. */
    private void assertQueueDrainsTo(ForceItemPlayer entry, Material expected, String because) {
        this.assignment.advanceFor(entry, false);
        assertEquals(expected, entry.activeNextMaterial(), because);
    }

    // --- the tests --------------------------------------------------------------------------

    @Nested
    class OneItem {

        @Test
        void itBecomesTheItemBeingHunted() {
            ForceItemPlayer entry = joinPlaying("Admin");

            run(mockOf(entry), "diamond");

            assertEquals(Material.DIAMOND, entry.activeMaterial());
        }

        /** With no second argument the round generates the next one as it normally would. */
        @Test
        void theItemAfterItIsGeneratedNormally() {
            ForceItemPlayer entry = joinPlaying("Admin");

            run(mockOf(entry), "diamond");

            assertEquals(Material.BEDROCK, entry.activeNextMaterial());
        }

        @Test
        void nothingIsQueued() {
            ForceItemPlayer entry = joinPlaying("Admin");

            run(mockOf(entry), "diamond");

            assertQueueDrainsTo(entry, Material.BEDROCK, "a single item queues nothing behind it");
        }

        @Test
        void theConfirmationNamesTheItem() {
            ForceItemPlayer entry = joinPlaying("Admin");

            run(mockOf(entry), "diamond_sword");

            assertSaid(mockOf(entry), "Diamond Sword");
        }

        @Test
        void theItemNameIsCaseInsensitive() {
            ForceItemPlayer entry = joinPlaying("Admin");

            run(mockOf(entry), "DIAMOND");

            assertEquals(Material.DIAMOND, entry.activeMaterial());
        }
    }

    @Nested
    class AWholeRow {

        /** Two arguments fill both slots and leave the queue empty. */
        @Test
        void theSecondArgumentIsTheItemAfterIt() {
            ForceItemPlayer entry = joinPlaying("Admin");

            run(mockOf(entry), "diamond", "emerald");

            assertEquals(Material.DIAMOND, entry.activeMaterial());
            assertEquals(Material.EMERALD, entry.activeNextMaterial());
            assertQueueDrainsTo(entry, Material.BEDROCK,
                    "two items fill both slots, so nothing is left over to queue");
        }

        /** From the third argument on, the row is queued in order. */
        @Test
        void everythingFromTheThirdIsQueuedInOrder() {
            ForceItemPlayer entry = joinPlaying("Admin");

            run(mockOf(entry), "diamond", "emerald", "gold_ingot", "iron_ingot");

            assertEquals(Material.DIAMOND, entry.activeMaterial());
            assertEquals(Material.EMERALD, entry.activeNextMaterial());
            assertEquals(List.of(Material.GOLD_INGOT, Material.IRON_INGOT), nextTwoItems(entry));
        }

        /** A second call replaces the row rather than appending to what is still draining. */
        @Test
        void aSecondCallClearsTheQueueFirst() {
            ForceItemPlayer entry = joinPlaying("Admin");

            run(mockOf(entry), "diamond", "emerald", "gold_ingot");
            run(mockOf(entry), "stone", "dirt", "sand");

            assertEquals(List.of(Material.SAND, Material.BEDROCK), nextTwoItems(entry),
                    "the first row's leftovers are gone, not draining behind the second");
        }

        @Test
        void theConfirmationListsTheWholeUpcomingRow() {
            ForceItemPlayer entry = joinPlaying("Admin");

            run(mockOf(entry), "diamond", "emerald", "gold_ingot");

            String said = screenOf(mockOf(entry));
            assertTrue(said.contains("Diamond"), said);
            assertTrue(said.contains("Emerald"), said);
            assertTrue(said.contains("Gold Ingot"), said);
        }
    }

    @Nested
    class Refusals {

        @Test
        void noArgumentsShowsTheForm() {
            ForceItemPlayer entry = joinPlaying("Admin");

            run(mockOf(entry));

            assertSaid(mockOf(entry), "Usage: /forceitem");
            assertEquals(Material.DIRT, entry.activeMaterial(), "nothing was assigned");
        }

        @Test
        void anUnknownItemNameIsRefused() {
            ForceItemPlayer entry = joinPlaying("Admin");

            run(mockOf(entry), "not_an_item");

            assertSaid(mockOf(entry), "Unknown item");
            assertEquals(Material.DIRT, entry.activeMaterial());
        }

        /** A block that cannot be held is not an item, so it cannot be a force item either. */
        @Test
        void somethingThatIsNotAnObtainableItemIsRefused() {
            ForceItemPlayer entry = joinPlaying("Admin");

            run(mockOf(entry), "water");

            assertSaid(mockOf(entry), "Unknown item");
            assertEquals(Material.DIRT, entry.activeMaterial());
        }

        /**
         * The reason the whole row is parsed up front: one bad argument must not leave a partly
         * applied row behind.
         */
        @Test
        void oneBadArgumentAppliesNoneOfTheRow() {
            ForceItemPlayer entry = joinPlaying("Admin");
            // An earlier row, so there is something to disturb: current DIAMOND, next EMERALD,
            // STONE queued behind them.
            run(mockOf(entry), "diamond", "emerald", "stone");
            screenOf(mockOf(entry));

            run(mockOf(entry), "sand", "dirt", "not_an_item", "gold_ingot");

            assertSaid(mockOf(entry), "Unknown item");
            assertEquals(Material.DIAMOND, entry.activeMaterial(), "the current item is untouched");
            assertEquals(Material.EMERALD, entry.activeNextMaterial(), "and so is the one after it");
            assertEquals(List.of(Material.STONE, Material.BEDROCK), nextTwoItems(entry),
                    "the queue is untouched, not cleared and half-filled");
        }

        @Test
        void aNonOpIsRefused() {
            ForceItemPlayer entry = joinPlaying("Admin");
            mockOf(entry).setOp(false);

            run(mockOf(entry), "diamond");

            assertSaid(mockOf(entry), "permission");
            assertEquals(Material.DIRT, entry.activeMaterial());
        }

        @Test
        void thereIsNothingToForceOutsideARound() {
            ForceItemPlayer entry = joinPlaying("Admin");
            roundPhase.moveTo(GameState.PRE_GAME);

            run(mockOf(entry), "diamond");

            assertSaid(mockOf(entry), "not running");
            assertEquals(Material.DIRT, entry.activeMaterial());
        }

        /** The reworded PARTICIPANT refusal, which is what a spectating op gets. */
        @Test
        void aSpectatorIsToldTheyAreNotAnActivePlayer() {
            ForceItemPlayer entry = joinPlaying("Admin");
            entry.setSpectator(true);

            run(mockOf(entry), "diamond");

            assertSaid(mockOf(entry), "active player");
            assertEquals(Material.DIRT, entry.activeMaterial());
        }

        @Test
        void someoneWhoJoinedMidRoundIsRefusedToo() {
            PlayerMock latecomer = server.addPlayer("Latecomer");
            latecomer.setOp(true);

            run(latecomer, "diamond");

            assertSaid(latecomer, "active player");
        }
    }

    @Nested
    class TabCompletion {

        @Test
        void whatIsTypedNarrowsToMatchingItems() {
            ForceItemPlayer entry = joinPlaying("Admin");

            List<String> offered =
                    command.onTabComplete(mockOf(entry), "forceitem", new String[]{"diamond_sw"});

            assertEquals(List.of("diamond_sword"), offered);
        }

        /** Completion follows the last argument, so a row can be built one item at a time. */
        @Test
        void itCompletesTheLastArgumentNotTheFirst() {
            ForceItemPlayer entry = joinPlaying("Admin");

            List<String> offered = command.onTabComplete(mockOf(entry), "forceitem",
                    new String[]{"diamond", "emerald_bl"});

            assertEquals(List.of("emerald_block"), offered);
        }

        /**
         * The completion offers only what the command would accept. {@code water} is a block, not
         * an obtainable item, so it is not offered even though the prefix matches it exactly —
         * {@code water_bucket}, which is an item, still is.
         */
        @Test
        void nothingUnobtainableIsOffered() {
            ForceItemPlayer entry = joinPlaying("Admin");

            List<String> offered =
                    command.onTabComplete(mockOf(entry), "forceitem", new String[]{"water"});

            assertFalse(offered.contains("water"), offered.toString());
            assertTrue(offered.contains("water_bucket"), offered.toString());
        }
    }
}
