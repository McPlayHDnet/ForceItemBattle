package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.Text;
import java.util.Comparator;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class ScoreboardManager implements Manager {

    private final ForceItemBattle plugin;

    public ScoreboardManager(ForceItemBattle plugin) {
        this.plugin = plugin;
    }

    public void setupForPlayer(Player player) {
        if (player.getScoreboard() == Bukkit.getScoreboardManager().getMainScoreboard()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        updateForPlayer(player);
    }

    public void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateForPlayer(player);
        }
    }

    public void updateForPlayer(Player viewer) {
        Gamemanager gameManager = plugin.getGamemanager();
        GameSettings settings = plugin.getSettings();
        Scoreboard board = viewer.getScoreboard();

        board.getTeams().forEach(Team::unregister);

        List<ForceItemPlayer> fibPlayers = gameManager.forceItemPlayerMap().values()
                .stream()
                .sorted(Comparator.comparingInt(p -> {
                    forceitembattle.util.Team team = p.currentTeam();
                    return team != null ? team.getTeamId() : Integer.MAX_VALUE;
                }))
                .toList();

        for (ForceItemPlayer fibPlayer : fibPlayers) {
            Player target = fibPlayer.player();
            if (target == null) continue;

            String teamName = getUniqueTeamName(settings, fibPlayer);
            org.bukkit.scoreboard.Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }

            if (settings.isSettingEnabled(GameSetting.TEAM) && fibPlayer.currentTeam() != null) {
                team.prefix(Text.of(fibPlayer.currentTeam().getTeamDisplay() + " "));
            } else {
                team.prefix(Text.of(""));
            }

            Material mat;

            if (settings.isSettingEnabled(GameSetting.TEAM) && fibPlayer.currentTeam() != null) {
                mat = fibPlayer.currentTeam().getCurrentMaterial();
            } else {
                mat = fibPlayer.currentMaterial();
            }

            if (mat != null) {
                String itemIcon = this.plugin
                        .getItemDifficultiesManager()
                        .getUnicodeFromMaterial(true, mat);

                team.suffix(Text.of(
                        " <gray>[<gold>" + gameManager.getMaterialName(mat)
                                + " <reset><shadow:black:0.4>" + itemIcon + "</shadow><gray>]"
                ));
            } else {
                team.suffix(Component.empty());
            }

            team.addPlayer(fibPlayer.player());
        }
    }

    private String getUniqueTeamName(GameSettings settings, ForceItemPlayer fibPlayer) {
        if (!settings.isSettingEnabled(GameSetting.TEAM) || fibPlayer.currentTeam() == null) {
            return "P_" + fibPlayer.player().getUniqueId().toString().substring(0, 10);
        }
        return "T_" + fibPlayer.currentTeam().getTeamId();
    }
}
