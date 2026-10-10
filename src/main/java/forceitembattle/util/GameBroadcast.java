package forceitembattle.util;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

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

    /** Played at each player's own position, so everyone hears it at full volume. */
    public static void playToAll(Sound sound, float volume, float pitch) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}
