package forceitembattle.achievements;

import static forceitembattle.achievements.Finds.found;
import static forceitembattle.achievements.Finds.participant;
import static forceitembattle.achievements.Finds.skipped;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.achievements.handlers.CollectionAchievementHandler;
import forceitembattle.achievements.handlers.InventoryFullAchievementHandler;
import forceitembattle.achievements.progress.CollectionAchievementProgress;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.MaterialCategory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Two handlers that read something other than the event they are handed.
 *
 * <p>{@code InventoryFullAchievementHandler} ignores the event entirely and inspects the player's
 * inventory; the wood-types collection variant accumulates a set across finds. Both were
 * unreachable before the seam for the usual reason, and both are now a matter of stubbing two
 * methods.
 */
class InventoryAndCollectionHandlersTest {

    private static ItemStack stack(Material material) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        return item;
    }

    @Nested
    class InventoryFull {

        /** Fills the 36 storage slots; armour and offhand are deliberately not counted. */
        private PlayerInventory fullInventory() {
            PlayerInventory inv = mock(PlayerInventory.class);
            ItemStack dirt = stack(Material.DIRT);
            for (int i = 0; i < 36; i++) {
                when(inv.getItem(i)).thenReturn(dirt);
            }
            return inv;
        }

        private ForceItemPlayer playerWith(PlayerInventory inv) {
            ForceItemPlayer alice = participant("a");
            when(alice.player().getInventory()).thenReturn(inv);
            return alice;
        }

        @Test
        void thirtySixFullSlotsIsAFullInventory() {
            ForceItemPlayer alice = playerWith(fullInventory());
            InventoryFullAchievementHandler handler = new InventoryFullAchievementHandler();

            assertTrue(handler.check(found(alice, Material.DIRT), handler.createProgress(), alice,
                    new FakeAchievementWorld().backpack(false, null)));
        }

        @Test
        void oneEmptySlotIsNot() {
            PlayerInventory inv = fullInventory();
            when(inv.getItem(17)).thenReturn(null);
            ForceItemPlayer alice = playerWith(inv);
            InventoryFullAchievementHandler handler = new InventoryFullAchievementHandler();

            assertFalse(handler.check(found(alice, Material.DIRT), handler.createProgress(), alice,
                    new FakeAchievementWorld().backpack(false, null)));
        }

        /** An AIR stack counts as empty, which is not the same check as null. */
        @Test
        void anAirStackCountsAsEmpty() {
            PlayerInventory inv = fullInventory();
            ItemStack air = stack(Material.AIR);
            when(inv.getItem(0)).thenReturn(air);
            ForceItemPlayer alice = playerWith(inv);
            InventoryFullAchievementHandler handler = new InventoryFullAchievementHandler();

            assertFalse(handler.check(found(alice, Material.DIRT), handler.createProgress(), alice,
                    new FakeAchievementWorld().backpack(false, null)));
        }

        /** With the backpack on, a full inventory is not enough — the backpack has to be full too. */
        @Test
        void aRoomyBackpackMeansTheInventoryIsNotFull() {
            ForceItemPlayer alice = playerWith(fullInventory());
            Inventory backpack = mock(Inventory.class);
            ItemStack[] roomy = {stack(Material.DIRT), null};
            when(backpack.getContents()).thenReturn(roomy);
            InventoryFullAchievementHandler handler = new InventoryFullAchievementHandler();

            assertFalse(handler.check(found(alice, Material.DIRT), handler.createProgress(), alice,
                    new FakeAchievementWorld().backpack(true, backpack)));
        }

        @Test
        void aFullBackpackAlongsideAFullInventoryCounts() {
            ForceItemPlayer alice = playerWith(fullInventory());
            Inventory backpack = mock(Inventory.class);
            ItemStack[] full = {stack(Material.DIRT)};
            when(backpack.getContents()).thenReturn(full);
            InventoryFullAchievementHandler handler = new InventoryFullAchievementHandler();

            assertTrue(handler.check(found(alice, Material.DIRT), handler.createProgress(), alice,
                    new FakeAchievementWorld().backpack(true, backpack)));
        }

        /**
         * The backpack is only consulted when the round has it switched on — the one thing this
         * handler asks the world, and the reason it was among the five that held the plugin.
         */
        @Test
        void aDisabledBackpackIsNotConsulted() {
            ForceItemPlayer alice = playerWith(fullInventory());
            InventoryFullAchievementHandler handler = new InventoryFullAchievementHandler();

            assertTrue(handler.check(found(alice, Material.DIRT), handler.createProgress(), alice,
                    new FakeAchievementWorld().backpack(false, mock(Inventory.class))));
        }
    }

    @Nested
    class WoodCollection {

        /**
         * Grants only once every wood category has been found — a set, not a count.
         *
         * <p>Driven by one representative material per required category rather than by walking
         * {@code Material.values()}, which also checks something worth checking: that every
         * category the achievement requires is actually reachable from some material. An
         * unreachable one would make the achievement quietly uncompletable.
         */
        @Test
        void needsEveryWoodCategoryBeforeItGrants() {
            Set<String> required = MaterialCategory.getAllWoodCategories();
            Map<String, Material> representative = new LinkedHashMap<>();
            for (Material material : Material.values()) {
                String category = MaterialCategory.getWoodCategory(material);
                if (category != null && required.contains(category)) {
                    representative.putIfAbsent(category, material);
                }
            }

            assertEquals(required, representative.keySet(),
                    "every required wood category must be reachable from some material");

            ForceItemPlayer alice = participant("a");
            CollectionAchievementHandler<String> handler = CollectionAchievementHandler.woodTypesHandler();
            CollectionAchievementProgress<String> progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld();

            int seen = 0;
            boolean granted = false;
            for (Material material : representative.values()) {
                seen++;
                granted = handler.check(found(alice, material), progress, alice, world);
                if (seen < required.size()) {
                    assertFalse(granted, "granted after only " + seen + " of " + required.size());
                }
            }

            assertTrue(granted, "the last outstanding category completes it");
        }

        /** A skipped item is not collected, so it cannot complete the set. */
        @Test
        void aSkippedItemDoesNotCount() {
            ForceItemPlayer alice = participant("a");
            CollectionAchievementHandler<String> handler = CollectionAchievementHandler.woodTypesHandler();
            CollectionAchievementProgress<String> progress = handler.createProgress();

            Material anyWood = null;
            for (Material material : Material.values()) {
                if (MaterialCategory.getWoodCategory(material) != null) {
                    anyWood = material;
                    break;
                }
            }

            handler.check(skipped(alice, anyWood), progress, alice, new FakeAchievementWorld());

            assertTrue(progress.collected.isEmpty(), "a skip collects nothing");
        }
    }
}
