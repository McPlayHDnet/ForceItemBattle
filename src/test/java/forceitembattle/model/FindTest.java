package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.event.FoundItemEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

/**
 * That a back-to-back's odds survive the trip from the event to the find.
 *
 * <p>This is the seam the "null rarity" bug lived on. A chain is detected when the next item is
 * <em>handed out</em>, so {@code BackToBackManager} computes the odds a tick before the event that
 * records that item exists. The resolver used to take the odds returned by the <em>current</em>
 * find's {@code handleAfterFind} instead, which is the number for the item after this one — so every
 * item wore its successor's rarity, and the last link of a chain, having no successor, wore none.
 * A single back-to-back is a chain of one, which is why it vanished from the website entirely.
 */
class FindTest {

    @Test
    void carriesTheOddsTheEventWasGiven() {
        BackToBackProbability odds = BackToBackProbability.of(120, 1_367, 1, false);

        Find find = Find.of(event(Material.DIRT, true, odds), mock(ForceItemPlayer.class));

        assertSame(odds, find.backToBackOdds(),
                "the odds computed for this item must reach the find that records it");
    }

    @Test
    void anOrdinaryFindCarriesNoOdds() {
        Find find = Find.of(event(Material.DIRT, false, null), mock(ForceItemPlayer.class));

        assertNull(find.backToBackOdds());
    }

    private static FoundItemEvent event(Material material, boolean backToBack,
                                        BackToBackProbability odds) {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(material);

        FoundItemEvent event = new FoundItemEvent(mock(Player.class));
        event.setFoundItem(stack);
        event.setBackToBack(backToBack);
        event.setBackToBackProbability(odds);
        return event;
    }
}
