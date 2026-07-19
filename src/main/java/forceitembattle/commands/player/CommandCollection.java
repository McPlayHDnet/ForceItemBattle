package forceitembattle.commands.player;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.gui.CollectionBookInventory;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Opens the collection book — your own, or another player's.
 *
 * <p>The book fills itself: {@link CollectionBookInventory} kicks off the read-through load and
 * repaints when it lands, so this just resolves the target and opens.
 */
public class CommandCollection extends CustomCommand implements CustomTabCompleter {

    public CommandCollection(ForceItemBattle plugin) {
        super(plugin, "collection");

        setUsage("[player]");
        setDescription("Show your collection book");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (args.length == 0) {
            new CollectionBookInventory(this.plugin, player.getName(), player.getUniqueId()).open(player);
            return;
        }

        UUID targetUuid = resolvePlayer(args[0]);
        if (targetUuid == null) {
            player.sendMessage(Text.of("<yellow>" + args[0] + " <red>was not found"));
            return;
        }

        new CollectionBookInventory(this.plugin, args[0], targetUuid).open(player);
    }

    /**
     * Online exact match first, then the offline cache. Deliberately not
     * {@code Bukkit.getOfflinePlayer(name)} — that can block on a Mojang lookup for an unknown name.
     */
    private UUID resolvePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(name);
        if (offline != null) {
            return offline.getUniqueId();
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                completions.add(online.getName());
            }
        }
        return completions;
    }
}
