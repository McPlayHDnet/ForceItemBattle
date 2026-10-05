package forceitembattle.util;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

public final class GameBroadcast {

    private GameBroadcast() {
    }

    /** The whole server outside event mode, otherwise only the score owner's members. */
    public static void announce(Component message, ForceItemPlayer forceItemPlayer, GameContext context) {
        if (context.eventDisabled()) {
            Bukkit.broadcast(message);
            return;
        }

        forceItemPlayer.squad().forEach(member -> member.player().sendMessage(message));
    }
}
