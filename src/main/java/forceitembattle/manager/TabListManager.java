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
 * The single writer of the player-list footer; {@link WanderingTraderManager} and the random events
 * only expose state and never touch it.
 *
 * <p>Refreshed once per second from {@link TimerManager}'s tick, <em>after</em> the pool-unlock poll
 * and after the event clock advances. That ordering is what makes the pool countdown flip to
 * "active" on the exact tick the unlock is announced, and a concluding event's block disappear on
 * the tick its winner is.
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

    /** Pool and trader info is global; the joker line is per-player. Call once per second mid-game. */
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

    /** Used pre-game, while paused, and once over. */
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
     * Always the overworld, whichever dimension the reader is standing in: the nether and end have no
     * day cycle, so a player checking whether it is safe to go back up wants the surface clock.
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
     * Nothing when the icon map is missing: {@code getUnicodeFromMaterial} falls back to the literal
     * string "NULL", which would sit in everyone's tab list forever.
     */
    private String clockIcon() {
        String icon = this.itemDifficultiesManager
                .getUnicodeFromMaterial(true, Material.CLOCK);
        return "NULL".equals(icon) ? "" : "<reset><shadow:black:0.4>" + icon + "</shadow> ";
    }

    private String buildJokerLine(Player player) {
        ForceItemPlayer forceItemPlayer = this.roster.participant(player.getUniqueId()).orElse(null);
        if (forceItemPlayer == null) {
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
