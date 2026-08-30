package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * That a rarity reads back what it writes.
 *
 * <p>Twice deferred, now possible. Candidate 6 made both halves of the table speak
 * {@link RarityCounts} so the symmetry became expressible — and then it could not be asserted,
 * because {@code Rarity} holds {@code Sound} constants, {@code Sound} is registry-backed, and
 * class-initialising it threw {@code NoClassDefFoundError: org.bukkit.Registry}. That was recorded
 * in {@code HeadlessBoundaryTest} as a second wall.
 *
 * <p>Adding the plain Paper API to the test classpath for MockBukkit moved that wall. The
 * assertion below is the one this table has wanted since pass 1: the write mapping and the read
 * mapping are the same table, so one of a rarity must read back as exactly one of it.
 */
class RarityTest {

    @ParameterizedTest
    @EnumSource(Rarity.class)
    void oneOfARarityReadsBackAsExactlyOne(Rarity rarity) {
        assertEquals(1, rarity.count(rarity.asIncrement()));
        assertEquals(1, Rarity.total(rarity.asIncrement()));
    }

    @ParameterizedTest
    @EnumSource(Rarity.class)
    void aRarityIncrementTouchesNoOtherRarity(Rarity rarity) {
        for (Rarity other : Rarity.values()) {
            if (other != rarity) {
                assertEquals(0, other.count(rarity.asIncrement()),
                        other + " should be untouched by a " + rarity + " increment");
            }
        }
    }

    @Test
    void countsAreReadFromTheirOwnSlot() {
        RarityCounts counts = new RarityCounts(1, 2, 3, 4, 5);

        assertEquals(1, Rarity.RARE.count(counts));
        assertEquals(2, Rarity.EPIC.count(counts));
        assertEquals(3, Rarity.LEGENDARY.count(counts));
        assertEquals(4, Rarity.RNGESUS.count(counts));
        assertEquals(5, Rarity.EXTRAORDINARY.count(counts));
        assertEquals(15, Rarity.total(counts));
    }

    /** Absent stats are zero, not a crash — the case every caller used to null-check for itself. */
    @Test
    void absentCountsAreZero() {
        assertEquals(0, Rarity.RARE.count(null));
        assertEquals(0, Rarity.total(null));
        assertEquals(0, Rarity.total(RarityCounts.NONE));
    }

    @Test
    void classifyIsUnchangedByAnyOfThis() {
        assertEquals(Rarity.EXTRAORDINARY, Rarity.classify(0.5, true));
        assertEquals(Rarity.RNGESUS, Rarity.classify(0.001, false));
        assertEquals(Rarity.LEGENDARY, Rarity.classify(0.01, false));
        assertEquals(Rarity.EPIC, Rarity.classify(0.05, false));
        assertEquals(Rarity.RARE, Rarity.classify(0.5, false));
    }
}
