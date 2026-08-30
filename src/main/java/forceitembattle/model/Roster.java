package forceitembattle.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Who holds a place in the current round: the roll itself, and the rules for arriving and leaving.
 *
 * <h2>Why the map lives here now</h2>
 *
 * <p>It was {@code Gamemanager.forceItemPlayerMap}, and an earlier pass left it there on the
 * grounds that moving it would relocate call sites without concentrating anything. That was the
 * wrong reading, and measuring the dependency graph is what showed it: of the twenty edges pointing
 * <em>into</em> {@code Gamemanager} from the managers it was mutually entangled with, eleven were
 * asking for this map and seven for the round's phase. Nobody depended on {@code Gamemanager} for
 * what it <em>does</em> — they depended on it for two pieces of state it happened to hold, and
 * holding them is what made seven dependency cycles.
 *
 * <p>So this is not a tidying move. It is the one that makes the graph acyclic: a module every
 * other module needs has to depend on nothing, and this depends on nothing — no Bukkit, no plugin,
 * no managers. Keep it that way.
 *
 * <p>The roster is frozen once {@code /start} begins its countdown. Someone joining after that is a
 * spectator for the round; someone leaving keeps their spot and their assignment.
 */
public final class Roster {

    private final Map<UUID, ForceItemPlayer> players = new HashMap<>();

    /** Whoever holds this UUID's place, or {@code null} if they hold none. */
    @Nullable
    public ForceItemPlayer get(UUID uuid) {
        return this.players.get(uuid);
    }

    /** Whether this UUID holds a place in the current round. */
    public boolean contains(UUID uuid) {
        return this.players.containsKey(uuid);
    }

    /** Everyone on the roster, spectators included. */
    public Map<UUID, ForceItemPlayer> players() {
        return this.players;
    }

    public void add(UUID uuid, ForceItemPlayer forceItemPlayer) {
        this.players.put(uuid, forceItemPlayer);
    }

    public void remove(UUID uuid) {
        this.players.remove(uuid);
    }

    /**
     * Every score owner playing this round, each appearing once: one per solo player, one per team
     * however many members it has. Spectators hold no stake in a round and are left out.
     *
     * <p>The de-duplication is the point. Anything that acts on "the thing that scores" — dealing
     * the opening pair, skipping the whole server's item — has to run once per owner, and the
     * roster hands out one entry per player.
     */
    public List<ScoreOwner> activeScoreOwners() {
        return this.players.values().stream()
                .filter(forceItemPlayer -> !forceItemPlayer.isSpectator())
                .map(ForceItemPlayer::scoreOwner)
                .distinct()
                .toList();
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
