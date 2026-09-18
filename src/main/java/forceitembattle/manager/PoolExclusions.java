package forceitembattle.manager;

import forceitembattle.manager.ItemDifficultiesManager.ItemDefinition;
import forceitembattle.manager.ItemDifficultiesManager.ItemTag;

/**
 * Which registered items the current settings keep out of the generation pool.
 *
 * <p><b>HARD subsumes EXTREME.</b> Turning HARD off removes the nether items and the extreme ones
 * together, so the EXTREME setting only has anything left to decide while HARD is on.
 *
 * <p>The website mirrors this rule in {@code vendor-pool.mjs} so its item index matches the deployed
 * server. If the logic here changes, that changes too.
 */
final class PoolExclusions {

    private PoolExclusions() {
    }

    /**
     * @param hard    the HARD setting — off means no nether and no extreme items at all
     * @param extreme the EXTREME setting — only consulted while {@code hard} is on
     * @param end     the END setting — off means no end items
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
