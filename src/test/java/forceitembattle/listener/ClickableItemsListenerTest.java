package forceitembattle.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.gui.GuiContext;
import forceitembattle.manager.BackpackManager;
import forceitembattle.manager.ItemDifficultiesManager;
import forceitembattle.manager.LocatorManager;
import forceitembattle.manager.PlayerOutfitter;
import forceitembattle.manager.TimerManager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameItems;
import forceitembattle.model.GameState;
import forceitembattle.model.MenuItem;
import forceitembattle.model.Roster;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.settings.GameSettings;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Which right-clicks are menu buttons and which are just items. {@link Lookalikes} pins that an item
 * merely <em>looking</em> like a button is not one; {@link RealButtons} takes every stack it clicks
 * from a player who has been through {@link PlayerOutfitter} rather than building one by hand.
 */
class ClickableItemsListenerTest extends ListenerTestBase {

    private Roster roster;
    private GuiContext gui;
    private ClickableItemsListener listener;

    @BeforeEach
    void setUpListener() {
        this.roster = new Roster();
        this.gui = mock(GuiContext.class, RETURNS_DEEP_STUBS);
        this.listener = new ClickableItemsListener(
                () -> null,
                this.gui,
                mock(ItemDifficultiesManager.class),
                this.roster,
                mock(BackpackManager.class),
                mock(FIBServiceClient.class),
                this.roundPhase,
                mock(LocatorManager.class),
                mock(GameSettings.class),
                mock(TimerManager.class));
    }

    private PlayerMock inTheOverworld(String name) {
        PlayerMock player = player(name);
        player.teleport(at(0, 64, 0));
        return player;
    }

    private ForceItemPlayer onRoster(PlayerMock player) {
        ForceItemPlayer entry = new ForceItemPlayer(player, Material.DIRT, 0, 0);
        this.roster.add(player.getUniqueId(), entry);
        return entry;
    }

