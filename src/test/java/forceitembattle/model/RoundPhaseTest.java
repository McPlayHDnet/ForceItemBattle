package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The distinction between "the round is running" and "the round is happening".
 *
 * <p>{@link GameState#PAUSED_GAME} is a sibling of {@link GameState#MID_GAME} rather than a flag on
 * it, so the old {@code isMidGame()} silently meant "and not while paused" — a decision fifty-one
 * call sites were making by omission, none of them recording whether they meant it.
 *
 * <p>The rule lives on {@link GameState} rather than on {@code Gamemanager}: which phases count as
 * a round is a fact about the phases, and putting it there is what lets it be asked — and asserted —
 * without a plugin. {@code Gamemanager} delegates.
 */
class RoundPhaseTest {

    @Test
    void aPausedRoundIsNotRunning() {
        assertFalse(GameState.PAUSED_GAME.roundRunning());
    }

    /**
     * The whole point. A pause stops this plugin's clock, not the world's — blocks tick, primed TNT
     * detonates, lava flows, fire spreads — so anything guarding the world rather than gating play
     * has to still consider the round to be happening.
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
     * The countdown is deliberately outside both. Teams and force items are assigned by then — see
     * {@link GameState#STARTING} — but nothing is playable yet, so a gate that opened here would
     * let a player act on an item before the round began.
     */
    @Test
    void theCountdownIsNeitherRunningNorInProgress() {
        assertFalse(GameState.STARTING.roundRunning());
        assertFalse(GameState.STARTING.roundInProgress());
    }
}
