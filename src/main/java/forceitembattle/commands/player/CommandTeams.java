package forceitembattle.commands.player;

import static forceitembattle.commands.Precondition.PRE_GAME;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.Precondition;
import forceitembattle.manager.TeamsManager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Roster;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.Text;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CommandTeams extends CustomCommand {

    /** The subcommands that take a player name as their second argument. */
    private static final Set<String> TARGETED_SUBCOMMANDS = Set.of("invite", "accept", "decline");

    private final Roster roster;
    private final TeamsManager teamManager;

    public CommandTeams(Roster roster, TeamsManager teamManager) {
        super("teams");
        this.roster = roster;
        this.teamManager = teamManager;
        setDescription("Everything about teams");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(Precondition.setting(GameSetting.TEAM, "<red>Teams are not enabled!"),
                PRE_GAME);
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        // Resolved once, and required. Everything below hands it to TeamsManager, which
        // dereferences it without checking -- and a player with no roster entry is possible even
        // in PRE_GAME if the roster has not caught up with them yet.
        ForceItemPlayer self = this.roster.get(player.getUniqueId());
        if (self == null) {
            player.sendMessage(Text.of("<red>You are not in this round."));
            return;
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("leave")) {
                this.teamManager.leave(self);
                return;
            }
            if (args[0].equalsIgnoreCase("list")) {
                this.teamManager.showTeamList(self);
                return;
            }
            this.sendHelpMessage(player);
            return;
        }

        if (args.length == 2) {
            if (!TARGETED_SUBCOMMANDS.contains(args[0].toLowerCase())) {
                this.sendHelpMessage(player);
                return;
            }

            // Looked up once: a second getPlayer call per branch would guard one object and
            // dereference another.
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(Text.of("<yellow>" + args[1] + " <red>is not online"));
                return;
            }

            ForceItemPlayer other = this.roster.get(target.getUniqueId());
            if (other == null) {
                player.sendMessage(Text.of("<yellow>" + target.getName() + " <red>is not in this round."));
                return;
            }

            switch (args[0].toLowerCase()) {
                case "invite" -> this.teamManager.invite(self, other);
                case "accept" -> this.teamManager.accept(self, other);
                case "decline" -> this.teamManager.decline(self, other);
                default -> this.sendHelpMessage(player);
            }
            return;
        }

        this.sendHelpMessage(player);
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(" ");
        player.sendMessage(Text.of("<gold><b>Teams</b> <gray>- <white>Help"));
        player.sendMessage(Text.of("<dark_gray>- <white>/teams invite <player> <dark_gray>- <gray>Invite a player"));
        player.sendMessage(Text.of("<dark_gray>- <white>/teams accept <player> <dark_gray>- <gray>Accept a team invite"));
        player.sendMessage(Text.of("<dark_gray>- <white>/teams decline <player> <dark_gray>- <gray>Decline a team invite"));
        player.sendMessage(Text.of("<dark_gray>- <white>/teams list <dark_gray>- <gray>Shows all team member"));
        player.sendMessage(Text.of("<dark_gray>- <white>/teams leave <dark_gray>- <gray>Leave the team"));
        player.sendMessage(" ");
    }
}
