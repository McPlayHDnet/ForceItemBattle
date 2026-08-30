package forceitembattle.commands.player;

import forceitembattle.model.Standings;
import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.settings.GameSetting;
import forceitembattle.gui.FinishInventory;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Team;
import forceitembattle.util.Text;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandResult extends CustomCommand {

    public int place;
    /** Match the reveal counter belongs to — a new match restarts the paging from last place. */
    private UUID lastSeenMatchId;

    public CommandResult(ForceItemBattle plugin) {
        super(plugin, "result");
        setDescription("Show the next player's result");

        this.place = -1;
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        // The reveal pages from last place to the winner, so each match needs a fresh counter.
        UUID matchId = this.plugin.getGamemanager().getMatchHistory().getMatchId();
        if (!Objects.equals(matchId, this.lastSeenMatchId)) {
            this.lastSeenMatchId = matchId;
            this.place = -1;
        }

        if (this.plugin.getTimerManager().getTimeLeft() > 0) {
            return;
        }

        if (args.length == 1) {
            UUID uuid = null;
            Team team = null;
            if (!this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
                try {
                    uuid = UUID.fromString(args[0]);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(Text.of("<red>Invalid UUID."));
                    return;
                }
            } else {
                try {
                    team = this.plugin.getTeamManager().getTeams().get(Integer.parseInt(args[0].replace("#", "")) - 1);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(Text.of("<red>Invalid team."));
                }
            }

            new FinishInventory(
                    this.plugin,
                    this.plugin.getRoster().get(uuid),
                    team,
                    null,
                    false
            ).open(player);
            return;
        }

        if (args.length == 0 && player.isOp()) {
            showNextPlayer(player);
        }
    }

    private void showNextPlayer(Player player) {
        if (!this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            if (this.plugin.getRoster().players().isEmpty() || this.place == 0) {
                player.sendMessage("No more players left.");
                return;
            }

            Map<UUID, ForceItemPlayer> sortedMapDesc = Standings.sortedByScore(this.plugin.getRoster().players().entrySet().stream().filter(entry -> !entry.getValue().isSpectator()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)), false);
            if (this.place == -1) {
                this.place = sortedMapDesc.size();
            }

            Map<ForceItemPlayer, Integer> placesMap = Standings.ofPlayers(sortedMapDesc);

            ForceItemPlayer currentPlayer = sortedMapDesc.values().toArray(new ForceItemPlayer[0])[this.place - 1];
            int currentPlace = placesMap.get(currentPlayer);

            // The winner is revealed last; the link may only go out once that reveal finished.
            Runnable onRevealComplete = this.place == 1
                    ? () -> this.plugin.getGamemanager().getMatchHistory().markResultsRevealed()
                    : null;
            Bukkit.getOnlinePlayers().forEach(players -> new FinishInventory(this.plugin, currentPlayer, null, currentPlace, true, onRevealComplete).open(players));

            this.place--;

        } else {
            if (this.plugin.getRoster().players().isEmpty() || this.place == 0) {
                player.sendMessage("No more teams left.");
                return;
            }

            Map<Team, Integer> placesMap = Standings.ofTeams(this.plugin.getTeamManager().getTeams());
            if (this.place == -1) {
                this.place = placesMap.size();
            }

            Team currentTeam = placesMap.keySet().toArray(new Team[0])[this.place - 1];
            int currentPlace = placesMap.get(currentTeam);

            Runnable onRevealComplete = this.place == 1
                    ? () -> this.plugin.getGamemanager().getMatchHistory().markResultsRevealed()
                    : null;
            Bukkit.getOnlinePlayers().forEach(players -> new FinishInventory(this.plugin, null, currentTeam, currentPlace, true, onRevealComplete).open(players));

            this.place--;

        }

    }


}
