package forceitembattle.commands.admin;

import forceitembattle.commands.CustomCommand;
import forceitembattle.util.GameState;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;

public class CommandResume extends CustomCommand {

    public CommandResume() {
        super("resume");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!this.plugin.getGamemanager().isPausedGame()) {
            player.sendMessage("§cThe timer is not paused!");
            return;
        }

        Bukkit.broadcastMessage("§6The timer has been resumed!");
        Bukkit.getWorld("world").setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        this.plugin.getGamemanager().setCurrentGameState(GameState.MID_GAME);
    }
}
