package forceitembattle.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.threeseconds.openapi.fibservice.client.model.FibItemCountDto;
import de.threeseconds.openapi.fibservice.client.model.FibLeaderboardEntryDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerIdentityDto;
import de.threeseconds.openapi.fibservice.client.model.FibRaritiesDto;
import forceitembattle.model.stats.ItemCount;
import forceitembattle.model.stats.LeaderboardEntry;
import forceitembattle.model.stats.PlayerIdentity;
import forceitembattle.model.RarityCounts;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The translation at the seam, and the one decision inside it.
 *
 * <p>Every generated field is a boxed type that can arrive null. Deciding that an absent count is
 * zero is a translation decision, and it used to be made — or forgotten — at each of forty call
 * sites: {@code entry.getTimesCollected() != null ? entry.getTimesCollected() : 0L} appeared
 * verbatim in the collection loaders, and not at all in the leaderboard renderers.
 */
class ReadModelTest {

    private static final UUID UUID_A =
            UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Test
    void anIdentityCrossesIntact() {
        PlayerIdentity identity = ReadModel.identity(
                new FibPlayerIdentityDto().uuid(UUID_A).name("Steve"));

        assertEquals(UUID_A, identity.uuid());
        assertEquals("Steve", identity.name());
    }

    @Test
    void anAbsentIdentityStaysAbsentRatherThanBecomingAnEmptyOne() {
        assertNull(ReadModel.identity(null));
    }

    @Test
    void absentRaritiesBecomeNoneRatherThanNull() {
        assertEquals(RarityCounts.NONE, ReadModel.rarities(null));
    }

    /**
     * A rarities payload with only some fields set is the normal case — the service sends what it
     * has — and every unset one is a zero, not a null to be checked downstream.
     */
    @Test
    void unsetRarityFieldsBecomeZero() {
        RarityCounts counts = ReadModel.rarities(new FibRaritiesDto().rare(4L));

        assertEquals(4, counts.rare());
        assertEquals(0, counts.epic());
        assertEquals(0, counts.legendary());
        assertEquals(0, counts.rngesus());
        assertEquals(0, counts.extraordinary());
    }

    @Test
    void anAbsentListBecomesAnEmptyOne() {
        assertTrue(ReadModel.itemCounts(null).isEmpty());
        assertTrue(ReadModel.memberStats(null).isEmpty());
        assertTrue(ReadModel.leaderboard(null).isEmpty());
        assertTrue(ReadModel.duoLeaderboard(null).isEmpty());
        assertTrue(ReadModel.unlocks(null).isEmpty());
        assertTrue(ReadModel.unlockedIds(null).isEmpty());
    }

    @Test
    void itemCountsCrossIntact() {
        List<ItemCount> items = ReadModel.itemCounts(
                List.of(new FibItemCountDto().itemName("DIRT").count(7L)));

        assertEquals(1, items.size());
        assertEquals("DIRT", items.get(0).itemName());
        assertEquals(7, items.get(0).count());
    }

    @Test
    void aLeaderboardRowCrossesIntact() {
        List<LeaderboardEntry> rows = ReadModel.leaderboard(List.of(
                new FibLeaderboardEntryDto()
                        .rank(1)
                        .player(new FibPlayerIdentityDto().uuid(UUID_A).name("Steve"))
                        .value(42L)));

        assertEquals(1, rows.get(0).rank());
        assertEquals(42, rows.get(0).value());
        assertEquals("Steve", PlayerIdentity.displayName(rows.get(0).player(), "?"));
    }

    /** An absent count on a real row is zero, which is what the renderer prints. */
    @Test
    void anUnsetCountOnARowIsZero() {
        List<ItemCount> items = ReadModel.itemCounts(
                List.of(new FibItemCountDto().itemName("DIRT")));

        assertEquals(0, items.get(0).count());
    }

    @Test
    void absentPlayerStatsBecomeZeroRatherThanNull() {
        assertEquals(0, ReadModel.playerStats(null).highestWinStreak());
    }

    /** Read models are handed out, so they must not be writable by whoever received them. */
    @Test
    void listsHandedOutAreImmutable() {
        List<ItemCount> items = ReadModel.itemCounts(
                List.of(new FibItemCountDto().itemName("DIRT").count(1L)));

        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> items.add(new ItemCount("STONE", 1)));
    }
}
