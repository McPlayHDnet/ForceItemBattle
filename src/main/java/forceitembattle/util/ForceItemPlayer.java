package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.Player;

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
    private int remainingJokers;
    @Setter
    private Integer currentScore;
    @Setter
    private Team currentTeam;
    @Setter
    private int backToBackStreak;
    @Setter
    private boolean isSpectator;

    public ForceItemPlayer(Player player, List<ForceItem> foundItems, Material currentMaterial, int remainingJokers, Integer currentScore) {
        this.player = player;
        this.foundItems = foundItems;
        this.currentMaterial = currentMaterial;
        this.remainingJokers = remainingJokers;
        this.currentScore = currentScore;
    }

    public Player player() {
        return player;
    }

    public List<ForceItem> foundItems() {
        return foundItems;
    }

    public void addFoundItemToList(ForceItem forceItem) {
        this.foundItems.add(forceItem);
    }

    public Material currentMaterial() {
        return currentMaterial;
    }

    public Material getCurrentMaterial() {
        if (ForceItemBattle.getInstance().getSettings().isSettingEnabled(GameSetting.TEAM)) {
            return currentTeam().getCurrentMaterial();
        }

        return currentMaterial();
    }

    public Material nextMaterial() {
        return nextMaterial;
    }

    public Material getNextMaterial() {
        if (ForceItemBattle.getInstance().getSettings().isSettingEnabled(GameSetting.TEAM)) {
            return currentTeam().getNextMaterial();
        }

        return nextMaterial();
    }

    public Material previousMaterial() {
        return this.foundItems.getLast().material();
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

}
