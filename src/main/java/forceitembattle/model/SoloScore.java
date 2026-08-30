package forceitembattle.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Setter;
import org.bukkit.Material;

/**
 * A player's own item, score, jokers and found-list — the {@link ScoreOwner} in a solo game.
 *
 * <p>Every {@link ForceItemPlayer} holds one of these for their whole round, whether or not they
 * are on a team. That is not waste: it is what makes the plain accessors on {@code ForceItemPlayer}
 * keep working, and what lets a player leaving a team fall back to exactly the values they had
 * before joining it, which is what the previous field-based design did too.
 */
public class SoloScore implements ScoreOwner {

    /**
     * The player these values belong to. Held only so {@link #members()} can answer "just me"
     * without the caller branching.
     */
    private final ForceItemPlayer player;

    private final List<ForceItem> foundItems = new ArrayList<>();

    @Setter(AccessLevel.PACKAGE)
    private Material currentMaterial;
    @Setter(AccessLevel.PACKAGE)
    private Material nextMaterial;
    @Setter(AccessLevel.PACKAGE)
    private Material previousMaterial;
    @Setter(AccessLevel.PACKAGE)
    private int remainingJokers;
    @Setter(AccessLevel.PACKAGE)
    private int currentScore;
    @Setter(AccessLevel.PACKAGE)
    private long lastItemAssignedAt;

    SoloScore(ForceItemPlayer player, Material currentMaterial, int remainingJokers, int currentScore) {
        this.player = player;
        this.currentMaterial = currentMaterial;
        this.remainingJokers = remainingJokers;
        this.currentScore = currentScore;
    }

    @Override
    public Material material() {
        return this.currentMaterial;
    }

    @Override
    public Material nextMaterial() {
        return this.nextMaterial;
    }

    @Override
    @Nullable
    public Material previousMaterial() {
        return this.previousMaterial;
    }

    @Override
    public int score() {
        return this.currentScore;
    }

    @Override
    public int jokers() {
        return this.remainingJokers;
    }

    @Override
    public long itemAssignedAt() {
        return this.lastItemAssignedAt;
    }

    @Override
    public void setJokers(int jokers) {
        this.remainingJokers = jokers;
    }

    @Override
    public int spendJoker() {
        this.remainingJokers = Math.max(0, this.remainingJokers - 1);
        return this.remainingJokers;
    }

    @Override
    public void startRound(Material current, Material next, long at) {
        this.currentScore = 0;
        this.currentMaterial = current;
        this.nextMaterial = next;
        this.lastItemAssignedAt = at;
    }

    @Override
    public void advance(Material next, long at) {
        this.previousMaterial = this.currentMaterial;
        this.currentMaterial = this.nextMaterial;
        this.nextMaterial = next;
        this.lastItemAssignedAt = at;
    }

    @Override
    public void assignMaterials(Material current, Material next) {
        this.currentMaterial = current;
        this.nextMaterial = next;
    }

    @Override
    public void record(ForceItem forceItem) {
        this.currentScore++;
        addFoundItem(forceItem);
    }

    @Override
    public List<ForceItemPlayer> members() {
        return List.of(this.player);
    }

    void addFoundItem(ForceItem forceItem) {
        if (forceItem != null) {
            this.foundItems.add(forceItem);
        }
    }

    @Override
    public List<ForceItem> foundItems() {
        return Collections.unmodifiableList(this.foundItems);
    }
}
