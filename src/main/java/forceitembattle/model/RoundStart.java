package forceitembattle.model;

import forceitembattle.settings.GamePreset;

/**
 * What a round is going to be, decided before anything is written: duration, jokers, and what has
 * to happen to teams — or a refusal naming which rule refused. See {@code CONTEXT.md § Round Start}.
 *
 * <p>Decides in numbers and names; one adapter turns them into effects. Nothing here touches Bukkit,
 * the plugin, the settings or the team manager.
 */
public sealed interface RoundStart {

    /** The most jokers a round may be started with by hand. */
    int MAX_JOKERS = 64;

    /** Teams need four players. Why {@code Invoke-RoundTest.ps1} runs four bots for team mode. */
    int MIN_PLAYERS_FOR_TEAMS = 4;

    /** A round that is going to happen. */
    record Planned(int durationMinutes, int jokers, Teams teams) implements RoundStart {

        public int durationSeconds() {
            return this.durationMinutes * 60;
        }
    }

    /** A round that is not, and the rule that said so. */
    record Refused(Refusal refusal) implements RoundStart {
    }

    enum Refusal {
        TOO_MANY_JOKERS
    }

    /** What has to happen to teams before the countdown runs. */
    enum Teams {
        /** Not a team round. Nothing to do. */
        NONE,

        /** A team round with enough players: build them. */
        BUILD,

        /**
         * A team round with too few players. The setting is turned off and any teams cleared —
         * load-bearing rather than tidiness, and the reason a round writes to config. See
         * {@code CONTEXT.md § Round Start}.
         */
        TOO_FEW_PLAYERS
    }

    /** A round started from a saved preset. The joker cap deliberately does not apply. */
    static RoundStart fromPreset(GamePreset preset, boolean teamsConfigured, int rosterSize) {
        return new Planned(preset.getCountdown(), preset.getJokers(),
                teamsFor(teamsConfigured, rosterSize));
    }

    /** A round started as {@code /start <minutes> <jokers>}. */
    static RoundStart fromArguments(int durationMinutes, int jokers,
                                    boolean teamsConfigured, int rosterSize) {
        if (jokers > MAX_JOKERS) {
            return new Refused(Refusal.TOO_MANY_JOKERS);
        }

        return new Planned(durationMinutes, jokers, teamsFor(teamsConfigured, rosterSize));
    }

    private static Teams teamsFor(boolean teamsConfigured, int rosterSize) {
        if (!teamsConfigured) {
            return Teams.NONE;
        }

        return rosterSize < MIN_PLAYERS_FOR_TEAMS ? Teams.TOO_FEW_PLAYERS : Teams.BUILD;
    }
}
