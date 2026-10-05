package forceitembattle.gui;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.ScoreOwner;
import forceitembattle.model.Team;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;

/** Presentation, so it lives here rather than on the scoring type {@code ScoreOwner}. */
public final class ResultDisplay {

    private ResultDisplay() {
    }

    /** The name shown in the reveal title and the chat line. */
    public static String nameOf(ScoreOwner owner) {
        if (!(owner instanceof Team team)) {
            return owner.members().isEmpty() ? "?" : owner.members().get(0).player().getName();
        }
        if (team.getName() != null) {
            return team.getName();
        }
        return team.getPlayers().stream()
                .map(member -> member.player().getName())
                .collect(Collectors.joining(", "));
    }

    /** The window title of a reopened screen. */
    public static String windowTitleFor(ScoreOwner owner) {
        return owner instanceof Team team ? "Team " + team.getTeamDisplay() : nameOf(owner);
    }

    /** What to pass back to {@code /result} to reopen this owner's screen. */
    public static String resultArgumentFor(ScoreOwner owner) {
        if (owner instanceof Team team) {
            return "#" + team.getTeamId();
        }
        return owner.members().isEmpty() ? "" : owner.members().get(0).player().getUniqueId().toString();
    }

    /** Null when it cannot be attributed; whether it is shown is {@link #attributesCollectors(ScoreOwner)}. */
    public static String collectorName(ScoreOwner owner, java.util.UUID collectedBy) {
        if (collectedBy == null) {
            return null;
        }
        return owner.members().stream()
                .map(ForceItemPlayer::player)
                .filter(member -> member != null && member.getUniqueId().equals(collectedBy))
                .map(Player::getName)
                .findFirst()
                .orElse(null);
    }

    /** More than one member is the condition, not "is a team"; they only coincide today. */
    public static boolean attributesCollectors(ScoreOwner owner) {
        return owner.members().size() > 1;
    }
}
