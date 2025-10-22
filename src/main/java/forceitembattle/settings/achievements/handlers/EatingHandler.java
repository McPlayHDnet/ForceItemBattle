package forceitembattle.settings.achievements.handlers;

import forceitembattle.settings.achievements.Trigger;
import forceitembattle.util.CustomItem;
import forceitembattle.util.ForceItemPlayer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handler for eating-based achievements
 */
public class EatingHandler implements AchievementHandler<SimpleProgress> {

    private final int targetAmount;
    private final CustomItem requiredItem;

    public EatingHandler(int targetAmount, CustomItem requiredItem) {
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
    public boolean check(Event event, SimpleProgress progress, ForceItemPlayer forceItemPlayer) {
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
            String displayName = PlainTextComponentSerializer.plainText().serialize(item.displayName());
            if (!displayName.contains(requiredItem.getCheckedName())) {
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

        return true;
    }

    @Override
    public SimpleProgress createProgress() {
        return new SimpleProgress();
    }
}