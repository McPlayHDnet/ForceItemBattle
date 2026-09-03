package forceitembattle.model;

import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Material;

/**
 * Whoever owns the score a find is credited to: the {@link Team} in a team game, the player
 * themselves when solo. Chosen once, when the round assigns a team, so no caller has to pick between
 * two families of accessor — a mistake either way used to be silent.
 */
public interface ScoreOwner {

    Material material();

    /** The item queued behind the current one, shown by the CHAIN setting. */
    Material nextMaterial();

    @Nullable
    Material previousMaterial();

    int score();

    /** Skips left to spend. */
    int jokers();

    /**
     * How many consecutive items this owner was handed that they already had.
     *
     * <p>Here rather than on {@link ForceItemPlayer} because it is a Score Owner value like every
     * other on this interface: the team's when there is a team, the player's otherwise. It was two
     * fields — one per player and one per team, bumped and reset separately — and the two paths had
     * already drifted: a bump raised the finder plus whoever held the item, while a reset zeroed
     * every member. That was invisible only because in a team game nothing read the per-player value.
     * {@code recordBackToBackPeak} sends the <em>team's</em> streak to both member rows and ignores
     * the player's, and the odds read the team's too. With one holder the two paths cannot disagree
     * about who they touch.
     */
    int backToBackStreak();

    /** Extends the chain by one. */
    void bumpStreak();

    /** Ends the chain. */
    void resetStreak();

    /** When the current item was handed out, for measuring how long it took to find. */
    long itemAssignedAt();

    /** Spends one skip and returns what is left. Never goes below zero. */
    int spendJoker();

    void setJokers(int jokers);

    /**
     * Starts a round: score back to zero, the opening pair assigned, the clock started. Called once
     * per owner, not once per member — drawing a pair per member lets the last one win, so a pair of
     * players quietly consumes two draws from the pool to be handed one item.
     */
    void startRound(Material current, Material next, long at);

    /**
     * Advances to the next item and restarts the find clock. Also once per owner: running it twice on
     * one team does not advance by two, it discards the queued item and leaves current and next both
     * holding {@code next}.
     */
    void advance(Material next, long at);

    /**
     * Replaces the current and queued items outright, leaving the score and the find clock alone.
     * What a skip and {@code /forceitem} do: neither is a find, so neither restarts the clock.
     */
    void assignMaterials(Material current, Material next);

    void record(ForceItem forceItem);

    /**
     * Everything this owner has found this round, in order. Unmodifiable, and includes skips — a
     * skipped item is recorded with {@code usedSkip()} set — so a caller asking how many items were
     * actually collected has to filter them out.
     */
    List<ForceItem> foundItems();

    /** Everyone this owner's item and score belong to. */
    List<ForceItemPlayer> members();
}
