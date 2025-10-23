package forceitembattle.util;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Getter
public class Team {

    private final int teamId;
    @Setter
    @Nullable
    private String name;
    @Getter
    private DyeColor color;
    private final List<ForceItemPlayer> players;
    private final List<ForceItem> foundItems;
    @Setter
    private Material currentMaterial;
    @Setter
    private Material nextMaterial;
    @Setter
    private Material previousMaterial;
    @Setter
    private int backToBackStreak;
    @Setter
    private Integer currentScore, remainingJokers;

    public Team(int teamId, Material currentMaterial, Integer currentScore, Integer remainingJokers, ForceItemPlayer... teamPlayers) {
        this.teamId = teamId;
        this.color = getRandomColor();
        this.foundItems = new ArrayList<>();
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

        return "<color:" + colorToHex() + ">[#" + this.teamId + "]";
    }

    private DyeColor getRandomColor() {
        DyeColor[] colors = DyeColor.values();
        return colors[new Random().nextInt(colors.length)];
    }

    private String colorToHex() {
        return String.format("#%02X%02X%02X", color.getColor().getRed(), color.getColor().getGreen(), color.getColor().getBlue());
    }

    public void addPlayer(ForceItemPlayer player) {
        players.add(player);
    }

    public void removePlayer(ForceItemPlayer player) {
        players.remove(player);
    }

    public void addFoundItemToList(ForceItem forceItem) {
        if (forceItem != null) {
            this.foundItems.add(forceItem);
        }
    }

    public List<ForceItem> getFoundItems() {
        return Collections.unmodifiableList(foundItems);
    }
}
