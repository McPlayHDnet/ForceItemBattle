package forceitembattle.util;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameContext;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class GameBroadcast {

    private GameBroadcast() {
    }

    /**
     * The whole server outside event mode, otherwise only the score owner's members. In Mirror Battle
     * everyone else gets {@code redacted}: the shared row must not leak to the owners behind.
     */
    public static void announce(Component message, Component redacted, ForceItemPlayer forceItemPlayer,
                                GameContext context) {
        List<ForceItemPlayer> squad = forceItemPlayer.squad();
        squad.forEach(member -> member.player().sendMessage(message));

        if (!context.eventDisabled()) {
            return;
        }

        Bukkit.getConsoleSender().sendMessage(message);
        Component others = context.mirrored() ? redacted : message;
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> squad.stream().noneMatch(member -> member.player() == player))
                .forEach(player -> player.sendMessage(others));
    }

    /** Played at each player's own position, so everyone hears it at full volume. */
    public static void playToAll(Sound sound, float volume, float pitch) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}
