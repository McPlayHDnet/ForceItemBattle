package forceitembattle.commands;

import static forceitembattle.commands.CommandTestSupport.assertSaid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.player.CommandBed;
import forceitembattle.commands.player.CommandSpawn;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * {@code /bed} and {@code /spawn}: the two teleports, which are the same command twice.
 *
 * <p>They differ only in where they read the destination from and what they say when there isn't
 * one. Everything else â€” the null guard, and the dismount-teleport-remount dance around
 * passengers â€” is duplicated between them line for line, so it is tested once per command
 * deliberately: the pair is exactly the shape where a fix lands in one copy and not the other.
 *
 * <p>The passenger dance is the substance. Bukkit will not teleport a vehicle's rider along with
 * it, so a player teleporting while someone rides them (or while they ride a boat) leaves the
 * passenger behind at the old coordinates. Dropping the remount half is invisible until someone
 * does it with a passenger.
 */
class CommandTeleportTest {

    private ServerMock server;
    private WorldMock world;
    private ForceItemBattle plugin;
    private CommandBed bed;
    private CommandSpawn spawn;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.world = this.server.addSimpleWorld("world");
        this.plugin = mock(ForceItemBattle.class);

        this.bed = new CommandBed(this.plugin);
        this.spawn = new CommandSpawn(this.plugin);
        ((CustomCommand) this.bed).setContext(new CommandContext(null, null, null));
        ((CustomCommand) this.spawn).setContext(new CommandContext(null, null, null));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- fixtures ---------------------------------------------------------------------------

    private PlayerMock join(String name) {
        PlayerMock player = this.server.addPlayer(name);
        player.teleport(at(0, 64, 0));
        return player;
    }

    private Location at(double x, double y, double z) {
        return new Location(this.world, x, y, z);
    }

    private static void assertAt(PlayerMock player, Location expected) {
        assertEquals(expected.getBlockX(), player.getLocation().getBlockX());
        assertEquals(expected.getBlockY(), player.getLocation().getBlockY());
        assertEquals(expected.getBlockZ(), player.getLocation().getBlockZ());
    }

    @Nested
    class Spawn {

        @Test
        void itTeleportsToTheConfiguredSpawn() {
            PlayerMock player = join("Understudy1");
            Location destination = at(100, 70, -40);
            when(plugin.getSpawnLocation()).thenReturn(destination);

            spawn.onCommand(player, null, "spawn", new String[0]);

            assertAt(player, destination);
        }

        @Test
        void withNoSpawnSetItRefuses() {
            PlayerMock player = join("Understudy1");
            when(plugin.getSpawnLocation()).thenReturn(null);

            spawn.onCommand(player, null, "spawn", new String[0]);

            assertSaid(player, "has not been set yet");
            assertAt(player, at(0, 64, 0));
        }

        /** A passenger rides along rather than being left at the old coordinates. */
        @Test
        void aPassengerComesAlong() {
            PlayerMock player = join("Understudy1");
            Entity passenger = world.spawnEntity(at(0, 64, 0), EntityType.PIG);
            player.addPassenger(passenger);
            when(plugin.getSpawnLocation()).thenReturn(at(100, 70, -40));

            spawn.onCommand(player, null, "spawn", new String[0]);

            assertTrue(player.getPassengers().contains(passenger),
                    "the passenger must be remounted after the teleport");
        }
    }

    @Nested
    class Bed {

        @Test
        void itTeleportsToTheRespawnPoint() {
            PlayerMock player = join("Understudy1");
            Location destination = at(-20, 65, 33);
            player.setRespawnLocation(destination, true);

            bed.onCommand(player, null, "bed", new String[0]);

            assertAt(player, destination);
        }

        @Test
        void withNoRespawnPointItRefuses() {
            PlayerMock player = join("Understudy1");

            bed.onCommand(player, null, "bed", new String[0]);

            assertSaid(player, "don't have a bed respawn point");
            assertAt(player, at(0, 64, 0));
        }

        @Test
        void aPassengerComesAlong() {
            PlayerMock player = join("Understudy1");
            Entity passenger = world.spawnEntity(at(0, 64, 0), EntityType.PIG);
            player.addPassenger(passenger);
            player.setRespawnLocation(at(-20, 65, 33), true);

            bed.onCommand(player, null, "bed", new String[0]);

            assertTrue(player.getPassengers().contains(passenger),
                    "the passenger must be remounted after the teleport");
        }
    }
}
