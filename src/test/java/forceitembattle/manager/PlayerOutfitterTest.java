package forceitembattle.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.model.GameItems;
import forceitembattle.model.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * {@link PlayerOutfitter}: what each of the four round states actually writes.
 *
 * <p>{@code PlayerLifecycleListenerTest} owns which state an arriving player is put into; this owns
 * what being in one means.
 *
 * <p>Every state is asserted from a deliberately dirty player: hurt, mounted, levelled, carrying loot
 * and wearing a coloured tab name. Starting from a fresh {@code PlayerMock} would let a state that
 * writes nothing at all pass every assertion.
 */
class PlayerOutfitterTest {

    private ServerMock server;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.world = this.server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** Someone in the state a round leaves you in, so a no-op cannot pass for a reset. */
    private PlayerMock dirty(String name) {
        PlayerMock player = this.server.addPlayer(name);
        player.teleport(new Location(this.world, 400, 72, -400));
        player.setHealth(6);
        player.setFoodLevel(3);
        player.setLevel(30);
        player.setExp(0.5f);
        player.getInventory().addItem(new ItemStack(Material.DIRT));
        player.playerListName(Component.text(name, NamedTextColor.RED));

        Entity mount = this.world.spawnEntity(player.getLocation(), EntityType.PIG);
        player.addPassenger(mount);
        return player;
    }

    private static Material inSlot(PlayerMock player, int slot) {
        ItemStack stack = player.getInventory().getItem(slot);
        return stack == null ? Material.AIR : stack.getType();
    }

    @Nested
    class ToPlayer {

        @Test
        void theyStartTheRoundWholeAndInSurvival() {
            PlayerMock player = dirty("Understudy1");

            PlayerOutfitter.toPlayer(player, 0);

            assertEquals(GameMode.SURVIVAL, player.getGameMode());
            assertEquals(20.0, player.getHealth());
            assertEquals(0, player.getLevel());
            assertTrue(player.getPassengers().isEmpty(), "nobody starts a round carrying a pig");
            assertFalse(player.getInventory().contains(Material.DIRT));
        }

        @Test
        void theyGetTheStartingKit() {
            PlayerMock player = dirty("Understudy1");

            PlayerOutfitter.toPlayer(player, 0);

            for (Material tool : RoundSetup.STARTING_KIT) {
                assertTrue(player.getInventory().contains(tool), "missing " + tool);
            }
        }

        /**
         * Slot 4 is written before the tools, because {@code addItem} fills the first free slot and
         * would otherwise take it.
         */
        @Test
        void theJokersLandInSlotFourAndTheToolsGoElsewhere() {
            PlayerMock player = dirty("Understudy1");

            PlayerOutfitter.toPlayer(player, 3);

            assertEquals(GameItems.jokerMaterial(), inSlot(player, 4));
            assertEquals(3, player.getInventory().getItem(4).getAmount());
        }

        /** Zero jokers means no stack, which is not the same as a stack of zero. */
        @Test
        void noJokersMeansAnEmptySlotNotAnEmptyStack() {
            PlayerMock player = dirty("Understudy1");

            PlayerOutfitter.toPlayer(player, 0);

            assertEquals(Material.AIR, inSlot(player, 4));
        }
    }

    @Nested
    class ToSpectator {

        @Test
        void theyWatchWithNothing() {
            PlayerMock player = dirty("Understudy1");

            PlayerOutfitter.toSpectator(player);

            assertEquals(GameMode.SPECTATOR, player.getGameMode());
            assertFalse(player.getInventory().contains(Material.DIRT));
        }

        @Test
        void theirLevelBarIsClearedToo() {
            PlayerMock player = dirty("Understudy1");

            PlayerOutfitter.toSpectator(player);

            assertEquals(0, player.getLevel());
            assertEquals(0f, player.getExp());
        }
    }

    @Nested
    class ToResultScreen {

        @Test
        void theyArriveWholeAndInCreative() {
            PlayerMock player = dirty("Understudy1");

            PlayerOutfitter.toResultScreen(player, null);

            assertEquals(GameMode.CREATIVE, player.getGameMode());
            assertEquals(20.0, player.getHealth());
            assertEquals(0, player.getLevel());
            assertTrue(player.getPassengers().isEmpty());
            assertFalse(player.getInventory().contains(Material.DIRT));
        }

        @Test
        void theirTabNameLosesTheRoundsColour() {
            PlayerMock player = dirty("Understudy1");

            PlayerOutfitter.toResultScreen(player, null);

            assertEquals(Component.text("Understudy1"), player.playerListName());
        }

        @Test
        void theyAreGatheredAtTheDestinationTheyWereGiven() {
            PlayerMock player = dirty("Understudy1");
            Location destination = new Location(world, 10, 64, 20);

            PlayerOutfitter.toResultScreen(player, destination);

            assertEquals(10, player.getLocation().getBlockX());
            assertEquals(20, player.getLocation().getBlockZ());
        }

        /**
         * {@code Dimension.OVERWORLD.world()} is nullable, so null has to mean "leave them" rather
         * than throw halfway through outfitting them.
         */
        @Test
        void noDestinationLeavesThemWhereTheyStand() {
            PlayerMock player = dirty("Understudy1");

            PlayerOutfitter.toResultScreen(player, null);

            assertEquals(400, player.getLocation().getBlockX());
        }

