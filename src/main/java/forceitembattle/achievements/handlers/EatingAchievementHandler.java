package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Trigger;
import forceitembattle.util.CustomItem;
import forceitembattle.util.ForceItemPlayer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

/**
 * Handler for eating-based achievements
 */
public class EatingAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    private final int targetAmount;
    private final CustomItem requiredItem;

    public EatingAchievementHandler(int targetAmount, CustomItem requiredItem) {
        if (targetAmount < 1) {
            throw new IllegalArgumentException("targetAmount must be at least 1");
        }
        this.targetAmount = targetAmount;
        this.requiredItem = requiredItem;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.EATING;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin ) {
        if (!(event instanceof PlayerItemConsumeEvent consumeEvent)) {
            return false;
        }

        if (requiredItem != null && !matchesCustomItem(consumeEvent.getItem())) {
            return false;
        }

        progress.count++;
        return progress.count >= targetAmount;
    }

    private boolean matchesCustomItem(ItemStack item) {
        if (requiredItem == null) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (requiredItem.getCheckedName() != null) {
            // Case-insensitive: the CONNOISSEUR definition uses "cavendish" while
            // the item is displayed "Cavendish", so a case-sensitive match failed.
            String displayName = PlainTextComponentSerializer.plainText().serialize(item.displayName());
            if (!displayName.toLowerCase(Locale.ROOT).contains(requiredItem.getCheckedName().toLowerCase(Locale.ROOT))) {
                return false;
            }
        }

        if (requiredItem.getCustomModelDataString() != null) {
            try {
                var cmd = item.getData(io.papermc.paper.datacomponent.DataComponentTypes.CUSTOM_MODEL_DATA);
                if (cmd == null) return false;
                var strings = cmd.strings();
                if (strings == null || !strings.contains(requiredItem.getCustomModelDataString())) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

        if (requiredItem.getCustomModelData() > 0) {
            if (!meta.hasCustomModelData() || meta.getCustomModelData() != requiredItem.getCustomModelData()) {
                return false;
            }
        }

        if (requiredItem.getMaterial() != null) {
            if (item.getType() != requiredItem.getMaterial()) {
                return false;
            }
        }

        if (requiredItem.getCustomDataKey() != null) {
            NamespacedKey key = NamespacedKey.fromString(requiredItem.getCustomDataKey());
            if (key == null) {
                return false;
            }
            String value = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (value == null || !value.equals(requiredItem.getCustomDataValue())) {
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