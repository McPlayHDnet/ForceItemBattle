package forceitembattle.util;

import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private Integer currentScore;
    @Setter
    private Team currentTeam;
    @Setter
    private int backToBackStreak;
    @Setter
    private boolean isSpectator;
    @Setter
    private boolean lastItemWasSkipped;
    @Setter
    private Material lastSkippedMaterial;

    public ForceItemPlayer(Player player, Material currentMaterial, int remainingJokers, Integer currentScore) {
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

    public Material currentMaterial() {
        return currentMaterial;
    }

    public Material getCurrentMaterial() {
        if (currentTeam != null) {
            return currentTeam.getCurrentMaterial();
        }
        return currentMaterial;
    }

    public Material nextMaterial() {
        return nextMaterial;
    }

    public Material getNextMaterial() {
        if (currentTeam != null) {
            return currentTeam.getNextMaterial();
        }
        return nextMaterial;
    }

    @Nullable
    public Material previousMaterial() {
        return previousMaterial;
    }

    @Nullable
    public Material getPreviousMaterial() {
        if (currentTeam != null) {
            return currentTeam.getPreviousMaterial();
        }
        return previousMaterial;
    }

    public int remainingJokers() {
        return remainingJokers;
    }

    public Integer currentScore() {
        return currentScore;
    }

    public Team currentTeam() {
        return currentTeam;
    }

    public int backToBackStreak() {
        return backToBackStreak;
    }

    public boolean isSpectator() {
        return isSpectator;
    }

    public boolean isLastItemWasSkipped() {
        return lastItemWasSkipped;
    }

    public Material getLastSkippedMaterial() {
        return lastSkippedMaterial;
    }

}