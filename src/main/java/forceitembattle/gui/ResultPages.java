package forceitembattle.gui;

import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItem;
import forceitembattle.model.ScoreOwner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.inventory.ItemStack;

/** The paged inventory an owner's {@code [Inventory]} link reopens, built in one go. */
public final class ResultPages {

    static final int FIRST_ITEM_SLOT = 10;
    static final int ROW_WIDTH = 7;
    static final int PAGE_CAPACITY = 35;

    private ResultPages() {
    }

    /** Empty when the owner found nothing, which {@link ResultScreen} shows as "No Items found". */
    public static Map<Integer, Map<Integer, ItemStack>> build(ScoreOwner owner) {
        List<ForceItem> items = owner.foundItems();
        Map<Integer, Map<Integer, ItemStack>> pages = new HashMap<>();
        for (int index = 0; index < items.size(); index++) {
            pages.computeIfAbsent(index / PAGE_CAPACITY, page -> new HashMap<>())
                    .put(slotOf(index % PAGE_CAPACITY), render(owner, items.get(index)));
        }
        return pages;
    }

    /** Seven wide inside a nine-wide inventory, one slot in from each edge. */
    static int slotOf(int indexOnPage) {
        return FIRST_ITEM_SLOT + (indexOnPage / ROW_WIDTH) * 9 + indexOnPage % ROW_WIDTH;
    }

    static ItemStack render(ScoreOwner owner, ForceItem forceItem) {
        List<String> lore = new ArrayList<>();

        if (ResultDisplay.attributesCollectors(owner)) {
            String collector = ResultDisplay.collectorName(owner, forceItem.collectedBy());
            if (collector != null) {
                lore.add("<dark_gray>» <gray>" + (forceItem.usedSkip() ? "Skipped" : "Found")
                        + " by <yellow>" + collector);
            }
        }

        if (forceItem.usedSkip()) {
            lore.add("");
            lore.add("<dark_gray>[<red>Joker<dark_gray>]");
        }
        if (forceItem.back2Back() != null && forceItem.back2Back().isActive()) {
            String rarity = forceItem.back2Back().getRarity();
            lore.add("");
            lore.add(rarity == null
                    ? "<dark_gray>[<dark_aqua>B2B<dark_gray>]"
                    : "<dark_gray>[<dark_aqua>B2B <dark_gray>» <aqua>" + rarity + "<dark_gray>]");
        }

        return new ItemBuilder(CustomMaterials.itemStackOf(forceItem.material()))
                .setDisplayName(CustomMaterials.nameOf(forceItem.material())
                        + " <dark_gray>» <gold>" + forceItem.timeNeeded())
                .setLore(lore)
                .setGlowing(forceItem.usedSkip())
                .getItemStack();
    }
}
