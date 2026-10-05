package forceitembattle.manager;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameContext;
import java.util.List;
import org.bukkit.Material;

public final class RoundSetup {

    private RoundSetup() {
    }

    public static final List<Material> STARTING_KIT =
            List.of(Material.STONE_AXE, Material.STONE_PICKAXE, Material.STONE_SHOVEL);

    /**
     * Remainder goes to the earliest members. Shares must sum to {@code pool}, or a team's joker split
     * would leave a skip nobody can reach.
     *
     * @return one share per member; empty when there are no members
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
     * Zero during the countdown for a team member, because the pool split overwrites it moments later.
     * Run mode suppresses the button for a solo player but not a team, as it always has.
     */
    public static int jokersOnHotbar(ForceItemPlayer player, GameContext context,
                                     int roundJokers, boolean duringCountdown) {
        if (!player.isInTeam()) {
            return context.runMode() ? 0 : roundJokers;
        }

        return duringCountdown ? 0 : player.activeJokers();
    }
}
