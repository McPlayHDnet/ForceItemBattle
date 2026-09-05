package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.commands.Precondition;
import forceitembattle.gui.CollectionBookInventory;
import forceitembattle.gui.GuiContext;
import forceitembattle.util.Text;
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
public final class CommandCollection extends CustomCommand implements CustomTabCompleter {

    private final GuiContext gui;

    public CommandCollection(GuiContext gui) {
        super("collection");
        this.gui = gui;

        setUsage("[player]");
        setDescription("Show your collection book");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of();
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (args.length == 0) {
            new CollectionBookInventory(this.gui, player.getName(), player.getUniqueId()).open(player);
            return;
        }

        UUID targetUuid = resolvePlayer(args[0]);
        if (targetUuid == null) {
            player.sendMessage(Text.of("<yellow>" + args[0] + " <red>was not found"));
            return;
        }

        new CollectionBookInventory(this.gui, args[0], targetUuid).open(player);
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
        if (args.length != 1) {
            return List.of();
        }
        return CustomTabCompleter.onlinePlayerNames();
    }
}
