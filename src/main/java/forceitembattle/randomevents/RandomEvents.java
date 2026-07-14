package forceitembattle.randomevents;

import forceitembattle.ForceItemBattle;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Every random event that can fire, and how to build one.
 */
@Getter
public enum RandomEvents {

    ITEM_HUNT("Item Hunt", "<gold>", ItemHunt::new);

    private final String displayName;
    private final String color;
    private final Function<ForceItemBattle, RandomEvent> factory;

    RandomEvents(String displayName, String color, Function<ForceItemBattle, RandomEvent> factory) {
        this.displayName = displayName;
        this.color = color;
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

    public static RandomEvents random() {
        RandomEvents[] values = values();
        return values[ThreadLocalRandom.current().nextInt(values.length)];
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
