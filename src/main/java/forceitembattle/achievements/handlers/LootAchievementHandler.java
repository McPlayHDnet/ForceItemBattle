package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.SimpleAchievementProgress;
import forceitembattle.model.CustomItem;
import forceitembattle.model.ForceItemPlayer;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;

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
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
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
            Material needed = forceItemPlayer.activeMaterial();
            return loot.stream().anyMatch(item -> item != null && item.getType() == needed);
        }

        if (customItem != null) {
            for (ItemStack item : loot) {
                if (customItem.matches(item)) {
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

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }
}
