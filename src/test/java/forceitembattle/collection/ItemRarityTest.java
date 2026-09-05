package forceitembattle.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link ItemRarity}: how rare an item is across everyone who has played.
 *
 * <p>Pure arithmetic over a map, and reachable without a server — the collection loaders stopped
 * needing a plugin when the read side of the service seam was finished.
 */
class ItemRarityTest {

    @Test
    void countsTheHoldersOfAnItem() {
        ItemRarity rarity = new ItemRarity(Map.of("DIRT", 7L), 10L);

        assertEquals(7, rarity.playersWith("DIRT"));
    }

    /** An item nobody holds is zero, not absent — the screen shows 0%, it does not omit the row. */
    @Test
    void anUnheldItemIsZeroRatherThanMissing() {
        ItemRarity rarity = new ItemRarity(Map.of("DIRT", 7L), 10L);

        assertEquals(0, rarity.playersWith("NETHER_STAR"));
    }

    @Test
    void thePercentageIsOutOfTheTotalPlayers() {
        ItemRarity rarity = new ItemRarity(Map.of("DIRT", 7L), 10L);

        assertEquals(70.0, rarity.percentWith("DIRT"), 0.0001);
    }

    /**
     * The guard that matters. Before anyone has finished a round the total is zero, and the naive
     * division would put {@code NaN} or {@code Infinity} on the collection screen rather than 0%.
     */
    @Test
    void nobodyHavingPlayedIsZeroPercentNotNaN() {
        ItemRarity rarity = new ItemRarity(Map.of("DIRT", 7L), 0L);

        double percent = rarity.percentWith("DIRT");

        assertEquals(0.0, percent);
        assertTrue(Double.isFinite(percent), "must not be NaN or Infinity");
    }

    /** A negative total is nonsense but must not produce a negative percentage either. */
    @Test
    void aNegativeTotalIsAlsoZeroPercent() {
        assertEquals(0.0, new ItemRarity(Map.of("DIRT", 7L), -1L).percentWith("DIRT"));
    }

    @Test
    void theEmptyRarityHoldsNothingAndDividesByNothing() {
        ItemRarity empty = ItemRarity.empty();

        assertEquals(0, empty.totalPlayers());
        assertEquals(0, empty.playersWith("DIRT"));
        assertEquals(0.0, empty.percentWith("DIRT"));
    }

    @Test
    void everyoneHoldingItIsAHundredPercent() {
        assertEquals(100.0, new ItemRarity(Map.of("DIRT", 4L), 4L).percentWith("DIRT"), 0.0001);
    }
}
