package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.SimpleAchievementProgress;
import forceitembattle.model.CustomItem;
import forceitembattle.model.ForceItemPlayer;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handler for loot-based achievements
 */
public class LootAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    private final int targetAmount;
    private final NamespacedKey lootTableKey; // null = any loot table
    private final CustomItem customItem;      // null = don't require a specific item
    private final boolean neededItem;         // true = match the player's current needed material

    public LootAchievementHandler(int targetAmount, NamespacedKey lootTableKey,
                                  CustomItem customItem, boolean neededItem) {
        if (targetAmount < 1) {
            throw new IllegalArgumentException("targetAmount must be at least 1");
        }
        this.targetAmount = targetAmount;
        this.lootTableKey = lootTableKey;
        this.customItem = customItem;
        this.neededItem = neededItem;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.LOOT;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin) {
        if (!(event instanceof LootGenerateEvent lootEvent)) {
            return false;
        }

        // Table-bound achievements only count their own table.
        if (lootTableKey != null) {
            if (lootEvent.getLootTable() == null
                    || !lootTableKey.equals(lootEvent.getLootTable().getKey())) {
                return false;
            }
        }

        List<ItemStack> loot = lootEvent.getLoot();

        if (neededItem) {
            Material needed = forceItemPlayer.getCurrentMaterial();
            for (ItemStack item : loot) {
                if (item != null && item.getType() == needed) {
                    return true;
                }
            }
            return false;
        }

        if (customItem != null) {
            for (ItemStack item : loot) {
                if (item != null && item.getType() != Material.AIR && matchesCustomItem(item)) {
                    progress.count++;
                    return progress.count >= targetAmount;
                }
            }
            return false;
        }

        // No item filter: rolling the bound table is enough.
        if (lootTableKey != null) {
            progress.count++;
            return progress.count >= targetAmount;
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
            if (!displayName.toLowerCase(Locale.ROOT).contains(customItem.getCheckedName().toLowerCase(Locale.ROOT))) {
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

        if (customItem.getCustomDataKey() != null) {
            NamespacedKey key = NamespacedKey.fromString(customItem.getCustomDataKey());
            if (key == null) {
                return false;
            }
            String value = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (value == null || !value.equals(customItem.getCustomDataValue())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }
}