    /**
     * Against a real block, deliberately: with none, {@code useInteractedBlock} starts at
     * {@code DENY} and {@code isCancelled()} is already true before any handler runs, so every
     * assertion about a click being consumed would pass for free.
     */
    private PlayerInteractEvent rightClick(PlayerMock player, ItemStack held) {
        return new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, held,
                this.world.getBlockAt(0, 63, 0), BlockFace.UP);
    }

    private PlayerInteractEvent clickIn(GameState state, PlayerMock player, ItemStack held) {
        phase(state);
        PlayerInteractEvent event = rightClick(player, held);
        this.listener.onMenuButton(event);
        return event;
    }

    private PlayerInteractEvent clickAfterGame(PlayerMock player, ItemStack held) {
        return clickIn(GameState.END_GAME, player, held);
    }

    private PlayerInteractEvent clickBeforeGame(PlayerMock player, ItemStack held) {
        return clickIn(GameState.PRE_GAME, player, held);
    }

    /** The stack the writer actually puts in that slot, rather than one built here to match. */
    private ItemStack resultScreenButton(PlayerMock player, int slot) {
        PlayerOutfitter.toResultScreen(player, null);
        ItemStack button = player.getInventory().getItem(slot);
        assertNotNull(button, "the writer put nothing in slot " + slot);
        return button;
    }

    private ItemStack lobbyButton(PlayerMock player, int slot) {
        PlayerOutfitter.toLobby(player, GameState.PRE_GAME);
        ItemStack button = player.getInventory().getItem(slot);
        assertNotNull(button, "the writer put nothing in slot " + slot);
        return button;
    }

    /** Ordinary items, obtained the ordinary way, that share a material with a menu button. */
    @Nested
    class Lookalikes {

        /** At {@code END_GAME} everyone is in creative with the whole block palette available. */
        @Test
        void aRealGrassBlockIsNotATeleportButton() {
            PlayerMock player = inTheOverworld("Understudy1");

            PlayerInteractEvent event = clickAfterGame(player, new ItemStack(Material.GRASS_BLOCK));

            assertFalse(event.isCancelled(),
                    "a block a player is holding to build with must still be placeable");
        }

        @Test
        void andItDoesNotTalkToThemEither() {
            PlayerMock player = inTheOverworld("Understudy1");

            clickAfterGame(player, new ItemStack(Material.GRASS_BLOCK));

            assertFalse(screenOf(player).contains("already in the"),
                    "an ordinary block should not answer back");
        }

        @Test
        void aRealSpyglassDoesNotPutThemIntoSpectator() {
            PlayerMock player = inTheOverworld("Understudy1");
            player.setGameMode(GameMode.CREATIVE);

            clickAfterGame(player, new ItemStack(Material.SPYGLASS));

            assertEquals(GameMode.CREATIVE, player.getGameMode(),
                    "looking through a spyglass is not a request to stop playing");
        }

        /** Silent and it costs the round: the only sign is a chat line they did not ask for. */
        @Test
        void aRealEnderPearlDoesNotOptThemOutOfTheRound() {
            PlayerMock player = inTheOverworld("Understudy1");
            ForceItemPlayer entry = onRoster(player);

            clickBeforeGame(player, new ItemStack(Material.ENDER_PEARL));

            assertFalse(entry.isSpectator(),
                    "holding a pearl is not opting out of the round");
        }

        @Test
        void aRealEnderEyeDoesNotOptThemBackIn() {
            PlayerMock player = inTheOverworld("Understudy1");
            ForceItemPlayer entry = onRoster(player);
            entry.setSpectator(true);

            clickBeforeGame(player, new ItemStack(Material.ENDER_EYE));

            assertTrue(entry.isSpectator(), "holding an eye is not opting back in");
        }
    }

    /** Every stack here comes from {@link PlayerOutfitter}, never built by hand. */
    @Nested
    class RealButtons {

        @Test
        void theResultScreensOverworldButtonIsStillAButton() {
            PlayerMock player = inTheOverworld("Understudy1");
            ItemStack button = resultScreenButton(player, 5);

            PlayerInteractEvent event = clickAfterGame(player, button);

            assertTrue(event.isCancelled(), "a button click is consumed, not passed to the world");
            assertTrue(screenOf(player).contains("already in the"),
                    "and it answers, because they are already in the overworld");
        }

        @Test
        void theResultScreensSpectateButtonStillToggles() {
            PlayerMock player = inTheOverworld("Understudy1");
            ItemStack button = resultScreenButton(player, 8);
            player.setGameMode(GameMode.CREATIVE);

            clickAfterGame(player, button);

            assertEquals(GameMode.SPECTATOR, player.getGameMode());
        }

        @Test
        void andTogglesBackAgain() {
            PlayerMock player = inTheOverworld("Understudy1");
            ItemStack button = resultScreenButton(player, 8);
            player.setGameMode(GameMode.SPECTATOR);

            clickAfterGame(player, button);

            assertEquals(GameMode.CREATIVE, player.getGameMode());
        }

        @Test
        void theLobbysSpectateButtonStillOptsThemOut() {
            PlayerMock player = inTheOverworld("Understudy1");
            ForceItemPlayer entry = onRoster(player);
            ItemStack button = lobbyButton(player, 8);

            clickBeforeGame(player, button);

            assertTrue(entry.isSpectator());
        }

        /** Opting out rewrites slot 8 into the opposite button, and clicking that one puts them back. */
        @Test
        void andTheReplacementButtonPutsThemBack() {
            PlayerMock player = inTheOverworld("Understudy1");
            ForceItemPlayer entry = onRoster(player);

            clickBeforeGame(player, lobbyButton(player, 8));
            ItemStack replacement = player.getInventory().getItem(8);
            assertNotNull(replacement, "opting out must leave a way back in");
            clickBeforeGame(player, replacement);

            assertFalse(entry.isSpectator(), "the round trip has to close");
        }
    }

    /** A button is only a button in a phase it is live in. */
    @Nested
    class PhaseGating {

        @Test
        void aResultScreenButtonDoesNothingBeforeARound() {
            PlayerMock player = inTheOverworld("Understudy1");
            onRoster(player);
            ItemStack button = resultScreenButton(player, 5);

            PlayerInteractEvent event = clickBeforeGame(player, button);

            assertFalse(event.isCancelled(), "the overworld button is not live before a round");
        }

        @Test
        void aLobbyButtonDoesNothingOnTheResultScreen() {
            PlayerMock player = inTheOverworld("Understudy1");
            ForceItemPlayer entry = onRoster(player);
            ItemStack button = lobbyButton(player, 8);

            clickAfterGame(player, button);

            assertFalse(entry.isSpectator(), "the lobby's spectate button is not live at END_GAME");
        }

        /** Neither handler runs while the round itself is being played. */
        @Test
        void noMenuButtonIsLiveMidRound() {
            PlayerMock player = inTheOverworld("Understudy1");
            ItemStack button = resultScreenButton(player, 5);

            PlayerInteractEvent event = clickIn(GameState.MID_GAME, player, button);

            assertFalse(event.isCancelled());
        }
    }

    /** Left-clicks are not button presses; only a right-click is. */
    @Nested
    class ClickShape {

        @Test
        void aLeftClickIsNotAButtonPress() {
            PlayerMock player = inTheOverworld("Understudy1");
            ItemStack button = resultScreenButton(player, 8);
            player.setGameMode(GameMode.CREATIVE);
            phase(GameState.END_GAME);

            PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK,
                    button, world.getBlockAt(0, 63, 0), BlockFace.UP);
            listener.onMenuButton(event);

            assertEquals(GameMode.CREATIVE, player.getGameMode());
        }

        @Test
        void anEmptyHandIsNotAButtonPress() {
            PlayerMock player = inTheOverworld("Understudy1");
            phase(GameState.END_GAME);

            PlayerInteractEvent event = rightClick(player, null);
            listener.onMenuButton(event);

            assertFalse(event.isCancelled());
        }
    }

    /**
     * The three arms that open a GUI. Each hands the open to {@code Scheduler.runSync} rather than
     * doing it inline, so the assertion in {@link #nothingOpensUntilTheSchedulerRuns} is really the
     * one that describes this handler: on return from the event, nothing has happened yet.
     */
    @Nested
    class MenuArms {

        /** The stack the writer puts in the slot this button owns, found by its marker. */
        private ItemStack buttonFor(PlayerMock player, MenuItem wanted) {
            if (wanted.menu() == MenuItem.Menu.RESULT) {
                PlayerOutfitter.toResultScreen(player, null);
            } else {
                PlayerOutfitter.toLobby(player, GameState.PRE_GAME);
            }
            for (ItemStack stack : player.getInventory().getContents()) {
                if (PlayerOutfitter.buttonOf(stack) == wanted) {
                    return stack;
                }
            }
            throw new AssertionError("the " + wanted.menu() + " bar carries no " + wanted + " button");
        }

        /**
         * Rows of whatever the player has open, or 0 for nothing. MockBukkit hands back a
         * {@code null} top inventory rather than a crafting view when no menu is open, so the
         * null is the "nothing opened" case and not a fault.
         */
        private int openInventorySize(PlayerMock player) {
            Inventory top = player.getOpenInventory().getTopInventory();
            return top == null ? 0 : top.getSize();
        }

        @Test
        void theTeleporterArmOpensTheTeleporterMenu() {
            PlayerMock player = inTheOverworld("Understudy1");
            ItemStack button = buttonFor(player, MenuItem.TELEPORTER);

            PlayerInteractEvent event = clickAfterGame(player, button);
            tick(1L);

            assertTrue(event.isCancelled(), "a button click is consumed, not passed to the world");
            assertEquals(9 * 6, openInventorySize(player), "the teleporter menu is six rows");
        }

        @Test
        void theAchievementsArmOpensTheCategoryMenu() {
            PlayerMock player = inTheOverworld("Understudy1");
            when(gui.achievements().getAchievementStorage().getPlayerAchievements(any()))
                    .thenReturn(Set.of());
            ItemStack button = buttonFor(player, MenuItem.ACHIEVEMENTS);

            clickBeforeGame(player, button);
            tick(1L);

            assertEquals(9 * 3, openInventorySize(player), "the category menu is three rows");
        }

        /**
         * The collection book loads asynchronously and fills itself in later; what this pins is that
         * the click reaches the menu and opens it, not what lands in it.
         */
        @Test
        void theCollectionArmOpensTheBook() {
            PlayerMock player = inTheOverworld("Understudy1");
            ItemStack button = buttonFor(player, MenuItem.COLLECTION);

            clickBeforeGame(player, button);
            tick(1L);

            assertEquals(9 * 6, openInventorySize(player), "the collection book is six rows");
        }

        /**
         * The handler schedules rather than opens. Worth its own test because every assertion above
         * would pass just as well if it opened inline, and the indirection is deliberate.
         */
        @Test
        void nothingOpensUntilTheSchedulerRuns() {
            PlayerMock player = inTheOverworld("Understudy1");
            ItemStack button = buttonFor(player, MenuItem.TELEPORTER);

            clickAfterGame(player, button);

            assertEquals(0, openInventorySize(player),
                    "the open is scheduled; nothing should have happened on return from the event");
        }
    }

    /**
     * Clicking a joker: the three arms of {@link JokerSpend}, wired.
     *
     * <p>What each outcome <em>is</em> belongs to {@code JokerSpendTest}, which is headless. What is
     * pinned here is that the listener acts on it — writes the stack, hands over the item, strips a
     * dead button — because that is the half a module test cannot see.
     */
    @Nested
    class SpendingAJoker {

        private ForceItemPlayer holdingJokers(PlayerMock player, int jokers) {
            ForceItemPlayer entry = new ForceItemPlayer(player, Material.DIAMOND, jokers, 0);
            roster.add(player.getUniqueId(), entry);
            PlayerOutfitter.setJokerStack(player, jokers);
            return entry;
        }

        private void clickTheJoker(PlayerMock player) {
            phase(GameState.MID_GAME);
            listener.onClick(rightClick(player, GameItems.jokers(1)));
        }

        @Test
        void theStackFollowsTheSpend() {
            PlayerMock player = inTheOverworld("Understudy1");
            ForceItemPlayer entry = holdingJokers(player, 3);

            clickTheJoker(player);

            assertEquals(2, entry.activeJokers());
            assertEquals(2, PlayerOutfitter.jokerStackIn(player).orElse(0),
                    "the button and the pool must agree");
        }

        @Test
        void theHuntedItemIsHandedOver() {
            PlayerMock player = inTheOverworld("Understudy1");
            holdingJokers(player, 3);

            clickTheJoker(player);

            assertTrue(player.getInventory().contains(Material.DIAMOND),
                    "a skip hands you the item you were hunting");
        }

        @Test
        void spendingTheLastOneRemovesTheButton() {
            PlayerMock player = inTheOverworld("Understudy1");
            holdingJokers(player, 1);

            clickTheJoker(player);

            assertTrue(PlayerOutfitter.jokerStackIn(player).isEmpty(),
                    "no jokers left means no button, not a stack of zero");
        }

        @Test
        void anEmptyPoolIsRefusedAndSaysSo() {
            PlayerMock player = inTheOverworld("Understudy1");
            holdingJokers(player, 0);
            // A leftover button with an empty pool behind it — what Exhausted exists to repair.
            PlayerOutfitter.setJokerStack(player, 2);

            clickTheJoker(player);

            assertTrue(screenOf(player).contains("No more skips left"));
        }

        @Test
        void andTheDeadButtonIsStripped() {
            PlayerMock player = inTheOverworld("Understudy1");
            holdingJokers(player, 0);
            PlayerOutfitter.setJokerStack(player, 2);

            clickTheJoker(player);

            assertTrue(PlayerOutfitter.jokerStackIn(player).isEmpty(),
                    "a button with nothing behind it is removed rather than left to be clicked again");
        }

        @Test
        void aRefusalHandsOverNothing() {
            PlayerMock player = inTheOverworld("Understudy1");
            holdingJokers(player, 0);
            PlayerOutfitter.setJokerStack(player, 2);

            clickTheJoker(player);

            assertFalse(player.getInventory().contains(Material.DIAMOND));
        }
    }

    /** Everything the player was told since this was last called. */
    private static String screenOf(PlayerMock player) {
        StringBuilder said = new StringBuilder();
        String line;
        while ((line = player.nextMessage()) != null) {
            said.append(line).append('\n');
        }
        return said.toString();
    }
}
