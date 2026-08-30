package forceitembattle.randomevents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.Text;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

/**
 * The Point Hunt tab-footer block: how long the hunt has left.
 *
 * The hunt used to run blind — no way to tell how much of the ten minutes remained until the
 * winner was announced.
 */
class PointHuntTest {

    private static String rendered(PointHunt hunt) {
        return PlainTextComponentSerializer.plainText().serialize(Text.of(hunt.tabFooterBlock()));
    }

    private static PointHunt hunt() {
        return new PointHunt(mock(ForceItemBattle.class));
    }

    @Test
    void theBlockNamesTheEventAndShowsTheFullDurationAtTheStart() {
        assertEquals("\n\nPoint Hunt · 10:00", rendered(hunt()));
    }

    @Test
    void theCountdownTicksDown() {
        PointHunt hunt = hunt();

        for (int i = 0; i < 61; i++) {
            hunt.tick();
        }

        assertTrue(rendered(hunt).endsWith("08:59"), rendered(hunt));
    }

    /** The block is viewer-independent, so the footer builds it once per refresh, not per player. */
    @Test
    void theBlockIsTheSameForEveryone() {
        PointHunt hunt = hunt();

        assertEquals(hunt.tabFooterBlock(), hunt.tabFooterBlock());
    }

    /** Events that have nothing ongoing to report contribute nothing. */
    @Test
    void otherEventsContributeNothingByDefault() {
        assertEquals("", new ItemHunt(mock(ForceItemBattle.class)).tabFooterBlock());
        assertEquals("", new SpecialTrader(mock(ForceItemBattle.class)).tabFooterBlock());
    }
}
