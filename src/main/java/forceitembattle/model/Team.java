package forceitembattle.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

/** As a {@link ScoreOwner} it is interchangeable with a solo player; its id, colour and name are its own. */
@Getter
public class Team implements ScoreOwner {

    private final int teamId;
    private final List<ForceItemPlayer> players;
    private final List<ForceItem> foundItems;
    @Setter
    @Nullable
    private String name;
    private DyeColor color;
    @Setter
    private Material currentMaterial;
    @Setter
    private Material nextMaterial;
    @Setter
    private Material previousMaterial;
    private int backToBackStreak;
    @Setter
    private long lastItemAssignedAt;
    @Setter
    private int currentScore, remainingJokers;

    public Team(int teamId, Material currentMaterial, int currentScore, int remainingJokers, ForceItemPlayer... teamPlayers) {
        this.teamId = teamId;
        this.color = getRandomColor();
        this.foundItems = new ArrayList<>();
        this.currentMaterial = currentMaterial;
        this.currentScore = currentScore;
        this.remainingJokers = remainingJokers;
        this.players = new ArrayList<>();
        players.addAll(Arrays.asList(teamPlayers));
    }

    /** Raw name for storage is {@link #getName()}. */
    public String getTeamDisplay() {
        return "<color:" + colorToHex() + ">[" + (this.name != null ? this.name : "#" + this.teamId) + "]";
    }

    private DyeColor getRandomColor() {
        DyeColor[] colors = DyeColor.values();
        return colors[ThreadLocalRandom.current().nextInt(colors.length)];
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

    /** The member of this team that isn't {@code player}, or empty if they are the only one on it. */
    public Optional<ForceItemPlayer> teammateOf(ForceItemPlayer player) {
        return this.players.stream()
                .filter(member -> !member.equals(player))
                .findFirst();
    }

    /**
     * Both members write the same team row, so counting stats (gamesPlayed, gamesWon) are sent by the
     * lowest UUID only. Not for per-player stats like win streaks, which both report.
     */
    public boolean isPrimaryWriter(ForceItemPlayer player) {
        if (player == null || player.player() == null) {
            return false;
        }
        String own = player.player().getUniqueId().toString();
        return this.players.stream()
                .filter(member -> member.player() != null)
                .noneMatch(member -> member.player().getUniqueId().toString().compareTo(own) < 0);
    }

    public List<ForceItem> getFoundItems() {
        return Collections.unmodifiableList(foundItems);
    }

    @Override
    public List<ForceItem> foundItems() {
        return this.getFoundItems();
    }

    // Delegation onto the fields above. The Lombok accessors stay, because the places that address a
    // team *as a team* still call them directly.

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
        addFoundItemToList(forceItem);
    }

    @Override
    public List<ForceItemPlayer> members() {
        return this.players;
    }
}
