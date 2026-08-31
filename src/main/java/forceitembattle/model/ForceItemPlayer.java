package forceitembattle.model;

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
 * <ul>
 *   <li><b>{@code active*}</b> — the value in effect, read from the current {@link ScoreOwner}:
 *       the team's in a team game, this player's own otherwise. What callers want in nearly
 *       every case.</li>
 *   <li><b>plain ({@code currentMaterial()}, {@code currentScore()}, …)</b> — this player's own
 *       values, ignoring the team. Only correct where the team has already been ruled out, or
 *       where the per-player value is genuinely the subject.</li>
 * </ul>
 *
 * <p>{@link #setCurrentTeam(Team)} is the one place the choice is made. Full account, including
 * why {@code backToBackStreak} stays a plain field here, in {@code CONTEXT.md § Score Owner}.
 */
public class ForceItemPlayer {

    @Setter
    private Player player;

    /** This player's own item, score, jokers and found-list. Never replaced, never null. */
    private final SoloScore own;

    /**
     * Whoever owns the score right now: {@link #own} when solo, the team once one is assigned.
     * Set only by {@link #setCurrentTeam(Team)}.
     */
    private ScoreOwner scoreOwner;

    private Team currentTeam;
    @Setter
    private int backToBackStreak;
    @Setter
    private int itemStreak;
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
        this.own = new SoloScore(this, currentMaterial, remainingJokers, currentScore);
        this.scoreOwner = this.own;
        this.lastItemWasSkipped = false;
        this.lastSkippedMaterial = null;
    }

    public Player player() {
        return player;
    }

    public List<ForceItem> foundItems() {
        return own.foundItems();
    }

    public void addFoundItemToList(ForceItem forceItem) {
        own.addFoundItem(forceItem);
    }

    // --- plain family: this player's own values, team or not -------------------------------

    public Material currentMaterial() {
        return own.material();
    }

    public Material nextMaterial() {
        return own.nextMaterial();
    }

    @Nullable
    public Material previousMaterial() {
        return own.previousMaterial();
    }

    public int remainingJokers() {
        return own.jokers();
    }

    public int currentScore() {
        return own.score();
    }

    public long lastItemAssignedAt() {
        return own.itemAssignedAt();
    }

    // Package-private on purpose: outside model/ there is no reason to write a player's own
    // values, since everything now addresses the ScoreOwner. These remain for the model's tests.

    void setCurrentMaterial(Material currentMaterial) {
        own.setCurrentMaterial(currentMaterial);
    }

    void setNextMaterial(Material nextMaterial) {
        own.setNextMaterial(nextMaterial);
    }

    void setPreviousMaterial(Material previousMaterial) {
        own.setPreviousMaterial(previousMaterial);
    }

    void setRemainingJokers(int remainingJokers) {
        own.setRemainingJokers(remainingJokers);
    }

    void setCurrentScore(int currentScore) {
        own.setCurrentScore(currentScore);
    }

    void setLastItemAssignedAt(long lastItemAssignedAt) {
        own.setLastItemAssignedAt(lastItemAssignedAt);
    }

    // --- active family: whoever owns the score right now -----------------------------------

    /** The force item this player is currently hunting. */
    public Material activeMaterial() {
        return scoreOwner.material();
    }

    /** The item queued behind the current one, shown by the CHAIN setting. */
    public Material activeNextMaterial() {
        return scoreOwner.nextMaterial();
    }

    /** The item held before the current one, or {@code null} at the start of a round. */
    @Nullable
    public Material activePreviousMaterial() {
        return scoreOwner.previousMaterial();
    }

    /** Skips left to spend — the shared team pool in a team game. */
    public int activeJokers() {
        return scoreOwner.jokers();
    }

    /** Score on the board for this player — the shared team score in a team game. */
    public int activeScore() {
        return scoreOwner.score();
    }

    /** When the current item was handed out, for measuring how long it took to find. */
    public long activeItemAssignedAt() {
        return scoreOwner.itemAssignedAt();
    }

    /** Whoever owns this player's score right now. The one thing the active family reads. */
    public ScoreOwner scoreOwner() {
        return scoreOwner;
    }

    public Team currentTeam() {
        return currentTeam;
    }

    /**
     * Joins or leaves a team, and repoints the score owner with it — the single place the choice
     * between the two families is made. Passing {@code null} restores this player's own values,
     * which are still exactly where they were before the team was assigned.
     */
    public void setCurrentTeam(Team currentTeam) {
        this.currentTeam = currentTeam;
        this.scoreOwner = currentTeam != null ? currentTeam : this.own;
    }

    public boolean isInTeam() {
        return currentTeam != null;
    }

    /**
     * The other member of this player's team, or empty when playing solo. Teams are pairs, so
     * "the teammate" is well defined; a team holding more than two yields the first other member.
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
        return scoreOwner.members();
    }

    /**
     * Spends one skip from whichever pool this player draws on, and returns what is left. The pool
     * is shared in a team game, which is why this exists: {@code setRemainingJokers()} writes the
     * player's own pool, which nobody reads while they are on a team.
     */
    public int spendJoker() {
        return scoreOwner.spendJoker();
    }

    /**
     * Credits a found item to whoever owns the score. The item lands in that owner's found-list,
     * which is what the match history and the result screen read back.
     */
    public void recordFoundItem(ForceItem forceItem) {
        scoreOwner.record(forceItem);
    }

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
