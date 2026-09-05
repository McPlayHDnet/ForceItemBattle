package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The distinction between "the round is running" and "the round is happening".
 * {@link GameState#PAUSED_GAME} is a sibling of {@link GameState#MID_GAME} rather than a flag on it,
 * so a single predicate would silently mean "and not while paused".
 */
class RoundPhaseTest {

    @Test
    void aPausedRoundIsNotRunning() {
        assertFalse(GameState.PAUSED_GAME.roundRunning());
    }

    /**
     * A pause stops this plugin's clock, not the world's — blocks tick, primed TNT detonates, lava
     * flows — so anything guarding the world must still consider the round to be happening.
     */
    @Test
    void aPausedRoundIsStillInProgress() {
        assertTrue(GameState.PAUSED_GAME.roundInProgress());
    }

    @Test
    void theTwoAgreeEverywhereExceptOnAPause() {
        Set<GameState> disagree = EnumSet.noneOf(GameState.class);
        for (GameState state : GameState.values()) {
            if (state.roundRunning() != state.roundInProgress()) {
                disagree.add(state);
            }
        }

        assertEquals(EnumSet.of(GameState.PAUSED_GAME), disagree,
                "a pause is the only thing the two predicates disagree about");
    }

    @Test
    void neitherIsTrueBeforeOrAfterARound() {
        for (GameState state : EnumSet.of(GameState.PRE_GAME, GameState.STARTING, GameState.END_GAME)) {
            assertFalse(state.roundRunning(), state + " is not a running round");
            assertFalse(state.roundInProgress(), state + " is not a round in progress");
        }
    }

    /**
     * The countdown is deliberately outside both: teams and force items are assigned by then, but
     * nothing is playable yet, so a gate that opened here would let a player act early.
     */
    @Test
    void theCountdownIsNeitherRunningNorInProgress() {
        assertFalse(GameState.STARTING.roundRunning());
        assertFalse(GameState.STARTING.roundInProgress());
    }
}
