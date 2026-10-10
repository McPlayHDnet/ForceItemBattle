package forceitembattle.commands;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Roster;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.Ruleset;
import java.util.UUID;
import javax.annotation.Nullable;

public record CommandContext(RoundPhase roundPhase, Ruleset ruleset, Roster roster) {

    public boolean settingEnabled(GameSetting setting) {
        return this.ruleset.enabled(setting);
    }

    /** Null for both "joined after the roster froze" and "never on it". */
    @Nullable
    public ForceItemPlayer entryFor(UUID uuid) {
        return this.roster.get(uuid);
    }
}
