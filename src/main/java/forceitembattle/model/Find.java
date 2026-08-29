package forceitembattle.model;

import forceitembattle.event.FoundItemEvent;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * One player obtaining their force item: what was obtained, by whom, and how.
 *
 * <p>The domain shape of a {@link FoundItemEvent}, carrying the four things anything downstream
 * actually reads. The event itself stays on the Bukkit bus — {@code AchievementListener} and the
 * achievement handlers subscribe to it, and {@code BackToBackManager} re-fires it to drive a chain
 * — but nothing past {@code FoundItemListener} needs to speak Bukkit to describe a find.
 *
 * <p><b>Why this is a {@link Material} and not an {@code ItemStack}.</b> Every reader of the found
 * item only ever called {@code getType()} on it. Holding the stack instead put the whole find
 * pipeline behind {@code ItemStack}'s static initialiser, which needs a running server — see
 * {@code HeadlessBoundaryTest}. Unwrapping it at the listener is what lets everything after it be
 * tested without one.
 */
public record Find(ForceItemPlayer finder,
                   @Nullable Material material,
                   boolean skipped,
                   boolean backToBack) {

    /**
     * Reads a find off the event. The material is null when the event carries no stack, which is a
     * state the event permits and callers have always had to check for.
     */
    public static Find of(FoundItemEvent event, ForceItemPlayer finder) {
        return new Find(
                finder,
                event.getFoundItem() == null ? null : event.getFoundItem().getType(),
                event.isSkipped(),
                event.isBackToBack());
    }

    public Player player() {
        return finder.player();
    }
}
