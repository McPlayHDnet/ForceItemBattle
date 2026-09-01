package forceitembattle.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

/**
 * A team of players sharing one item, score and joker pool.
 *
 * <p>Two roles: as a {@link ScoreOwner} it is interchangeable with a solo player, and as a team it is
 * a social unit with an id, colour, name and members that the tab list, team stat rows and match
 * submission care about. Only the first role is shared.
 */
@Getter
public class Team implements ScoreOwner {

    private final int teamId;
    private final List<ForceItemPlayer> players;
    private final List<ForceItem> foundItems;
    @Setter
    @Nullable
    private String name;
    @Getter
    private DyeColor color;
    @Setter
    private Material currentMaterial;
    @Setter
    private Material nextMaterial;
    @Setter
    private Material previousMaterial;
    @Setter
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

    /**
     * The team's label as MiniMessage — named or auto-generated teams have to render alike side by
     * side in the tab list. Callers that need the raw name for storage use {@link #getName()}.
     */
    public String getTeamDisplay() {
        return "<color:" + colorToHex() + ">[" + (this.name != null ? this.name : "#" + this.teamId) + "]";
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

    /** The member of this team that isn't {@code player}, or empty if they are the only one on it. */
    public Optional<ForceItemPlayer> teammateOf(ForceItemPlayer player) {
        return this.players.stream()
                .filter(member -> !member.equals(player))
                .findFirst();
    }

    /**
     * Whether {@code player} is the member responsible for this team's once-per-team writes. Both
     * members write the same normalised team row, so a stat that counts rather than maxes
     * (gamesPlayed, gamesWon) would be doubled if both sides sent it. Picking the lowest UUID is
     * arbitrary but stable, and both members agree on the answer without coordinating.
     *
     * <p>Not for per-player stats: a win streak is owned by each member, so both report those.
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

    // --- ScoreOwner ---
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
