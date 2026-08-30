package forceitembattle.model;

import forceitembattle.settings.GamePreset;

/**
 * What a round is going to be, decided before anything is written.
 *
 * <h2>Why this exists</h2>
 *
 * <p>These rules lived inside {@code CommandStart}'s argument handler, so the only way to ask "does
 * a five-player round build teams?" was to start a server, connect five clients and type the
 * command. {@code Gamemanager.startGame}'s javadoc has said all along that "/start only parses
 * arguments and runs the countdown, then hands over" — this is the half that had drifted out of
 * that arrangement and back into the command.
 *
 * <p>The split is the one {@link RoundSetup} and {@link Roster} already use: decide in numbers and
 * names, let one adapter turn them into effects. Nothing here touches Bukkit, the plugin, the
 * settings or the team manager.
 *
 * <p>A refusal names <em>which</em> rule refused rather than carrying a message, the way
 * {@link ProtectionVerdict} does, so the wording stays with the command that has a sender to send
 * it to.
 */
public sealed interface RoundStart {

    /** The most jokers a round may be started with by hand. */
    int MAX_JOKERS = 64;

    /**
     * Teams need four players.
     *
     * <p>Was a bare {@code 4} in the middle of the command. It is the reason
     * {@code Invoke-RoundTest.ps1} runs four bots for its team mode and two for solo.
     */
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
         * A team round with too few players. The setting is turned off and any teams cleared.
         *
         * <p>Turning the setting off is load-bearing rather than tidiness: fourteen places still
         * read it to decide round-level behaviour — how {@code /result} pages, what the match
         * submission reports, whether a lead change is looked for among teams — and leaving it on
         * with no teams to find makes all of them wrong. It does mean a round writes to config,
         * which is worth removing one day, but not by dropping the write.
         */
        TOO_FEW_PLAYERS
    }

    /**
     * A round started from a saved preset.
     *
     * <p>The joker cap deliberately does not apply. A preset is written once by an operator and
     * reused; the cap exists to catch a typo in a command line, and presets have always been exempt.
     */
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
