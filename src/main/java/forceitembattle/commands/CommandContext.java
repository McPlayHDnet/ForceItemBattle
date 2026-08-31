package forceitembattle.commands;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Roster;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.Ruleset;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Everything a {@link Precondition} is allowed to ask about the round it is being evaluated in.
 *
 * <p>The same shape, and the same reason, as {@code AchievementWorld}: taking the plugin would make
 * a precondition's real interface all 23 managers and would cost this package the test surface that
 * justifies it existing at all. Three named questions instead.
 *
 * <p>Depends on nothing that depends on a running server, so {@code CustomCommandTest} can hand a
 * literal one in.
 */
public record CommandContext(RoundPhase roundPhase, Ruleset ruleset, Roster roster) {

    /** Whether this setting is on for the round being played. */
    public boolean settingEnabled(GameSetting setting) {
        return this.ruleset.enabled(setting);
    }

    /**
     * This UUID's roster entry, or {@code null} if they hold none.
     *
     * <p>Null covers both "joined after the countdown froze the roster" and "never on it" — a
     * precondition cannot tell those apart and does not need to.
     */
    @Nullable
    public ForceItemPlayer entryFor(UUID uuid) {
        return this.roster.get(uuid);
    }
}
