package forceitembattle.commands;

import static forceitembattle.commands.CommandTestSupport.screenOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import forceitembattle.commands.admin.CommandCheckMods;
import forceitembattle.moddetection.ModDetections;
import forceitembattle.moddetection.ModFinding;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class CommandCheckModsTest {

    private ServerMock server;
    private ModDetections modDetections;
    private CommandCheckMods command;
    private PlayerMock op;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.modDetections = new ModDetections();
        this.command = new CommandCheckMods(this.modDetections);
        ((CustomCommand) this.command).setContext(new CommandContext(null, null, null));
        this.op = this.server.addPlayer("Admin");
        this.op.setOp(true);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private String run(String... args) {
        this.command.onCommand(this.op, null, "checkmods", args);
        return screenOf(this.op);
    }

    @Test
    void nothingDetectedSaysSo() {
        assertEquals("§7No client mods detected.\n", run());
    }

    @Test
    void eachFindingOfAPlayerIsListed() {
        PlayerMock cheater = this.server.addPlayer("Understudy1");
        this.modDetections.record(cheater.getUniqueId(), "Understudy1", ModFinding.FREECAM_INSTALLED);
        this.modDetections.record(cheater.getUniqueId(), "Understudy1", ModFinding.XAERO_MINIMAP);

        assertEquals("""
                §6Xaero's Minimap §7detected for §eUnderstudy1§7 - it got automatically disabled.
                §6Freecam §7detected for §eUnderstudy1§7.
                """, run());
    }

    @Test
    void aPlayerWhoLeftIsStillListedAndMarkedOffline() {
        this.modDetections.record(UUID.randomUUID(), "Gone", ModFinding.FREECAM_INSTALLED);

        assertEquals("§6Freecam §7detected for §eGone§7. §8(offline)\n", run());
    }

    @Test
    void aRepeatedFindingIsRecordedOnce() {
        UUID player = UUID.randomUUID();

        this.modDetections.record(player, "Understudy1", ModFinding.FREECAM_IN_USE);
        boolean again = this.modDetections.record(player, "Understudy1", ModFinding.FREECAM_IN_USE);

        assertFalse(again);
        assertEquals(1, this.modDetections.all().size());
    }

    @Test
    void clearForgetsEveryFinding() {
        this.modDetections.record(UUID.randomUUID(), "Understudy1", ModFinding.FREECAM_INSTALLED);

        assertEquals("§7Cleared all detections. Players are checked again when they reconnect.\n", run("clear"));
        assertEquals("§7No client mods detected.\n", run());
    }

    @Test
    void anUnknownArgumentShowsTheUsageAndClearsNothing() {
        this.modDetections.record(UUID.randomUUID(), "Understudy1", ModFinding.FREECAM_INSTALLED);

        run("wipe");

        assertEquals(1, this.modDetections.all().size());
    }
}
