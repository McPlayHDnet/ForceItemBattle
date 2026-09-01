package forceitembattle.model;

import java.util.List;
import org.bukkit.Material;

/**
 * What a player starts a round holding. Rules, not effects: they answer in counts and materials and
 * write nothing — {@code PlayerOutfitter} does the writing. Keeping them apart is what makes them
 * testable, since an inventory write puts a rule behind a running server.
 */
public final class RoundSetup {

    private RoundSetup() {
    }

    public static final List<Material> STARTING_KIT =
            List.of(Material.STONE_AXE, Material.STONE_PICKAXE, Material.STONE_SHOVEL);

    /**
     * How the round's joker pool splits across a team's members, in member order. The remainder goes
     * to the earlier members — 5 across 2 becomes 3 and 2. The shares must sum to {@code pool} for
     * any member count: the team's shared pool is set to the full amount separately, so a split that
     * lost a joker would leave a skip nobody could reach.
     *
     * @return one share per member; empty when there are no members to split across.
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
     * <ul>
     *   <li><b>Solo</b> — the round's full allowance, except in run mode, a race for the first find
     *       that hands out no skip button at all.</li>
     *   <li><b>On a team, during the countdown</b> — nothing; the pool split serves the whole roster
     *       moments later and would overwrite anything put here.</li>
     *   <li><b>On a team, rejoining later</b> — whatever the team has left. The stack is only the
     *       button; the count that gates a skip lives on the team.</li>
     * </ul>
     *
     * <p>The asymmetry is preserved from the original rather than chosen: run mode suppresses the
     * button for a solo player but not for a team.
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
