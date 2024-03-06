package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.util.PlayerStat;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandLeaderboard extends CustomCommand implements CustomTabCompleter {

    public CommandLeaderboard() {
        super("top");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (args.length == 0) {
            this.plugin.getStatsManager().topMessage(player, this.plugin.getStatsManager().top(PlayerStat.HIGHEST_SCORE), PlayerStat.HIGHEST_SCORE);
            return;
        }

        if(args.length == 1) {
            PlayerStat leaderStat = null;
            for(PlayerStat playerStat : PlayerStat.values()) {
                if(playerStat.name().equalsIgnoreCase(args[0])) {
                    leaderStat = playerStat;
                    break;
                }
            }

            if (leaderStat == null) {
                player.sendMessage("§e" + args[0] + " §cdoes not exist in leaderboard");
                return;
            }
            this.plugin.getStatsManager().topMessage(player, this.plugin.getStatsManager().top(leaderStat), leaderStat);
        }

    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        return Arrays.stream(PlayerStat.values()).filter(PlayerStat::isInLeaderboard).map(stat -> stat.name().toLowerCase()).collect(Collectors.toList());
    }
}
