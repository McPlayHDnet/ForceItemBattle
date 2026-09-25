package forceitembattle.ceremony;

import forceitembattle.model.BackToBack;
import forceitembattle.model.ForceItem;
import forceitembattle.model.Rarity;
import javax.annotation.Nullable;

/** How long each item holds the spotlight. A rarer find is given longer; a longer list is dealt faster. */
final class StageTimeline {

    static final int NORMAL_GAP = 8;
    static final int EVENT_GAP = 7;
    static final int FASTEST_GAP = 4;
    /** Lists up to this long are dealt at the full gap. */
    static final int UNHURRIED_ITEMS = 10;
    /** Past that, every this many items take a tick off the gap. */
    static final int ITEMS_PER_SPEEDUP = 8;
    static final int FLIGHT_TICKS = 6;
    /** The next item pops while this one is still flying, so the spotlight never sits empty. */
    static final int OVERLAP = 3;

    private StageTimeline() {
    }

    /** Ticks from this item popping up to the next one popping up, in a reveal of {@code count} items. */
    static int gapAfter(ForceItem item, boolean event, int count) {
        int base = baseGap(event, count);
        Rarity rarity = rarityOf(item);
        return rarity == null ? base : base + extraFor(rarity);
    }

    /** Ticks this item stays in the spotlight before flying to its slot. */
    static int holdFor(ForceItem item, boolean event, int count) {
        return Math.max(2, gapAfter(item, event, count) - OVERLAP);
    }

    static int baseGap(boolean event, int count) {
        int speedup = Math.max(0, count - UNHURRIED_ITEMS) / ITEMS_PER_SPEEDUP;
        return Math.max(FASTEST_GAP, (event ? EVENT_GAP : NORMAL_GAP) - speedup);
    }

    /** Rises from low to high across the reveal, so the pops build towards the name. */
    static float pitch(int index, int count) {
        if (count <= 1) {
            return 1.0f;
        }
        return 0.6f + 1.4f * index / (count - 1);
    }

    @Nullable
    static Rarity rarityOf(ForceItem item) {
        BackToBack backToBack = item.back2Back();
        if (backToBack == null || !backToBack.isActive()) {
            return null;
        }
        return backToBack.getRarityType();
    }

    private static int extraFor(Rarity rarity) {
        return switch (rarity) {
            case RARE -> 10;
            case EPIC -> 14;
            case LEGENDARY -> 20;
            case RNGESUS, EXTRAORDINARY -> 30;
        };
    }
}
