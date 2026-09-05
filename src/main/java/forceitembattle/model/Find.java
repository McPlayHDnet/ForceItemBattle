package forceitembattle.model;

import forceitembattle.event.FoundItemEvent;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * One player obtaining their force item: what was obtained, by whom, and how.
 *
 * <p>The domain shape of a {@link FoundItemEvent}. The event stays on the Bukkit bus for the
 * achievement handlers and for {@code BackToBackManager} to re-fire, but nothing past
 * {@code FoundItemListener} needs to speak Bukkit to describe a find.
 *
 * <p>A {@link Material} and not an {@code ItemStack} on purpose: every reader only calls
 * {@code getType()}, and holding the stack puts the whole find pipeline behind
 * {@code ItemStack}'s static initialiser, which needs a running server.
 */
public record Find(ForceItemPlayer finder,
                   @Nullable Material material,
                   boolean skipped,
                   boolean backToBack) {

    /** The material is null when the event carries no stack, which the event permits. */
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
