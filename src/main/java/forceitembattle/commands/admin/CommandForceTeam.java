package forceitembattle.commands.admin;

import forceitembattle.commands.CustomCommand;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandForceTeam extends CustomCommand {

    public CommandForceTeam() {
        super("forceteam");
        setUsage("<name> <player1> (player2)");
        setDescription("Force create a team");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!player.isOp()) {
            return;
        }

        if(!this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>Teams are not enabled!"));
            return;
        }


        if(!this.plugin.getGamemanager().isPreGame()) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>The game already started"));
            return;
        }

        if (args.length < 2 || args.length > 3) {
            msgUsage(player);
            return;
        }

        String teamName = args[0];
        Player player1 = Bukkit.getPlayer(args[1]);

        if (player1 == null) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<yellow>" + args[1] + " <red>is not online"));
            return;
        }

        ForceItemPlayer first = this.plugin.getGamemanager().getForceItemPlayer(player1.getUniqueId());

        if (args.length == 3) {
            Player player2 = Bukkit.getPlayer(args[2]);

            if (player2 == null) {
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<yellow>" + args[2] + " <red>is not online"));
                return;
            }

            ForceItemPlayer second = this.plugin.getGamemanager().getForceItemPlayer(player2.getUniqueId());
            this.plugin.getTeamManager().create(first, second, teamName);
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_aqua>Successfully created team <green>" + teamName));
        } else {
            // Create solo team
            this.plugin.getTeamManager().create(first, null, teamName);
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_aqua>Successfully created solo team <green>" + teamName));
        }
    }
}
