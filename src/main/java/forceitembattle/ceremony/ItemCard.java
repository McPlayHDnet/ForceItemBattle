package forceitembattle.ceremony;

import forceitembattle.gui.ResultDisplay;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItem;
import forceitembattle.model.ScoreOwner;
import java.util.ArrayList;
import java.util.List;

/** The text under the item in the spotlight: what the inventory screen shows as the item's lore. */
final class ItemCard {

    private ItemCard() {
    }

    static String of(ScoreOwner owner, ForceItem item) {
        return String.join("\n", linesOf(owner, item));
    }

    static List<String> linesOf(ScoreOwner owner, ForceItem item) {
        List<String> lines = new ArrayList<>();
        lines.add("<white><b>" + CustomMaterials.nameOf(item.material()) + "</b>");
        lines.add("<gray>⌚ <gold>" + item.timeNeeded());

        if (ResultDisplay.attributesCollectors(owner)) {
            String collector = ResultDisplay.collectorName(owner, item.collectedBy());
            if (collector != null) {
                lines.add("<gray>" + (item.usedSkip() ? "Skipped" : "Found") + " by <yellow>" + collector);
            }
        }

        if (item.usedSkip()) {
            lines.add("<dark_gray>[<red>Joker<dark_gray>]");
        }
        if (item.back2Back() != null && item.back2Back().isActive()) {
            String rarity = item.back2Back().getRarity();
            lines.add(rarity == null
                    ? "<dark_gray>[<dark_aqua>B2B<dark_gray>]"
                    : "<dark_aqua>B2B <dark_gray>» <aqua>" + rarity);
        }
        return lines;
    }
}
