package forceitembattle.manager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Roster;
import forceitembattle.model.Team;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Which backpack a player gets when they ask for one.
 *
 * <p>Exists because {@code /bp} was broken for the whole of every team round: it called
 * {@code openPlayerBackpack} unconditionally, which looked up the solo map, found nothing for a
 * player whose backpack is their team's, and passed {@code null} to {@code openInventory} — so the
 * command answered "An internal error occurred" while the slot-8 backpack item, which did branch on
 * {@code isInTeam()}, worked. The rule now lives in one place and both callers use it.
 */
class BackpackManagerTest {

    private ServerMock server;
    private Roster roster;
    private BackpackManager backpacks;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();

        JavaPlugin plugin = mock(JavaPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("settings.backpackRows", 3);
        when(plugin.getConfig()).thenReturn(config);

        this.roster = new Roster();
        this.backpacks = new BackpackManager(plugin, this.roster);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private ForceItemPlayer join(String name) {
        PlayerMock player = this.server.addPlayer(name);
        ForceItemPlayer entry = new ForceItemPlayer(player, Material.DIRT, 0, 0);
        this.roster.add(player.getUniqueId(), entry);
        return entry;
    }

    @Nested
    class Solo {

        @Test
        void opensTheirOwnBackpack() {
            ForceItemPlayer alice = join("Alice");
            backpacks.createBackpack(alice);

            assertTrue(backpacks.openBackpackFor(alice.player()));
            assertSame(backpacks.getPlayerBackpack(alice.player()),
                    alice.player().getOpenInventory().getTopInventory());
        }
    }

    @Nested
    class InATeam {

        /** The regression: this is the case that used to throw. */
        @Test
        void opensTheTeamBackpackRatherThanLookingUpAnEmptySoloMap() {
            ForceItemPlayer alice = join("Alice");
            ForceItemPlayer bob = join("Bob");
            Team team = new Team(1, null, 0, 0, alice, bob);
            alice.setCurrentTeam(team);
            bob.setCurrentTeam(team);

            backpacks.createTeamBackpack(team, alice);

            Inventory shared = backpacks.getTeamBackpack(team);

            assertTrue(backpacks.openBackpackFor(alice.player()));
            assertSame(shared, alice.player().getOpenInventory().getTopInventory());

            assertTrue(backpacks.openBackpackFor(bob.player()));
            assertSame(shared, bob.player().getOpenInventory().getTopInventory());
        }

        @Test
        void neverConsultsTheSoloMapForATeamedPlayer() {
            ForceItemPlayer alice = join("Alice");
            Team team = new Team(1, null, 0, 0, alice);
            alice.setCurrentTeam(team);
            backpacks.createTeamBackpack(team, alice);

            assertSame(backpacks.getTeamBackpack(team), backpacks.getBackpackForPlayer(alice.player()));
        }
    }

    @Nested
    class WithNoBackpackCreated {

        /**
         * Reachable in play: switching BACKPACK on mid-round means nobody had one created at start.
         * A refusal is the answer; the command used to die with an internal error.
         */
        @Test
        void refusesInsteadOfThrowing() {
            ForceItemPlayer alice = join("Alice");

            assertDoesNotThrow(() -> backpacks.openBackpackFor(alice.player()));
            assertFalse(backpacks.openBackpackFor(alice.player()));
        }

        @Test
        void refusesForSomeoneWithNoRosterEntryAtAll() {
            PlayerMock latecomer = server.addPlayer("Latecomer");

            assertFalse(backpacks.openBackpackFor(latecomer));
        }
    }
}
