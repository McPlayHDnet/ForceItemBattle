package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Who counts as playing, and what the roster hands out.
 *
 * <p>Kept apart from {@link RosterTest}, which is pure: the admission matrix needs no server, but a
 * roster entry needs a {@code Player}.
 *
 * <p>"Absent" and "spectating" are one answer here. They were written out as
 * {@code x == null || x.isSpectator()} at thirteen call sites, and one of them had the polarity the
 * other way round — which is the case for stating the rule once.
 */
class RosterParticipationTest {

    private ServerMock server;
    private Roster roster;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.roster = new Roster();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private ForceItemPlayer join(String name, boolean spectating) {
        PlayerMock player = this.server.addPlayer(name);
        ForceItemPlayer entry = new ForceItemPlayer(player, Material.DIRT, 0, 0);
        entry.setSpectator(spectating);
        this.roster.add(player.getUniqueId(), entry);
        return entry;
    }

    @Nested
    class Participant {

        @Test
        void someoneOnTheRosterAndPlayingIsPresent() {
            ForceItemPlayer entry = join("Understudy1", false);

            assertSame(entry, roster.participant(entry.player().getUniqueId()).orElseThrow());
        }

        /** A spectator holds an entry, which is exactly why the entry alone is not the answer. */
        @Test
        void aSpectatorIsAbsentEvenThoughTheyHoldAnEntry() {
            ForceItemPlayer entry = join("Understudy2", true);
            UUID uuid = entry.player().getUniqueId();

            assertTrue(roster.contains(uuid), "the entry is still on the roster");
            assertEquals(Optional.empty(), roster.participant(uuid));
        }

        /** Someone who joined after the countdown froze the roster holds no entry at all. */
        @Test
        void someoneWithNoEntryIsAbsent() {
            assertEquals(Optional.empty(), roster.participant(UUID.randomUUID()));
        }
    }

    @Nested
    class IsPlaying {

        @Test
        void nullIsNotPlaying() {
            assertFalse(Roster.isPlaying(null));
        }

        @Test
        void aSpectatorIsNotPlaying() {
            assertFalse(Roster.isPlaying(join("Understudy1", true)));
        }

        @Test
        void aParticipantIsPlaying() {
            assertTrue(Roster.isPlaying(join("Understudy1", false)));
        }
    }

    @Nested
    class TheRoll {

        @Test
        void isNotStructurallyModifiableFromOutside() {
            ForceItemPlayer entry = join("Understudy1", false);

            assertThrows(UnsupportedOperationException.class,
                    () -> roster.players().remove(entry.player().getUniqueId()));
            assertThrows(UnsupportedOperationException.class,
                    () -> roster.players().clear());
        }

        /** A view, not a snapshot: it must still reflect a later arrival. */
        @Test
        void reflectsLaterArrivals() {
            var view = roster.players();
            assertTrue(view.isEmpty());

            join("Understudy1", false);

            assertEquals(1, view.size());
        }

        @Test
        void includesSpectators() {
            join("Understudy1", false);
            join("Understudy2", true);

            assertEquals(2, roster.players().size());
        }
    }
}
