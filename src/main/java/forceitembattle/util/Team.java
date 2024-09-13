package forceitembattle.util;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class Team {

    private final int teamId;
    @Setter
    @Nullable
    private String name;
    private final List<ForceItemPlayer> players;
    private final List<ForceItem> foundItems;
    @Setter
    private Material currentMaterial;
    @Setter
    private Material nextMaterial;
    @Setter
    private Integer currentScore, remainingJokers;

    public Team(int teamId, List<ForceItem> foundItems, Material currentMaterial, Integer currentScore, Integer remainingJokers, ForceItemPlayer... teamPlayers) {
        this.teamId = teamId;
        this.foundItems = foundItems;
        this.currentMaterial = currentMaterial;
        this.currentScore = currentScore;
        this.remainingJokers = remainingJokers;
        this.players = new ArrayList<>();
        players.addAll(Arrays.asList(teamPlayers));
    }

    public String getTeamDisplay() {
        if (this.name != null) {
            return this.name;
        }

        return "#" + this.teamId;
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
