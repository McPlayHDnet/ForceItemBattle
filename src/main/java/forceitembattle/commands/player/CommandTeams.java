package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandTeams extends CustomCommand {

    public CommandTeams() {
        super("teams");
        setDescription("Everything about teams");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if(!this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>Teams are not enabled!"));
            return;
        }

        if(this.plugin.getTimer().getTime() != 0 || this.plugin.getGamemanager().isMidGame()) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>The game already started"));
            return;
        }

        if (args.length == 1) {
            if(args[0].equalsIgnoreCase("leave")) {
                ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
                this.plugin.getTeamManager().leave(forceItemPlayer);
                return;
            }
            if(args[0].equalsIgnoreCase("list")) {
                ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
                this.plugin.getTeamManager().showTeamList(forceItemPlayer);
                return;
            }
            this.sendHelpMessage(player);
            return;
        }

        if (args.length == 2) {
            if(args[0].equalsIgnoreCase("invite")) {
                if(Bukkit.getPlayer(args[1]) == null) {
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<yellow>" + args[1] + " <red>is not online"));
                    return;
                }
                ForceItemPlayer inviter = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
                ForceItemPlayer invitee = this.plugin.getGamemanager().getForceItemPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
                this.plugin.getTeamManager().invite(inviter, invitee);
                return;
            }

            if(args[0].equalsIgnoreCase("accept")) {
                if(Bukkit.getPlayer(args[1]) == null) {
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<yellow>" + args[1] + " <red>is not online"));
                    return;
                }
                ForceItemPlayer inviter = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
                ForceItemPlayer invitee = this.plugin.getGamemanager().getForceItemPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
                this.plugin.getTeamManager().accept(inviter, invitee);
                return;
            }

            if(args[0].equalsIgnoreCase("decline")) {
                if(Bukkit.getPlayer(args[1]) == null) {
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<yellow>" + args[1] + " <red>is not online"));
                    return;
                }
                ForceItemPlayer inviter = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
                ForceItemPlayer invitee = this.plugin.getGamemanager().getForceItemPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
                this.plugin.getTeamManager().decline(inviter, invitee);
                return;
            }
            this.sendHelpMessage(player);
            return;
        }

        this.sendHelpMessage(player);
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(" ");
        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<gold><b>Teams</b> <gray>- <white>Help"));
        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>- <white>/teams invite <player> <dark_gray>- <gray>Invite a player"));
        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>- <white>/teams accept <player> <dark_gray>- <gray>Accept a team invite"));
        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>- <white>/teams decline <player> <dark_gray>- <gray>Decline a team invite"));
        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>- <white>/teams list <dark_gray>- <gray>Shows all team member"));
        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>- <white>/teams leave <dark_gray>- <gray>Leave the team"));
        player.sendMessage(" ");
    }
}
