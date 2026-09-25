package forceitembattle.ceremony;

import forceitembattle.gui.ResultDisplay;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItem;
import forceitembattle.model.Rarity;
import forceitembattle.model.ScoreOwner;
import forceitembattle.util.TimeFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The stats panel beside the grid while the results are browsed. */
final class SummaryCard {

    private SummaryCard() {
    }

    /** @param secondsTaken the play time of each of the owner's finds, in the same order */
    static String of(ScoreOwner owner, List<Long> secondsTaken) {
        return String.join("\n", linesOf(owner, secondsTaken));
    }

    static List<String> linesOf(ScoreOwner owner, List<Long> secondsTaken) {
        List<ForceItem> items = owner.foundItems();
        List<String> lines = new ArrayList<>();
        lines.add("<gold><b>STATS");
        lines.add("<gray>Items <white>" + items.size());

        Map<Rarity, Integer> chains = new EnumMap<>(Rarity.class);
        int backToBacks = 0;
        for (ForceItem item : items) {
            if (item.back2Back() == null || !item.back2Back().isActive()) {
                continue;
            }
            backToBacks++;
            Rarity rarity = StageTimeline.rarityOf(item);
            if (rarity != null) {
                chains.merge(rarity, 1, Integer::sum);
            }
        }
        lines.add("<gray>Back-to-backs <white>" + backToBacks);
        chains.forEach((rarity, count) -> lines.add("  " + rarity.displayName() + " <white>×" + count));

        if (!items.isEmpty() && secondsTaken.size() == items.size()) {
            int longest = 0;
            int fastest = -1;
            for (int index = 0; index < items.size(); index++) {
                if (secondsTaken.get(index) > secondsTaken.get(longest)) {
                    longest = index;
                }
                // A back-to-back is handed in instantly and a joker is no find at all.
                if (isEarnedFind(items.get(index))
                        && (fastest < 0 || secondsTaken.get(index) < secondsTaken.get(fastest))) {
                    fastest = index;
                }
            }
            lines.add("<gray>Longest <white>" + nameOf(items.get(longest)) + " <gold>" + duration(secondsTaken.get(longest)));
            if (fastest >= 0) {
                lines.add("<gray>Fastest <white>" + nameOf(items.get(fastest)) + " <gold>" + duration(secondsTaken.get(fastest)));
            }
        }

        if (ResultDisplay.attributesCollectors(owner)) {
            Map<String, Integer> finds = new LinkedHashMap<>();
            owner.members().stream()
                    .filter(member -> member.player() != null)
                    .forEach(member -> finds.put(member.player().getName(), 0));
            for (ForceItem item : items) {
                String collector = ResultDisplay.collectorName(owner, item.collectedBy());
                if (collector != null) {
                    finds.merge(collector, 1, Integer::sum);
                }
            }
            lines.add("<gray>Finds");
            finds.forEach((name, count) -> lines.add("  <white>" + name + " <gold>" + count));
        }
        return lines;
    }

    private static boolean isEarnedFind(ForceItem item) {
        return !item.usedSkip() && (item.back2Back() == null || !item.back2Back().isActive());
    }

    private static String nameOf(ForceItem item) {
        return CustomMaterials.nameOf(item.material());
    }

    private static String duration(long seconds) {
        return seconds == 0 ? "0s" : TimeFormat.humanised((int) seconds).trim();
    }
}
