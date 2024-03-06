package forceitembattle.commands.admin;

import forceitembattle.commands.CustomCommand;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CommandStopTimer extends CustomCommand {

    public CommandStopTimer() {
        super("stoptimer");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!this.forceItemBattle.getGamemanager().isMidGame()) {
            player.sendMessage(ChatColor.RED + "The game is not running. Start it first with /start");
            return;
        }

        this.forceItemBattle.getTimer().setTime(1);
    }
}
