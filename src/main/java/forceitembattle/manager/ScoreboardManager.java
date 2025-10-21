package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.ForceItemPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Comparator;
import java.util.List;

public class ScoreboardManager {

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
        MiniMessage mini = gameManager.getMiniMessage();
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
                forceitembattle.util.Team viewerTeam = gameManager.forceItemPlayerMap()
                        .getOrDefault(viewer.getUniqueId(), null) != null
                        ? gameManager.forceItemPlayerMap().get(viewer.getUniqueId()).currentTeam()
                        : null;

                boolean sameTeam = viewerTeam != null && viewerTeam.equals(fibPlayer.currentTeam());

                String color = sameTeam ? "<green>" : "<yellow>";

                team.prefix(mini.deserialize(color + "[" + fibPlayer.currentTeam().getTeamDisplay() + "] "));
            } else {
                team.prefix(mini.deserialize(""));
            }

            Material mat;

            if (settings.isSettingEnabled(GameSetting.TEAM) && fibPlayer.currentTeam() != null) {
                mat = fibPlayer.currentTeam().getCurrentMaterial();
            } else {
                mat = fibPlayer.currentMaterial();
            }

            if (mat != null) {
                String itemIcon = ForceItemBattle.getInstance()
                        .getItemDifficultiesManager()
                        .getUnicodeFromMaterial(true, mat);

                team.suffix(mini.deserialize(
                        " <gray>[<gold>" + gameManager.getMaterialName(mat)
                                + " <reset><color:#4e5c24>" + itemIcon + "<gray>]"
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