        @Test
        void theyGetTheResultScreenButtons() {
            PlayerMock player = dirty("Understudy1");

            PlayerOutfitter.toResultScreen(player, null);

            assertEquals(Material.LIME_DYE, inSlot(player, 1));
            assertEquals(Material.WRITTEN_BOOK, inSlot(player, 2));
            assertEquals(Material.COMPASS, inSlot(player, 3));
            assertEquals(Material.GRASS_BLOCK, inSlot(player, 5));
            assertEquals(Material.NETHERRACK, inSlot(player, 6));
            assertEquals(Material.ENDER_EYE, inSlot(player, 7));
            assertEquals(Material.SPYGLASS, inSlot(player, 8));
        }

        /** Slot 0 and 4 stay clear; the buttons are deliberately not a solid row. */
        @Test
        void theGapsInTheRowAreGaps() {
            PlayerMock player = dirty("Understudy1");

            PlayerOutfitter.toResultScreen(player, null);

            assertEquals(Material.AIR, inSlot(player, 0));
            assertEquals(Material.AIR, inSlot(player, 4));
        }
    }

    @Nested
    class ToLobby {

        @Test
        void theyWaitInAdventureModeAndFed() {
            PlayerMock player = dirty("Newcomer");

            PlayerOutfitter.toLobby(player, GameState.PRE_GAME);

            assertEquals(GameMode.ADVENTURE, player.getGameMode());
            assertEquals(20.0, player.getHealth());
            assertEquals(20, player.getFoodLevel());
            assertEquals(0, player.getLevel());
            assertFalse(player.getInventory().contains(Material.DIRT));
        }

        @Test
        void theyGetTheLobbyButtons() {
            PlayerMock player = dirty("Newcomer");

            PlayerOutfitter.toLobby(player, GameState.PRE_GAME);

            assertEquals(Material.WRITTEN_BOOK, inSlot(player, 0));
            assertEquals(Material.LIME_DYE, inSlot(player, 4));
            assertEquals(Material.ENDER_PEARL, inSlot(player, 8));
        }

        /**
         * The lobby hotbar is not the result screen's: both carry Achievements and Collection, in
         * different slots.
         */
        @Test
        void itIsADifferentBarFromTheResultScreens() {
            PlayerMock lobby = dirty("Newcomer");
            PlayerMock result = dirty("Understudy1");

            PlayerOutfitter.toLobby(lobby, GameState.PRE_GAME);
            PlayerOutfitter.toResultScreen(result, null);

            assertEquals(Material.LIME_DYE, inSlot(lobby, 4));
            assertEquals(Material.LIME_DYE, inSlot(result, 1));
            assertEquals(Material.AIR, inSlot(lobby, 1));
        }

        /**
         * A player reaching the lobby state at {@code END_GAME} — which happens, because
         * {@code END_GAME} shares the {@code LOBBY} admission with {@code PRE_GAME} — gets no
         * spectate pearl, since only the {@code PRE_GAME} click handler would answer it.
         */
        @Test
        void theSpectateButtonIsOfferedBeforeARoundAndNotAfterOne() {
            PlayerMock beforeARound = dirty("Newcomer");
            PlayerMock afterOne = dirty("Latecomer");

            PlayerOutfitter.toLobby(beforeARound, GameState.PRE_GAME);
            PlayerOutfitter.toLobby(afterOne, GameState.END_GAME);

            assertNotNull(beforeARound.getInventory().getItem(8));
            assertEquals(Material.ENDER_PEARL, inSlot(beforeARound, 8));
            assertEquals(Material.AIR, inSlot(afterOne, 8),
                    "there is nothing to opt out of once the round is over");
        }

        /** The rest of the lobby bar is unaffected: only slot 8 was ever phase-dependent. */
        @Test
        void theOtherLobbyButtonsSurviveIntoTheResultScreenPhase() {
            PlayerMock player = dirty("Latecomer");

            PlayerOutfitter.toLobby(player, GameState.END_GAME);

            assertEquals(Material.WRITTEN_BOOK, inSlot(player, 0));
            assertEquals(Material.LIME_DYE, inSlot(player, 4));
        }
    }

    /**
     * Not a state: one team member's share of a shared pool, handed out after the whole team has
     * already been through {@code toPlayer}.
     */
    @Nested
    class GiveJokerShare {

        @Test
        void theShareLandsInTheJokerSlot() {
            PlayerMock player = server.addPlayer("Understudy1");

            PlayerOutfitter.giveJokerShare(player, 2);

            assertEquals(GameItems.jokerMaterial(), inSlot(player, 4));
            assertEquals(2, player.getInventory().getItem(4).getAmount());
        }

        @Test
        void anEmptyShareWritesNothing() {
            PlayerMock player = server.addPlayer("Understudy1");

            PlayerOutfitter.giveJokerShare(player, 0);

            assertEquals(Material.AIR, inSlot(player, 4));
        }

        @Test
        void anOfflineMemberIsSkipped() {
            PlayerMock player = server.addPlayer("Understudy1");
            player.disconnect();

            PlayerOutfitter.giveJokerShare(player, 2);

            assertEquals(Material.AIR, inSlot(player, 4));
        }
    }
}
