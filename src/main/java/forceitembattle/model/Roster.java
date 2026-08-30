package forceitembattle.model;

/**
 * Who holds a place in the current round, and what happens when someone arrives or leaves.
 *
 * <p>The roster itself is still {@code Gamemanager.forceItemPlayerMap} — moving the map would
 * relocate 86 call sites without concentrating anything, which is the deletion test answering no.
 * What moved here is the <em>rule</em>, which was the part nobody could reach: it lived inside a
 * Bukkit join handler, interleaved with game modes and item stacks.
 *
 * <p>Two questions, both pure. No Bukkit, no plugin, no roster map — a game state and a yes/no is
 * everything either of them needs, which is exactly why they were worth lifting out.
 */
public final class Roster {

    private Roster() {
    }

    /**
     * What an arriving player becomes.
     *
     * <p>An existing roster entry always wins. Someone who disconnected during the countdown still
     * owns their team, their force item and their score, so they come back as the participant they
     * were and never as a freshly created spectator — that precedence is the first thing this
     * method encodes and the easiest thing to lose when it is written as a nested if.
     *
     * @param onRoster whether this player already holds a place in the current round
     * @param state    where the round is
     */
    public static Admission admit(boolean onRoster, GameState state) {
        if (onRoster) {
            return switch (state) {
                case MID_GAME, PAUSED_GAME -> Admission.RETURNING_PARTICIPANT;
                case END_GAME -> Admission.RESULT_SCREEN;
                case PRE_GAME, STARTING -> Admission.RECONNECTING_BEFORE_START;
            };
        }

        return switch (state) {
            case MID_GAME, PAUSED_GAME -> Admission.LATE_SPECTATOR;
            case STARTING -> Admission.COUNTDOWN_SPECTATOR;
            // END_GAME lands here with PRE_GAME: someone who never played this round is not on the
            // result screen, so the lobby is what is left for them.
            case PRE_GAME, END_GAME -> Admission.LOBBY;
        };
    }

    /**
     * Whether a player leaving now gives up their place.
     *
     * <p>Deliberately false during STARTING: once the countdown runs, teams and force items are
     * already assigned, and dropping the player would tear their team apart and cost them the
     * round. They keep their spot and are restored on rejoin as a
     * {@link Admission#RETURNING_PARTICIPANT}. This is the mirror of {@code admit}'s
     * roster-entry-wins rule and belongs beside it.
     */
    public static boolean releasesSpotOnQuit(GameState state) {
        return state == GameState.PRE_GAME || state == GameState.END_GAME;
    }
}
