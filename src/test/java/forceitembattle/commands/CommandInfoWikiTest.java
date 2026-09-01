package forceitembattle.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.player.CommandInfoWiki;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Roster;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@code /infowiki}: which item it links to.
 *
 * <p>The same shape as {@code /info}, and it carried the same regression — a refactor dropped the
 * line assigning the force item, so during a round the link pointed at whatever was in hand. It was
 * fixed at the same time and had no test until this one.
 *
 * <p>The observable outcome is a chat message carrying a minecraft.wiki URL, so these assert on the
 * slug in it.
 */
class CommandInfoWikiTest {

    private ServerMock server;
    private RoundPhase roundPhase;
    private Roster roster;
    private CommandInfoWiki command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        ForceItemBattle plugin = mock(ForceItemBattle.class);
        this.roundPhase = new RoundPhase();
        this.roster = new Roster();

        when(plugin.getRoundPhase()).thenReturn(this.roundPhase);
        when(plugin.getRoster()).thenReturn(this.roster);

        this.command = new CommandInfoWiki(plugin);
        ((CustomCommand) this.command).setContext(
                new CommandContext(this.roundPhase, null, this.roster));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private PlayerMock playerHunting(Material forceItem, Material inHand, boolean spectating) {
        PlayerMock player = this.server.addPlayer("Understudy1");
        player.getInventory().setItemInMainHand(new ItemStack(inHand));

        ForceItemPlayer entry = new ForceItemPlayer(player, forceItem, 0, 0);
        entry.setSpectator(spectating);
        this.roster.add(player.getUniqueId(), entry);
        return player;
    }

    /**
     * The wiki URL lives in a click event, not in the text — {@code nextMessage()} returns the
     * legacy string and drops it, so the message is serialised back to MiniMessage to see it.
     */
    private void assertLinkedTo(PlayerMock player, String slug) {
        String rendered = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .serialize(player.nextComponentMessage());

        assertTrue(rendered.contains("https://minecraft.wiki/" + slug),
                "expected a link to " + slug + " but the message was: " + rendered);
    }

    @Nested
    class DuringARound {

        @BeforeEach
        void roundIsRunning() {
            roundPhase.moveTo(GameState.MID_GAME);
        }

        /** The regression, pinned: the link is for the force item, not the held one. */
        @Test
        void linksToTheForceItem() {
            PlayerMock player = playerHunting(Material.DIAMOND, Material.STONE, false);

            command.onCommand(player, null, "infowiki", new String[0]);

            assertLinkedTo(player, "Diamond");
        }

        @Test
        void anEmptyHandIsStillFine() {
            PlayerMock player = playerHunting(Material.DIAMOND, Material.AIR, false);

            command.onCommand(player, null, "infowiki", new String[0]);

            assertLinkedTo(player, "Diamond");
        }

        @Test
        void aSpectatorIsRefused() {
            PlayerMock player = playerHunting(Material.DIAMOND, Material.STONE, true);

            command.onCommand(player, null, "infowiki", new String[0]);

            assertTrue(player.nextMessage().contains("not playing"));
            assertNull(player.nextMessage(), "nothing follows the refusal");
        }

        @Test
        void aMidRoundJoinerIsRefused() {
            PlayerMock player = server.addPlayer("Latecomer");
            player.getInventory().setItemInMainHand(new ItemStack(Material.STONE));

            command.onCommand(player, null, "infowiki", new String[0]);

            assertTrue(player.nextMessage().contains("not playing"));
            assertNull(player.nextMessage(), "nothing follows the refusal");
        }

        /**
         * The wiki title-cases every word but the short joining ones, which is why the slug is built
         * rather than derived from the material name — see {@code CustomMaterials.wikiSlugOf}.
         */
        @Test
        void theSlugFollowsTheWikiTitleRules() {
            PlayerMock player = playerHunting(Material.HEART_OF_THE_SEA, Material.STONE, false);

            command.onCommand(player, null, "infowiki", new String[0]);

            assertLinkedTo(player, "Heart_of_the_Sea");
        }
    }

    @Nested
    class OutsideARound {

        @Test
        void linksToWhatIsHeld() {
            PlayerMock player = playerHunting(Material.DIAMOND, Material.STONE, false);

            command.onCommand(player, null, "infowiki", new String[0]);

            assertLinkedTo(player, "Stone");
        }

        @Test
        void anEmptyHandIsRefused() {
            PlayerMock player = playerHunting(Material.DIAMOND, Material.AIR, false);

            command.onCommand(player, null, "infowiki", new String[0]);

            assertTrue(player.nextMessage().contains("need to hold an item"));
        }
    }
}
