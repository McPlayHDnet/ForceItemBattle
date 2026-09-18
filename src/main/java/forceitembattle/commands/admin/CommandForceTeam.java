package forceitembattle.commands.admin;

import static forceitembattle.commands.Precondition.OP;
import static forceitembattle.commands.Precondition.PRE_GAME;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.Precondition;
import forceitembattle.manager.TeamsManager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Roster;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.Text;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CommandForceTeam extends CustomCommand {

    private final Roster roster;
    private final TeamsManager teamManager;

    public CommandForceTeam(Roster roster, TeamsManager teamManager) {
        super("forceteam");
        this.roster = roster;
        this.teamManager = teamManager;
        setUsage("<name> <player1> (player2)");
        setDescription("Force create a team");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(OP,
                Precondition.setting(GameSetting.TEAM, "<red>Teams are not enabled!"),
                PRE_GAME);
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (args.length < 2 || args.length > 3) {
            msgUsage(player);
            return;
        }

        String teamName = args[0];
        Player player1 = Bukkit.getPlayer(args[1]);

        if (player1 == null) {
            player.sendMessage(Text.of("<yellow>" + args[1] + " <red>is not online"));
            return;
        }

        ForceItemPlayer first = this.roster.get(player1.getUniqueId());

        if (args.length == 3) {
            Player player2 = Bukkit.getPlayer(args[2]);

            if (player2 == null) {
                player.sendMessage(Text.of("<yellow>" + args[2] + " <red>is not online"));
                return;
            }

            ForceItemPlayer second = this.roster.get(player2.getUniqueId());
            this.teamManager.create(first, second, teamName);
            player.sendMessage(Text.of("<dark_aqua>Successfully created team <green>" + teamName));
        } else {
            this.teamManager.create(first, null, teamName);
            player.sendMessage(Text.of("<dark_aqua>Successfully created solo team <green>" + teamName));
        }
    }
}
