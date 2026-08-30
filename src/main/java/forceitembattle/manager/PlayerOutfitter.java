package forceitembattle.manager;

import forceitembattle.model.RoundSetup;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Puts a player into the state a round starts them in.
 *
 * <p>The adapter half of {@link RoundSetup}: everything here is a Bukkit write, and every number
 * and material it writes was decided elsewhere. That split is the whole point — the decisions are
 * testable without a server precisely because this class holds all the parts that are not.
 *
 * <p>Slot 4 is the joker slot by convention, which is why it is written before the tools rather
 * than after: {@code addItem} fills the first free slot, and handing out tools first would put one
 * of them where the joker belongs.
 */
final class PlayerOutfitter {

    /** The hotbar slot the joker stack lives in. */
    private static final int JOKER_SLOT = 4;

    private PlayerOutfitter() {
    }

    /** Someone watching rather than playing: no inventory, no survival. */
    static void toSpectator(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        player.getInventory().clear();
    }

    /**
     * Someone playing the round: full health, a clean inventory, their jokers and the starting kit.
     *
     * @param jokersOnHotbar from {@link RoundSetup#jokersOnHotbar}; zero means no stack at all,
     *                       which is not the same as a stack of zero
     */
    static void toPlayer(Player player, int jokersOnHotbar) {
        player.setHealth(20);
        player.setSaturation(20);
        player.getInventory().clear();

        if (jokersOnHotbar > 0) {
            player.getInventory().setItem(JOKER_SLOT, Gamemanager.getJokers(jokersOnHotbar));
        }

        for (Material tool : RoundSetup.STARTING_KIT) {
            player.getInventory().addItem(new ItemStack(tool));
        }

        player.setLevel(0);
        player.setExp(0);
        player.setWalkSpeed(0.2f);
        player.setStatistic(Statistic.TIME_SINCE_REST, 72000); // 1hr = 3600 seconds * 20 ticks
        player.getPassengers().forEach(Entity::remove);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.setGameMode(GameMode.SURVIVAL);
        player.playSound(player, Sound.BLOCK_END_PORTAL_SPAWN, 1, 1);
    }

    /** Hands one team member their share of the pool. Offline members are skipped. */
    static void giveJokerShare(Player player, int jokers) {
        if (player == null || !player.isOnline() || jokers <= 0) {
            return;
        }
        player.getInventory().setItem(JOKER_SLOT, Gamemanager.getJokers(jokers));
    }
}
