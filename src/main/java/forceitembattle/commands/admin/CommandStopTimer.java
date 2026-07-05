package forceitembattle.commands.admin;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.util.Text;
import org.bukkit.entity.Player;

public class CommandStopTimer extends CustomCommand {

    public CommandStopTimer(ForceItemBattle plugin) {
        super(plugin, "stoptimer");

        setDescription("Stop the timer and end the game");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (player.isOp()) {
            if (!this.plugin.getGamemanager().isMidGame()) {
                player.sendMessage(Text.of("<red>The game is not running. Start it first with /start"));
                return;
            }

            this.plugin.getTimer().setTimeLeft(1);
        }

    }
}
