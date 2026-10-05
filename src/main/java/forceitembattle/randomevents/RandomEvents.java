package forceitembattle.randomevents;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public enum RandomEvents {

    ITEM_HUNT("Item Hunt", "<gold>", 10, false, 0, ItemHunt::new),
    SPECIAL_TRADER("Special Trader", "<light_purple>", 2, true, 0, SpecialTrader::new),
    POINT_HUNT("Point Hunt", "<aqua>", 6, true, PointHunt.MIN_START_SECONDS, PointHunt::new);

    private final String displayName;
    private final String color;

    /** Relative pick weight among the events still eligible this game. */
    private final int weight;

    private final boolean oncePerGame;

    /** Time a timed event needs left on the clock to be picked; 0 for instant and find-resolved events. */
    private final int minSecondsToRun;

    private final Function<EventContext, RandomEvent> factory;

    RandomEvents(String displayName, String color, int weight, boolean oncePerGame,
                 int minSecondsToRun, Function<EventContext, RandomEvent> factory) {
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

    public String id() {
        return this.name().toLowerCase();
    }

    public RandomEvent create(EventContext context) {
        return this.factory.apply(context);
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
