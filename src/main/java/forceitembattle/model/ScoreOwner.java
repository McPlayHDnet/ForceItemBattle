package forceitembattle.model;

import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Material;

/**
 * Whoever owns the score a find is credited to: the {@link Team} in a team game, the player
 * themselves when solo. See {@code CONTEXT.md § Score Owner}, which also records what deliberately
 * stayed out of this interface.
 *
 * <p>Chosen once, when the round assigns a team, so no caller has to pick between two families of
 * accessor — a mistake either way used to be silent.
 */
public interface ScoreOwner {

    /** The force item currently being hunted. */
    Material material();

    /** The item queued behind the current one, shown by the CHAIN setting. */
    Material nextMaterial();

    /** The item held before the current one, or {@code null} at the start of a round. */
    @Nullable
    Material previousMaterial();

    /** Score on the board. */
    int score();

    /** Skips left to spend. */
    int jokers();

    /** When the current item was handed out, for measuring how long it took to find. */
    long itemAssignedAt();

    /** Spends one skip and returns what is left. Never goes below zero. */
    int spendJoker();

    /** Sets the skip pool for the round. */
    void setJokers(int jokers);

    /**
     * Starts a round: score back to zero, the opening pair assigned, the clock started.
     *
     * <p>Called once per owner, not once per member. That distinction is the reason this is a
     * method rather than four setters: the old per-player loop drew a fresh material pair for every
     * member of a team and let the last one win, so a pair of players quietly consumed two draws
     * from the pool to be handed one item.
     */
    void startRound(Material current, Material next, long at);

    /**
     * Advances to the next item: the current one becomes previous, the queued one becomes current,
     * and {@code next} takes its place. Restarts the find clock.
     *
     * <p>Also once per owner. Running it twice on one team does not advance by two — it discards
     * the queued item and leaves current and next both holding {@code next}.
     */
    void advance(Material next, long at);

    /**
     * Replaces the current and queued items outright, leaving the score and the find clock alone.
     * What a skip does, and what {@code /forceitem} does: neither is a find, so neither restarts
     * the clock that measures how long a find took.
     */
    void assignMaterials(Material current, Material next);

    /** Credits a found item: one point, and the item onto this owner's found-list. */
    void record(ForceItem forceItem);

    /**
     * Everything this owner has found this round, in the order it came in. Unmodifiable.
     *
     * <p>Includes skips — a skipped item is still recorded, with {@code usedSkip()} set — so a
     * caller asking "how many items has this owner actually collected" has to filter them out.
     */
    List<ForceItem> foundItems();

    /** Everyone this owner's item and score belong to. */
    List<ForceItemPlayer> members();
}
