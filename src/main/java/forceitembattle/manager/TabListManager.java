package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.manager.ItemDifficultiesManager.State;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.util.Text;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Owns the player-list footer. Composes a compact permanent block (active item
 * pools with a countdown to the next unlock, and remaining jokers) plus, while one
 * is alive, the wandering trader's location and despawn timer — rendered in its
 * original standalone style.
 * <p>
 * This is the single writer of the footer: {@link WanderingTraderManager} only
 * exposes trader state now, it no longer touches the footer. Refreshed once per
 * second from {@link TimerManager}'s tick, after the pool-unlock poll, so the pool
 * countdown flips to "active" on the exact tick the unlock message is announced.
 */
public class TabListManager implements Manager {

    private final ForceItemBattle plugin;

    public TabListManager(ForceItemBattle plugin) {
        this.plugin = plugin;
    }

    /**
     * Recomputes and pushes the footer to every online player. Pool and trader info
     * is global; the joker line is per-player (team-aware). Call once per second
     * while mid-game.
     */
    public void update() {
        String poolLine = buildPoolLine();
        String traderBlock = buildTraderBlock();

        for (Player player : Bukkit.getOnlinePlayers()) {
            String footer = "\n" + poolLine + buildJokerLine(player) + traderBlock;
            player.sendPlayerListFooter(Text.of(footer));
        }
    }

    /** Clears the footer for everyone. Used pre-game, while paused, and once over. */
    public void clearFooter() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendPlayerListFooter(Component.empty());
        }
    }

    @Override
    public void disable() {
        clearFooter();
    }

    private String buildPoolLine() {
        ItemDifficultiesManager items = this.plugin.getItemDifficultiesManager();
        List<State> active = items.getActiveStates();

        StringBuilder line = new StringBuilder("<gray>Pools ");
        if (active.isEmpty()) {
            line.append("—");
        } else {
            for (int i = 0; i < active.size(); i++) {
                State state = active.get(i);
                if (i > 0) {
                    line.append("<gray>, ");
                }
                line.append("<").append(state.getColor()).append(">").append(state.getDisplayName());
            }
        }

        int secondsLeft = items.secondsUntilNextPool();
        if (secondsLeft >= 0) {
            State next = items.getNextState();
            String nextName = next != null ? next.getDisplayName() : "next";
            line.append(" <gray>· ").append(nextName).append(" in ").append(formatColoredTime(secondsLeft));
        }

        return line.toString();
    }

    private String buildJokerLine(Player player) {
        if (!this.plugin.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
            return "";
        }
        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        if (forceItemPlayer.isSpectator()) {
            return "";
        }
        return "\n<gray>Jokers · <aqua>" + forceItemPlayer.getRemainingJokers();
    }

    private String buildTraderBlock() {
        WanderingTraderManager trader = this.plugin.getWanderingTraderManager();
        if (!trader.isTraderActive() || trader.getTraderLocation() == null) {
            return "";
        }
        return "\n\n<green><b>Wandering Trader</b>\n"
                + locationToString(trader.getTraderLocation()) + "\n"
                + formatColoredTime(trader.getTraderTimer()) + "\n";
    }

    private String locationToString(Location location) {
        if (location.getWorld() == null) {
            return "<red>unknown location";
        }
        return "<dark_aqua>" + location.getBlockX() + "<gray>, <dark_aqua>" + location.getBlockY() + "<gray>, <dark_aqua>" + location.getBlockZ();
    }

    private String formatColoredTime(int remainingSeconds) {
        remainingSeconds = Math.max(remainingSeconds, 0);

        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;

        String timeString = String.format("%02d:%02d", minutes, seconds);

        String colorTag;
        if (remainingSeconds <= 10) {
            colorTag = "<dark_red>";
        } else if (remainingSeconds <= 30) {
            colorTag = "<red>";
        } else if (remainingSeconds <= 120) {
            colorTag = "<gold>";
        } else {
            colorTag = "<green>";
        }

        return colorTag + timeString;
    }
}
