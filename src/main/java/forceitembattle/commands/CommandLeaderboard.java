package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.PlayerStat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandLeaderboard implements CommandExecutor {

    private final ForceItemBattle forceItemBattle;

    public CommandLeaderboard(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getCommand("top").setTabCompleter(new TabCompletion(this.forceItemBattle));
        this.forceItemBattle.getCommand("top").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player player)) return false;

        if (strings.length == 0) {
            this.forceItemBattle.getStatsManager().topMessage(player, this.forceItemBattle.getStatsManager().top(PlayerStat.HIGHEST_SCORE), PlayerStat.HIGHEST_SCORE);
            return false;
        }

        if (strings.length == 1) {
            PlayerStat leaderStat = null;
            for (PlayerStat playerStat : PlayerStat.values()) {
                if (playerStat.name().equalsIgnoreCase(strings[0])) {
                    leaderStat = playerStat;
                    break;
                }
            }

            if (leaderStat == null) {
                player.sendMessage("§e" + strings[0] + " §cdoes not exist in leaderboard");
                return false;
            }
            this.forceItemBattle.getStatsManager().topMessage(player, this.forceItemBattle.getStatsManager().top(leaderStat), leaderStat);
        }

        return false;
    }
}
