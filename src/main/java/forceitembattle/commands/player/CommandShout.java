package forceitembattle.commands.player;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.util.Text;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CommandShout extends CustomCommand {

    private static final Set<Player> shoutingPlayers = new HashSet<>();

    public CommandShout(ForceItemBattle plugin) {
        super(plugin, "shout");
        setDescription("Send global message when team chat is enabled");
    }

    public static boolean isShouting(Player player) {
        return shoutingPlayers.contains(player);
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (args.length == 0) {
            if (shoutingPlayers.contains(player)) {
                shoutingPlayers.remove(player);
                player.sendMessage(Text.of("<gray>Shout mode: <red>OFF"));
            } else {
                shoutingPlayers.add(player);
                player.sendMessage(Text.of("<gray>Shout mode: <green>ON"));
            }
            return;
        }

        // With a message, shout it once without touching shout mode.
        Bukkit.broadcast(Text.of(
                "<gold>" + player.getName() + " <dark_gray>» <white>" + String.join(" ", args)
        ));
    }
}
