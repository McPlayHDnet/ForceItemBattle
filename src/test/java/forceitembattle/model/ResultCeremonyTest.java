package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.model.ResultCeremony.Reveal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link ResultCeremony}: the walk, the archive, and which turn is the winner's.
 *
 * <p>Headless, because the ceremony depends on nothing — no Bukkit behaviour, no plugin, no
 * managers. Before it existed this was a {@code public int place} on a command, a match id compared
 * on every invocation, and two maps on {@code Gamemanager}, none of which could be asked a question
 * without standing up a server.
 *
 * <p>The pages are never constructed here: {@code ItemStack} appears only as a type argument, and
 * an empty map is enough to prove the archive stores and returns what it is given. Constructing one
 * would need a running server — see {@code HeadlessBoundaryTest}.
 */
class ResultCeremonyTest {

    /** A Score Owner is all the ceremony needs; a stub keeps the test off the roster entirely. */
    private static ScoreOwner owner(String name) {
        return new StubOwner(name);
    }

    private static Map<ScoreOwner, Integer> placesBestFirst(ScoreOwner... owners) {
        Map<ScoreOwner, Integer> places = new LinkedHashMap<>();
        for (int index = 0; index < owners.length; index++) {
            places.put(owners[index], index + 1);
        }
        return places;
    }

    @Nested
    class TheWalk {

        /** Best-first in, worst-first out: the winner is revealed last. */
        @Test
        void revealsWorstFirst() {
            ScoreOwner first = owner("first");
            ScoreOwner second = owner("second");
            ScoreOwner third = owner("third");

            ResultCeremony ceremony = new ResultCeremony();
            ceremony.beginFor(UUID.randomUUID(),
                    ResultCeremony.orderFrom(placesBestFirst(first, second, third)));

            assertSame(third, ceremony.nextReveal().orElseThrow().owner());
            assertSame(second, ceremony.nextReveal().orElseThrow().owner());
            assertSame(first, ceremony.nextReveal().orElseThrow().owner());
        }

        @Test
        void carriesThePlaceEachOwnerFinishedIn() {
            ScoreOwner first = owner("first");
            ScoreOwner second = owner("second");

            ResultCeremony ceremony = new ResultCeremony();
            ceremony.beginFor(UUID.randomUUID(), ResultCeremony.orderFrom(placesBestFirst(first, second)));

            assertEquals(2, ceremony.nextReveal().orElseThrow().place());
            assertEquals(1, ceremony.nextReveal().orElseThrow().place());
        }

        /** Tied owners share a place, and the ceremony must not renumber them. */
        @Test
        void tiedPlacesArePreserved() {
            Map<ScoreOwner, Integer> tied = new LinkedHashMap<>();
            tied.put(owner("a"), 1);
            tied.put(owner("b"), 1);
            tied.put(owner("c"), 2);

            List<Reveal> order = ResultCeremony.orderFrom(tied);

            assertEquals(List.of(2, 1, 1), order.stream().map(Reveal::place).toList());
        }

        @Test
        void runsOutAfterTheWinner() {
            ResultCeremony ceremony = new ResultCeremony();
            ceremony.beginFor(UUID.randomUUID(), ResultCeremony.orderFrom(placesBestFirst(owner("only"))));

            assertTrue(ceremony.nextReveal().isPresent());
            assertEquals(Optional.empty(), ceremony.nextReveal());
            assertTrue(ceremony.isFinished());
        }

        @Test
        void anEmptyRoundIsFinishedImmediately() {
            ResultCeremony ceremony = new ResultCeremony();
            ceremony.beginFor(UUID.randomUUID(), List.of());

            assertTrue(ceremony.isFinished());
            assertEquals(Optional.empty(), ceremony.nextReveal());
        }

