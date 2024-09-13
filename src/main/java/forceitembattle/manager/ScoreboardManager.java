package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.ForceItemPlayer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class ScoreboardManager {

    public ScoreboardManager(Player player) {
        //this.updatePlayerPrefix(player);
    }

    public void updatePlayerPrefix(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Gamemanager gameManager = ForceItemBattle.getInstance().getGamemanager();
        MiniMessage miniMessage = gameManager.getMiniMessage();
        GameSettings settings = ForceItemBattle.getInstance().getSettings();

        for (ForceItemPlayer forceItemPlayer : gameManager.forceItemPlayerMap().values()) {
            Team currentTeam = scoreboard.getTeam(getTeamName(settings, forceItemPlayer));
            if (currentTeam == null) {
                currentTeam = scoreboard.registerNewTeam(getTeamName(settings, forceItemPlayer));
            }

            Material material = settings.isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getCurrentMaterial() : forceItemPlayer.currentMaterial();

            if(settings.isSettingEnabled(GameSetting.TEAM)) {
                currentTeam.prefix(miniMessage.deserialize("<yellow>[" + forceItemPlayer.currentTeam().getTeamDisplay() + "] "));
            }
            currentTeam.suffix(miniMessage.deserialize(" <gray>[<gold>" + gameManager.getMaterialName(material) + " <reset><color:#4e5c24>" + ForceItemBattle.getInstance().getItemDifficultiesManager().getUnicodeFromMaterial(true, material) + "<gray>]"));

            currentTeam.addPlayer(forceItemPlayer.player());

        }

    }

    private String getTeamName(GameSettings gameSettings, ForceItemPlayer forceItemPlayer) {
        if(!gameSettings.isSettingEnabled(GameSetting.TEAM)) {
            return String.valueOf(forceItemPlayer.player().getUniqueId());
        }
        return String.valueOf(forceItemPlayer.currentTeam().getTeamId()) + forceItemPlayer.currentTeam().getTeamId();
    }

}
