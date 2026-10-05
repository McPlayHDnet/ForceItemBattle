package forceitembattle.model;

import java.util.ArrayList;
import java.util.Arrays;
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
public class Team extends ScoreState {

    private final int teamId;
    private final List<ForceItemPlayer> players;
    @Setter
    @Nullable
    private String name;
    private DyeColor color;

    public Team(int teamId, Material currentMaterial, int currentScore, int remainingJokers, ForceItemPlayer... teamPlayers) {
        super(currentMaterial, currentScore, remainingJokers);
        this.teamId = teamId;
        this.color = getRandomColor();
        this.players = new ArrayList<>(Arrays.asList(teamPlayers));
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

    @Override
    public List<ForceItemPlayer> members() {
        return this.players;
    }
}
