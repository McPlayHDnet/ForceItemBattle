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
 *
 * @param backToBackOdds how unlikely this back-to-back was, computed when the item was handed out a
 *                       tick earlier and carried here on the event. Null whenever
 *                       {@code backToBack} is false — and that pairing is the whole reason the odds
 *                       travel rather than being asked for again: see {@link FoundItemEvent}.
 */
public record Find(ForceItemPlayer finder,
                   @Nullable Material material,
                   boolean skipped,
                   boolean backToBack,
                   @Nullable BackToBackProbability backToBackOdds) {

    public Find(ForceItemPlayer finder, @Nullable Material material, boolean skipped,
                boolean backToBack) {
        this(finder, material, skipped, backToBack, null);
    }

    /** The material is null when the event carries no stack, which the event permits. */
    public static Find of(FoundItemEvent event, ForceItemPlayer finder) {
        return new Find(
                finder,
                event.getFoundItem() == null ? null : event.getFoundItem().getType(),
                event.isSkipped(),
                event.isBackToBack(),
                event.getBackToBackProbability());
    }

    public Player player() {
        return finder.player();
    }
}
