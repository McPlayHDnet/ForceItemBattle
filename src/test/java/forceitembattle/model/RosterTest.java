package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The whole admission matrix: every game state, on the roster and off it.
 *
 * <p>Ten combinations, and the only way to ask about any of them used to be to join a server in
 * that state — the rule was a five-branch tree inside a Bukkit join handler whose every leaf built
 * an {@code ItemStack} or set a game mode.
 */
class RosterTest {

    @Nested
    class AlreadyOnTheRoster {

        /**
         * The precedence that matters most and is easiest to lose: an existing entry wins over
         * every default. Someone who disconnected during the countdown still owns their team, their
         * force item and their score.
         */
        @ParameterizedTest
        @EnumSource(GameState.class)
        void neverBecomesASpectator(GameState state) {
            Admission admission = Roster.admit(true, state);

            assertFalse(admission.isSpectating(),
                    "a player already on the roster is a participant, whatever the state");
            assertFalse(admission.joinsRoster(), "they are on it already");
        }

        @Test
        void aRunningRoundRestoresThemAsAParticipant() {
            assertEquals(Admission.RETURNING_PARTICIPANT, Roster.admit(true, GameState.MID_GAME));
        }

        /** A pause is still a running round; the roster does not care that the clock stopped. */
        @Test
        void aPausedRoundIsStillARunningRound() {
            assertEquals(Admission.RETURNING_PARTICIPANT, Roster.admit(true, GameState.PAUSED_GAME));
        }

        @Test
        void afterTheRoundTheyGetTheResultScreen() {
            assertEquals(Admission.RESULT_SCREEN, Roster.admit(true, GameState.END_GAME));
        }

        /**
         * The outcome that was previously a branch falling off the end of an if-chain: nothing to
         * write at all. Naming it is the point — an empty case is invisible as an omission and
         * obvious as a name.
         */
        @ParameterizedTest
        @EnumSource(value = GameState.class, names = {"PRE_GAME", "STARTING"})
        void beforeTheRoundNothingIsWritten(GameState state) {
            assertEquals(Admission.RECONNECTING_BEFORE_START, Roster.admit(true, state));
        }
    }

    @Nested
    class NotOnTheRoster {

        @ParameterizedTest
        @EnumSource(value = GameState.class, names = {"MID_GAME", "PAUSED_GAME"})
        void arrivingMidRoundMakesALateSpectator(GameState state) {
            assertEquals(Admission.LATE_SPECTATOR, Roster.admit(false, state));
        }

        @Test
        void arrivingDuringTheCountdownMakesACountdownSpectator() {
            assertEquals(Admission.COUNTDOWN_SPECTATOR, Roster.admit(false, GameState.STARTING));
        }

        @Test
        void arrivingBeforeTheRoundMakesALobbyPlayer() {
            assertEquals(Admission.LOBBY, Roster.admit(false, GameState.PRE_GAME));
        }

        /**
         * Someone who never played this round is not on the result screen, so the lobby is what is
         * left for them. Shares an outcome with PRE_GAME, which is easy to miss when it is the
         * unlabelled {@code else} at the bottom of a chain.
         */
        @Test
        void arrivingAfterTheRoundAlsoMakesALobbyPlayer() {
            assertEquals(Admission.LOBBY, Roster.admit(false, GameState.END_GAME));
        }

        /**
         * The asymmetry this candidate pins rather than fixes.
         *
         * <p>A countdown spectator gets a roster entry; a late spectator does not, and
         * {@code addPlayer} has no other caller — so a mid-round joiner has no {@code
         * ForceItemPlayer} at all, and is still not on the roster when the next round starts.
         * Production is spared because {@code scheduleReset} restarts the JVM between rounds; a
         * server playing two rounds in one session is not.
         *
         * <p>Not made consistent on purpose: a roster entry counts toward the
         * {@code forceItemPlayerMap().size() &lt; 4} head-count {@code /start} uses to decide
         * whether to build teams, so evening this up would change when teams appear. That is a
         * decision about the game.
         */
        @Test
        void aLateSpectatorIsTheOneArrivalThatCreatesNoRosterEntry() {
            assertTrue(Roster.admit(false, GameState.STARTING).joinsRoster());
            assertTrue(Roster.admit(false, GameState.PRE_GAME).joinsRoster());

            assertFalse(Roster.admit(false, GameState.MID_GAME).joinsRoster());
            assertFalse(Roster.admit(false, GameState.PAUSED_GAME).joinsRoster());
        }

        @Test
        void bothKindsOfSpectatorAreSpectating() {
            assertTrue(Roster.admit(false, GameState.MID_GAME).isSpectating());
            assertTrue(Roster.admit(false, GameState.STARTING).isSpectating());
            assertFalse(Roster.admit(false, GameState.PRE_GAME).isSpectating());
        }
    }

    @Nested
    class LeavingTheRound {

        /**
         * Deliberately false during STARTING: teams and force items are already assigned by then,
         * so dropping the player would tear their team apart and cost them the round.
         */
        @ParameterizedTest
        @EnumSource(value = GameState.class, names = {"STARTING", "MID_GAME", "PAUSED_GAME"})
        void aPlayerKeepsTheirSpotOnceTheRoundIsUnderway(GameState state) {
            assertFalse(Roster.releasesSpotOnQuit(state));
        }

        @ParameterizedTest
        @EnumSource(value = GameState.class, names = {"PRE_GAME", "END_GAME"})
        void aPlayerGivesUpTheirSpotOutsideARound(GameState state) {
            assertTrue(Roster.releasesSpotOnQuit(state));
        }

        /**
         * Quitting releases a spot in exactly the states where arriving hands out a lobby place.
         * The two rules are mirrors, which is why they sit in the same module.
         */
        @Test
        void releasingASpotMirrorsWhereArrivingMakesALobbyPlayer() {
            Set<GameState> releases = EnumSet.noneOf(GameState.class);
            Set<GameState> lobby = EnumSet.noneOf(GameState.class);

            for (GameState state : GameState.values()) {
                if (Roster.releasesSpotOnQuit(state)) {
                    releases.add(state);
                }
                if (Roster.admit(false, state) == Admission.LOBBY) {
                    lobby.add(state);
                }
            }

            assertEquals(lobby, releases);
        }
    }

    /** Every state is answered, both ways. A new GameState constant fails this rather than falling through. */
    @ParameterizedTest
    @EnumSource(GameState.class)
    void everyStateHasAnAnswer(GameState state) {
        assertTrue(Roster.admit(true, state) != null);
        assertTrue(Roster.admit(false, state) != null);
    }
}
