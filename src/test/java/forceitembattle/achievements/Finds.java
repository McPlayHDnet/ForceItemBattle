package forceitembattle.achievements;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.BackToBack;
import forceitembattle.model.ForceItem;
import forceitembattle.model.ForceItemPlayer;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Fixtures for the achievement handler tests.
 *
 * <p>{@link ItemStack} appears here and that is deliberate: {@code new ItemStack(...)} is the
 * headless wall that {@code HeadlessBoundaryTest} pins, but <em>mocking</em> one is fine — only the
 * constructor reaches for the attribute registry. {@link FoundItemEvent} holds a stack rather than
 * a {@code Material}, so a test that wants to drive a find has to go through one.
 */
final class Finds {

    private Finds() {
    }

    /** @param seed a single hex digit, so UUID ordering stays predictable */
    static Player mockPlayer(String seed) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(
                UUID.fromString("00000000-0000-0000-0000-00000000000" + seed));
        when(player.getName()).thenReturn("player_" + seed);
        return player;
    }

    static ForceItemPlayer participant(String seed) {
        return new ForceItemPlayer(mockPlayer(seed), Material.DIRT, 0, 0);
    }

    /** A find of {@code material} that was collected rather than skipped. */
    static FoundItemEvent found(ForceItemPlayer finder, Material material) {
        return event(finder, material, false, false);
    }

    /** A find that was skipped past. */
    static FoundItemEvent skipped(ForceItemPlayer finder, Material material) {
        return event(finder, material, true, false);
    }

    /** A find the player already had in hand when it was assigned. */
    static FoundItemEvent backToBack(ForceItemPlayer finder, Material material) {
        return event(finder, material, false, true);
    }

    private static FoundItemEvent event(ForceItemPlayer finder, Material material,
                                        boolean skipped, boolean backToBack) {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(material);

        FoundItemEvent event = new FoundItemEvent(finder.player());
        event.setFoundItem(stack);
        event.setSkipped(skipped);
        event.setBackToBack(backToBack);
        return event;
    }

    /**
     * Records a find against whoever owns this player's score, the way the resolver does before the
     * achievement listener sees the event.
     */
    static void record(ForceItemPlayer player, Material material, boolean usedSkip) {
        player.recordFoundItem(new ForceItem(
                material, "1s", System.currentTimeMillis(), new BackToBack(false), usedSkip,
                player.player().getUniqueId()));
    }
}
