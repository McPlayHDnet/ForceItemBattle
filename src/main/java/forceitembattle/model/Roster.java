package forceitembattle.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /** Whether this UUID holds a place in the current round, spectating or not. */
    public boolean contains(UUID uuid) {
        return this.players.containsKey(uuid);
    }

    /**
     * Whoever is <em>playing</em> this round under this UUID.
     *
     * <p>Empty covers both shapes of "watching rather than playing": someone who took the spectate
     * toggle keeps a roster entry with the flag set, and someone who connected after the countdown
     * froze the roster holds no entry at all. Callers cannot tell those apart and do not need to —
     * which is the whole point, because the pair was previously written out at thirteen sites as
     * {@code x == null || x.isSpectator()} and one of them had the polarity the other way round.
     */
    public Optional<ForceItemPlayer> participant(UUID uuid) {
        return Optional.ofNullable(this.players.get(uuid)).filter(Roster::isPlaying);
    }

    /**
     * The same rule for a caller that already holds the entry rather than a UUID.
     *
     * <p>Exists because {@code PlayerStatsWrite} receives a {@code ForceItemPlayer} as a parameter,
     * so it has nothing to look up. Static and null-tolerant so both halves of the rule live here
     * rather than half here and half at that call site.
     */
    public static boolean isPlaying(@Nullable ForceItemPlayer forceItemPlayer) {
        return forceItemPlayer != null && !forceItemPlayer.isSpectator();
    }

    /**
     * Everyone on the roster, spectators included. Unmodifiable.
     *
     * <p>A view rather than a copy: {@code ScoreboardManager} reads this on every find, so a
     * defensive copy would allocate a map per scoreboard update. Nothing adds or removes through
     * here — {@link #add} and {@link #remove} are the writers.
     */
    public Map<UUID, ForceItemPlayer> players() {
        return Collections.unmodifiableMap(this.players);
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
