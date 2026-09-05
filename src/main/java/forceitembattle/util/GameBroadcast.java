package forceitembattle.util;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

public final class GameBroadcast {

    private GameBroadcast() {
    }

    /**
     * Announces a find to whoever should hear it: the whole server outside event mode, otherwise only
     * the score owner's members — the team in a team game, just this player in solo.
     */
    public static void announce(Component message, ForceItemPlayer forceItemPlayer, GameContext context) {
        if (context.eventDisabled()) {
            Bukkit.broadcast(message);
            return;
        }

        forceItemPlayer.squad().forEach(member -> member.player().sendMessage(message));
    }
}
