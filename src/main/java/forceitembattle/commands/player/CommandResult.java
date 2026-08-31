package forceitembattle.commands.player;

import forceitembattle.commands.Precondition;
import java.util.List;
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
import javax.annotation.Nullable;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CommandResult extends CustomCommand {

    public int place;
    /** Match the reveal counter belongs to — a new match restarts the paging from last place. */
    private UUID lastSeenMatchId;

    public CommandResult(ForceItemBattle plugin) {
        super(plugin, "result");
        setDescription("Show the next player's result");

        this.place = -1;
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of();
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
            if (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
                Team team = this.teamAt(args[0]);
                if (team == null) {
                    player.sendMessage(Text.of("<red>Invalid team."));
                    return;
                }
                new FinishInventory(this.plugin, null, team, null, false).open(player);
                return;
            }

            UUID uuid;
            try {
                uuid = UUID.fromString(args[0]);
            } catch (IllegalArgumentException e) {
                player.sendMessage(Text.of("<red>Invalid UUID."));
                return;
            }

            ForceItemPlayer target = this.plugin.getRoster().get(uuid);
            if (target == null) {
                player.sendMessage(Text.of("<red>Nobody with that id played this round."));
                return;
            }

            new FinishInventory(this.plugin, target, null, null, false).open(player);
            return;
        }

        if (args.length == 0 && player.isOp()) {
            showNextPlayer(player);
        }
    }

    /**
     * The team {@code argument} names, or null when it names none.
     *
     * <p>Both halves of this used to be wrong. The lookup ran inside a
     * {@code catch (IllegalArgumentException)}, which catches the {@code NumberFormatException}
     * from bad text but <em>not</em> the {@code IndexOutOfBoundsException} from
     * {@code List.get()} — so {@code /result #99} threw out of the command. And the catch that did
     * fire had no {@code return} after it, so a failed parse fell through to open a result screen
     * with nothing to show.
     */
    @Nullable
    private Team teamAt(String argument) {
        int index;
        try {
            index = Integer.parseInt(argument.replace("#", "")) - 1;
        } catch (NumberFormatException e) {
            return null;
        }

        List<Team> teams = this.plugin.getTeamManager().getTeams();
        return index >= 0 && index < teams.size() ? teams.get(index) : null;
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
