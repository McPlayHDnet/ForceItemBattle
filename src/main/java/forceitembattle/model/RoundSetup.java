package forceitembattle.model;

import java.util.List;
import org.bukkit.Material;

/**
 * What a player starts a round holding.
 *
 * <p>These are rules, not effects: they answer in counts and materials and write nothing. The
 * writing is {@code PlayerOutfitter}'s job. Keeping them apart is what makes them testable at all
 * — the joker arithmetic used to be computed inside the loop that called {@code setItem}, which
 * put it behind {@code ItemStack} and therefore behind a running server, as
 * {@code HeadlessBoundaryTest} records.
 */
public final class RoundSetup {

    private RoundSetup() {
    }

    /** The tools everyone starts with. */
    public static final List<Material> STARTING_KIT =
            List.of(Material.STONE_AXE, Material.STONE_PICKAXE, Material.STONE_SHOVEL);

    /**
     * How the round's joker pool splits across a team's members, in member order.
     *
     * <p>An uneven pool cannot be shared evenly, so the remainder goes to the earlier members —
     * a pool of 5 across 2 becomes 3 and 2. The whole pool is always handed out: the shares sum to
     * {@code pool} for any member count, which matters because the team's shared pool is set to the
     * full amount separately, and a split that lost a joker would leave a skip nobody could reach.
     *
     * @return one share per member; empty when there are no members to split across, which is the
     *         only reason this does not simply divide.
     */
    public static int[] splitJokers(int pool, int memberCount) {
        if (memberCount <= 0) {
            return new int[0];
        }

        int[] shares = new int[memberCount];
        int base = pool / memberCount;
        int remainder = pool % memberCount;

        for (int member = 0; member < memberCount; member++) {
            shares[member] = base + (member < remainder ? 1 : 0);
        }
        return shares;
    }

    /**
     * How many jokers to put in a player's hotbar when their round setup runs.
     *
     * <p>Three cases, and the middle one is the subtle one:
     *
     * <ul>
     *   <li><b>Solo</b> — the round's full allowance, except in run mode, which is a race for the
     *       first find and hands out no skip button at all.</li>
     *   <li><b>On a team, during the countdown</b> — nothing. The whole roster is served moments
     *       later by the pool split, and putting a stack here would be overwritten by it anyway.</li>
     *   <li><b>On a team, rejoining later</b> — whatever the team has left. The stack is only the
     *       button; the count that actually gates a skip lives on the team.</li>
     * </ul>
     *
     * <p>Note the asymmetry, which is preserved from the original rather than chosen: run mode
     * suppresses the button for a solo player but not for a team. See the test that pins it.
     *
     * @param duringCountdown true while {@code /start}'s countdown is still running
     */
    public static int jokersOnHotbar(ForceItemPlayer player, GameContext context,
                                     int roundJokers, boolean duringCountdown) {
        if (!player.isInTeam()) {
            return context.runMode() ? 0 : roundJokers;
        }

        return duringCountdown ? 0 : player.activeJokers();
    }
}
