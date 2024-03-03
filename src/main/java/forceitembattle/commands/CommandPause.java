package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.GameState;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandPause implements CommandExecutor {

    private final ForceItemBattle forceItemBattle;

    public CommandPause(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getCommand("pause").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player player)) return false;

        if (!this.forceItemBattle.getGamemanager().isMidGame()) {
            player.sendMessage("§cThe timer is already paused.");
            return false;
        }

        Bukkit.broadcastMessage("§6The timer has been paused!");
        Bukkit.getWorld("world").setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        this.forceItemBattle.getGamemanager().setCurrentGameState(GameState.PAUSED_GAME);
        return false;
    }
}
