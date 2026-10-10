package forceitembattle.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/** Depends on nothing, which keeps the manager graph acyclic. */
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

    /** Empty for both a spectate-toggle entry and a late joiner with none. */
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

    /** One per solo player, one per team: owner-level work must run once per owner. */
    public List<ScoreOwner> activeScoreOwners() {
        return this.players.values().stream()
                .filter(forceItemPlayer -> !forceItemPlayer.isSpectator())
                .map(ForceItemPlayer::scoreOwner)
                .distinct()
                .toList();
    }

    /** An existing roster entry always wins over every default. */
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
