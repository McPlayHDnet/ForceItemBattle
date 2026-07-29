package forceitembattle.randomevents;

import forceitembattle.ForceItemBattle;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Every random event that can fire, how likely it is, and how to build one.
 */
@Getter
public enum RandomEvents {

    ITEM_HUNT("Item Hunt", "<gold>", 10, false, 0, ItemHunt::new),
    SPECIAL_TRADER("Special Trader", "<light_purple>", 2, true, 0, SpecialTrader::new),
    POINT_HUNT("Point Hunt", "<aqua>", 6, true, PointHunt.MIN_START_SECONDS, PointHunt::new);

    private final String displayName;
    private final String color;

    /**
     * Relative pick weight among the events still eligible this game. Only meaningful
     * against the other weights — a 10 next to a 2 is picked five times as often.
     */
    private final int weight;

    /**
     * When true, this event can fire at most once per round.
     */
    private final boolean oncePerGame;

    /**
     * Clock an event needs left on the timer to be worth starting. A timed event that would be
     * truncated by game end is filtered out of the pick until enough time remains; instant and
     * find-resolved events leave this at 0.
     */
    private final int minSecondsToRun;

    private final Function<ForceItemBattle, RandomEvent> factory;

    RandomEvents(String displayName, String color, int weight, boolean oncePerGame,
                 int minSecondsToRun, Function<ForceItemBattle, RandomEvent> factory) {
        this.displayName = displayName;
        this.color = color;
        this.weight = weight;
        this.oncePerGame = oncePerGame;
        this.minSecondsToRun = minSecondsToRun;
        this.factory = factory;
    }

    public String coloredName() {
        return this.color + this.displayName;
    }

    /**
     * The /randomevent argument for this event, e.g. {@code item_hunt}.
     */
    public String id() {
        return this.name().toLowerCase();
    }

    public RandomEvent create(ForceItemBattle plugin) {
        return this.factory.apply(plugin);
    }

    @Nullable
    public static RandomEvents byId(String id) {
        return Arrays.stream(values())
                .filter(randomEvent -> randomEvent.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    public static List<String> ids() {
        return Arrays.stream(values()).map(RandomEvents::id).toList();
    }
}
