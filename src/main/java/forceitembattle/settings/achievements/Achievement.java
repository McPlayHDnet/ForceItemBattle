package forceitembattle.settings.achievements;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.ForceItem;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.ForceItemPlayerStats;
import lombok.Builder;
import lombok.Data;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;

@Data
@Builder
public class Achievement {

    private final String title;
    private final List<String> description;
    private final List<AchievementRequirement> requirements;

    public void grantTo(ForceItemPlayer forceItemPlayer) {
        forceItemPlayer.player().playSound(forceItemPlayer.player(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1, 1);
        Bukkit.getOnlinePlayers().forEach(players -> {
            players.sendMessage(Component.empty());
            players.sendMessage(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize("<dark_gray>[<yellow>❋<dark_gray>] <gold>" + forceItemPlayer.player().getName() + " <gray>has made the achievement <hover:show_text:'<dark_aqua>" + this.title + "<newline><dark_aqua>" + this.description.get(1) + "'><dark_aqua>[" + this.title + "]</hover>"));
            players.sendMessage(Component.empty());
        });

        ForceItemBattle.getInstance().getAchievementManager().addAchievementDone(ForceItemBattle.getInstance().getStatsManager().playerStats(forceItemPlayer.player().getName()), this);
    }

    public boolean checkRequirements(Player player, Event event) {
        ForceItemPlayerStats playerStats = ForceItemBattle.getInstance().getStatsManager().playerStats(player.getName());
        if(playerStats.achievementsDone().contains(this.title)) {
            return false;
        }
        for(AchievementRequirement requirement : this.requirements) {
            if(!requirement.isMet(player, event)) {
                return false;
            }
        }
        return true;
    }
}
