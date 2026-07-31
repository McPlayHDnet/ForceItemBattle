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

@Getter
public class Team {

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
     * The team's label as MiniMessage: bracketed and in the team colour, whether it was named via
     * /forceteam or auto-generated. A named team used to return the bare name, which rendered
     * without brackets or colour and so looked nothing like the auto-team labels next to it in the
     * same tab list. Callers that need the raw name for storage use {@link #getName()}.
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

    /**
     * The member of this team that isn't {@code player}, or empty if they are the only one on it.
     *
     * Every stats write in a team game is addressed by the pair (player, teammate), so this lookup
     * used to be open-coded at nine call sites in three different shapes — a stream with
     * {@code findFirst}, a stream with {@code forEach}, and a {@code for} loop with {@code break}.
     */
    public Optional<ForceItemPlayer> teammateOf(ForceItemPlayer player) {
        return this.players.stream()
                .filter(member -> !member.equals(player))
                .findFirst();
    }

    /**
     * Whether {@code player} is the member responsible for this team's once-per-team writes.
     *
     * Both members write the same normalized team row, so a stat that counts rather than maxes
     * (gamesPlayed, gamesWon) would be doubled if both sides sent it. Picking the lowest UUID is
     * arbitrary but stable, and both members agree on the answer without coordinating.
     *
     * <p>Not for per-player stats: a win streak is owned by each member individually, so both
     * report those. See the callers in {@code Gamemanager} for which is which.
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
}
