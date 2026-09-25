package forceitembattle.ceremony;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.model.BackToBack;
import forceitembattle.model.ForceItem;
import forceitembattle.model.Rarity;
import org.bukkit.Material;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link StageTimeline} and {@link StagePalette}: how long each item holds the spotlight, and in what colour. */
class StageTimelineTest {

    static ForceItem plain() {
        return new ForceItem(Material.STONE, "00:42", 0L, new BackToBack(false), false, null);
    }

    static ForceItem joker() {
        return new ForceItem(Material.STONE, "00:42", 0L, new BackToBack(false), true, null);
    }

    static ForceItem backToBack(Rarity rarity) {
        BackToBack chain = new BackToBack(true).setRarity("0.5% (" + rarity.name() + ")");
        chain.setRarityType(rarity);
        return new ForceItem(Material.STONE, "00:42", 0L, chain, false, null);
    }

    @Nested
    class Pacing {

        @Test
        void aShortListIsDealtAtTheFullPace() {
            assertEquals(StageTimeline.NORMAL_GAP, StageTimeline.gapAfter(plain(), false, 10));
            assertEquals(StageTimeline.EVENT_GAP, StageTimeline.gapAfter(plain(), true, 10));
        }

        @Test
        void aLongerListIsDealtFaster() {
            int previous = StageTimeline.baseGap(false, 10);
            for (int count = 11; count <= 200; count++) {
                int gap = StageTimeline.baseGap(false, count);
                assertTrue(gap <= previous, count + " items should not be dealt slower than one fewer");
                previous = gap;
            }
            assertTrue(StageTimeline.baseGap(false, 40) < StageTimeline.NORMAL_GAP);
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        void thePaceNeverDropsBelowTheFloor(boolean event) {
            assertEquals(StageTimeline.FASTEST_GAP, StageTimeline.baseGap(event, 1000));
        }

        /** A skip glows red but is not worth waiting on. */
        @Test
        void aJokerKeepsThePace() {
            assertEquals(StageTimeline.gapAfter(plain(), false, 10), StageTimeline.gapAfter(joker(), false, 10));
        }

        @ParameterizedTest
        @ValueSource(ints = {10, 1000})
        void rarerChainsHoldTheSpotlightLonger(int count) {
            int previous = StageTimeline.gapAfter(plain(), false, count);
            for (Rarity rarity : new Rarity[]{Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY, Rarity.RNGESUS}) {
                int gap = StageTimeline.gapAfter(backToBack(rarity), false, count);
                assertTrue(gap > previous, rarity + " should outlast the tier below it");
                previous = gap;
            }
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 10, 40, 1000})
        void anItemLeavesTheSpotlightBeforeTheNextArrives(int count) {
            for (ForceItem item : java.util.List.of(plain(), backToBack(Rarity.RARE), backToBack(Rarity.EXTRAORDINARY))) {
                for (boolean event : new boolean[]{false, true}) {
                    int hold = StageTimeline.holdFor(item, event, count);
                    assertTrue(hold >= 2 && hold < StageTimeline.gapAfter(item, event, count));
                }
            }
        }

        @Test
        void thePitchClimbsAcrossTheReveal() {
            assertEquals(0.6f, StageTimeline.pitch(0, 20), 1e-6);
            assertEquals(2.0f, StageTimeline.pitch(19, 20), 1e-6);
            assertTrue(StageTimeline.pitch(5, 20) < StageTimeline.pitch(6, 20));
            assertEquals(1.0f, StageTimeline.pitch(0, 1), 1e-6);
        }
    }

    @Nested
    class Glow {

        @Test
        void aPlainItemDoesNotGlow() {
            assertNull(StagePalette.glowOf(plain()));
        }

        @Test
        void aJokerGlowsRed() {
            assertEquals(StagePalette.JOKER, StagePalette.glowOf(joker()));
        }

        @ParameterizedTest
        @EnumSource(Rarity.class)
        void aChainGlowsInItsRaritysColour(Rarity rarity) {
            assertEquals(StagePalette.glowOf(rarity), StagePalette.glowOf(backToBack(rarity)));
        }

        @Test
        void everyRarityHasItsOwnColour() {
            long distinct = java.util.Arrays.stream(Rarity.values()).map(StagePalette::glowOf).distinct().count();
            assertEquals(Rarity.values().length, distinct);
        }

        /** An inactive chain keeps its old rarity on the object; it must not light up. */
        @Test
        void anInactiveChainDoesNotGlow() {
            BackToBack broken = new BackToBack(false);
            broken.setRarityType(Rarity.LEGENDARY);
            assertNull(StagePalette.glowOf(new ForceItem(Material.STONE, "00:42", 0L, broken, false, null)));
        }
    }
}
