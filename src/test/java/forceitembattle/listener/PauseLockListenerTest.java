package forceitembattle.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.model.GameState;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class PauseLockListenerTest extends ListenerTestBase {

    private PauseLockListener listener;
    private PlayerMock player;
    private Block block;

    @BeforeEach
    void setUpListener() {
        this.listener = new PauseLockListener(this.roundPhase);
        this.player = player("Understudy1");
        this.block = this.world.getBlockAt(0, 64, 0);
        this.block.setType(Material.FURNACE);
    }

    private BlockBreakEvent breaking() {
        return new BlockBreakEvent(this.block, this.player);
    }

    private BlockPlaceEvent placing() {
        return new BlockPlaceEvent(this.block, this.block.getState(),
                this.block.getRelative(BlockFace.DOWN), new ItemStack(Material.STONE),
                this.player, true, EquipmentSlot.HAND);
    }

    private PlayerInteractEvent rightClicking() {
        return new PlayerInteractEvent(this.player, Action.RIGHT_CLICK_BLOCK,
                new ItemStack(Material.COAL), this.block, BlockFace.UP);
    }

    @Test
    void aPausedRoundLocksTheWorld() {
        phase(GameState.PAUSED_GAME);

        BlockBreakEvent breaking = breaking();
        BlockPlaceEvent placing = placing();
        PlayerInteractEvent clicking = rightClicking();
        PlayerInteractEntityEvent touching = new PlayerInteractEntityEvent(this.player,
                this.world.spawn(at(1, 64, 1), org.bukkit.entity.Villager.class));

        listener.onBlockBreak(breaking);
        listener.onBlockPlace(placing);
        listener.onInteract(clicking);
        listener.onInteractEntity(touching);

        assertTrue(breaking.isCancelled(), "breaking a block");
        assertTrue(placing.isCancelled(), "placing a block");
        assertEquals(Event.Result.DENY, clicking.useInteractedBlock(), "opening a furnace");
        assertTrue(touching.isCancelled(), "trading with a villager");
    }

    @ParameterizedTest
    @EnumSource(value = GameState.class, names = "PAUSED_GAME", mode = EnumSource.Mode.EXCLUDE)
    void everyOtherPhaseIsLeftToTheOtherListeners(GameState state) {
        phase(state);

        BlockBreakEvent breaking = breaking();
        PlayerInteractEvent clicking = rightClicking();

        listener.onBlockBreak(breaking);
        listener.onInteract(clicking);

        assertFalse(breaking.isCancelled(), state.name());
        assertFalse(clicking.useInteractedBlock() == Event.Result.DENY, state.name());
    }
}
