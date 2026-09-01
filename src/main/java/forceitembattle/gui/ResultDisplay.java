package forceitembattle.gui;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.ScoreOwner;
import forceitembattle.model.Team;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;

/**
 * How a {@link ScoreOwner} is named on the result screens.
 *
 * <p>Presentation, so it lives here rather than on {@code ScoreOwner}, which is a scoring type — a
 * team's name and colour are its social half and have no solo counterpart, and giving the interface
 * a display name is how that split starts eroding.
 */
final class ResultDisplay {

    private ResultDisplay() {
    }

    /** The name shown in the reveal title and the chat line. */
    static String nameOf(ScoreOwner owner) {
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
    static String windowTitleFor(ScoreOwner owner) {
        return owner instanceof Team team ? "Team " + team.getTeamDisplay() : nameOf(owner);
    }

    /** What to pass back to {@code /result} to reopen this owner's screen. */
    static String resultArgumentFor(ScoreOwner owner) {
        if (owner instanceof Team team) {
            return "#" + team.getTeamId();
        }
        return owner.members().isEmpty() ? "" : owner.members().get(0).player().getUniqueId().toString();
    }

    /**
     * The member who handed this item in, or null when it cannot be attributed. Whether it is
     * <em>shown</em> is {@link #attributesCollectors(ScoreOwner)}.
     */
    static String collectorName(ScoreOwner owner, java.util.UUID collectedBy) {
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

    /**
     * Whether the screen names who collected each item. More than one member is the actual condition,
     * not "is this a team" — those coincide only while teams are the sole owner with members.
     */
    static boolean attributesCollectors(ScoreOwner owner) {
        return owner.members().size() > 1;
    }
}
