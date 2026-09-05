package forceitembattle.randomevents;

import forceitembattle.model.Find;
import forceitembattle.util.Prefix;
import forceitembattle.util.Text;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class ItemHunt implements RandomEvent {

    private static final int MIN_WHEELS = 1;
    private static final int MAX_WHEELS = 3;

    private final EventContext context;

    @Override
    public void start() {
        Bukkit.broadcast(Text.of(Prefix.RANDOM_EVENT + "<gold><b>Item Hunt</b><reset><gray> has begun!"));
        Bukkit.broadcast(Text.of(Prefix.RANDOM_EVENT + "<gray>First to collect their current item <red>without skipping "
                + "<gray>wins <yellow>" + MIN_WHEELS + "-" + MAX_WHEELS + " Wheels of Fortune<gray>!"));

        Bukkit.getOnlinePlayers().forEach(players ->
                players.playSound(players.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1.4f));
    }

    @Override
    public boolean onFoundItem(Find find) {
        if (find.skipped() || find.backToBack()) {
            return false;
        }

        Player winner = find.player();
        int wheels = ThreadLocalRandom.current().nextInt(MIN_WHEELS, MAX_WHEELS + 1);
        // Only the finder is paid, teammates included -- the hunt is a personal race.
        EventRewards.giveWheels(winner, wheels);

        Bukkit.broadcast(Text.of(Prefix.RANDOM_EVENT + "<green>" + winner.getName() + " <gray>won the "
                + RandomEvents.ITEM_HUNT.coloredName() + " <gray>and receives <yellow>" + wheels
                + (wheels == 1 ? " Wheel" : " Wheels") + " of Fortune<gray>!"));

        Bukkit.getOnlinePlayers().forEach(players ->
                players.playSound(players.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1));

        return true;
    }

}
