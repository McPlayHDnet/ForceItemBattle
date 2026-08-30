package forceitembattle.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * {@link TeamPairing}, which stops auto-assigned teams from reproducing the previous round's pairs.
 *
 * The randomness is seeded here so a failure is reproducible, but every assertion holds for any
 * seed — nothing below depends on a particular draw.
 */
class TeamPairingTest {

    private static UUID id(String seed) {
        return UUID.fromString("00000000-0000-0000-0000-00000000000" + seed);
    }

    private static List<UUID> ids(String... seeds) {
        List<UUID> ids = new ArrayList<>();
        for (String seed : seeds) {
            ids.add(id(seed));
        }
        return ids;
    }

    /** The pairings an ordering produces, chunked the way {@code autoTeams} chunks it. */
    private static Set<String> pairsOf(List<UUID> ordered) {
        Set<String> pairs = new HashSet<>();
        for (int i = 0; i + 1 < ordered.size(); i += 2) {
            pairs.add(TeamPairing.pairKey(ordered.get(i), ordered.get(i + 1)));
        }
        return pairs;
    }

    @Test
    void pairKeyIsTheSameWhicheverWayRound() {
        assertEquals(TeamPairing.pairKey(id("a"), id("b")), TeamPairing.pairKey(id("b"), id("a")));
        assertFalse(TeamPairing.pairKey(id("a"), id("b")).equals(TeamPairing.pairKey(id("a"), id("c"))));
    }

    @Test
    void lastRoundsPairsDoNotComeBack() {
        Set<String> forbidden = Set.of(
                TeamPairing.pairKey(id("a"), id("b")),
                TeamPairing.pairKey(id("c"), id("d")));

        // Four players allow exactly three pairings, so this is the case that has to work every time
        // rather than most of the time.
        for (int attempt = 0; attempt < 200; attempt++) {
            List<UUID> ordered = TeamPairing.orderAvoidingPairs(
                    ids("a", "b", "c", "d"), forbidden, new Random(attempt));

            for (String pair : pairsOf(ordered)) {
                assertFalse(forbidden.contains(pair), "reproduced last round's pairing: " + pair);
            }
        }
    }

    /**
     * The point of the whole thing: four players should walk through all three pairings before any
     * of them can repeat, instead of a fair coin landing on the same teams two rounds running.
     */
    @Test
    void fourPlayersCycleThroughEveryPairingBeforeRepeating() {
        Set<String> seen = new HashSet<>();
        Set<String> forbidden = Set.of();

        for (int round = 0; round < 3; round++) {
            List<UUID> ordered = TeamPairing.orderAvoidingPairs(
                    ids("a", "b", "c", "d"), forbidden, new Random(round));
            Set<String> pairs = pairsOf(ordered);

            for (String pair : pairs) {
                assertTrue(seen.add(pair), "pairing came round again too early: " + pair);
            }
            forbidden = pairs;
        }

        assertEquals(6, seen.size()); // three rounds x two pairs, all distinct
    }

    @Test
    void everyPlayerSurvivesTheReordering() {
        List<UUID> input = ids("a", "b", "c", "d", "e");
        List<UUID> ordered = TeamPairing.orderAvoidingPairs(
                input, Set.of(TeamPairing.pairKey(id("a"), id("b"))), new Random(7));

        assertEquals(input.size(), ordered.size());
        assertEquals(new HashSet<>(input), new HashSet<>(ordered));
    }

    /**
     * With every pairing forbidden there is nothing valid left to return. Building teams anyway
     * beats refusing to start the game, so the fallback still has to be a usable ordering.
     */
    @Test
    void anUnsatisfiableRuleFallsBackToAPlainShuffle() {
        List<UUID> input = ids("a", "b", "c", "d");
        Set<String> forbidden = new HashSet<>();
        for (UUID first : input) {
            for (UUID second : input) {
                if (!first.equals(second)) forbidden.add(TeamPairing.pairKey(first, second));
            }
        }

        List<UUID> ordered = TeamPairing.orderAvoidingPairs(input, forbidden, new Random(1));

        assertEquals(new HashSet<>(input), new HashSet<>(ordered));
    }

    @Test
    void anEmptyHistoryIsJustAShuffle() {
        List<UUID> input = ids("a", "b", "c", "d");
        List<UUID> ordered = TeamPairing.orderAvoidingPairs(input, Set.of(), new Random(3));

        assertEquals(new HashSet<>(input), new HashSet<>(ordered));
    }

    /** One player cannot be paired with anybody, and must not trip the search. */
    @Test
    void aSinglePlayerIsReturnedUntouched() {
        List<UUID> ordered = TeamPairing.orderAvoidingPairs(ids("a"), Set.of(), new Random(0));

        assertEquals(ids("a"), ordered);
    }
}
