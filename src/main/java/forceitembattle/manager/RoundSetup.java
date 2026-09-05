package forceitembattle.manager;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameContext;
import java.util.List;
import org.bukkit.Material;

/**
 * What a player starts a round holding. Rules, not effects — they answer in counts and materials;
 * {@code PlayerOutfitter} does the writing.
 */
public final class RoundSetup {

    private RoundSetup() {
    }

    public static final List<Material> STARTING_KIT =
            List.of(Material.STONE_AXE, Material.STONE_PICKAXE, Material.STONE_SHOVEL);

    /**
     * A pool split as evenly as it goes across a team's members, in member order, with the remainder
     * handed to the earliest members.
     *
     * <p>The shares must sum to {@code pool} for any member count. That matters for the round's
     * jokers, its first caller: the team's shared pool is set to the full amount separately, so a
     * split that lost one would leave a skip nobody could reach. {@code PointHunt} splits its wheel
     * payout by the same rule and used to carry its own copy of this arithmetic.
     *
     * @return one share per member; empty when there are no members to split across
     */
    public static int[] splitEvenly(int pool, int memberCount) {
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
     * How many jokers to put in a player's hotbar. Zero during the countdown for a team member,
     * because the pool split overwrites the whole roster moments later.
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
