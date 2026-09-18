package forceitembattle.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import forceitembattle.commands.player.CommandInfo;
import forceitembattle.manager.ItemDifficultiesManager;
import forceitembattle.manager.RecipeManager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Roster;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.ArgumentCaptor;

/**
 * {@code /info}: which item it describes.
 *
 * <p>That is the whole command, and it was silently wrong. A refactor that replaced a
 * {@code contains()}-then-{@code get()} guard dropped the line that assigns the force item, so
 * during a round {@code /info} began describing whatever the player happened to be holding. Nothing
 * caught it: the unit suite did not reach this command, the round-test harness never issues it, and
 * static analysis cannot see that a deleted assignment mattered.
 *
 * <p>The observable outcome is the stack handed to {@code createRecipeViewer}, so that is what
 * these assert.
 */
class CommandInfoTest {

    private ServerMock server;
    private RoundPhase roundPhase;
    private Roster roster;
    private RecipeManager recipeManager;
    private CommandInfo command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.roundPhase = new RoundPhase();
        this.roster = new Roster();
        this.recipeManager = mock(RecipeManager.class);

        ItemDifficultiesManager items = mock(ItemDifficultiesManager.class);
        when(items.itemHasDescription(any())).thenReturn(false);

        this.command = new CommandInfo(this.roster, this.roundPhase, items, this.recipeManager);
        ((CustomCommand) this.command).setContext(
                new CommandContext(this.roundPhase, null, this.roster));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** Joins someone hunting {@code forceItem}, holding {@code inHand}. */
    private PlayerMock playerHunting(Material forceItem, Material inHand, boolean spectating) {
        PlayerMock player = this.server.addPlayer("Understudy1");
        player.getInventory().setItemInMainHand(new ItemStack(inHand));

        ForceItemPlayer entry = new ForceItemPlayer(player, forceItem, 0, 0);
        entry.setSpectator(spectating);
        this.roster.add(player.getUniqueId(), entry);
        return player;
    }

    private Material describedItem() {
        ArgumentCaptor<ItemStack> captor = ArgumentCaptor.forClass(ItemStack.class);
        verify(this.recipeManager).createRecipeViewer(any(), captor.capture());
        return captor.getValue().getType();
    }

    private void assertTold(PlayerMock player, String expected) {
        String said = player.nextMessage();
        assertTrue(said != null && PlainTextComponentSerializer.plainText()
                        .serialize(net.kyori.adventure.text.Component.text(said)).contains(expected),
                "expected to be told " + expected + " but got " + said);
    }

    @Nested
    class DuringARound {

        @BeforeEach
        void roundIsRunning() {
            roundPhase.moveTo(GameState.MID_GAME);
        }

        /**
         * The regression, pinned. With no argument, a participant is told about the item they are
         * hunting — not the one in their hand.
         */
        @Test
        void withNoArgumentItDescribesTheForceItem() {
            PlayerMock player = playerHunting(Material.DIAMOND, Material.STONE, false);

            command.onCommand(player, null, "info", new String[0]);

            assertEquals(Material.DIAMOND, describedItem(),
                    "/info must describe the force item, not whatever is in hand");
        }

        /** Even with an empty hand, which is the case that used to hit "hold an item". */
        @Test
        void anEmptyHandIsStillFine() {
            PlayerMock player = playerHunting(Material.DIAMOND, Material.AIR, false);

            command.onCommand(player, null, "info", new String[0]);

            assertEquals(Material.DIAMOND, describedItem());
        }

        @Test
        void aSpectatorIsRefusedAndNothingIsOpened() {
            PlayerMock player = playerHunting(Material.DIAMOND, Material.STONE, true);

            command.onCommand(player, null, "info", new String[0]);

            assertTold(player, "You are not playing");
            verify(recipeManager, never()).createRecipeViewer(any(), any());
        }

        /** Somebody who joined after the countdown froze the roster holds no entry at all. */
        @Test
        void aMidRoundJoinerIsRefusedAndNothingIsOpened() {
            PlayerMock player = server.addPlayer("Latecomer");
            player.getInventory().setItemInMainHand(new ItemStack(Material.STONE));

            command.onCommand(player, null, "info", new String[0]);

            assertTold(player, "You are not playing");
            verify(recipeManager, never()).createRecipeViewer(any(), any());
        }

        /** An explicit argument still wins over the force item. */
        @Test
        void anArgumentBeatsTheForceItem() {
            PlayerMock player = playerHunting(Material.DIAMOND, Material.STONE, false);

            command.onCommand(player, null, "info", new String[]{"emerald"});

            assertEquals(Material.EMERALD, describedItem());
        }
    }

    @Nested
    class OutsideARound {

        @Test
        void withNoArgumentItDescribesWhatIsHeld() {
            PlayerMock player = playerHunting(Material.DIAMOND, Material.STONE, false);

            command.onCommand(player, null, "info", new String[0]);

            assertEquals(Material.STONE, describedItem(),
                    "with no round running there is no force item to describe");
        }

        @Test
        void anEmptyHandIsRefused() {
            PlayerMock player = playerHunting(Material.DIAMOND, Material.AIR, false);

            command.onCommand(player, null, "info", new String[0]);

            assertTold(player, "You need to hold an item in your hand!");
            verify(recipeManager, never()).createRecipeViewer(any(), any());
        }

        @Test
        void anUnknownItemNameIsRefused() {
            PlayerMock player = playerHunting(Material.DIAMOND, Material.STONE, false);

            command.onCommand(player, null, "info", new String[]{"not_an_item"});

            assertTold(player, "Invalid item name");
            verify(recipeManager, never()).createRecipeViewer(any(), any());
        }

        @Test
        void anArgumentIsDescribedWithoutTouchingTheRoster() {
            PlayerMock player = server.addPlayer("Bystander");

            command.onCommand(player, null, "info", new String[]{"emerald"});

            assertEquals(Material.EMERALD, describedItem());
        }
    }
}
