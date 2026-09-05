package forceitembattle.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Who holds a place in the current round, and the rules for arriving and leaving.
 *
 * <p><b>Depends on nothing</b> — no Bukkit, no plugin, no managers. That is what keeps the manager
 * graph acyclic; keep it that way.
 */
public final class Roster {

    private final Map<UUID, ForceItemPlayer> players = new HashMap<>();

    @Nullable
    public ForceItemPlayer get(UUID uuid) {
        return this.players.get(uuid);
    }

    /** Whether this UUID holds a place in the current round, spectating or not. */
    public boolean contains(UUID uuid) {
        return this.players.containsKey(uuid);
    }

    /**
     * Whoever is <em>playing</em> under this UUID. Empty covers both shapes of watching rather than
     * playing: the spectate toggle keeps a roster entry, a late joiner has none.
     */
    public Optional<ForceItemPlayer> participant(UUID uuid) {
        return Optional.ofNullable(this.players.get(uuid)).filter(Roster::isPlaying);
    }

    /** The same rule for a caller holding the entry rather than a UUID. Null-tolerant on purpose. */
    public static boolean isPlaying(@Nullable ForceItemPlayer forceItemPlayer) {
        return forceItemPlayer != null && !forceItemPlayer.isSpectator();
    }

    /** Everyone, spectators included. A view, not a copy: this is read on every find. */
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
     * Every score owner playing, each once: one per solo player, one per team. The de-duplication is
     * the point — the roster holds an entry per player, and owner-level work must run once per owner.
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

    /** False during STARTING on purpose: teams and items are assigned, so a quitter keeps their spot. */
    public static boolean releasesSpotOnQuit(GameState state) {
        return state == GameState.PRE_GAME || state == GameState.END_GAME;
    }
}
