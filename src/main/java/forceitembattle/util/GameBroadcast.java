package forceitembattle.util;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

public final class GameBroadcast {

    private GameBroadcast() {
    }

    public static void announce(Component message, ForceItemPlayer forceItemPlayer, GameContext context) {
        if (context.eventDisabled()) {
            Bukkit.broadcast(message);
        } else if (context.teamGame()) {
            forceItemPlayer.currentTeam().getPlayers().forEach(p -> p.player().sendMessage(message));
        } else {
            forceItemPlayer.player().sendMessage(message);
        }
    }
}
