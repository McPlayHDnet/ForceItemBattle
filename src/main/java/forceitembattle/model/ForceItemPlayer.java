package forceitembattle.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * One participant in the current round.
 *
 * <h2>Two accessor families, on purpose</h2>
 *
 * In a team game the item, the score and the joker pool live on the {@link Team}, not on the
 * player — but almost every caller only wants "the value that applies to this player right now"
 * and does not care which of the two owns it. So there are two families:
 *
 * <ul>
 *   <li><b>{@code active*}</b> — the value in effect: the team's in a team game, this player's own
 *       otherwise. This is what callers want in nearly every case.</li>
 *   <li><b>plain ({@code currentMaterial()}, {@code currentScore()}, …)</b> — this player's own
 *       field, ignoring the team. Only correct where the team has already been ruled out, or where
 *       the per-player value is genuinely the subject (solo placements, the raw roster).</li>
 * </ul>
 *
 * These used to be {@code getCurrentMaterial()} (team-aware) and {@code currentMaterial()} (not),
 * a one-character difference that decided which of two values you got. Callers could not tell them
 * apart at a glance and several hand-rolled the team branch that the getter already did.
 *
 * <p>Note the deliberate asymmetry: reads route through the team, but the Lombok setters always
 * write this player's own field. Mutating something the team owns goes through the routed mutators
 * ({@link #spendJoker()}, {@link #recordFoundItem(ForceItem)}), never a setter.
 */
public class ForceItemPlayer {

    @Setter
    private Player player;
    private List<ForceItem> foundItems;
    @Setter
    private Material currentMaterial;
    @Setter
    private Material nextMaterial;
    @Setter
    private Material previousMaterial;
    @Setter
    private int remainingJokers;
    @Setter
    private int currentScore;
    @Setter
    private Team currentTeam;
    @Setter
    private int backToBackStreak;
    @Setter
    private int itemStreak;
    @Setter
    private long lastItemAssignedAt;
    @Setter
    private boolean isSpectator;
    /**
     * Whether this player already received the round setup (gamemode, jokers, tools, backpack).
     *
     * The countdown-end pass only reaches players who are online at that instant, so a player who
     * was disconnected then gets the same setup when they rejoin. This flag is what keeps that from
     * handing out a second set of jokers to everyone else.
     */
    @Setter
    private boolean startSetupApplied;
    @Setter
    private boolean lastItemWasSkipped;
    @Setter
    private Material lastSkippedMaterial;

    public ForceItemPlayer(Player player, Material currentMaterial, int remainingJokers, int currentScore) {
        this.player = player;
        this.foundItems = new ArrayList<>();
        this.currentMaterial = currentMaterial;
        this.remainingJokers = remainingJokers;
        this.currentScore = currentScore;
        this.lastItemWasSkipped = false;
        this.lastSkippedMaterial = null;
    }

    public Player player() {
        return player;
    }

    public List<ForceItem> foundItems() {
        return Collections.unmodifiableList(foundItems);
    }

    public void addFoundItemToList(ForceItem forceItem) {
        if (forceItem != null) {
            this.foundItems.add(forceItem);
        }
    }

    // ==================== OWN VALUES (this player's fields, team ignored) ====================

    public Material currentMaterial() {
        return currentMaterial;
    }

    public Material nextMaterial() {
        return nextMaterial;
    }

    @Nullable
    public Material previousMaterial() {
        return previousMaterial;
    }

    public int remainingJokers() {
        return remainingJokers;
    }

    public int currentScore() {
        return currentScore;
    }

    public long lastItemAssignedAt() {
        return lastItemAssignedAt;
    }

    // ==================== ACTIVE VALUES (the team's in a team game) ====================

    /** The force item this player is currently hunting. */
    public Material activeMaterial() {
        return currentTeam != null ? currentTeam.getCurrentMaterial() : currentMaterial;
    }

    /** The item queued behind the current one, shown by the CHAIN setting. */
    public Material activeNextMaterial() {
        return currentTeam != null ? currentTeam.getNextMaterial() : nextMaterial;
    }

    /** The item held before the current one, or {@code null} at the start of a round. */
    @Nullable
    public Material activePreviousMaterial() {
        return currentTeam != null ? currentTeam.getPreviousMaterial() : previousMaterial;
    }

    /** Skips left to spend — the shared team pool in a team game. */
    public int activeJokers() {
        return currentTeam != null ? currentTeam.getRemainingJokers() : remainingJokers;
    }

    /** Score on the board for this player — the shared team score in a team game. */
    public int activeScore() {
        return currentTeam != null ? currentTeam.getCurrentScore() : currentScore;
    }

    /** When the current item was handed out, for measuring how long it took to find. */
    public long activeItemAssignedAt() {
        return currentTeam != null ? currentTeam.getLastItemAssignedAt() : lastItemAssignedAt;
    }

    // ==================== TEAM ====================

    public Team currentTeam() {
        return currentTeam;
    }

    public boolean isInTeam() {
        return currentTeam != null;
    }

    /**
     * The other member of this player's team, or empty when playing solo. Teams are pairs, so
     * "the teammate" is well defined; if a team ever held more than two, this returns the first
     * other member, which is what every call site did by hand before.
     */
    public Optional<ForceItemPlayer> teammate() {
        return currentTeam == null ? Optional.empty() : currentTeam.teammateOf(this);
    }

    /**
     * Everyone who shares this player's item and score: the whole team in a team game, just this
     * player when solo. Lets a caller announce or reward "the people this find belongs to" without
     * branching on the mode.
     */
    public List<ForceItemPlayer> squad() {
        return currentTeam == null ? List.of(this) : currentTeam.getPlayers();
    }

    // ==================== ROUTED MUTATORS ====================

    /**
     * Spends one skip from whichever pool this player draws on, and returns what is left.
     *
     * The pool is shared in a team game, so this must not be a plain setter call — writing
     * {@code setRemainingJokers()} on a player in a team updates a field nobody reads.
     */
    public int spendJoker() {
        if (currentTeam != null) {
            int left = Math.max(0, activeJokers() - 1);
            currentTeam.setRemainingJokers(left);
            return left;
        }
        this.remainingJokers = Math.max(0, this.remainingJokers - 1);
        return this.remainingJokers;
    }

    /**
     * Credits a found item to whoever owns the score: the team in a team game, this player
     * otherwise. The item lands in the same owner's found-list, which is what the match history
     * and the result screen read back.
     */
    public void recordFoundItem(ForceItem forceItem) {
        if (currentTeam != null) {
            currentTeam.setCurrentScore(activeScore() + 1);
            currentTeam.addFoundItemToList(forceItem);
            return;
        }
        this.currentScore = activeScore() + 1;
        addFoundItemToList(forceItem);
    }

    // ==================== MISC ====================

    public int backToBackStreak() {
        return backToBackStreak;
    }

    public int itemStreak() {
        return itemStreak;
    }

    public boolean isSpectator() {
        return isSpectator;
    }

    public boolean isStartSetupApplied() {
        return startSetupApplied;
    }

    public boolean isLastItemWasSkipped() {
        return lastItemWasSkipped;
    }

    public Material getLastSkippedMaterial() {
        return lastSkippedMaterial;
    }

}
