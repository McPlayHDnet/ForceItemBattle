package forceitembattle.util;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class Teams {

    private final int teamId;
    private final List<ForceItemPlayer> players;
    private final List<ForceItem> foundItems;
    @Setter
    private Material currentMaterial;
    @Setter
    private Integer currentScore, remainingJokers;

    public Teams(int teamId, List<ForceItem> foundItems, Material currentMaterial, Integer currentScore, Integer remainingJokers, ForceItemPlayer... teamPlayers) {
        this.teamId = teamId;
        this.foundItems = foundItems;
        this.currentMaterial = currentMaterial;
        this.currentScore = currentScore;
        this.remainingJokers = remainingJokers;
        this.players = new ArrayList<>();
        players.addAll(Arrays.asList(teamPlayers));
    }

    public void addPlayer(ForceItemPlayer player) {
        players.add(player);
    }

    public void removePlayer(ForceItemPlayer player) {
        players.remove(player);
    }

    public void addFoundItemToList(ForceItem forceItem) {
        this.foundItems.add(forceItem);
    }

    public Material getPreviousMaterial() {
        return this.foundItems.get(this.foundItems.size() - 1).material();
    }
}
