package forceitembattle.manager;

import forceitembattle.model.Roster;
import forceitembattle.manager.ItemDifficultiesManager.State;
import forceitembattle.model.ActiveTrader;
import forceitembattle.model.Dimension;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.util.LocationFormat;
import forceitembattle.util.Text;
import forceitembattle.util.TimeFormat;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Owns the player-list footer. Composes a compact permanent block (active item
 * pools with a countdown to the next unlock, and remaining jokers) plus, while one
 * is alive, the wandering trader's location and despawn timer — rendered in its
 * original standalone style.
 * <p>
 * This is the single writer of the footer; {@link WanderingTraderManager} only exposes
 * trader state and never touches it. Refreshed once per second from {@link TimerManager}'s
 * tick, after the pool-unlock poll, so the pool countdown flips to "active" on the exact
 * tick the unlock message is announced.
 * <p>
 * A running random event contributes its own block, rendered by the event rather than
 * here — this class owns only when and to whom the footer is pushed. The same tick
 * ordering applies: the event's clock is advanced before this runs, so a concluding
 * event's block is already gone on the tick its winner is announced.
 */
public class TabListManager implements Manager {
    private final Roster roster;
    private final Gamemanager gamemanager;
    private final ItemDifficultiesManager itemDifficultiesManager;
    private final RandomEventManager randomEventManager;
    private final WanderingTraderManager wanderingTraderManager;
    public TabListManager(Roster roster, Gamemanager gamemanager, ItemDifficultiesManager itemDifficultiesManager, RandomEventManager randomEventManager, WanderingTraderManager wanderingTraderManager) {
        this.roster = roster;
        this.gamemanager = gamemanager;
        this.itemDifficultiesManager = itemDifficultiesManager;
        this.randomEventManager = randomEventManager;
        this.wanderingTraderManager = wanderingTraderManager;
    }

    /**
     * Recomputes and pushes the footer to every online player. Pool and trader info
     * is global; the joker line is per-player (team-aware). Call once per second
     * while mid-game.
     */
    public void update() {
        String poolLine = buildPoolLine();
        String timeLine = buildTimeLine();
        String traderBlock = buildTraderBlock();
        String eventBlock = this.randomEventManager.tabFooterBlock();

        for (Player player : Bukkit.getOnlinePlayers()) {
            String footer = "\n" + poolLine + buildJokerLine(player) + timeLine
                    + traderBlock + eventBlock;
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
        ItemDifficultiesManager items = this.itemDifficultiesManager;
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
            line.append(" <gray>· ").append(nextName).append(" in ").append(TimeFormat.colored(secondsLeft));
        }

        return line.toString();
    }

    /**
     * The overworld's time of day, with a day/night marker.
     *
     * Always the overworld, whichever dimension the reader is standing in: the nether and end have
     * no day cycle, so a player checking whether it is safe to go back up wants the surface clock,
     * not their own. Colour follows day/night as well as the word does, but the word is what makes
     * it readable to someone who does not know Minecraft's clock off by heart.
     */
    private String buildTimeLine() {
        World world = Dimension.OVERWORLD.world();
        if (world == null) {
            return "";
        }

        boolean day = world.isDayTime();
        String color = day ? "<gold>" : "<aqua>";

        return "\n<gray>Time · " + clockIcon() + color + TimeFormat.worldClock(world.getTime())
                + " <dark_gray>(<gray>" + (day ? "Day" : "Night") + "<dark_gray>)";
    }

    /**
     * The clock glyph from the resourcepack font, or nothing when the icon map is missing —
     * {@code getUnicodeFromMaterial} falls back to the literal string "NULL", which would sit in
     * everyone's tab list forever rather than failing once and visibly.
     */
    private String clockIcon() {
        String icon = this.itemDifficultiesManager
                .getUnicodeFromMaterial(true, Material.CLOCK);
        return "NULL".equals(icon) ? "" : "<reset><shadow:black:0.4>" + icon + "</shadow> ";
    }

    private String buildJokerLine(Player player) {
        if (!this.roster.contains(player.getUniqueId())) {
            return "";
        }
        ForceItemPlayer forceItemPlayer = this.roster.get(player.getUniqueId());
        if (forceItemPlayer.isSpectator()) {
            return "";
        }
        return "\n<gray>Jokers · <aqua>" + forceItemPlayer.activeJokers();
    }

    private String buildTraderBlock() {
        StringBuilder block = new StringBuilder();

        for (ActiveTrader trader : this.wanderingTraderManager.activeTraders()) {
            block.append("\n\n").append(trader.getKind().boldColoredName()).append("\n")
                    .append(LocationFormat.xyz(trader.getLocation())).append("\n")
                    .append(TimeFormat.colored(trader.getTimer())).append("\n");
        }

        return block.toString();
    }

}
