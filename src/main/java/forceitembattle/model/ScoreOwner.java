package forceitembattle.model;

import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Material;

/**
 * Whoever owns the score a find is credited to: the {@link Team} in a team game, the player
 * themselves when solo. Every value it holds is shared when a team shares it.
 */
public interface ScoreOwner {

    Material material();

    Material nextMaterial();

    @Nullable
    Material previousMaterial();

    int score();

    int jokers();

    /** How many consecutive items this owner was handed that they already had. */
    int backToBackStreak();

    void bumpStreak();

    void resetStreak();

    long itemAssignedAt();

    /** Spends one skip and returns what is left. Never goes below zero. */
    int spendJoker();

    void setJokers(int jokers);

    /** Once per owner, not per member: a draw per member lets the last one win. */
    void startRound(Material current, Material next, long at);

    /** Also once per owner: running it twice discards the queued item rather than advancing twice. */
    void advance(Material next, long at);

    /** A skip and {@code /forceitem}: neither is a find, so neither restarts the find clock. */
    void assignMaterials(Material current, Material next);

    void record(ForceItem forceItem);

    /** Unmodifiable, and includes skips — filter on {@code usedSkip()} for items actually collected. */
    List<ForceItem> foundItems();

    List<ForceItemPlayer> members();
}
