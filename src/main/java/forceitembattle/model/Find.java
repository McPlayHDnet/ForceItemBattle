package forceitembattle.model;

import forceitembattle.event.FoundItemEvent;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * A Material rather than an ItemStack, which would need a running server.
 *
 * @param backToBackOdds computed when the item was handed out a tick earlier; null whenever {@code backToBack} is false
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
