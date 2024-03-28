package forceitembattle.commands.admin;

import forceitembattle.commands.CustomCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CommandSkip extends CustomCommand {

    public CommandSkip() {
        super("skip");

        setDescription("Skip current item for player");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!this.plugin.getGamemanager().isMidGame()) {
            player.sendMessage(ChatColor.RED + "The game is not running. Start it first with /start");
            return;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /skip <player_name>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target != null) {
            player.sendMessage("§7Skipped this item for " + target.getName());
            //this.forceItemBattle.logToFile("[" + this.forceItemBattle.getTimer().getTime() + "] | " + args[0] + " skipped " + this.forceItemBattle.getGamemanager().getCurrentMaterial(Bukkit.getPlayer(args[0])));
            this.plugin.getGamemanager().forceSkipItem(target);
        } else {
            player.sendMessage(ChatColor.RED + "This player is not online");
        }
    }
}
