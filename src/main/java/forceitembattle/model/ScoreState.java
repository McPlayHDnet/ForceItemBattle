package forceitembattle.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Setter;
import org.bukkit.Material;

/** The item, score, joker and found-list state shared by both kinds of {@link ScoreOwner}. */
@Setter(AccessLevel.PACKAGE)
abstract class ScoreState implements ScoreOwner {

    @Setter(AccessLevel.NONE)
    private final List<ForceItem> foundItems = new ArrayList<>();

    private Material currentMaterial;
    private Material nextMaterial;
    private Material previousMaterial;
    private int remainingJokers;
    private int currentScore;
    private long lastItemAssignedAt;
    @Setter(AccessLevel.NONE)
    private int backToBackStreak;

    ScoreState(Material currentMaterial, int currentScore, int remainingJokers) {
        this.currentMaterial = currentMaterial;
        this.currentScore = currentScore;
        this.remainingJokers = remainingJokers;
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
    public int backToBackStreak() {
        return this.backToBackStreak;
    }

    @Override
    public void bumpStreak() {
        this.backToBackStreak++;
    }

    @Override
    public void resetStreak() {
        this.backToBackStreak = 0;
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
