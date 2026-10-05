package forceitembattle.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Decides who plays with whom, avoiding pairings that already happened last round. */
public final class TeamPairing {

    /** Unreachable at realistic player counts; stops a pathological forbidden set hanging /start. */
    private static final int MAX_BRANCHES = 50_000;

    private TeamPairing() {
    }

    /** Order-independent, so {@code {a,b}} and {@code {b,a}} are the same entry. */
    public static String pairKey(UUID first, UUID second) {
        String a = first.toString();
        String b = second.toString();
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    /** Falls back to a plain shuffle when no such order exists. With an odd count the last entry is a one-player team. */
    public static List<UUID> orderAvoidingPairs(List<UUID> ids, Set<String> forbidden, Random random) {
        List<UUID> pool = new ArrayList<>(ids);
        Collections.shuffle(pool, random);

        if (forbidden.isEmpty() || pool.size() < 2) {
            return pool;
        }

        List<UUID> ordered = new ArrayList<>(pool.size());
        if (search(pool, forbidden, random, ordered, new int[] {MAX_BRANCHES})) {
            return ordered;
        }
        return pool;
    }

    /** Undoes its own moves on the way out, so a failed branch leaves both lists as it found them. */
    private static boolean search(List<UUID> remaining, Set<String> forbidden, Random random,
                                  List<UUID> ordered, int[] budget) {
        if (remaining.size() < 2) {
            ordered.addAll(remaining); // the odd one out, if there is one
            return true;
        }
        if (budget[0]-- <= 0) {
            return false;
        }

        UUID first = remaining.remove(0);

        List<Integer> candidates = new ArrayList<>(remaining.size());
        for (int index = 0; index < remaining.size(); index++) {
            candidates.add(index);
        }
        // So that when several partners are allowed the choice between them stays random.
        Collections.shuffle(candidates, random);

        for (int index : candidates) {
            UUID partner = remaining.get(index);
            if (forbidden.contains(pairKey(first, partner))) {
                continue;
            }

            remaining.remove(index);
            ordered.add(first);
            ordered.add(partner);

            if (search(remaining, forbidden, random, ordered, budget)) {
                return true;
            }

            ordered.remove(ordered.size() - 1);
            ordered.remove(ordered.size() - 1);
            remaining.add(index, partner);
        }

        remaining.add(0, first);
        return false;
    }
}
