package forceitembattle.randomevents;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.util.Prefix;
import forceitembattle.util.Text;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class ItemHunt implements RandomEvent {

    private static final int MIN_WHEELS = 1;
    private static final int MAX_WHEELS = 5;

    private final ForceItemBattle plugin;

    @Override
    public void start() {
        Bukkit.broadcast(Text.of(Prefix.RANDOM_EVENT + "<gold><b>Item Hunt</b><reset><gray> has begun!"));
        Bukkit.broadcast(Text.of(Prefix.RANDOM_EVENT + "<gray>First to collect their current item <red>without skipping "
                + "<gray>wins <yellow>" + MIN_WHEELS + "-" + MAX_WHEELS + " Wheels of Fortune<gray>!"));

        Bukkit.getOnlinePlayers().forEach(players ->
                players.playSound(players.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1.4f));
    }

    @Override
    public boolean onFoundItem(FoundItemEvent foundItemEvent, ForceItemPlayer forceItemPlayer) {
        if (foundItemEvent.isSkipped() || foundItemEvent.isBackToBack()) {
            return false;
        }

        Player winner = forceItemPlayer.player();
        int wheels = ThreadLocalRandom.current().nextInt(MIN_WHEELS, MAX_WHEELS + 1);
        this.giveWheels(winner, wheels);

        Bukkit.broadcast(Text.of(Prefix.RANDOM_EVENT + "<green>" + winner.getName() + " <gray>won the "
                + RandomEvents.ITEM_HUNT.coloredName() + " <gray>and receives <yellow>" + wheels
                + (wheels == 1 ? " Wheel" : " Wheels") + " of Fortune<gray>!"));

        Bukkit.getOnlinePlayers().forEach(players ->
                players.playSound(players.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1));

        return true;
    }

    /**
     * Only the finder is paid, teammates included — the hunt is a personal race.
     */
    private void giveWheels(Player player, int amount) {
        ItemStack wheels = CustomMaterials.WHEEL_OF_FORTUNE.itemStack(amount);

        player.getInventory().addItem(wheels).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }
}
