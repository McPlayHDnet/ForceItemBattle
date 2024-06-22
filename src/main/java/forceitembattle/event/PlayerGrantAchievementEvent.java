package forceitembattle.event;

import forceitembattle.settings.achievements.Achievements;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerGrantAchievementEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;
    private final Achievements achievement;

    public PlayerGrantAchievementEvent(Player player, Achievements achievement) {
        this.player = player;
        this.achievement = achievement;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
