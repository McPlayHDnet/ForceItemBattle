package forceitembattle.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.model.BackToBack;
import forceitembattle.model.ForceItem;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.ScoreOwner;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/** {@link ResultPages}: the archive the {@code [Inventory]} link reopens, in the inventory's own layout. */
class ResultPagesTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private ScoreOwner ownerWith(int items) {
        ForceItemPlayer participant = new ForceItemPlayer(this.server.addPlayer(), Material.STONE, 0, 0);
        for (int index = 0; index < items; index++) {
            participant.scoreOwner().record(
                    new ForceItem(Material.STONE, "00:0" + index % 10, 0L, new BackToBack(false), false, null));
        }
        return participant.scoreOwner();
    }

    @Test
    void nothingFoundIsNoPagesAtAll() {
        assertTrue(ResultPages.build(ownerWith(0)).isEmpty());
    }

    /** Seven per row, one slot in from each edge, five rows. */
    @Test
    void aFullPageFillsTheInnerGrid() {
        Map<Integer, Map<Integer, ItemStack>> pages = ResultPages.build(ownerWith(35));

        assertEquals(1, pages.size());
        List<Integer> expected = IntStream.range(0, 5)
                .flatMap(row -> IntStream.rangeClosed(10 + row * 9, 16 + row * 9))
                .boxed()
                .toList();
        assertEquals(expected, pages.get(0).keySet().stream().sorted().toList());
    }

    @Test
    void theThirtySixthItemStartsAPage() {
        Map<Integer, Map<Integer, ItemStack>> pages = ResultPages.build(ownerWith(36));

        assertEquals(2, pages.size());
        assertEquals(Map.of(10, pages.get(1).get(10)), pages.get(1));
    }

    @Test
    void anItemIsNamedWithItsTime() {
        ItemStack first = ResultPages.build(ownerWith(1)).get(0).get(10);

        String name = PlainTextComponentSerializer.plainText().serialize(first.getItemMeta().displayName());
        assertTrue(name.endsWith("» 00:00"), name);
    }
}
