package forceitembattle.ceremony;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.model.BackToBack;
import forceitembattle.model.ForceItem;
import forceitembattle.model.Rarity;
import org.bukkit.Color;
import org.bukkit.Material;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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

        /** The inventory reveal's pace, kept so a plain round takes as long as it always has. */
        @Test
        void aPlainItemKeepsTheOldPace() {
            assertEquals(10, StageTimeline.gapAfter(plain(), false));
            assertEquals(8, StageTimeline.gapAfter(plain(), true));
        }

        /** A skip glows red but is not worth waiting on. */
        @Test
        void aJokerKeepsThePace() {
            assertEquals(StageTimeline.gapAfter(plain(), false), StageTimeline.gapAfter(joker(), false));
        }

        @Test
        void rarerChainsHoldTheSpotlightLonger() {
            int previous = StageTimeline.gapAfter(plain(), false);
            for (Rarity rarity : new Rarity[]{Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY, Rarity.RNGESUS}) {
                int gap = StageTimeline.gapAfter(backToBack(rarity), false);
                assertTrue(gap > previous, rarity + " should outlast the tier below it");
                previous = gap;
            }
        }

        @ParameterizedTest
        @EnumSource(Rarity.class)
        void anItemLeavesTheSpotlightBeforeTheNextArrives(Rarity rarity) {
            ForceItem item = backToBack(rarity);
            assertTrue(StageTimeline.holdFor(item, false) < StageTimeline.gapAfter(item, false));
            assertTrue(StageTimeline.holdFor(plain(), true) >= 1);
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

        @Test
        void aTintedCardKeepsItsTransparency() {
            Color tinted = StagePalette.cardFor(StagePalette.glowOf(Rarity.LEGENDARY));
            assertEquals(StagePalette.CARD.getAlpha(), tinted.getAlpha());
            assertEquals(StagePalette.CARD, StagePalette.cardFor(null));
        }
    }
}
