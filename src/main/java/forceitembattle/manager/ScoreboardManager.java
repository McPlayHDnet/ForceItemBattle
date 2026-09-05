package forceitembattle.manager;

import forceitembattle.model.ActiveTrader;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Roster;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class ScoreboardManager implements Manager {

    private final Roster roster;
    private final GameSettings settings;
    private final WanderingTraderManager wanderingTraderManager;
    private final ItemDifficultiesManager itemDifficultiesManager;

    public ScoreboardManager(Roster roster, GameSettings settings,
                             WanderingTraderManager wanderingTraderManager,
                             ItemDifficultiesManager itemDifficultiesManager) {
        this.roster = roster;
        this.settings = settings;
        this.wanderingTraderManager = wanderingTraderManager;
        this.itemDifficultiesManager = itemDifficultiesManager;
    }

    public void setupForPlayer(Player player) {
        if (player.getScoreboard() == Bukkit.getScoreboardManager().getMainScoreboard()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        updateForPlayer(player);
    }

    public void updateAllPlayers() {
        List<Nameplate> nameplates = buildNameplates();
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateForPlayer(player, nameplates);
        }
    }

    public void updateForPlayer(Player viewer) {
        updateForPlayer(viewer, buildNameplates());
    }

    /**
     * One rostered player's row, as it appears on <em>every</em> board: none of it depends on who is
     * looking.
     */
    private record Nameplate(String teamName, Component prefix, Component suffix, Player target) {
    }

    /**
     * Builds every row once.
     *
     * <p>This used to happen inside {@link #updateForPlayer(Player)}, which meant one sort and two
     * MiniMessage parses per rostered player <em>per viewer</em> — quadratic in the player count, on
     * a method called on every find. Components are immutable, so a single instance is safe to hand
     * to every board.
     */
    private List<Nameplate> buildNameplates() {
        List<Nameplate> nameplates = new ArrayList<>();

        List<ForceItemPlayer> fibPlayers = this.roster.players().values()
                .stream()
                .sorted(Comparator.comparingInt(p -> {
                    forceitembattle.model.Team team = p.currentTeam();
                    return team != null ? team.getTeamId() : Integer.MAX_VALUE;
                }))
                .toList();

        for (ForceItemPlayer fibPlayer : fibPlayers) {
            Player target = fibPlayer.player();
            if (target == null) continue;

            Component prefix = fibPlayer.isInTeam()
                    ? Text.of(fibPlayer.currentTeam().getTeamDisplay() + " ")
                    : Text.of("");

            Material mat = fibPlayer.activeMaterial();
            Component suffix;
            if (mat != null) {
                String itemIcon = this.itemDifficultiesManager
                        .getUnicodeFromMaterial(true, mat);

                suffix = Text.of(
                        " <gray>[<gold>" + CustomMaterials.nameOf(mat)
                                + " <reset><shadow:black:0.4>" + itemIcon + "</shadow><gray>]"
                );
            } else {
                suffix = Component.empty();
            }

            nameplates.add(new Nameplate(
                    getUniqueTeamName(this.settings, fibPlayer), prefix, suffix, target));
        }
        return nameplates;
    }

    private void updateForPlayer(Player viewer, List<Nameplate> nameplates) {
        Scoreboard board = viewer.getScoreboard();

        board.getTeams().forEach(Team::unregister);

        for (Nameplate nameplate : nameplates) {
            // Teammates share a team name ("T_<id>"), so the second one finds the team the first
            // registered. Registering it twice would throw.
            org.bukkit.scoreboard.Team team = board.getTeam(nameplate.teamName());
            if (team == null) {
                team = board.registerNewTeam(nameplate.teamName());
            }

            team.prefix(nameplate.prefix());
            team.suffix(nameplate.suffix());
            team.addPlayer(nameplate.target());
        }

        for (ActiveTrader trader : this.wanderingTraderManager.activeTraders()) {
            String name = "TRADER_" + trader.getKind().name();
            org.bukkit.scoreboard.Team traderTeam = board.getTeam(name);
            if (traderTeam == null) {
                traderTeam = board.registerNewTeam(name);
                traderTeam.color(trader.getKind().getGlowColor());
            }
            traderTeam.addEntry(trader.getUuid().toString());
        }
    }

    private String getUniqueTeamName(GameSettings settings, ForceItemPlayer fibPlayer) {
        if (!fibPlayer.isInTeam()) {
            return "P_" + fibPlayer.player().getUniqueId().toString().substring(0, 10);
        }
        return "T_" + fibPlayer.currentTeam().getTeamId();
    }
}
