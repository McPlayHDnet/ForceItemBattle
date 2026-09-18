package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * How unlikely a chain was, and how that number is printed.
 *
 * <p><b>Headless, and that is the whole point of the move.</b> All of this lived inside
 * {@code BackToBackManager}, welded to {@code player.getInventory()} and
 * {@code backpacks.getTeamBackpack(...)}, so none of it was reachable — including
 * {@code formatPercent}, seventeen lines of leading-zero arithmetic that decide how many decimal
 * places a very long chain prints with, and the least-defended code in the file.
 */
class BackToBackProbabilityTest {

    private static double percentOf(int owned, int pool, int streak) {
        return BackToBackProbability.of(owned, pool, streak, false).percentage();
    }

    @Nested
    class TheOdds {

        @Test
        @DisplayName("owning half the pool, a 1-chain is 50%")
        void oneChainIsTheBaseChance() {
            assertEquals(50.0, percentOf(500, 1000, 1), 0.0001);
        }

        /** Each extra link multiplies: half, then a quarter, then an eighth. */
        @Test
        void eachLinkCompounds() {
            assertEquals(25.0, percentOf(500, 1000, 2), 0.0001);
            assertEquals(12.5, percentOf(500, 1000, 3), 0.0001);
        }

        /** Owning more distinct materials than the pool holds is possible; the odds cap at 100%. */
        @Test
        void owningMoreThanThePoolCapsAtCertain() {
            assertEquals(100.0, percentOf(1500, 1000, 4), 0.0001);
        }

        @Test
        void owningNothingIsImpossible() {
            assertEquals(0.0, percentOf(0, 1000, 1), 0.0001);
        }

        /**
         * An empty pool is reachable when the settings exclude every item. The raw division gives
         * {@code Infinity} or {@code NaN}, which survived {@code Math.min} and {@code Math.pow} and
         * printed as a garbage percentage, because nothing downstream checks.
         */
        @Test
        @DisplayName("an empty pool is 0%, not NaN")
        void anEmptyPoolIsZeroRatherThanNaN() {
            double percent = percentOf(12, 0, 3);

            assertFalse(Double.isNaN(percent), "NaN reached the formatter before this guard");
            assertFalse(Double.isInfinite(percent));
            assertEquals(0.0, percent, 0.0001);
        }

        @Test
        void andItsLabelIsPrintable() {
            String formatted = BackToBackProbability.of(12, 0, 3, false).formatted();

            assertTrue(formatted.contains("0%"), formatted);
            assertFalse(formatted.contains("NaN"), formatted);
            assertFalse(formatted.contains("�"), formatted);
        }
    }

    /**
     * A long chain runs to a very small number, and a fixed two decimal places would print every one
     * of them as "0%". The formatter counts the leading zeros and keeps two significant digits past
     * them.
     */
    @Nested
    class HowItPrints {

        private String labelOf(double percent) {
            // Reach the formatter through a pool that yields the percentage wanted.
            return BackToBackProbability.of((int) Math.round(percent * 100), 10_000, 1, false)
                    .formatted();
        }

        @Test
        void aPercentageAboveOneKeepsTwoPlaces() {
            assertTrue(labelOf(12.34).startsWith("12.34%"), labelOf(12.34));
        }

        @Test
        @DisplayName("exactly 100% prints without decimals")
        void certaintyPrintsWhole() {
            assertTrue(BackToBackProbability.of(10_000, 10_000, 1, false).formatted()
                    .startsWith("100%"));
        }

        @Test
        @DisplayName("a tiny chance keeps its significant digits rather than rounding to 0%")
        void aTinyChanceIsNotRoundedAway() {
            String formatted = BackToBackProbability.of(1, 1000, 3, false).formatted();

            // (1/1000)^3 = 1e-9, i.e. 0.0000001%
            assertTrue(formatted.startsWith("0.0000001%"), formatted);
        }

        @Test
        void onePercentSitsOnTheBoundary() {
            assertTrue(BackToBackProbability.of(100, 10_000, 1, false).formatted().startsWith("1%"));
        }
    }

    @Nested
    class TheRarityLabel {

        @Test
        void theLabelIsCarriedInTheFormattedString() {
            BackToBackProbability odds = BackToBackProbability.of(500, 1000, 1, false);

            assertTrue(odds.formatted().contains(odds.rarity().label()), odds.formatted());
        }

        /**
         * Being handed the same item twice running is its own tier, whatever the odds were:
         * {@code Rarity.classify} short-circuits to {@code EXTRAORDINARY} before it looks at the
         * probability at all.
         */
        @Test
        void aRepeatOfThePreviousItemOutranksTheOdds() {
            BackToBackProbability plain = BackToBackProbability.of(500, 1000, 1, false);
            BackToBackProbability repeat = BackToBackProbability.of(500, 1000, 1, true);

            assertEquals(plain.percentage(), repeat.percentage(), 0.0001,
                    "the odds are the same; only the classification differs");
            assertEquals(Rarity.RARE, plain.rarity(), "a 50% chain is the common tier");
            assertEquals(Rarity.EXTRAORDINARY, repeat.rarity());
        }

        @Test
        void aVanishinglyUnlikelyChainIsTheTopTier() {
            assertEquals(Rarity.RNGESUS, BackToBackProbability.of(1, 1000, 3, false).rarity());
        }
    }
}
