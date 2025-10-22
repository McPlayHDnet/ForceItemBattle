package forceitembattle.settings.achievements.handlers;

import forceitembattle.settings.achievements.Trigger;
import forceitembattle.util.CustomItem;
import forceitembattle.util.ForceItemPlayer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handler for loot-based achievements
 */
public class LootHandler implements AchievementHandler<SimpleProgress> {

    private final int targetAmount;
    private final CustomItem customItem;
    private final boolean neededItem;

    public LootHandler(int targetAmount, CustomItem customItem, boolean neededItem) {
        if (targetAmount < 1) {
            throw new IllegalArgumentException("targetAmount must be at least 1");
        }
        this.targetAmount = targetAmount;
        this.customItem = customItem;
        this.neededItem = neededItem;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.LOOT;
    }

    @Override
    public boolean check(Event event, SimpleProgress progress, ForceItemPlayer forceItemPlayer) {
        if (!(event instanceof org.bukkit.event.inventory.InventoryOpenEvent openEvent)) {
            return false;
        }

        if (!(openEvent.getInventory().getHolder() instanceof Chest)) {
            return false;
        }

        Inventory chest = openEvent.getInventory();

        if (neededItem) {
            Material needed = forceItemPlayer.getCurrentMaterial();
            for (ItemStack item : chest.getContents()) {
                if (item != null && item.getType() == needed) {
                    return true;
                }
            }
            return false;
        }

        if (customItem != null) {
            for (ItemStack item : chest.getContents()) {
                if (item != null && item.getType() != Material.AIR && matchesCustomItem(item)) {
                    progress.count++;
                    return progress.count >= targetAmount;
                }
            }
        }

        return false;
    }

    private boolean matchesCustomItem(ItemStack item) {
        if (customItem == null) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (customItem.getCheckedName() != null) {
            String displayName = PlainTextComponentSerializer.plainText().serialize(item.displayName());
            if (!displayName.contains(customItem.getCheckedName())) {
                return false;
            }
        }

        if (customItem.getCustomModelDataString() != null) {
            try {
                var cmd = item.getData(io.papermc.paper.datacomponent.DataComponentTypes.CUSTOM_MODEL_DATA);
                if (cmd == null) return false;
                var strings = cmd.strings();
                if (strings == null || !strings.contains(customItem.getCustomModelDataString())) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

        if (customItem.getCustomModelData() > 0) {
            if (!meta.hasCustomModelData() || meta.getCustomModelData() != customItem.getCustomModelData()) {
                return false;
            }
        }

        if (customItem.getMaterial() != null) {
            if (item.getType() != customItem.getMaterial()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public SimpleProgress createProgress() {
        return new SimpleProgress();
    }
}