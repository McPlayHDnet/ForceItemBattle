package forceitembattle.model;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * One participant in the current round. See {@code CONTEXT.md § Score Owner} for the two accessor
 * families: {@code active*} reads through the {@link ScoreOwner}, the plain ones ignore the team.
 * {@link #setCurrentTeam(Team)} is the one place the choice is made.
 */
public class ForceItemPlayer {

    @Setter
    private Player player;

    /** This player's own item, score, jokers and found-list. Never replaced, never null. */
    private final SoloScore own;

    /** {@link #own} when solo, the team once one is assigned. Set only by {@link #setCurrentTeam}. */
    private ScoreOwner scoreOwner;

    private Team currentTeam;
    @Setter
    private int itemStreak;
    @Setter
    private boolean isSpectator;
    /** Stops the rejoin path handing a second set of jokers to everyone already set up. */
    @Setter
    private boolean startSetupApplied;

    public ForceItemPlayer(Player player, Material currentMaterial, int remainingJokers, int currentScore) {
        this.player = player;
        this.own = new SoloScore(this, currentMaterial, remainingJokers, currentScore);
        this.scoreOwner = this.own;
    }

    public Player player() {
        return player;
    }

    public List<ForceItem> foundItems() {
        return own.foundItems();
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

    // Package-private on purpose: outside model/ everything addresses the ScoreOwner instead.

    void setNextMaterial(Material nextMaterial) {
        own.setNextMaterial(nextMaterial);
    }

    void setPreviousMaterial(Material previousMaterial) {
        own.setPreviousMaterial(previousMaterial);
    }

    void setCurrentScore(int currentScore) {
        own.setCurrentScore(currentScore);
    }

    void setLastItemAssignedAt(long lastItemAssignedAt) {
        own.setLastItemAssignedAt(lastItemAssignedAt);
    }

    // --- active family: whoever owns the score right now -----------------------------------

    public Material activeMaterial() {
        return scoreOwner.material();
    }

    public Material activeNextMaterial() {
        return scoreOwner.nextMaterial();
    }

    @Nullable
    public Material activePreviousMaterial() {
        return scoreOwner.previousMaterial();
    }

    /** Skips left to spend — the shared team pool in a team game. */
    public int activeJokers() {
        return scoreOwner.jokers();
    }

    /** Score on the board — the shared team score in a team game. */
    public int activeScore() {
        return scoreOwner.score();
    }

    public long activeItemAssignedAt() {
        return scoreOwner.itemAssignedAt();
    }

    public ScoreOwner scoreOwner() {
        return scoreOwner;
    }

    public Team currentTeam() {
        return currentTeam;
    }

    /**
     * Joins or leaves a team, repointing the score owner with it. Passing {@code null} restores this
     * player's own values, which are still exactly where they were before the team was assigned.
     */
    public void setCurrentTeam(Team currentTeam) {
        this.currentTeam = currentTeam;
        this.scoreOwner = currentTeam != null ? currentTeam : this.own;
    }

    public boolean isInTeam() {
        return currentTeam != null;
    }

    /** Teams are pairs; a team holding more than two yields the first other member. */
    public Optional<ForceItemPlayer> teammate() {
        return currentTeam == null ? Optional.empty() : currentTeam.teammateOf(this);
    }

    /** Everyone who shares this player's item and score: the whole team, or just them when solo. */
    public List<ForceItemPlayer> squad() {
        return scoreOwner.members();
    }

    /** From whichever pool this player draws on, which is not their own while they are on a team. */
    public int spendJoker() {
        return scoreOwner.spendJoker();
    }

    public void recordFoundItem(ForceItem forceItem) {
        scoreOwner.record(forceItem);
    }

    /** No plain counterpart: there is only one streak, so there is nothing to disambiguate. */
    public int backToBackStreak() {
        return scoreOwner.backToBackStreak();
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

}
