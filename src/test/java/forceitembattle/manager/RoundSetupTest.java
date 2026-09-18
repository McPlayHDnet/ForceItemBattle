package forceitembattle.manager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import forceitembattle.Players;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameContext;
import forceitembattle.model.Team;
import java.util.Arrays;
import org.bukkit.Material;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * What a player starts a round holding.
 *
 * <p>None of this was reachable before: the joker arithmetic was computed inside the loop that
 * called {@code setItem}, so exercising it meant building an {@code ItemStack}, which needs a
 * running server. Splitting the decision from the write is what put it in reach.
 */
class RoundSetupTest {

    private static GameContext context(boolean runMode) {
        return new GameContext(false, runMode, false, true, false);
    }

    private static ForceItemPlayer player() {
        return new ForceItemPlayer(Players.mockPlayer("a"), Material.DIRT, 0, 0);
    }

    private static ForceItemPlayer onTeamWith(int teamJokers) {
        ForceItemPlayer player = player();
        Team team = new Team(1, Material.DIRT, 0, teamJokers, player);
        player.setCurrentTeam(team);
        return player;
    }

    @Nested
    class SplittingThePool {

        @Test
        void anEvenPoolSplitsEvenly() {
            assertArrayEquals(new int[] {3, 3}, RoundSetup.splitEvenly(6, 2));
        }

        /** The remainder goes to the earlier members: 5 across 2 is 3 and 2, never 2 and 2. */
        @Test
        void anUnevenPoolGivesTheRemainderToTheEarlierMembers() {
            assertArrayEquals(new int[] {3, 2}, RoundSetup.splitEvenly(5, 2));
            assertArrayEquals(new int[] {2, 2, 1}, RoundSetup.splitEvenly(5, 3));
            assertArrayEquals(new int[] {1, 1, 1, 0}, RoundSetup.splitEvenly(3, 4));
        }

        /**
         * The shares must always sum to the pool. The team's shared count is set to the full amount
         * separately, so a split that lost a joker would leave a skip nobody could reach.
         */
        @Test
        void everyJokerInThePoolIsHandedOut() {
            for (int pool = 0; pool <= 20; pool++) {
                for (int members = 1; members <= 5; members++) {
                    int handedOut = Arrays.stream(RoundSetup.splitEvenly(pool, members)).sum();

                    assertEquals(pool, handedOut, "pool " + pool + " across " + members);
                }
            }
        }

        @Test
        void aSoleMemberTakesTheWholePool() {
            assertArrayEquals(new int[] {7}, RoundSetup.splitEvenly(7, 1));
        }

        @Test
        void anEmptyPoolGivesEveryoneNothing() {
            assertArrayEquals(new int[] {0, 0}, RoundSetup.splitEvenly(0, 2));
        }

        /** An empty team would otherwise divide by zero. */
        @Test
        void aTeamWithNoMembersSplitsNothing() {
            assertArrayEquals(new int[0], RoundSetup.splitEvenly(6, 0));
        }
    }

    @Nested
    class TheHotbarStack {

        @Test
        void aSoloPlayerGetsTheRoundsAllowance() {
            assertEquals(3, RoundSetup.jokersOnHotbar(player(), context(false), 3, false));
        }

        /** Run mode is a race for the first find and hands out no skip button. */
        @Test
        void aSoloPlayerInRunModeGetsNoButton() {
            assertEquals(0, RoundSetup.jokersOnHotbar(player(), context(true), 3, false));
        }

        /**
         * During the countdown the whole roster is served moments later by the pool split, so a
         * stack placed here would only be overwritten.
         */
        @Test
        void aTeamMemberGetsNothingDuringTheCountdown() {
            assertEquals(0, RoundSetup.jokersOnHotbar(onTeamWith(6), context(false), 6, true));
        }

        /** Rejoining later: hand them what the team has left, not what the round started with. */
        @Test
        void aTeamMemberRejoiningGetsWhatTheTeamHasLeft() {
            assertEquals(2, RoundSetup.jokersOnHotbar(onTeamWith(2), context(false), 6, false));
        }

        @Test
        void aTeamWithNoJokersLeftGetsNoStack() {
            assertEquals(0, RoundSetup.jokersOnHotbar(onTeamWith(0), context(false), 6, false));
        }

        /**
         * Preserved from the original rather than chosen: run mode suppresses the button for a solo
         * player but not for a team member rejoining. If this is ever made consistent, it should be
         * because someone decided to, not because a refactor drifted.
         */
        @Test
        void runModeDoesNotSuppressTheButtonForTeams() {
            assertEquals(2, RoundSetup.jokersOnHotbar(onTeamWith(2), context(true), 6, false));
        }
    }

    @Nested
    class TheStartingKit {

        @Test
        void isThreeStoneTools() {
            assertEquals(
                    java.util.List.of(Material.STONE_AXE, Material.STONE_PICKAXE, Material.STONE_SHOVEL),
                    RoundSetup.STARTING_KIT);
        }
    }
}
