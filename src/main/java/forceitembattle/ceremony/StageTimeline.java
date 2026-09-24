package forceitembattle.ceremony;

import forceitembattle.model.BackToBack;
import forceitembattle.model.ForceItem;
import forceitembattle.model.Rarity;
import javax.annotation.Nullable;

/** How long each item holds the spotlight. A rarer find is given longer to be read. */
final class StageTimeline {

    static final int NORMAL_GAP = 10;
    static final int EVENT_GAP = 8;
    static final int FLIGHT_TICKS = 6;
    /** The next item pops while this one is still flying, so the spotlight never sits empty. */
    static final int OVERLAP = 4;

    private StageTimeline() {
    }

    /** Ticks from this item popping up to the next one popping up. */
    static int gapAfter(ForceItem item, boolean event) {
        int base = event ? EVENT_GAP : NORMAL_GAP;
        Rarity rarity = rarityOf(item);
        return rarity == null ? base : base + extraFor(rarity);
    }

    /** Ticks this item stays in the spotlight before flying to its slot. */
    static int holdFor(ForceItem item, boolean event) {
        return Math.max(1, gapAfter(item, event) - OVERLAP);
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
            case RARE -> 15;
            case EPIC -> 20;
            case LEGENDARY -> 30;
            case RNGESUS, EXTRAORDINARY -> 45;
        };
    }
}
