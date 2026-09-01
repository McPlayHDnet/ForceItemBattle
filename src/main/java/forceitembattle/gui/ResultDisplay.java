package forceitembattle.gui;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.ScoreOwner;
import forceitembattle.model.Team;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;

/**
 * How a {@link ScoreOwner} is named on the result screens.
 *
 * <p>Presentation, so it lives here rather than on {@code ScoreOwner} or on the ceremony's
 * {@code Reveal}. {@code ScoreOwner} is a scoring type — {@code CONTEXT.md § Team} records that a
 * team's name and colour are the <em>social</em> half and have no solo counterpart — and giving it a
 * display name is how that split starts eroding. One branch, stated once, on the drawing side.
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
     * The member who handed this item in, or null when it cannot be attributed.
     *
     * <p>Reads {@code members()} rather than a team's player list, so it is the same lookup for
     * both kinds of owner. Whether it is <em>shown</em> is a different question — see
     * {@link #attributesCollectors(ScoreOwner)}.
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
     * Whether the screen names who collected each item.
     *
     * <p>More than one member is the actual condition — attribution exists because several people
     * could have found it. It used to be written as "is this a team", which is the same thing only
     * for as long as teams are the only owner with members to tell apart.
     */
    static boolean attributesCollectors(ScoreOwner owner) {
        return owner.members().size() > 1;
    }
}
