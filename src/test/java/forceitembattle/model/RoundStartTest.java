package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import forceitembattle.settings.GamePreset;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link RoundStart}: what a round is going to be, before anything is written. */
class RoundStartTest {

    private static GamePreset preset(int countdown, int jokers) {
        GamePreset preset = new GamePreset();
        preset.setCountdown(countdown);
        preset.setJokers(jokers);
        return preset;
    }

    private static RoundStart.Planned planned(RoundStart start) {
        return assertInstanceOf(RoundStart.Planned.class, start);
    }

    @Nested
    class FromArguments {

        @Test
        void carriesTheDurationAndJokersItWasGiven() {
            RoundStart.Planned plan = planned(RoundStart.fromArguments(90, 3, false, 1));

            assertEquals(90, plan.durationMinutes());
            assertEquals(3, plan.jokers());
        }

        @Test
        void reportsTheDurationInSecondsForTheTimer() {
            assertEquals(5400, planned(RoundStart.fromArguments(90, 3, false, 1)).durationSeconds());
        }

        @Test
        void theCapItselfIsAllowed() {
            assertEquals(RoundStart.MAX_JOKERS,
                    planned(RoundStart.fromArguments(90, RoundStart.MAX_JOKERS, false, 1)).jokers());
        }

        @Test
        void oneOverTheCapIsRefused() {
            RoundStart start = RoundStart.fromArguments(90, RoundStart.MAX_JOKERS + 1, false, 1);

            RoundStart.Refused refused = assertInstanceOf(RoundStart.Refused.class, start);
            assertEquals(RoundStart.Refusal.TOO_MANY_JOKERS, refused.refusal());
        }
    }

    @Nested
    class FromAPreset {

        @Test
        void takesItsDurationAndJokersFromThePreset() {
            RoundStart.Planned plan = planned(RoundStart.fromPreset(preset(30, 5), false, 1));

            assertEquals(30, plan.durationMinutes());
            assertEquals(5, plan.jokers());
        }

        /**
         * The cap catches a typo on a command line. A preset is written once by an operator and
         * reused, and has always been exempt — worth pinning, because the exemption is a single
         * {@code gamePreset == null} that reads like an oversight.
         */
        @Test
        void isNotSubjectToTheJokerCap() {
            RoundStart start = RoundStart.fromPreset(preset(30, RoundStart.MAX_JOKERS + 100), false, 1);

            assertEquals(RoundStart.MAX_JOKERS + 100, planned(start).jokers());
        }
    }

    @Nested
    class WhetherTeamsHappen {

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 3, 4, 40})
        void aRoundNotConfiguredForTeamsNeverBuildsThem(int rosterSize) {
            assertEquals(RoundStart.Teams.NONE,
                    planned(RoundStart.fromArguments(90, 3, false, rosterSize)).teams());
        }

        /** The threshold is "fewer than four", so four itself builds. */
        @ParameterizedTest
        @CsvSource({
                "0, TOO_FEW_PLAYERS",
                "1, TOO_FEW_PLAYERS",
                "3, TOO_FEW_PLAYERS",
                "4, BUILD",
                "5, BUILD",
                "40, BUILD",
        })
        void aTeamRoundTurnsOnTheHeadCount(int rosterSize, RoundStart.Teams expected) {
            assertEquals(expected, planned(RoundStart.fromArguments(90, 3, true, rosterSize)).teams());
        }

        @Test
        void theThresholdIsTheOneTheHarnessRunsFourBotsFor() {
            assertEquals(4, RoundStart.MIN_PLAYERS_FOR_TEAMS);
        }

        /** A preset round answers the head-count the same way; the rule is not the command's. */
        @Test
        void aPresetRoundIsHeldToTheSameHeadCount() {
            assertEquals(RoundStart.Teams.TOO_FEW_PLAYERS,
                    planned(RoundStart.fromPreset(preset(30, 3), true, 3)).teams());
            assertEquals(RoundStart.Teams.BUILD,
                    planned(RoundStart.fromPreset(preset(30, 3), true, 4)).teams());
        }

        /**
         * A refusal is decided before teams are, so nothing is touched on the way out. This is the
         * ordering the command relied on implicitly by returning early, and it is now a property of
         * the rule rather than of where the {@code return} happened to sit.
         */
        @Test
        void aRefusedRoundDecidesNothingAboutTeams() {
            assertInstanceOf(RoundStart.Refused.class,
                    RoundStart.fromArguments(90, RoundStart.MAX_JOKERS + 1, true, 3));
        }
    }
}
