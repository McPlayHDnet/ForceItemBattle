package forceitembattle.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import forceitembattle.ForceItemBattle;
import forceitembattle.manager.BackpackManager;
import forceitembattle.manager.LocatorManager;
import forceitembattle.manager.PlayerOutfitter;
import forceitembattle.manager.TimerManager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.model.Roster;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.settings.GameSettings;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@link ClickableItemsListener}: which right-clicks are menu buttons and which are just items.
 *
 * <p>The two menu handlers dispatch on raw {@link Material}, so anything that <em>looks</em> like a
 * button is one. At {@code END_GAME} a grass block a player is holding to build with teleports
 * them and a spyglass they picked up puts them in spectator mode; before a round, an ender pearl
 * quietly drops them out of it. Nothing distinguishes the item the plugin handed out from an
 * identical one the world produced.
 *
 * <p>{@link Lookalikes} is the failing half, and it stays failing until the buttons carry a marker
 * and the handler dispatches on that instead of on the material.
 *
 * <p>{@link RealButtons} is the regression net, and the way it is written is the point: every
 * stack it clicks is taken from a player who has been through {@link PlayerOutfitter}, never built
 * by hand. So when the writer starts stamping a marker and the reader starts reading one, these
 * tests keep passing without being touched — which is the only way they can testify that the
 * rewrite preserved behaviour.
 *
 * <p>Three arms are deliberately not exercised: Achievements, Collection and the Teleporter GUI all
 * run through {@code Scheduler.runSync}, which needs a registered plugin behind Bukkit's scheduler,
 * and the plugin is a mock here. The arms that are covered â€” the dimension buttons and both
 * spectate toggles â€” are the ones that write player state directly, which is also where a
 * misfiring lookalike does the visible damage.
 */
class ClickableItemsListenerTest extends ListenerTestBase {

    private Roster roster;
    private ClickableItemsListener listener;

    @BeforeEach
    void setUpListener() {
        this.roster = new Roster();
        this.listener = new ClickableItemsListener(
                mock(ForceItemBattle.class),
                this.roster,
                mock(BackpackManager.class),
                mock(FIBServiceClient.class),
                this.roundPhase,
                mock(LocatorManager.class),
                mock(GameSettings.class),
                mock(TimerManager.class));
    }

    // --- fixtures ---------------------------------------------------------------------------

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
     * A right-click with this stack in hand, as the handlers see it.
     *
     * <p>Against a real block, deliberately. {@code PlayerInteractEvent} decides its initial
     * cancelled state from whether a block was clicked â€” with none, {@code useInteractedBlock}
     * starts at {@code DENY} and {@code isCancelled()} is already true before any handler runs, so
     * every assertion about a click being consumed would pass for free. Clicking a block starts it
     * at {@code ALLOW}, which makes {@code setCancelled(true)} an actual signal.
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

    // --- the tests --------------------------------------------------------------------------

    /**
     * The bug. Each of these is an ordinary item, obtained the ordinary way, that the menu handler
     * mistakes for one of its own.
     */
    @Nested
    class Lookalikes {

        /**
         * The report's own example. At {@code END_GAME} everyone is in creative with the whole
         * block palette available, so holding a grass block is not an exotic situation â€” and the
         * click is cancelled, which means the block cannot be placed.
         */
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

        /** A spyglass is a real item players carry. Holding one should not eject them into spectator. */
        @Test
        void aRealSpyglassDoesNotPutThemIntoSpectator() {
            PlayerMock player = inTheOverworld("Understudy1");
            player.setGameMode(GameMode.CREATIVE);

            clickAfterGame(player, new ItemStack(Material.SPYGLASS));

            assertEquals(GameMode.CREATIVE, player.getGameMode(),
                    "looking through a spyglass is not a request to stop playing");
        }

        /**
         * The worst of the three, because it is silent and it costs the round. An ender pearl is
         * an ordinary lobby item; throwing one before the round starts drops the player out of it,
         * and the only sign is a chat line they did not ask for.
         */
        @Test
        void aRealEnderPearlDoesNotOptThemOutOfTheRound() {
            PlayerMock player = inTheOverworld("Understudy1");
            ForceItemPlayer entry = onRoster(player);

            clickBeforeGame(player, new ItemStack(Material.ENDER_PEARL));

            assertFalse(entry.isSpectator(),
                    "holding a pearl is not opting out of the round");
        }

        /** And the mirror: a real ender eye does not opt them back in. */
        @Test
        void aRealEnderEyeDoesNotOptThemBackIn() {
            PlayerMock player = inTheOverworld("Understudy1");
            ForceItemPlayer entry = onRoster(player);
            entry.setSpectator(true);

            clickBeforeGame(player, new ItemStack(Material.ENDER_EYE));

            assertTrue(entry.isSpectator(), "holding an eye is not opting back in");
        }
    }

    /**
     * The regression net. Every stack here comes from {@link PlayerOutfitter}, so these keep
     * passing across the rewrite without being edited â€” which is what makes them evidence.
     */
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

        /**
         * Opting out rewrites slot 8 into the opposite button, and clicking that one puts them
         * back. Today the click handler builds that replacement itself; after the rewrite it asks
         * the writer for it, and this is the test that says the round trip still works.
         */
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

    /**
     * Phase gating, pinned as it stands. The two handlers each return early on the wrong phase,
     * and the table that replaces them has to keep that true through its own phase column.
     */
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
