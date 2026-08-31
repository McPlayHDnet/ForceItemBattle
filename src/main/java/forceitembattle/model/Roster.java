package forceitembattle.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Who holds a place in the current round: the roll itself, and the rules for arriving and leaving.
 * See {@code CONTEXT.md § Roster} for the admission table and the freeze rule.
 *
 * <p>This depends on nothing — no Bukkit, no plugin, no managers — and that is what makes the
 * manager graph acyclic. Keep it that way.
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
     * What an arriving player becomes. An existing roster entry always wins over every default.
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
     * Whether a player leaving now gives up their place. Deliberately false during STARTING: teams
     * and force items are already assigned by then, so they keep their spot and come back as a
     * {@link Admission#RETURNING_PARTICIPANT}. The mirror of {@code admit}, and belongs beside it.
     */
    public static boolean releasesSpotOnQuit(GameState state) {
        return state == GameState.PRE_GAME || state == GameState.END_GAME;
    }
}
