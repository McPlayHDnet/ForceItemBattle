package forceitembattle.manager;

import forceitembattle.manager.ItemDifficultiesManager.ItemDefinition;
import forceitembattle.manager.ItemDifficultiesManager.ItemTag;

/**
 * HARD subsumes EXTREME: turning HARD off removes nether and extreme items together.
 * The website mirrors this rule in {@code vendor-pool.mjs}; change both together.
 */
final class PoolExclusions {

    private PoolExclusions() {
    }

    /**
     * @param extreme only consulted while {@code hard} is on
     * @return true when this item should be left out of the pool
     */
    static boolean isExcluded(ItemDefinition definition, boolean hard, boolean extreme, boolean end) {
        if (definition == null) {
            return false;
        }

        if (!hard) {
            if (definition.hasAnyTag(ItemTag.NETHER, ItemTag.EXTREME)) {
                return true;
            }
        } else if (!extreme && definition.hasTag(ItemTag.EXTREME)) {
            return true;
        }

        return !end && definition.hasTag(ItemTag.END);
    }
}