        /**
         * A new match restarts the walk. This used to be a match id compared against a field on the
         * command, which is why the counter lived there at all.
         */
        @Test
        void beginningAgainRestartsTheWalk() {
            ScoreOwner first = owner("first");
            ResultCeremony ceremony = new ResultCeremony();

            ceremony.beginFor(UUID.randomUUID(), ResultCeremony.orderFrom(placesBestFirst(first)));
            ceremony.nextReveal();
            assertTrue(ceremony.isFinished());

            ceremony.beginFor(UUID.randomUUID(), ResultCeremony.orderFrom(placesBestFirst(first)));

            assertFalse(ceremony.isFinished());
            assertSame(first, ceremony.nextReveal().orElseThrow().owner());
        }
    }

    @Nested
    class TheWinnerHook {

        /** Exactly one reveal is the last, and it is the one handed out last. */
        @Test
        void onlyTheFinalRevealIsMarkedLast() {
            List<Reveal> order = ResultCeremony.orderFrom(
                    placesBestFirst(owner("first"), owner("second"), owner("third")));

            assertEquals(List.of(false, false, true), order.stream().map(Reveal::last).toList());
        }

        @Test
        void aSingleOwnerIsImmediatelyTheLast() {
            List<Reveal> order = ResultCeremony.orderFrom(placesBestFirst(owner("only")));

            assertEquals(List.of(true), order.stream().map(Reveal::last).toList());
        }
    }

    @Nested
    class TheArchive {

        @Test
        void storesAndReturnsWhatItWasGiven() {
            ScoreOwner subject = owner("subject");
            Map<Integer, Map<Integer, ItemStack>> pages = Map.of();

            ResultCeremony ceremony = new ResultCeremony();
            ceremony.archive(subject, pages);

            assertSame(pages, ceremony.pagesFor(subject).orElseThrow());
        }

        @Test
        void hasNothingForAnOwnerThatNeverFinished() {
            ResultCeremony ceremony = new ResultCeremony();

            assertEquals(Optional.empty(), ceremony.pagesFor(owner("stranger")));
        }

        @Test
        void toleratesANullOwner() {
            ResultCeremony ceremony = new ResultCeremony();

            assertEquals(Optional.empty(), ceremony.pagesFor(null));
        }

        /** Two owners are two entries even if they would compare equal — the archive is by identity. */
        @Test
        void keysByIdentityNotEquality() {
            ScoreOwner one = owner("same");
            ScoreOwner two = owner("same");
            Map<Integer, Map<Integer, ItemStack>> pages = Map.of();

            ResultCeremony ceremony = new ResultCeremony();
            ceremony.archive(one, pages);

            assertTrue(ceremony.pagesFor(one).isPresent());
            assertEquals(Optional.empty(), ceremony.pagesFor(two));
        }

        /** A new match clears it, so last round's screens cannot be reopened in this one. */
        @Test
        void beginningAgainDiscardsTheOldScreens() {
            ScoreOwner subject = owner("subject");

            ResultCeremony ceremony = new ResultCeremony();
            ceremony.archive(subject, Map.of());
            ceremony.beginFor(UUID.randomUUID(), List.of());

            assertEquals(Optional.empty(), ceremony.pagesFor(subject));
        }
    }

    /** Enough of a Score Owner to be walked and keyed; nothing here is exercised. */
    private record StubOwner(String name) implements ScoreOwner {

        @Override
        public org.bukkit.Material material() {
            return null;
        }

        @Override
        public org.bukkit.Material nextMaterial() {
            return null;
        }

        @Override
        public org.bukkit.Material previousMaterial() {
            return null;
        }

        @Override
        public int score() {
            return 0;
        }

        @Override
        public int jokers() {
            return 0;
        }

        @Override
        public long itemAssignedAt() {
            return 0;
        }

        @Override
        public int spendJoker() {
            return 0;
        }

        @Override
        public void setJokers(int jokers) {
        }

        @Override
        public void startRound(org.bukkit.Material current, org.bukkit.Material next, long at) {
        }

        @Override
        public void advance(org.bukkit.Material next, long at) {
        }

        @Override
        public void assignMaterials(org.bukkit.Material current, org.bukkit.Material next) {
        }

        @Override
        public void record(ForceItem forceItem) {
        }

        @Override
        public List<ForceItem> foundItems() {
            return List.of();
        }

        @Override
        public List<ForceItemPlayer> members() {
            return List.of();
        }
    }
}
